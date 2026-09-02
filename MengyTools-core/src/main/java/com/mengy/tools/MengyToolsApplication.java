package com.mengy.tools;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 个人数字资产平台（博客/工具/导航）后端启动类。
 * 纯 RESTful API 提供者，前后端分离。
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan("com.mengy.tools")
@MapperScan("com.mengy.tools.mapper")
public class MengyToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(MengyToolsApplication.class, args);
    }
}
