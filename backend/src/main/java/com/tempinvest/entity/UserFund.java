package com.tempinvest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("user_fund")
public class UserFund {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String fundCode;
    private BigDecimal totalCapital;
    private BigDecimal currentHolding;
    private BigDecimal currentCost;
    private BigDecimal highestPrice;
    private BigDecimal baseMonthlyAmount;
    private LocalDate lastSellDate;
    private Integer sellCooldownDays;
    private Integer profitTierExecuted;
    private Integer trailingStopActive;
    private LocalDateTime createdAt;
}
