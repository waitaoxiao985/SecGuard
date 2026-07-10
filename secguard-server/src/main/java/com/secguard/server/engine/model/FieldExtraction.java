package com.secguard.server.engine.model;

import lombok.Data;

/**
 * 字段提取规则
 *
 * 规则匹配成功后，从原始事件的指定字段中用正则捕获组提取新字段。
 * 例如：从 message "Failed password for root from 192.168.1.1" 中提取 source_ip。
 */
@Data
public class FieldExtraction {

    /** 提取后的字段名 */
    private String field;

    /** 源字段名（从哪个字段提取） */
    private String from;

    /** 正则表达式（需包含一个捕获组） */
    private String regex;
}
