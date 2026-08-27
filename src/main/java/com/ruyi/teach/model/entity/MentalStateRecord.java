package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "mental_state_record")
@Data
public class MentalStateRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 评估用户ID */
    @TableField("user_id")
    private Long userId;

    /** 评估模式: dual(双通道融合) / subjective(纯主观) */
    @TableField("assessment_mode")
    private String assessmentMode;

    /** 五轮主观问卷答案(JSON数组) */
    @TableField("answers_json")
    private String answersJson;

    /** 压力负荷 1-10 */
    @TableField("stress_level")
    private Integer stressLevel;

    /** 能量水平 1-10 */
    @TableField("energy_level")
    private Integer energyLevel;

    /** 专注状态 1-10 */
    @TableField("focus_level")
    private Integer focusLevel;

    /** 认知负荷 1-10 */
    @TableField("cognitive_load")
    private Integer cognitiveLoad;

    /** 心流指数 1-10 */
    @TableField("flow_score")
    private Integer flowScore;

    /** 情绪效价 1-10 */
    @TableField("emotion_score")
    private Integer emotionScore;

    /** 综合评估结论 */
    private String verdict;

    /** 理论依据(JSON数组) */
    @TableField("theories_json")
    private String theoriesJson;

    /** 风险标识(JSON数组) */
    @TableField("risk_flags_json")
    private String riskFlagsJson;

    /** 三级建议(JSON对象: immediate/shortTerm/habit) */
    @TableField("suggestions_json")
    private String suggestionsJson;

    /** 疲劳监测数据快照(events/earSamples/marSamples JSON) */
    @TableField("fatigue_snapshot")
    private String fatigueSnapshot;

    /** 监测时长(秒) */
    @TableField("monitor_seconds")
    private Integer monitorSeconds;

    /** 哈欠次数 */
    @TableField("yawn_count")
    private Integer yawnCount;

    /** 闭眼疲劳次数 */
    @TableField("fatigue_count")
    private Integer fatigueCount;

    /** 专注率(%) */
    @TableField("focus_rate")
    private Integer focusRate;

    /** 学习画像统计窗口(天) */
    @TableField("learning_profile_days")
    private Integer learningProfileDays;

    /** 本次评估使用的学习画像摘要 */
    @TableField("learning_context_summary")
    private String learningContextSummary;

    /** 本次评估使用的学习画像快照(JSON) */
    @TableField("learning_profile_snapshot")
    private String learningProfileSnapshot;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    @TableField("is_delete")
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
