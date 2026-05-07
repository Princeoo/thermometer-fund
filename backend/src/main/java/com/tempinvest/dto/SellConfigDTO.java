package com.tempinvest.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SellConfigDTO {
    private String fundCode;
    private Integer sellCooldownDays;
    private BigDecimal trailingStopTrigger;
    private BigDecimal trailingStopDrawdown;
}
