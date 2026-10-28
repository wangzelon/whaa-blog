package cn.whaa.blog.common.result;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {
    SUCCESS(0, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    INTERNAL_ERROR(500, "internal error");

    private final int code;
    private final String message;
}
