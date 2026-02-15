package com.wisehero.stocktrading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스프링 부트 애플리케이션 진입점.
 */
@SpringBootApplication
@EnableScheduling
public class StockTradingApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockTradingApplication.class, args);
    }
}
