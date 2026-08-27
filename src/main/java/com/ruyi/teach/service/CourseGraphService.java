package com.ruyi.teach.service;

import com.ruyi.teach.model.dto.coursegraph.CourseGraphLinkCreateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphNodeCreateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphNodeUpdateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphPreferenceUpdateRequest;
import com.ruyi.teach.model.dto.coursegraph.CourseGraphResourceBindRequest;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphAnalysisFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphLinkVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphClassFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphCommunityDeskFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphCommunityFocusVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphDataVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphNodeVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphPreferenceVO;
import com.ruyi.teach.model.vo.coursegraph.CourseGraphResourceFocusVO;

public interface CourseGraphService {

    CourseGraphDataVO getGraphData(Long teacherId);

    CourseGraphNodeVO getNodeDetail(Long teacherId, String nodeId);

    CourseGraphNodeVO updateNode(Long teacherId, CourseGraphNodeUpdateRequest updateRequest);

    CourseGraphPreferenceVO getPreferences(Long teacherId);

    CourseGraphPreferenceVO updatePreferences(Long teacherId, CourseGraphPreferenceUpdateRequest updateRequest);

    CourseGraphResourceFocusVO getResourceFocus(Long teacherId, String nodeId, String resourceType);

    boolean bindResource(Long teacherId, CourseGraphResourceBindRequest bindRequest);

    CourseGraphAnalysisFocusVO getAnalysisFocus(Long teacherId, String nodeId);

    CourseGraphClassFocusVO getClassFocus(Long teacherId, String nodeId);

    CourseGraphCommunityFocusVO getCommunityFocus(Long teacherId, String nodeId);

    CourseGraphCommunityDeskFocusVO getCommunityDeskFocus(Long teacherId, String nodeId);

    CourseGraphNodeVO createNode(Long teacherId, CourseGraphNodeCreateRequest createRequest);

    boolean deleteNodeCascade(Long teacherId, String nodeId);

    CourseGraphLinkVO createLink(Long teacherId, CourseGraphLinkCreateRequest createRequest);

    boolean deleteLink(Long teacherId, Long linkId);

    CourseGraphDataVO seedDefaultGraph(Long teacherId);

    // ═══════════════════════════════════════════
    //  知识点-学习活动绑定（Phase 2.5 真实数据）
    // ═══════════════════════════════════════════
    boolean bindNodeActivity(Long teacherId, String nodeId, String activityType, Long activityId);

    boolean unbindNodeActivity(Long teacherId, Long activityId);

    java.util.List<com.ruyi.teach.model.entity.CourseGraphNodeActivity> listNodeActivities(String nodeId);

    java.util.Map<String, Object> computeNodeProgress(String nodeId, java.util.List<Long> studentIds);

    java.util.List<com.ruyi.teach.model.vo.coursegraph.CourseGraphQuizVO> listNodeQuizzes(Long teacherId, String nodeId);

    java.util.List<com.ruyi.teach.model.vo.coursegraph.CourseGraphMaterialVO> listNodeMaterials(Long teacherId, String nodeId);
}
