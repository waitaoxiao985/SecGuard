package com.secguard.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 已安装软件条目（Agent -> Server）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSoftwareDTO {

    /** 软件名称 */
    private String name;

    /** 版本号 */
    private String version;

    /** 厂商 */
    private String vendor;

    /** 安装格式 (deb/rpm/msi/exe/appx) */
    private String format;
}
