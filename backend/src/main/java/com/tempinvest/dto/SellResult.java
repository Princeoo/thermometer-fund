package com.tempinvest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SellResult {
    private BigDecimal sellAmount;
    private String reason;
    private String strategyType;
}
