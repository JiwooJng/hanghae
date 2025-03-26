package io.hhplus.tdd.exception;

public class PointLimitExceedException extends RuntimeException {
    public PointLimitExceedException() {
        super("충전 후 포인트 잔액이 충전 가능한 최대 금액을 초과합니다.");
    }
}
