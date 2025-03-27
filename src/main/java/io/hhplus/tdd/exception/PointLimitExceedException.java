package io.hhplus.tdd.exception;

public class PointLimitExceedException extends RuntimeException {
    public PointLimitExceedException() {
        super("충전 한도를 초과 했습니다.");
    }
}
