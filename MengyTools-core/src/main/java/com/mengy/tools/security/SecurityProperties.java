package com.mengy.tools.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全相关配置：放行路径等。
 */
@Data
@Component
@ConfigurationProperties(prefix = "mengy.security")
public class SecurityProperties {

    /** 无需鉴权的放行路径 */
    private List<String> permitPaths = new ArrayList<>();
}
