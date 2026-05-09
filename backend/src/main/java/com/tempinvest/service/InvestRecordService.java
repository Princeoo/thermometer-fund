package com.tempinvest.service;

import com.tempinvest.entity.InvestRecord;
import com.tempinvest.entity.UserFund;
import com.tempinvest.mapper.InvestRecordMapper;
import com.tempinvest.mapper.UserFundMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class InvestRecordService {

    @Autowired
    private InvestRecordMapper investRecordMapper;

    @Autowired
    private UserFundMapper userFundMapper;

    @Autowired
    private InvestDecisionService investDecisionService;

    @Transactional
    public void confirmSell(Long userId, String fundCode, BigDecimal actualAmount, String strategy) {
        // 1. 记录投资记录
        InvestRecord record = new InvestRecord();
        record.setUserId(userId);
        record.setFundCode(fundCode);
        record.setDate(LocalDate.now());
        record.setActualAmount(actualAmount.negate());
        record.setSellStrategy(strategy);
        record.setStatus(1);
        investRecordMapper.insert(record);

        // 2. 更新用户持仓
        UserFund userFund = userFundMapper.selectByUserIdAndCode(userId, fundCode);
        if (userFund != null) {
            userFund.setCurrentHolding(userFund.getCurrentHolding().subtract(actualAmount));
            userFund.setLastSellDate(LocalDate.now());

            // 3. 根据策略类型更新其他字段
            if ("TRAILING_STOP".equals(strategy)) {
                userFund.setHighestPrice(null);
                userFund.setTrailingStopActive(0);
            } else if ("BATCH_PROFIT".equals(strategy)) {
                int tier = determineCurrentTier(userFund);
                int executed = userFund.getProfitTierExecuted() != null ? userFund.getProfitTierExecuted() : 0;
                userFund.setProfitTierExecuted(executed | (1 << (tier - 1)));
            }

            userFundMapper.updateById(userFund);
        }
    }

    private int determineCurrentTier(UserFund userFund) {
        if (userFund.getCurrentCost() == null || userFund.getCurrentCost().compareTo(BigDecimal.ZERO) == 0) {
            return 0;
        }
        BigDecimal profitRate = userFund.getCurrentHolding()
            .subtract(userFund.getCurrentCost())
            .divide(userFund.getCurrentCost(), 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal rate100 = profitRate.multiply(new BigDecimal("100"));

        if (rate100.compareTo(new BigDecimal("50")) >= 0) return 4;
        if (rate100.compareTo(new BigDecimal("40")) >= 0) return 3;
        if (rate100.compareTo(new BigDecimal("30")) >= 0) return 2;
        if (rate100.compareTo(new BigDecimal("20")) >= 0) return 1;
        return 0;
    }
}