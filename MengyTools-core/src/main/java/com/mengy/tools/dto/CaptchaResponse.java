package com.mengy.tools.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 滑动拼图验证码生成结果。
 * background：带缺口的背景图（base64，不含前缀）。
 * piece：需被拖动的拼图块图（base64，含 alpha 透明通道，不含前缀）。
 * y：拼图块与缺口的垂直坐标（前端据此定位，水平坐标由用户拖动后提交校验）。
 */
@Data
@Builder
public class CaptchaResponse {

    private String captchaId;
    private String background;
    private String piece;
    private Integer y;
    /** 拼图块尺寸，前端据此渲染 */
    private Integer pieceSize;
    private Integer width;
    private Integer height;
}
