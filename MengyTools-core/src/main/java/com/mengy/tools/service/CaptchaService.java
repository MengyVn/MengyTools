package com.mengy.tools.service;

import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.CaptchaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 滑动拼图验证码服务。
 * 流程：generate 生成带缺口背景图 + 拼图块 -> 前端拖动 -> verify 校验水平坐标 ->
 * 通过后颁发一次性 captchaToken，注册时校验该 token。
 * 防刷：生成频率限制 / 验证尝试次数限制 / 一次性消费。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CaptchaService {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 150;
    private static final int PIECE_SIZE = 46;
    /** 容差：|拖动x - 缺口x| <= TOLERANCE 视为成功 */
    private static final int TOLERANCE = 6;
    private static final int MAX_VERIFY_TRIES = 5;

    private static final String KEY_GAP = "captcha:gap:";        // captchaId -> gapX
    private static final String KEY_TRIES = "captcha:tries:";     // captchaId -> 已校验次数
    private static final String KEY_TOKEN = "captcha:token:";    // 校验通过令牌
    private static final String KEY_GEN_LIMIT = "captcha:gen:";  // IP -> 生成次数

    private static final long GAP_TTL_MIN = 5;
    private static final long TOKEN_TTL_MIN = 10;

    private final StringRedisTemplate redisTemplate;
    private final Random random = new Random();

    /**
     * 生成验证码：返回带缺口背景图 + 拼图块图（base64，不含 data: 前缀）。
     */
    public CaptchaResponse generate(String clientIp) {
        // 生成频率限制：每 IP 10 分钟最多 10 次，防止脚本滥刷
        String genKey = KEY_GEN_LIMIT + clientIp;
        Long cnt = redisTemplate.opsForValue().increment(genKey);
        if (cnt != null && cnt == 1L) {
            redisTemplate.expire(genKey, 10, TimeUnit.MINUTES);
        }
        if (cnt != null && cnt > 10) {
            throw new BusinessException(ResultCode.TOO_MANY_REQUESTS, "验证码请求过于频繁，请稍后再试");
        }

        String captchaId = UUID.randomUUID().toString().replace("-", "");

        // 缺口位置：水平随机（留出拼图块宽度 + 边距），垂直随机但避开顶部/底部
        int gapX = PIECE_SIZE + 10 + random.nextInt(WIDTH - PIECE_SIZE * 2 - 20);
        int gapY = 10 + random.nextInt(HEIGHT - PIECE_SIZE - 20);

        BufferedImage bg = drawBackground();
        BufferedImage piece = makePiece(bg, gapX, gapY);
        punchGap(bg, gapX, gapY);

        // 存缺口坐标
        redisTemplate.opsForValue().set(KEY_GAP + captchaId, String.valueOf(gapX), GAP_TTL_MIN, TimeUnit.MINUTES);

        return CaptchaResponse.builder()
                .captchaId(captchaId)
                .background(toBase64(bg))
                .piece(toBase64(piece))
                .y(gapY)
                .pieceSize(PIECE_SIZE)
                .width(WIDTH)
                .height(HEIGHT)
                .build();
    }

    /**
     * 校验拖动结果，通过后颁发一次性 token。
     */
    public String verify(String captchaId, int x) {
        String gapKey = KEY_GAP + captchaId;
        String gapXStr = redisTemplate.opsForValue().get(gapKey);
        if (gapXStr == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码已过期，请刷新重试");
        }
        // 尝试次数限制
        String triesKey = KEY_TRIES + captchaId;
        Long tries = redisTemplate.opsForValue().increment(triesKey);
        if (tries != null && tries == 1L) {
            redisTemplate.expire(triesKey, GAP_TTL_MIN, TimeUnit.MINUTES);
        }
        if (tries != null && tries > MAX_VERIFY_TRIES) {
            redisTemplate.delete(gapKey);
            throw new BusinessException(ResultCode.BAD_REQUEST, "尝试次数过多，请刷新验证码");
        }

        int gapX = Integer.parseInt(gapXStr);
        if (Math.abs(x - gapX) > TOLERANCE) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "未对齐缺口，请重试");
        }

        // 通过：消费验证码 + 颁发一次性 token
        redisTemplate.delete(gapKey);
        redisTemplate.delete(triesKey);
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(KEY_TOKEN + token, "1", TOKEN_TTL_MIN, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 注册时校验并消费 captchaToken（一次性）。
     */
    public void consumeToken(String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "请先完成真人验证");
        }
        String key = KEY_TOKEN + captchaToken;
        String v = redisTemplate.opsForValue().get(key);
        if (v == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证已失效，请重新完成真人验证");
        }
        redisTemplate.delete(key);
    }

    // ------------------------------------------------------------------
    // 图像绘制
    // ------------------------------------------------------------------

    /** 生成带纹理的彩色背景图 */
    private BufferedImage drawBackground() {
        BufferedImage img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 随机渐变底色
            Color c1 = randomColor(180, 255);
            Color c2 = randomColor(120, 200);
            g.setPaint(new GradientPaint(0, 0, c1, WIDTH, HEIGHT, c2));
            g.fillRect(0, 0, WIDTH, HEIGHT);
            // 随机几何纹理
            for (int i = 0; i < 18; i++) {
                g.setColor(new Color(randomColor(60, 230).getRGB() & 0x66FFFFFF | 0x33000000, true));
                int cx = random.nextInt(WIDTH);
                int cy = random.nextInt(HEIGHT);
                int r = 8 + random.nextInt(28);
                switch (random.nextInt(3)) {
                    case 0 -> g.fillOval(cx, cy, r * 2, r * 2);
                    case 1 -> g.fillRect(cx, cy, r, r);
                    default -> {
                        Polygon p = new Polygon();
                        p.addPoint(cx, cy);
                        p.addPoint(cx + r, cy + r);
                        p.addPoint(cx - r / 2, cy + r);
                        g.fillPolygon(p);
                    }
                }
            }
        } finally {
            g.dispose();
        }
        return img;
    }

    /** 从背景缺口位置裁剪出拼图块（圆角方形，外区域透明），并加描边/阴影便于辨识 */
    private BufferedImage makePiece(BufferedImage bg, int gapX, int gapY) {
        BufferedImage piece = new BufferedImage(PIECE_SIZE, PIECE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = piece.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 圆角裁剪：先填充透明，再按圆角形状裁剪绘制背景内容
            Shape clip = new RoundRectangle2D.Double(0, 0, PIECE_SIZE, PIECE_SIZE, 14, 14);
            g.setClip(clip);
            g.drawImage(bg, 0, 0, PIECE_SIZE, PIECE_SIZE, gapX, gapY, gapX + PIECE_SIZE, gapY + PIECE_SIZE, null);
            g.setClip(null);
            // 描边
            g.setColor(new Color(255, 255, 255, 200));
            g.setStroke(new BasicStroke(2f));
            g.draw(clip);
        } finally {
            g.dispose();
        }
        return piece;
    }

    /** 在背景上打出缺口（半透明暗罩 + 白色虚线边） */
    private void punchGap(BufferedImage bg, int gapX, int gapY) {
        Graphics2D g = bg.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // 缺口阴影
            g.setColor(new Color(0, 0, 0, 120));
            g.fillRoundRect(gapX, gapY, PIECE_SIZE, PIECE_SIZE, 14, 14);
            // 白色虚线边提示缺口位置
            g.setColor(new Color(255, 255, 255, 220));
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND,
                    1f, new float[]{6f, 6f}, 0f));
            g.drawRoundRect(gapX, gapY, PIECE_SIZE, PIECE_SIZE, 14, 14);
        } finally {
            g.dispose();
        }
    }

    private Color randomColor(int min, int max) {
        return new Color(min + random.nextInt(max - min),
                min + random.nextInt(max - min),
                min + random.nextInt(max - min));
    }

    private String toBase64(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "验证码生成失败");
        }
    }
}
