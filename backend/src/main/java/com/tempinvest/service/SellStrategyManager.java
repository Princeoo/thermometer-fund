package com.tempinvest.service;

import com.tempinvest.dto.SellResult;
import com.tempinvest.entity.FundInfo;
import com.tempinvest.entity.UserFund;
import com.tempinvest.mapper.FundInfoMapper;
import com.tempinvest.mapper.InvestRecordMapper;
import com.tempinvest.mapper.UserFundMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class SellStrategyManager {

    @Autowired
    private UserFundMapper userFundMapper;

    @Autowired
    private InvestRecordMapper investRecordMapper;

    @Autowired
    private FundInfoMapper fundInfoMapper;

    // 默认参数
    private static final BigDecimal TRAILING_STOP_TRIGGER = new BigDecimal("0.15");
    private static final BigDecimal TRAILING_STOP_DRAWDOWN = new BigDecimal("0.10");

    /**
     * 检查移动止盈
     */
    public SellResult checkTrailingStop(Long userId, String fundCode) {
        UserFund userFund = userFundMapper.selectByUserIdAndCode(userId, fundCode);
        FundInfo fundInfo = fundInfoMapper.selectByCode(fundCode);

        if (userFund == null || fundInfo == null || fundInfo.getNetValue() == null) {
            return null;
        }

        // 计算当前收益率
        BigDecimal profitRate = calculateProfitRate(userFund, fundInfo);
        if (profitRate == null) return null;

        // 判断是否启动移动止盈
        if (profitRate.compareTo(TRAILING_STOP_TRIGGER) < 0) {
            // 未达到触发门槛，记录最高净值
            updateHighestPrice(userFund, fundInfo.getNetValue());
            return null;
        }

        // 已启动，检查回撤
        BigDecimal highestPrice = userFund.getHighestPrice();
        if (highestPrice == null) {
            highestPrice = fundInfo.getNetValue();
            userFund.setHighestPrice(highestPrice);
            userFundMapper.updateById(userFund);
        }

        BigDecimal currentPrice = fundInfo.getNetValue();
        BigDecimal threshold = highestPrice.multiply(BigDecimal.ONE.subtract(TRAILING_STOP_DRAWDOWN));

        if (currentPrice.compareTo(threshold) < 0) {
            // 触发移动止盈
            BigDecimal sellAmount = userFund.getCurrentHolding().multiply(new BigDecimal("0.5"));
            BigDecimal drawdown = calculateDrawdown(highestPrice, currentPrice);
            String reason = String.format("移动止盈触发：收益率%.2f%%，从最高点回撤%.2f%%",
                profitRate.multiply(new BigDecimal("100")),
                drawdown.multiply(new BigDecimal("100")));

            return new SellResult(sellAmount, reason, "TRAILING_STOP");
        }

        // 继续跟踪，更新最高净值
        updateHighestPrice(userFund, currentPrice);
        return null;
    }

    /**
     * 计算收益率
     */
    private BigDecimal calculateProfitRate(UserFund userFund, FundInfo fundInfo) {
        if (userFund.getCurrentCost() == null || userFund.getCurrentCost().compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return userFund.getCurrentHolding()
            .subtract(userFund.getCurrentCost())
            .divide(userFund.getCurrentCost(), 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算回撤比例
     */
    private BigDecimal calculateDrawdown(BigDecimal highestPrice, BigDecimal currentPrice) {
        return highestPrice.subtract(currentPrice).divide(highestPrice, 4, RoundingMode.HALF_UP);
    }

    /**
     * 更新最高净值
     */
    private void updateHighestPrice(UserFund userFund, BigDecimal currentPrice) {
        if (userFund.getHighestPrice() == null || currentPrice.compareTo(userFund.getHighestPrice()) > 0) {
            userFund.setHighestPrice(currentPrice);
            userFundMapper.updateById(userFund);
        }
    }

    /**
     * 检查分批止盈
     */
    public SellResult checkBatchProfit(Long userId, String fundCode) {
        UserFund userFund = userFundMapper.selectByUserIdAndCode(userId, fundCode);
        FundInfo fundInfo = fundInfoMapper.selectByCode(fundCode);

        if (userFund == null || fundInfo == null) {
            return null;
        }

        BigDecimal profitRate = calculateProfitRate(userFund, fundInfo);
        if (profitRate == null) return null;

        // 确定档位
        int tier = determineProfitTier(profitRate);
        if (tier == 0) return null;

        // 检查是否已执行
        if (isTierExecuted(userFund.getProfitTierExecuted(), tier)) {
            return null;
        }

        // 触发止盈
        BigDecimal sellRatio = getTierSellRatio(tier);
        BigDecimal sellAmount = userFund.getCurrentHolding().multiply(sellRatio);
        String reason = String.format("收益率达到%.2f%%，触发第%d档止盈",
            profitRate.multiply(new BigDecimal("100")), tier);

        return new SellResult(sellAmount, reason, "BATCH_PROFIT");
    }

    /**
     * 确定收益率档位
     */
    private int determineProfitTier(BigDecimal profitRate) {
        BigDecimal rate100 = profitRate.multiply(new BigDecimal("100"));
        if (rate100.compareTo(new BigDecimal("50")) >= 0) return 4;
        if (rate100.compareTo(new BigDecimal("40")) >= 0) return 3;
        if (rate100.compareTo(new BigDecimal("30")) >= 0) return 2;
        if (rate100.compareTo(new BigDecimal("20")) >= 0) return 1;
        return 0;
    }

    /**
     * 获取档位卖出比例
     */
    private BigDecimal getTierSellRatio(int tier) {
        switch (tier) {
            case 1: return new BigDecimal("0.30");
            case 2: return new BigDecimal("0.40");
            case 3: return new BigDecimal("0.50");
            case 4: return new BigDecimal("0.60");
            default: return BigDecimal.ZERO;
        }
    }

    /**
     * 检查档位是否已执行
     */
    private boolean isTierExecuted(Integer executedMask, int tier) {
        if (executedMask == null) return false;
        return (executedMask & (1 << (tier - 1))) != 0;
    }
    public SellResult checkMultiFactorSignal(Long userId, String fundCode) { return null; }
    public boolean validateCooldown(Long userId, String fundCode, String strategyType) { return true; }
}