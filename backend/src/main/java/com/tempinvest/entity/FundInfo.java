package com.tempinvest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("fund_info")
public class FundInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String fundCode;
    private String fundName;
    private Integer fundType;
    private BigDecimal peCurrent;
    private BigDecimal pePercentile;
    private BigDecimal pbCurrent;
    private BigDecimal pbPercentile;
    private Integer temperature;
    private BigDecimal dailyChange;
    private BigDecimal netValue;
    private LocalDate netValueDate;
    private LocalDateTime updateTime;
}
