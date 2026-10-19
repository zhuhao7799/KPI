package cn.edu.kpi.ruoyi;

import org.dromara.common.core.domain.R;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps KPI business failures to stable HTTP responses without exposing stack traces. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = KpiAssessmentController.class)
public final class KpiExceptionHandler {
    @ExceptionHandler(KpiUnauthenticatedException.class)
    public ResponseEntity<R<Void>> unauthenticated(KpiUnauthenticatedException error) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(R.fail(401, "请先登录"));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<R<Void>> forbidden(SecurityException error) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail(403, "没有绩效业务或单位范围权限"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<R<Void>> invalid(IllegalArgumentException error) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(R.fail(400, error.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<R<Void>> conflict(IllegalStateException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(R.fail(409, error.getMessage()));
    }
}
