package com.tempinvest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("invest_record")
public class InvestRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String fundCode;
    private LocalDate date;
    private BigDecimal suggestedAmount;
    private BigDecimal actualAmount;
    private Integer temperature;
    private String reason;
    private String sellStrategy;
    private Integer status;
}