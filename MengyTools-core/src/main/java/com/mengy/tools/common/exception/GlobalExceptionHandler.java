package com.mengy.tools.common.exception;

import com.mengy.tools.common.Result;
import com.mengy.tools.common.ResultCode;
import com.mengy.tools.util.DbErrorHint;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import javax.sql.DataSource;

/**
 * 全局异常处理：业务异常、参数校验异常、安全异常及系统未知异常。
 * need.md 6. 工程化规范 —— 避免向前端暴露底层报错信息。
 *
 * 数据库类异常额外做一次「翻译」：只写进服务端日志，响应体仍是统一的「系统繁忙」，
 * 不向客户端泄露连接串、账号等内部信息。
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectProvider<DataSource> dataSourceProvider;
    private final Environment environment;

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletRequest request) {
        log.warn("[业务异常] uri={}, code={}, msg={}", request.getRequestURI(), e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : ResultCode.BAD_REQUEST.getMessage();
        log.warn("[参数校验失败] {}", msg);
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBind(BindException e) {
        FieldError fieldError = e.getBindingResult().getFieldError();
        String msg = fieldError != null ? fieldError.getDefaultMessage() : ResultCode.BAD_REQUEST.getMessage();
        return Result.fail(ResultCode.BAD_REQUEST, msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(ResultCode.BAD_REQUEST, "缺少必要参数: " + e.getParameterName());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public Result<Void> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return Result.fail(ResultCode.METHOD_NOT_ALLOWED, e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuth(AuthenticationException e) {
        return Result.fail(ResultCode.UNAUTHORIZED);
    }

    /**
     * 访问不存在的路径。
     * Spring 6.1 起未匹配的请求会抛 NoResourceFoundException，若不单独处理会被下面的
     * Exception 兜底成 500「系统繁忙」——把「接口不存在」误报成服务端故障，排查时很误导。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResource(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("[路径不存在] uri={}", request.getRequestURI());
        return Result.fail(ResultCode.NOT_FOUND, "接口不存在：" + request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        return Result.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnknown(Exception e, HttpServletRequest request) {
        // 数据库不可用时（密码错/库名错/服务没起/表没建）额外打印一行能直接照做的提示：
        // MyBatis 会把真实原因裹成 message 为 null 的 MyBatisSystemException，
        // 不提炼出来就得翻到栈底才看得见。
        String hint = DbErrorHint.describe(e, currentJdbcUrl(), currentUsername());
        if (hint != null) {
            log.error("[系统异常] uri={}\n{}", request.getRequestURI(), hint);
        } else {
            log.error("[系统异常] uri={}", request.getRequestURI(), e);
        }
        return Result.fail(ResultCode.INTERNAL_ERROR);
    }

    /** 实际生效的连接串（Hikari 已解析过占位符），取不到就算了 */
    private String currentJdbcUrl() {
        HikariDataSource ds = hikari();
        if (ds != null) {
            try {
                return ds.getJdbcUrl();
            } catch (Exception ignored) {
                // 落到下面的 env 兜底
            }
        }
        return environment == null ? null : environment.getProperty("spring.datasource.url");
    }

    private String currentUsername() {
        HikariDataSource ds = hikari();
        if (ds != null) {
            try {
                return ds.getUsername();
            } catch (Exception ignored) {
                // 落到下面的 env 兜底
            }
        }
        return environment == null ? null : environment.getProperty("spring.datasource.username");
    }

    private HikariDataSource hikari() {
        try {
            DataSource ds = dataSourceProvider == null ? null : dataSourceProvider.getIfAvailable();
            return ds instanceof HikariDataSource h ? h : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
