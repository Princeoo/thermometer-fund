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
import java.time.temporal.ChronoUnit;

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
    /**
     * 检查多因子信号
     */
    public SellResult checkMultiFactorSignal(Long userId, String fundCode) {
        UserFund userFund = userFundMapper.selectByUserIdAndCode(userId, fundCode);
        FundInfo fundInfo = fundInfoMapper.selectByCode(fundCode);

        if (userFund == null || fundInfo == null) {
            return null;
        }

        // 计算各因子得分
        int temperatureScore = calculateTemperatureFactor(fundInfo.getTemperature());
        int trendScore = calculateTrendFactor(fundInfo);
        int holdingScore = calculateHoldingFactor(userFund, fundInfo);
        int sentimentScore = calculateSentimentFactor(fundInfo);

        // 加权求和
        int totalScore = (int)(temperatureScore * 0.4 + trendScore * 0.3 +
                               holdingScore * 0.2 + sentimentScore * 0.1);

        // 判断是否触发
        if (totalScore >= 70) {
            BigDecimal sellAmount = userFund.getCurrentHolding().multiply(new BigDecimal("0.4"));
            String reason = String.format("多因子信号强烈：得分%d（温度%d+趋势%d+持仓%d+情绪%d）",
                totalScore, temperatureScore, trendScore, holdingScore, sentimentScore);
            return new SellResult(sellAmount, reason, "MULTI_FACTOR");
        } else if (totalScore >= 50) {
            BigDecimal sellAmount = userFund.getCurrentHolding().multiply(new BigDecimal("0.2"));
            String reason = String.format("多因子信号中等：得分%d", totalScore);
            return new SellResult(sellAmount, reason, "MULTI_FACTOR");
        }

        return null;
    }

    /**
     * 验证冷却期
     */
    public boolean validateCooldown(Long userId, String fundCode, String strategyType) {
        UserFund userFund = userFundMapper.selectByUserIdAndCode(userId, fundCode);
        if (userFund == null) return false;

        if (userFund.getLastSellDate() == null) {
            return true;
        }

        long daysSinceLastSell = ChronoUnit.DAYS.between(
            userFund.getLastSellDate().atStartOfDay(),
            java.time.LocalDate.now().atStartOfDay()
        );

        int cooldownDays = userFund.getSellCooldownDays() != null ? userFund.getSellCooldownDays() : 7;
        if ("TRAILING_STOP".equals(strategyType)) {
            cooldownDays = cooldownDays / 2;
        }

        FundInfo fundInfo = fundInfoMapper.selectByCode(fundCode);
        if (fundInfo != null && fundInfo.getTemperature() != null && fundInfo.getTemperature() > 95) {
            return true;
        }

        return daysSinceLastSell >= cooldownDays;
    }

    /**
     * 温度因子计算
     */
    private int calculateTemperatureFactor(Integer temperature) {
        if (temperature == null) return 0;
        if (temperature > 90) return 100;
        if (temperature > 80) return 80;
        if (temperature > 70) return 60;
        if (temperature > 60) return 40;
        return 0;
    }

    /**
     * 趋势因子计算
     */
    private int calculateTrendFactor(FundInfo fundInfo) {
        if (fundInfo.getDailyChange() == null) return 0;
        BigDecimal change = fundInfo.getDailyChange();
        if (change.compareTo(new BigDecimal("3")) > 0) return 40;
        if (change.compareTo(new BigDecimal("2")) > 0) return 30;
        if (change.compareTo(new BigDecimal("-2")) < 0) return -30;
        return 0;
    }

    /**
     * 持仓因子计算
     */
    private int calculateHoldingFactor(UserFund userFund, FundInfo fundInfo) {
        BigDecimal profitRate = calculateProfitRate(userFund, fundInfo);
        if (profitRate == null) return 0;
        BigDecimal rate100 = profitRate.multiply(new BigDecimal("100"));
        if (rate100.compareTo(new BigDecimal("30")) > 0) return 100;
        if (rate100.compareTo(new BigDecimal("20")) > 0) return 80;
        if (rate100.compareTo(new BigDecimal("10")) > 0) return 50;
        return 0;
    }

    /**
     * 情绪因子计算
     */
    private int calculateSentimentFactor(FundInfo fundInfo) {
        if (fundInfo.getPePercentile() == null || fundInfo.getPbPercentile() == null) return 0;
        if (fundInfo.getPePercentile().compareTo(new BigDecimal("90")) > 0 &&
            fundInfo.getPbPercentile().compareTo(new BigDecimal("80")) > 0) {
            return 20;
        }
        if (fundInfo.getPePercentile().compareTo(new BigDecimal("30")) < 0 &&
            fundInfo.getPbPercentile().compareTo(new BigDecimal("30")) < 0) {
            return -20;
        }
        return 0;
    }
}