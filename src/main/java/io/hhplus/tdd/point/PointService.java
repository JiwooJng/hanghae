package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;

import io.hhplus.tdd.exception.InsufficientPointException;
import io.hhplus.tdd.exception.InvalidPointException;
import io.hhplus.tdd.exception.PointLimitExceedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@Service
public class PointService {
    private static final Logger log = LoggerFactory.getLogger(PointService.class);

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private static final long MAX_POINT_LIMIT = 1_000_000;

    public PointService(UserPointTable userPointTable, PointHistoryTable pointHistoryTable) {
        this.userPointTable = userPointTable;
        this.pointHistoryTable = pointHistoryTable;
    }

    public UserPoint getUserPoint(@PathVariable long id) {
        log.info("유저 포인트 조회 > 유저 id: {}", id);
        return userPointTable.selectById(id);
    }

    public List<PointHistory> getPointHistories(@PathVariable long id) {
        log.info("유저 포인트 내역 조회 > 유저 id: {}", id);
        return pointHistoryTable.selectAllByUserId(id);
    }

    public UserPoint charge(@PathVariable long id, @RequestBody long amount) {
        if (amount <= 0) {
            log.error("충전 금액 부족 > 유저 id: {} 포인트 잔액: {}", id, amount);
            throw new InvalidPointException("충전 금액은 0보다 커야 합니다.");
        }

        UserPoint userPoint = userPointTable.selectById(id);
        long newPoint = userPoint.point() + amount;

        if (newPoint > MAX_POINT_LIMIT) {
            throw new PointLimitExceedException();
        }

        UserPoint updateUserPoint = userPointTable.insertOrUpdate(id, newPoint);
        pointHistoryTable.insert(id, newPoint, TransactionType.CHARGE, System.currentTimeMillis());

        log.info("유저 id: {} 포인트 충전: {}원, 포인트 잔액: {}원", id, amount, newPoint);
        return updateUserPoint;
    }

    public UserPoint use(@PathVariable long id, @RequestBody long amount) {
        if (amount <= 0) {
            log.error("포인트 사용 금액 <= 0: 유저 id: {} 포인트 사용 금액: {}", id, amount);
            throw new InvalidPointException("사용 금액은 0보다 커야 합니다.");
        }

        UserPoint userPoint = userPointTable.selectById(id);
        if(userPoint.point() < amount) {
            log.error("포인트 잔액 부족 > 유저 id: {} 포인트 잔액: {}", id, userPoint);
            throw new InsufficientPointException();
        }

        long newPoint = userPoint.point() - amount;
        UserPoint updeateUserPoint = userPointTable.insertOrUpdate(id, newPoint);

        pointHistoryTable.insert(id, amount, TransactionType.USE, System.currentTimeMillis());
        log.info("유저 id: {} 포인트 사용: {}원, 포인트 잔액: {}원", id, amount, newPoint);

        return updeateUserPoint;
    }
}
