package com.mengy.tools.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

/**
 * 默认头像生成：取昵称（或用户名）首字，首字母大写，绘制带渐变背景的方形头像。
 * 注册成功时生成 PNG 落盘到 nginx 图片目录，返回相对路径 /images/<name>.png。
 */
@Slf4j
@Component
public class AvatarGenerator {

    private static final int SIZE = 200;

    /** 渐变色板，按昵称哈希取一组，保证同一昵称颜色稳定 */
    private static final Color[][] PALETTE = {
            {new Color(99, 102, 241), new Color(129, 140, 248)},   // indigo
            {new Color(236, 72, 153), new Color(244, 114, 182)},   // pink
            {new Color(6, 182, 212), new Color(34, 211, 238)},      // cyan
            {new Color(16, 185, 129), new Color(52, 211, 153)},     // green
            {new Color(245, 158, 11), new Color(251, 191, 36)},     // amber
            {new Color(239, 68, 68), new Color(248, 113, 113)},     // red
            {new Color(139, 92, 246), new Color(167, 139, 250)},    // violet
            {new Color(14, 165, 233), new Color(56, 189, 248)}      // sky
    };

    @Value("${mengy.upload.dir}")
    private String uploadDir;

    /**
     * 生成并保存头像，返回 nginx 可访问的相对路径（如 /images/xxx.png）。
     */
    public String generateAndSave(String nickname) {
        String label = resolveLabel(nickname);
        BufferedImage img = render(label, nickname);

        String filename = "avatar-" + UUID.randomUUID().toString().replace("-", "") + ".png";
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath();
            Files.createDirectories(dir);
            ImageIO.write(img, "png", dir.resolve(filename).toFile());
            return "/images/" + filename;
        } catch (IOException e) {
            // 落盘失败则回退为 base64 data URL，保证用户仍有头像
            log.warn("默认头像落盘失败，回退 base64: {}", e.getMessage());
            return "data:image/png;base64," + toBase64(img);
        }
    }

    /** 取首字；若为英文字母则大写 */
    private String resolveLabel(String nickname) {
        if (nickname == null || nickname.isBlank()) return "U";
        String first = String.valueOf(nickname.charAt(0));
        if (first.matches("[a-z]")) return first.toUpperCase();
        return first;
    }

    private BufferedImage render(String label, String nickname) {
        BufferedImage img = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int idx = nickname == null ? 0 : Math.floorMod(nickname.hashCode(), PALETTE.length);
            Color[] pair = PALETTE[idx];

            // 圆角方形 + 渐变背景
            g.setColor(pair[0]);
            g.fillRoundRect(0, 0, SIZE, SIZE, 48, 48);
            GradientPaint gp = new GradientPaint(0, 0, pair[0], SIZE, SIZE, pair[1]);
            g.setPaint(gp);
            g.fillRoundRect(0, 0, SIZE, SIZE, 48, 48);

            // 首字
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 120));
            FontMetrics fm = g.getFontMetrics();
            int tx = (SIZE - fm.stringWidth(label)) / 2;
            int ty = (SIZE - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(label, tx, ty);
        } finally {
            g.dispose();
        }
        return img;
    }

    private String toBase64(BufferedImage img) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (IOException e) {
            return "";
        }
    }
}
