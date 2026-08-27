from flask import Flask, g, request, jsonify
from flask_cors import CORS
import base64
import os
import threading
import time
import traceback

import cv2
import mediapipe as mp
import numpy as np

from agent.retriever import QdrantPrepareIndex
from agent.observability import resolve_correlation_id
from agent.tools import DEFAULT_TOOL_REGISTRY
from agent.workflows import run_prepare_agent
from agent.workflow_state import WORKFLOW_RUN_STORE, WorkflowStateError
from agent.micro_video_renderer import render_micro_video

AI_SERVER_ENV = os.getenv('AI_SERVER_ENV', 'dev').strip().lower()
default_cors_origins = 'http://localhost:5173' if AI_SERVER_ENV in {'dev', 'test'} else ''
allowed_cors_origins = [
    origin.strip()
    for origin in os.getenv('AI_SERVER_CORS_ALLOWED_ORIGINS', default_cors_origins).split(',')
    if origin.strip()
]

app = Flask(__name__)
app.config['ENVIRONMENT'] = AI_SERVER_ENV
CORS(app, resources={r"/*": {"origins": allowed_cors_origins}})

# 初始化 MediaPipe Face Mesh
mp_face_mesh = mp.solutions.face_mesh
face_mesh = mp_face_mesh.FaceMesh(
    max_num_faces=1,
    refine_landmarks=True,
    min_detection_confidence=0.5,
    min_tracking_confidence=0.5,
)

# FaceMesh 在并发场景下加一层锁更稳妥
face_mesh_lock = threading.Lock()
state_lock = threading.Lock()

# 关键点索引
LEFT_EYE = [362, 385, 387, 263, 373, 380]
RIGHT_EYE = [33, 160, 158, 133, 153, 144]

EAR_THRESHOLD = 0.20
MAR_THRESHOLD = 0.50
FATIGUE_SECONDS = 1.5

# 按 session 记录闭眼开始时间，避免多个学生串状态
eye_closed_state = {}

# 按 session 记录疲劳统计数据
fatigue_stats = {}
prev_status = {}


@app.before_request
def bind_request_trace():
    g.trace_id = resolve_correlation_id(request.headers.get('X-Trace-Id'))


@app.after_request
def expose_request_trace(response):
    response.headers['X-Trace-Id'] = getattr(g, 'trace_id', '')
    return response


def calculate_ear(landmarks, eye_indices):
    """计算 EAR（眼睛纵横比）"""
    a = np.linalg.norm(landmarks[eye_indices[1]] - landmarks[eye_indices[5]])
    b = np.linalg.norm(landmarks[eye_indices[2]] - landmarks[eye_indices[4]])
    c = np.linalg.norm(landmarks[eye_indices[0]] - landmarks[eye_indices[3]])
    if c == 0:
        return 0.0
    return float((a + b) / (2.0 * c))


def calculate_mar(landmarks):
    """计算 MAR（嘴部纵横比）"""
    vertical = np.linalg.norm(landmarks[13] - landmarks[14])
    horizontal = np.linalg.norm(landmarks[61] - landmarks[291])
    if horizontal == 0:
        return 0.0
    return float(vertical / horizontal)


def decode_base64_image(image_data):
    if not image_data or ',' not in image_data:
        raise ValueError('图片数据格式错误')

    img_str = image_data.split(',', 1)[1]
    img_bytes = base64.b64decode(img_str)
    nparr = np.frombuffer(img_bytes, np.uint8)
    frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    if frame is None:
        raise ValueError('图片解码失败')

    return frame


def get_session_id(payload):
    session_id = payload.get('sessionId') if isinstance(payload, dict) else None
    if session_id:
        return str(session_id)

    forwarded_for = request.headers.get('X-Forwarded-For', '')
    if forwarded_for:
        return forwarded_for.split(',')[0].strip()

    return request.remote_addr or 'anonymous'


@app.route('/health', methods=['GET'])
def health():
    return jsonify({
        'code': 0,
        'status': 'ok',
        'msg': 'face detect service running'
    })


@app.route('/agent/prepare/stream', methods=['POST'])
def prepare_agent_stream():
    payload = request.get_json(silent=True) or {}
    payload['traceId'] = g.trace_id
    api_key = request.headers.get('X-DeepSeek-Key') or ''

    def generate():
        for event in run_prepare_agent(payload, api_key):
            yield event

    return app.response_class(
        generate(),
        mimetype='application/x-ndjson; charset=utf-8',
        headers={
            'Cache-Control': 'no-cache, no-store, must-revalidate',
            'X-Accel-Buffering': 'no',
        },
    )


@app.route('/agent/tools', methods=['GET'])
def agent_tools():
    return jsonify({
        'code': 0,
        'data': DEFAULT_TOOL_REGISTRY.descriptors(),
    })


@app.route('/agent/runs/<request_id>/saved', methods=['POST'])
def mark_agent_run_saved(request_id):
    payload = request.get_json(silent=True) or {}
    teacher_id = payload.get('teacherId')
    resource_id = payload.get('resourceId')
    confirmed = payload.get('confirmed')
    if (
        isinstance(teacher_id, bool)
        or not isinstance(teacher_id, int)
        or teacher_id <= 0
        or isinstance(resource_id, bool)
        or not isinstance(resource_id, int)
        or resource_id <= 0
        or confirmed is not True
    ):
        return jsonify({
            'code': 400,
            'msg': 'teacherId, resourceId and explicit confirmation are required',
        }), 400
    try:
        workflow_run = WORKFLOW_RUN_STORE.mark_saved(
            request_id,
            actor_id=teacher_id,
            resource_id=resource_id,
            confirmed=confirmed,
        )
    except WorkflowStateError:
        return jsonify({
            'code': 409,
            'msg': 'workflow cannot be marked as saved',
        }), 409
    return jsonify({
        'code': 0,
        'data': workflow_run.snapshot(),
    })


@app.route('/agent/runs/<request_id>', methods=['GET'])
def get_agent_run(request_id):
    teacher_id = request.args.get('teacherId', type=int)
    if (
        len(request_id) != 32
        or any(char not in '0123456789abcdef' for char in request_id)
        or teacher_id is None
        or teacher_id <= 0
    ):
        return jsonify({'code': 400, 'msg': 'invalid workflow query'}), 400
    workflow_run = WORKFLOW_RUN_STORE.get(request_id)
    if workflow_run is None or workflow_run.actor_id != teacher_id:
        return jsonify({'code': 404, 'msg': 'workflow run was not found'}), 404
    return jsonify({
        'code': 0,
        'data': workflow_run.snapshot(),
    })


@app.route('/agent/index/upsert', methods=['POST'])
def agent_index_upsert():
    payload = request.get_json(silent=True) or {}
    documents = payload.get('documents')
    if documents is None:
        documents = [payload]
    if not isinstance(documents, list):
        return jsonify({'code': 400, 'msg': 'documents must be a list'}), 400

    result = QdrantPrepareIndex().upsert_documents(documents)
    status = 400 if result.get('error') or (
        documents and result.get('indexed', 0) == 0 and result.get('rejected', 0) > 0
    ) else 200
    return jsonify({
        'code': 0 if status == 200 else 400,
        'data': result,
    }), status


@app.route('/agent/index/delete', methods=['POST'])
def agent_index_delete():
    payload = request.get_json(silent=True) or {}
    result = QdrantPrepareIndex().delete_documents(
        teacher_id=payload.get('teacherId'),
        source_type=payload.get('sourceType'),
        source_id=payload.get('sourceId'),
        scope=payload.get('scope'),
    )
    status = 400 if result.get('error') else 200
    return jsonify({'code': 0 if status == 200 else 400, 'data': result}), status


@app.route('/agent/retrieve', methods=['POST'])
def agent_retrieve():
    payload = request.get_json(silent=True) or {}
    query = payload.get('query') or ''
    if not query.strip():
        return jsonify({'code': 400, 'msg': 'query is required'}), 400
    items = QdrantPrepareIndex().retrieve(
        query,
        teacher_id=payload.get('teacherId'),
        options=payload.get('retrievalOptions') or {},
    )
    return jsonify({'code': 0, 'data': {'items': items or [], 'fallback': items is None}})


@app.route('/micro-video/render', methods=['POST'])
def micro_video_render():
    payload = request.get_json(silent=True) or {}
    try:
        data = render_micro_video(payload, request.host_url)
        return jsonify({'code': 0, 'data': data})
    except Exception as exc:
        traceback.print_exc()
        return jsonify({
            'code': 500,
            'data': {
                'status': 'failed',
                'errorMessage': str(exc),
            }
        }), 500


@app.route('/detect', methods=['POST'])
def detect():
    data = request.get_json(silent=True) or {}
    image_data = data.get('image')

    if not image_data:
        return jsonify({'code': 400, 'status': 'error', 'msg': 'No image provided'}), 400

    session_id = get_session_id(data)

    try:
        frame = decode_base64_image(image_data)
    except Exception as exc:
        print('图片解析失败:', exc)
        return jsonify({'code': 400, 'status': 'error', 'msg': f'Image parse error: {exc}'}), 400

    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    with face_mesh_lock:
        results = face_mesh.process(rgb_frame)

    status = 'normal'
    msg = '状态良好'
    ear_value = 0.0
    mar_value = 0.0

    if results.multi_face_landmarks:
        face_landmarks = results.multi_face_landmarks[0]
        h, w, _ = frame.shape
        landmarks = np.array([(p.x * w, p.y * h) for p in face_landmarks.landmark], dtype=np.float32)

        left_ear = calculate_ear(landmarks, LEFT_EYE)
        right_ear = calculate_ear(landmarks, RIGHT_EYE)
        ear_value = (left_ear + right_ear) / 2.0
        mar_value = calculate_mar(landmarks)

        print(f'[{session_id}] EAR: {ear_value:.3f} | MAR: {mar_value:.3f}')

        # 1) 优先判断打哈欠
        if mar_value > MAR_THRESHOLD:
            status = 'yawn'
            msg = '检测到打哈欠（疲劳）'
            with state_lock:
                eye_closed_state[session_id] = None

        # 2) 再判断闭眼疲劳
        elif ear_value < EAR_THRESHOLD:
            now = time.time()
            with state_lock:
                start_time = eye_closed_state.get(session_id)
                if start_time is None:
                    eye_closed_state[session_id] = now
                    duration = 0.0
                else:
                    duration = now - start_time

            if duration > FATIGUE_SECONDS:
                status = 'fatigue'
                msg = f'已闭眼 {duration:.1f} 秒（疲劳）'
            else:
                status = 'normal'
                msg = '检测到闭眼，持续观察中'

        # 3) 眼睛正常睁开
        else:
            with state_lock:
                eye_closed_state[session_id] = None
            status = 'normal'
            msg = '精神状态正常'

    else:
        print(f'[{session_id}] 未检测到人脸')
        with state_lock:
            eye_closed_state[session_id] = None
        status = 'no_face'
        msg = '未检测到人脸'

    # ========== 累计统计 ==========
    with state_lock:
        if session_id not in fatigue_stats:
            fatigue_stats[session_id] = {
                'yawnCount': 0,
                'fatigueCount': 0,
                'noFaceCount': 0,
                'normalCount': 0,
                'totalDetections': 0,
                'sessionStart': time.time(),
                'events': [],
            }
        stats = fatigue_stats[session_id]
        stats['totalDetections'] += 1

        if status == 'normal':
            stats['normalCount'] += 1

        old = prev_status.get(session_id, 'normal')
        if status != old:
            if status == 'yawn':
                stats['yawnCount'] += 1
            elif status == 'fatigue':
                stats['fatigueCount'] += 1
            elif status == 'no_face':
                stats['noFaceCount'] += 1
            # 记录事件时间线（限制最多500条）
            if len(stats['events']) < 500:
                stats['events'].append({
                    't': time.time(),
                    'type': status,
                    'ear': round(float(ear_value), 4),
                    'mar': round(float(mar_value), 4),
                })
        prev_status[session_id] = status

    return jsonify({
        'code': 0,
        'status': status,
        'msg': msg,
        'ear': round(float(ear_value), 4),
        'mar': round(float(mar_value), 4),
        'stats': {
            'yawnCount': stats['yawnCount'],
            'fatigueCount': stats['fatigueCount'],
            'noFaceCount': stats['noFaceCount'],
            'normalCount': stats['normalCount'],
            'totalDetections': stats['totalDetections'],
        }
    })


@app.route('/stats', methods=['GET'])
def get_stats():
    """查询某个 session 的累计疲劳统计"""
    session_id = request.args.get('sessionId', 'anonymous')
    with state_lock:
        stats = fatigue_stats.get(session_id)

    if not stats:
        return jsonify({'code': 0, 'msg': '暂无数据', 'stats': None})

    total = stats['totalDetections']
    focus_rate = round((stats['normalCount'] / total * 100), 1) if total > 0 else 0
    duration = time.time() - stats['sessionStart']

    return jsonify({
        'code': 0,
        'stats': {
            'yawnCount': stats['yawnCount'],
            'fatigueCount': stats['fatigueCount'],
            'noFaceCount': stats['noFaceCount'],
            'normalCount': stats['normalCount'],
            'totalDetections': total,
            'focusRate': focus_rate,
            'durationSeconds': round(duration, 1),
            'sessionStart': stats['sessionStart'],
            'eventCount': len(stats['events']),
        }
    })


if __name__ == '__main__':
    host = os.getenv('AI_SERVER_HOST', '0.0.0.0')
    port = int(os.getenv('AI_SERVER_PORT', '5000'))
    debug_enabled = (
        AI_SERVER_ENV == 'dev'
        and os.getenv('AI_SERVER_DEBUG', 'false').strip().lower() == 'true'
    )
    print(f'AI Server started in {AI_SERVER_ENV} mode on {host}:{port}...')
    app.run(host=host, port=port, debug=debug_enabled)
