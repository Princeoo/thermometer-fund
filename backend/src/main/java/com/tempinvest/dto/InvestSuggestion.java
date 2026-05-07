package com.tempinvest.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class InvestSuggestion {
    private String fundCode;
    private String fundName;
    private Integer temperature;
    private BigDecimal suggestedAmount;
    private BigDecimal maxAvailable;
    private String reason;
    private String action;  // "BUY" / "SELL" / "HOLD"
    private String sellStrategy;  // 危出策略类型
    private List<String> factors;
}
