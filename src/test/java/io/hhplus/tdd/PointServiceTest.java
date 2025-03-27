package io.hhplus.tdd;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.exception.InsufficientPointException;
import io.hhplus.tdd.exception.InvalidPointException;
import io.hhplus.tdd.exception.PointLimitExceedException;
import io.hhplus.tdd.point.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PointServiceTest {
    private UserPointTable userPointTable;
    private PointHistoryTable pointHistoryTable;

    private PointService pointService;

    private final long userId = 1L;

    @BeforeEach
    void setUp() {
        userPointTable = new UserPointTable();
        pointHistoryTable = new PointHistoryTable();

        pointService = new PointService(userPointTable, pointHistoryTable);
    }

    // 정상 동작

    @DisplayName("특정 유저 포인트 조회 테스트 : get 유저 포인트 메소드는 반드시 유저 포인트를 반환해야 함")
    @Test
    void getUserPoint_shouldReturnUserPoint() {
        // given
        userPointTable.insertOrUpdate(userId, 50000);

        // when
        UserPoint userPoint = pointService.getUserPoint(userId);

        // then
        assertEquals(50000, userPoint.point());
    }

    @DisplayName("특정 유저 포인트 내역 조회 테스트 : get 유저 포인트 내역 메소드는 반드시 유저 포인트 내역 리스트를 반환해야 함")
    @Test
    void getPointHistories_shouldReturnPointHistoryList() {
        // given
        pointHistoryTable.insert(userId, 28000, TransactionType.CHARGE,System.currentTimeMillis());
        pointHistoryTable.insert(userId, 8500, TransactionType.USE, System.currentTimeMillis());

        // when
        List<PointHistory> historyList = pointService.getPointHistories(userId);

        // then
        assertEquals(2, historyList.size());

        assertEquals(28000, historyList.get(0).amount());
        assertEquals(TransactionType.CHARGE, historyList.get(0).type());

        assertEquals(8500, historyList.get(1).amount());
        assertEquals(TransactionType.USE, historyList.get(1).type());
    }

    @DisplayName("사용 금액 만큼 잔액 차감")
    @Test
    void usePoint_shouldBeDecreasePoint() {
        // given
        long useAmount = 85000;
        userPointTable.insertOrUpdate(userId, 130000);

        // when
        pointService.use(userId, useAmount);
        UserPoint updateUserPoint = pointService.getUserPoint(userId);

        // then
        assertEquals(130000-useAmount, updateUserPoint.point());
    }

    @DisplayName("충전 금액 만큼 잔액 증가")
    @Test
    void chargePoint_shouldBeIncreasePoint() {
        // given
        long chargeAmount = 77000;
        userPointTable.insertOrUpdate(userId, 110000);

        // when
        pointService.charge(userId, chargeAmount);
        UserPoint updateUserPoint = pointService.getUserPoint(userId);

        // then
        assertEquals(110000 + chargeAmount, updateUserPoint.point());
    }

    // 실패 테스트

    @DisplayName("충전 금액 <= 0 인 경우 예외 사항 발생")
    @Test
    void chargeException_whenChargeAmountIsZeroOrNegative() {
        // given
        long invalidAmount = 0;

        // when & then
        Exception exception = assertThrows(InvalidPointException.class, () -> {
            pointService.charge(userId, invalidAmount);
        });
        assertEquals("충전 금액은 0보다 커야 합니다.", exception.getMessage());
    }

    @DisplayName("잔액 + 충전 금액 > 최대 한도인 경우 예외 사항 발생")
    @Test
    void chargeException_whenExceedChargeAmountLimit() {
        // given
        long chargeAmount = 10001;
        userPointTable.insertOrUpdate(userId, 990000);

        // when & then
        Exception exception = assertThrows(PointLimitExceedException.class, () -> {
            pointService.charge(userId, chargeAmount);
        });
        assertEquals("충전 한도를 초과 했습니다.", exception.getMessage());
    }

    @DisplayName("사용 금액 <= 0인 경우 예외 사항 발생")
    @Test
    void useException_whenUseAmountIsZeroOrNegative() {
        // given
        long invalidAmount = -1;

        // when & then
        Exception exception = assertThrows(InvalidPointException.class, () -> {
            pointService.use(userId, invalidAmount);
        });
        assertEquals("사용 금액은 0보다 커야 합니다.", exception.getMessage());
    }

    @DisplayName("잔액이 부족한 경우 예외 사항 발생")
    @Test
    void useException_whenUserPointIsInsufficient() {
        // given
        long useAmount = 50000;
        userPointTable.insertOrUpdate(userId, 20000);

        // when & then
        Exception exception = assertThrows(InsufficientPointException.class, () -> {
            pointService.use(userId, useAmount);
        });
        assertEquals("포인트 잔액이 부족합니다.", exception.getMessage());
    }

}