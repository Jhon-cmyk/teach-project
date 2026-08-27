package com.ruyi.teach.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AliyunAsrService {

    @Value("${aliyun.asr.app-key}")
    private String appKey;

    @Value("${aliyun.asr.access-key-id}")
    private String accessKeyId;

    @Value("${aliyun.asr.access-key-secret}")
    private String accessKeySecret;

    // 阿里云录音文件识别的固定域名
    private static final String DOMAIN = "filetrans.cn-shanghai.aliyuncs.com";
    private static final String API_VERSION = "2018-08-17";

    /**
     * 核心方法：提交音频并一直等待，直到返回我们前端需要的 JSON 格式
     */
    public String transcribeAudio(String audioUrl) {
        try {
            // 1. 初始化客户端
            DefaultProfile profile = DefaultProfile.getProfile("cn-shanghai", accessKeyId, accessKeySecret);
            IAcsClient client = new DefaultAcsClient(profile);

            // 2. 提交转写任务
            String taskId = submitTask(client, audioUrl);
            if (taskId == null) {
                return "[]"; // 提交失败返回空数组
            }

            // 3. 轮询等待结果 (每3秒查一次)
            long deadlineAt = System.currentTimeMillis() + 30L * 60 * 1000;
            JSONObject result = null;
            while (true) {
                Thread.sleep(3000);
                if (System.currentTimeMillis() > deadlineAt) {
                    log.error("阿里云转写超时，taskId: {}", taskId);
                    return "[]";
                }
                result = getTaskResult(client, taskId);
                String status = result.getString("StatusCode");
                
                if ("21050000".equals(status)) {
                    log.info("阿里云转写成功！");
                    break; // 成功
                } else if ("21050001".equals(status)) {
                    log.info("阿里云转写中，请稍候..."); // 继续循环
                } else {
                    log.error("阿里云转写失败，状态码: " + status);
                    return "[]";
                }
            }

            // 4. 清洗数据，转换成我们前端 Vue 需要的格式
            return formatToFrontendJson(result);

        } catch (Exception e) {
            log.error("语音转写异常", e);
            return "[]";
        }
    }

    public String transcribeAudioToText(String audioUrl) {
        String transcriptJson = transcribeAudio(audioUrl);
        if (transcriptJson == null || transcriptJson.trim().isEmpty() || "[]".equals(transcriptJson.trim())) {
            return "";
        }

        try {
            JSONArray lines = JSON.parseArray(transcriptJson);
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < lines.size(); i++) {
                JSONObject line = lines.getJSONObject(i);
                String content = line.getString("content");
                if (content != null && !content.trim().isEmpty()) {
                    if (!text.isEmpty()) {
                        text.append(" ");
                    }
                    text.append(content.trim());
                }
            }
            return text.toString();
        } catch (Exception e) {
            log.warn("语音转写文本提取失败，返回空文本", e);
            return "";
        }
    }

    private String submitTask(IAcsClient client, String audioUrl) throws Exception {
        CommonRequest request = new CommonRequest();
        request.setDomain(DOMAIN);
        request.setVersion(API_VERSION);
        request.setAction("SubmitTask");
        request.setMethod(MethodType.POST);

        // 构造任务参数 (开启说话人分离！)
        JSONObject task = new JSONObject();
        task.put("appkey", appKey);
        task.put("file_link", audioUrl);
        task.put("version", "4.0");
        task.put("enable_words", false);
        // 🌟 核心：开启说话人分离，假设最多2人（师、生）
        task.put("diarization_enabled", true);
        task.put("enable_sample_rate_adaptive", true);

        request.putBodyParameter("Task", task.toJSONString());

        CommonResponse response = client.getCommonResponse(request);
        JSONObject resJson = JSON.parseObject(response.getData());
        
        if (resJson.containsKey("TaskId")) {
            return resJson.getString("TaskId");
        }
        return null;
    }

    private JSONObject getTaskResult(IAcsClient client, String taskId) throws Exception {
        CommonRequest request = new CommonRequest();
        request.setDomain(DOMAIN);
        request.setVersion(API_VERSION);
        request.setAction("GetTaskResult");
        request.setMethod(MethodType.GET);
        request.putQueryParameter("TaskId", taskId);

        CommonResponse response = client.getCommonResponse(request);
        return JSON.parseObject(response.getData());
    }

    /**
     * 将阿里云的数据转换为前端气泡需要的 JSON
     */
    private String formatToFrontendJson(JSONObject aliyunResult) {
        List<Map<String, String>> frontendList = new ArrayList<>();
        
        JSONObject resultObj = aliyunResult.getJSONObject("Result");
        JSONArray sentences = resultObj.getJSONArray("Sentences");

        if (sentences == null) return "[]";

        for (int i = 0; i < sentences.size(); i++) {
            JSONObject sentence = sentences.getJSONObject(i);
            
            // 提取毫秒时间戳并转换为 MM:SS
            long beginTime = sentence.getLongValue("BeginTime");
            String timeStr = formatTime(beginTime);
            
            // 提取说话人 ID。一般来说发言最多/最早的是老师(0)，其他人是学生(1)
            int speakerId = sentence.getIntValue("SpeakerId");
            String role = (speakerId == 0) ? "teacher" : "student";
            
            // 提取文本
            String text = sentence.getString("Text");

            Map<String, String> item = new HashMap<>();
            item.put("time", timeStr);
            item.put("role", role);
            item.put("content", text);
            frontendList.add(item);
        }

        return JSON.toJSONString(frontendList);
    }

    // 辅助方法：把毫秒变成 15:24 这种格式
    private String formatTime(long ms) {
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
