package com.ruyi.teach.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@TableName(value = "ai_model_config")
@Data
public class AiModelConfig implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("interface_key")
    private String interfaceKey;

    @TableField("interface_name")
    private String interfaceName;

    @TableField("endpoint_url")
    private String endpointUrl;

    @TableField("model_name")
    private String modelName;

    private String provider;

    private Integer enabled;

    private String remark;

    @TableField("sort_order")
    private Integer sortOrder;

    private Date createTime;

    private Date updateTime;

    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
