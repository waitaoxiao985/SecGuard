package com.secguard.server.engine.model;

import lombok.Data;

import java.util.List;

/**
 * 规则匹配条件
 *
 * 支持四种操作符：
 * - equals:     精确匹配字段值
 * - contains:   包含子串
 * - regex:      正则表达式匹配
 * - numeric_range: 数值范围（min/max）
 */
@Data
public class MatchCondition {

    /** 目标字段名 */
    private String field;

    /** 操作符: equals / contains / regex / numeric_range */
    private String operator;

    /** 匹配值（equals/contains/regex 使用） */
    private String value;

    /** 数值范围下限（numeric_range 使用） */
    private Double min;

    /** 数值范围上限（numeric_range 使用） */
    private Double max;
}
