package com.mengy.tools.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.common.exception.BusinessException;
import com.mengy.tools.dto.IpLockDTO;
import com.mengy.tools.entity.SysIpBan;
import com.mengy.tools.mapper.SysIpBanMapper;
import com.mengy.tools.util.ClientIpUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * IP 封禁服务：持久化封禁名单 + 读取/清理 Redis 临时锁定。
 *
 * 两套机制的关系：
 *   Redis  login:fail / login:lock —— 输错密码自动锁 30 分钟，自动过期，无需人工；
 *   本服务 sys_ip_ban            —— 管理员手工封禁，可永久或定时，需人工解除。
 *
 * 生效判定用 30 秒内存快照：拦截器每个请求都要判断，不能每请求查库；
 * 封禁/解封后立即失效快照，保证「一键封禁」马上生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBanService {

    private static final String FAIL_KEY_PREFIX = "login:fail:";
    private static final String LOCK_KEY_PREFIX = "login:lock:";
    /** 快照有效期：30 秒（多实例部署时最长 30 秒收敛） */
    private static final long SNAPSHOT_TTL_MS = 30_000L;

    private final SysIpBanMapper banMapper;
    private final IpRegionService regionService;
    private final StringRedisTemplate redisTemplate;

    private volatile Set<String> snapshot = Set.of();
    private volatile long snapshotAt = 0L;

    // ==================== 生效判定 ====================

    /** 该 IP 是否处于封禁状态（供登录与过滤器调用） */
    public boolean isBanned(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        return currentSnapshot().contains(ip.trim());
    }

    private Set<String> currentSnapshot() {
        long now = System.currentTimeMillis();
        if (now - snapshotAt > SNAPSHOT_TTL_MS) {
            synchronized (this) {
                if (now - snapshotAt > SNAPSHOT_TTL_MS) {
                    try {
                        snapshot = banMapper.selectActive().stream()
                                .map(SysIpBan::getIp)
                                .filter(x -> x != null && !x.isBlank())
                                .collect(Collectors.toSet());
                    } catch (Exception e) {
                        // 查库失败时保留旧快照（宁可多拦一点，也不要因为抖动把封禁放空）
                        log.warn("加载 IP 封禁名单失败，沿用上一次快照（{} 条）: {}", snapshot.size(), e.getMessage());
                    }
                    snapshotAt = now;
                }
            }
        }
        return snapshot;
    }

    /** 名单变化后立即失效快照 */
    public void invalidate() {
        snapshotAt = 0L;
    }

    public long countActive() {
        try {
            return banMapper.countActive();
        } catch (Exception e) {
            return 0L;
        }
    }

    // ==================== 封禁 / 解封 ====================

    /**
     * 手工封禁（同一 IP 重复封禁会覆盖原记录）。
     *
     * @param minutes null 或 <=0 表示永久封禁
     */
    @Transactional(rollbackFor = Exception.class)
    public SysIpBan ban(String ip, String reason, Integer minutes, Long operatorId, String operatorName) {
        String target = ip == null ? "" : ip.trim();
        if (!ClientIpUtils.isValidIp(target)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "IP 格式不正确");
        }
        String region = regionService.resolve(target);
        String text = reason == null || reason.isBlank() ? "管理员手工封禁" : reason.trim();
        if (text.length() > 255) {
            text = text.substring(0, 255);
        }
        LocalDateTime expireTime = (minutes == null || minutes <= 0)
                ? null
                : LocalDateTime.now().plusMinutes(minutes);

        SysIpBan exists = banMapper.selectByIp(target);
        if (exists == null) {
            SysIpBan ban = new SysIpBan();
            ban.setIp(target);
            ban.setRegion(region);
            ban.setReason(text);
            ban.setOperatorId(operatorId);
            ban.setOperatorName(operatorName == null ? "" : operatorName);
            ban.setExpireTime(expireTime);
            banMapper.insertBan(ban);
        } else {
            banMapper.reBan(target, region, text, operatorId,
                    operatorName == null ? "" : operatorName, expireTime);
        }

        // 已封禁的 IP 不需要再留着失败计数/临时锁，否则解封后仍会被 Redis 挡在门外
        clearLockKeys(target);
        invalidate();
        log.info("IP 已封禁: ip={} region={} 到期={} 操作人={}", target, region,
                expireTime == null ? "永久" : expireTime, operatorName);
        return banMapper.selectByIp(target);
    }

    /** 解除封禁（按 IP，幂等） */
    @Transactional(rollbackFor = Exception.class)
    public void unban(String ip) {
        String target = ip == null ? "" : ip.trim();
        if (target.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少 IP");
        }
        int affected = banMapper.releaseByIp(target);
        if (affected == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该 IP 当前不在封禁名单中");
        }
        clearLockKeys(target);
        invalidate();
        log.info("IP 已解封: ip={}", target);
    }

    /** 封禁名单分页 */
    public IPage<SysIpBan> page(int page, int size, String state, String keyword) {
        int p = Math.max(page, 1);
        int s = Math.min(Math.max(size, 1), 100);
        String st = ("active".equals(state) || "released".equals(state)) ? state : null;
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return banMapper.selectBanPage(new Page<>(p, s), kw, st);
    }

    // ==================== Redis 临时锁定 ====================

    /**
     * 当前被临时锁定的 IP 列表（含仅有失败计数、尚未锁定的）。
     * 数据量极小（只有触发过失败的 IP 才有 key），用 SCAN 逐个读，不用 KEYS。
     */
    public List<IpLockDTO> locks() {
        Set<String> lockIps = scanIps(LOCK_KEY_PREFIX);
        Set<String> failIps = scanIps(FAIL_KEY_PREFIX);
        Set<String> all = new TreeSet<>();
        all.addAll(lockIps);
        all.addAll(failIps);

        List<IpLockDTO> out = new ArrayList<>(all.size());
        for (String ip : all) {
            IpLockDTO dto = new IpLockDTO();
            dto.setIp(ip);
            dto.setRegion(regionService.resolve(ip));
            dto.setLocked(lockIps.contains(ip));
            Long ttl = lockIps.contains(ip)
                    ? redisTemplate.getExpire(LOCK_KEY_PREFIX + ip, TimeUnit.SECONDS)
                    : 0L;
            dto.setRemainSeconds(ttl == null || ttl < 0 ? 0L : ttl);
            String fail = redisTemplate.opsForValue().get(FAIL_KEY_PREFIX + ip);
            dto.setFailCount(parseInt(fail));
            out.add(dto);
        }
        // 锁定中的排前面，其次按剩余时间倒序
        out.sort((a, b) -> {
            if (!a.getLocked().equals(b.getLocked())) {
                return Boolean.TRUE.equals(a.getLocked()) ? -1 : 1;
            }
            return Long.compare(b.getRemainSeconds(), a.getRemainSeconds());
        });
        return out;
    }

    /** 解锁：清掉该 IP 的失败计数与锁定标记（人工介入后立即恢复可登录） */
    public void unlock(String ip) {
        String target = ip == null ? "" : ip.trim();
        if (target.isBlank()) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "缺少 IP");
        }
        if (isBanned(target)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该 IP 在封禁名单中，请到「IP 封禁」页解封");
        }
        clearLockKeys(target);
        log.info("IP 临时锁定已解除: ip={}", target);
    }

    // ==================== 内部 ====================

    private void clearLockKeys(String ip) {
        try {
            redisTemplate.delete(List.of(FAIL_KEY_PREFIX + ip, LOCK_KEY_PREFIX + ip));
        } catch (Exception e) {
            log.warn("清理 IP 锁定键失败: ip={} err={}", ip, e.getMessage());
        }
    }

    /** SCAN 出某前缀下的 IP 集合（模式固定，无注入风险） */
    private Set<String> scanIps(String prefix) {
        Set<String> ips = new HashSet<>();
        try {
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                ScanOptions options = ScanOptions.scanOptions().match(prefix + "*").count(200).build();
                try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
                    while (cursor.hasNext()) {
                        String key = new String(cursor.next(), StandardCharsets.UTF_8);
                        String ip = key.substring(prefix.length());
                        if (!ip.isBlank()) {
                            ips.add(ip);
                        }
                    }
                }
                return null;
            });
        } catch (Exception e) {
            log.warn("扫描 Redis 锁定键失败: prefix={} err={}", prefix, e.getMessage());
        }
        return ips;
    }

    private static Integer parseInt(String v) {
        if (v == null || v.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
