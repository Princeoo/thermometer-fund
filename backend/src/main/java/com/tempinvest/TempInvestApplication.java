package com.tempinvest;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.tempinvest.mapper")
@EnableScheduling
public class TempInvestApplication {
    public static void main(String[] args) {
        SpringApplication.run(TempInvestApplication.class, args);
    }
}
