package com.mengy.tools.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 把「数据库连不上/用不了」的深层原因翻译成一句能直接照做的排查提示。
 *
 * 背景：JDBC 连接失败时，MyBatis 会把它包成 `MyBatisSystemException`（其 message 往往是 null），
 * 日志里只剩一长串栈，真实原因藏在最底层的 `Caused by`。新人克隆项目到新机器时最常见的
 * 密码错/库名错/MySQL 没起，全靠翻栈底才能看出来 —— 这里把它提到最前面。
 *
 * 纯函数、无依赖，便于单测。
 */
public final class DbErrorHint {

    private DbErrorHint() {
    }

    /** 能在异常链里找到数据库连接/语法类问题时返回提示；否则返回 null（交回通用兜底） */
    public static String describe(Throwable error, String jdbcUrl, String username) {
        String root = rootMessage(error);
        if (root == null) {
            return null;
        }
        String lower = root.toLowerCase();

        List<String> tips = new ArrayList<>();
        String title;

        if (lower.contains("access denied")) {
            title = "数据库账号或密码不正确";
            tips.add("核对 MYSQL_USER / MYSQL_PASSWORD（默认 root / 123456）");
            tips.add("Ubuntu/WSL 上 root 常用 auth_socket 认证，用 TCP+密码必然 Access denied："
                    + "建议建专用账号 CREATE USER 'mengy'@'%' IDENTIFIED BY 'xxx'; GRANT ALL ON 库名.* TO 'mengy'@'%';");
        } else if (lower.contains("unknown database")) {
            title = "数据库不存在（库名对不上）";
            tips.add("核对 MYSQL_DB（默认 mengy_tools），确认 SHOW DATABASES 里有这个库");
            tips.add("从别的机器导入过数据的话，确认导进了同名的库");
        } else if (lower.contains("doesn't exist") || lower.contains("does not exist")
                || lower.contains("unknown table")) {
            title = "表不存在（库建好了但没导入表结构）";
            tips.add("执行 Sql/mengy_tools_schema.sql 建表；需要完整数据就从旧库 mysqldump 导出后导入");
        } else if (lower.contains("connection refused") || lower.contains("communications link failure")
                || lower.contains("connect timed out") || lower.contains("unknown host")) {
            title = "连不上 MySQL 服务";
            tips.add("确认 MySQL 已启动、端口正确（MYSQL_HOST / MYSQL_PORT，默认 127.0.0.1:3306）");
            tips.add("若 MySQL 在容器/WSL 内，确认 3306 已映射出来");
        } else if (lower.contains("public key retrieval is not allowed")) {
            title = "MySQL 8 认证插件要求公钥";
            tips.add("连接串加上 allowPublicKeyRetrieval=true（本项目 dev 配置已带）");
        } else {
            return null;
        }

        tips.add("当前连接串：" + (jdbcUrl == null ? "（未取到）" : jdbcUrl)
                + "，用户：" + (username == null ? "（未取到）" : username));

        StringBuilder sb = new StringBuilder();
        sb.append("[数据库不可用] ").append(title).append(" —— ").append(root);
        for (int i = 0; i < tips.size(); i++) {
            sb.append("\n    ").append(i + 1).append(") ").append(tips.get(i));
        }
        return sb.toString();
    }

    /** 取异常链最底层那条的信息（SQLException 优先，它才是真正的数据库报错），并压成单行 */
    private static String rootMessage(Throwable error) {
        Throwable cursor = error;
        String sqlMessage = null;
        int guard = 0;
        while (cursor != null && guard++ < 32) {
            if (cursor instanceof SQLException && cursor.getMessage() != null && !cursor.getMessage().isBlank()) {
                sqlMessage = cursor.getMessage();
            }
            Throwable next = cursor.getCause();
            if (next == null || next == cursor) {
                break;
            }
            cursor = next;
        }
        if (sqlMessage != null) {
            return flatten(sqlMessage);
        }
        // 非 SQLException（如 java.net.ConnectException）时退回最底层消息
        String last = cursor == null ? null : cursor.getMessage();
        if (last != null && !last.isBlank()) {
            return flatten(last);
        }
        return error == null ? null : flatten(error.getMessage());
    }

    /**
     * 压成单行并限长：MyBatis 的报错常是多行「### Error querying database...」，
     * 直接拼进提示里会把整段日志冲散，反而不易读。
     */
    private static String flatten(String message) {
        if (message == null) {
            return null;
        }
        String oneLine = message.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 160 ? oneLine : oneLine.substring(0, 160) + "…";
    }
}
