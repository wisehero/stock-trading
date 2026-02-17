package com.wisehero.stocktrading.order.scheduler;

import com.wisehero.stocktrading.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 장 종료 시점에 DAY 주문을 만료 처리하는 스케줄러.
 */
@Component
public class DayOrderExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(DayOrderExpirationScheduler.class);

    private final OrderService orderService;

    public DayOrderExpirationScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(
            cron = "${trading.day-expire-cron:0 40 15 * * MON-FRI}",
            zone = "${trading.market-time-zone:Asia/Seoul}"
    )
    public void expireDayOrders() {
        int expiredCount = orderService.expireDayOrders();
        log.info("DAY 주문 만료 배치 완료 - 만료 건수: {}", expiredCount);
    }
}
