package com.tempinvest.service;

import com.tempinvest.dto.InvestSuggestion;
import com.tempinvest.dto.SellResult;
import com.tempinvest.entity.FundInfo;
import com.tempinvest.entity.UserFund;
import com.tempinvest.mapper.FundInfoMapper;
import com.tempinvest.mapper.UserFundMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class InvestDecisionService {

    @Autowired
    private SellStrategyManager sellStrategyManager;

    @Autowired
    private FundInfoMapper fundInfoMapper;

    @Autowired
    private UserFundMapper userFundMapper;

    public InvestSuggestion calculateSuggestion(Long userId, String fundCode) {
        FundInfo fundInfo = fundInfoMapper.selectByCode(fundCode);
        UserFund userFund = userFundMapper.selectByUserIdAndCode(userId, fundCode);

        if (fundInfo == null) {
            return null;
        }

        InvestSuggestion suggestion = new InvestSuggestion();
        suggestion.setFundCode(fundCode);
        suggestion.setFundName(fundInfo.getFundName());
        suggestion.setTemperature(fundInfo.getTemperature());

        // 如果用户未设置持仓，仅返回估值信息
        if (userFund == null) {
            suggestion.setAction("HOLD");
            suggestion.setReason("请先添加持仓配置");
            return suggestion;
        }

        // 计算估值系数
        BigDecimal coefficient = calculateValuationCoefficient(fundInfo.getTemperature());

        // 若系数为负，进入卖出流程
        if (coefficient.compareTo(BigDecimal.ZERO) < 0) {
            SellResult sellResult = executeSellStrategies(userId, fundCode);

            if (sellResult != null) {
                suggestion.setSuggestedAmount(sellResult.getSellAmount().negate());
                suggestion.setAction("SELL");
                suggestion.setReason(sellResult.getReason());
                suggestion.setSellStrategy(sellResult.getStrategyType());
                suggestion.setFactors(new ArrayList<>());
                suggestion.getFactors().add(sellResult.getReason());
                return suggestion;
            } else {
                suggestion.setAction("HOLD");
                suggestion.setReason("暂无卖出信号或处于冷却期");
                return suggestion;
            }
        }

        // 买入逻辑
        BigDecimal suggestedAmount = calculateBuyAmount(userFund, fundInfo, coefficient);
        suggestion.setSuggestedAmount(suggestedAmount);
        suggestion.setAction("BUY");
        suggestion.setReason(generateBuyReason(fundInfo, coefficient));
        suggestion.setFactors(generateBuyFactors(fundInfo, coefficient));

        return suggestion;
    }

    private SellResult executeSellStrategies(Long userId, String fundCode) {
        SellResult result;

        // 优先级1：移动止盈
        result = sellStrategyManager.checkTrailingStop(userId, fundCode);
        if (result != null && sellStrategyManager.validateCooldown(userId, fundCode, "TRAILING_STOP")) {
            return result;
        }

        // 优先级2：分批止盈
        result = sellStrategyManager.checkBatchProfit(userId, fundCode);
        if (result != null && sellStrategyManager.validateCooldown(userId, fundCode, "BATCH_PROFIT")) {
            return result;
        }

        // 优先级3：多因子信号
        result = sellStrategyManager.checkMultiFactorSignal(userId, fundCode);
        if (result != null && sellStrategyManager.validateCooldown(userId, fundCode, "MULTI_FACTOR")) {
            return result;
        }

        // 优先级4：温度阈值
        result = checkTemperatureSignal(userId, fundCode);
        if (result != null && sellStrategyManager.validateCooldown(userId, fundCode, "TEMPERATURE")) {
            return result;
        }

        return null;
    }

    private SellResult checkTemperatureSignal(Long userId, String fundCode) {
        FundInfo fundInfo = fundInfoMapper.selectByCode(fundCode);
        UserFund userFund = userFundMapper.selectByUserIdAndCode(userId, fundCode);

        if (fundInfo == null || userFund == null) return null;

        Integer temperature = fundInfo.getTemperature();
        if (temperature == null) return null;

        if (temperature > 95) {
            BigDecimal sellAmount = userFund.getCurrentHolding().multiply(new BigDecimal("0.5"));
            return new SellResult(sellAmount, "极度高估：温度" + temperature + "℃", "TEMPERATURE");
        } else if (temperature > 85) {
            BigDecimal sellAmount = userFund.getCurrentHolding().multiply(new BigDecimal("0.2"));
            return new SellResult(sellAmount, "高估：温度" + temperature + "℃", "TEMPERATURE");
        }

        return null;
    }

    private BigDecimal calculateValuationCoefficient(Integer temperature) {
        if (temperature == null) return BigDecimal.ONE;

        if (temperature <= 20) return new BigDecimal("1.5");
        if (temperature <= 35) return new BigDecimal("1.3");
        if (temperature <= 50) return BigDecimal.ONE;
        if (temperature <= 70) return new BigDecimal("0.6");
        if (temperature <= 85) return new BigDecimal("0.2");
        return new BigDecimal("-0.3");
    }

    private BigDecimal calculateBuyAmount(UserFund userFund, FundInfo fundInfo, BigDecimal coefficient) {
        BigDecimal monthlyAmount = userFund.getBaseMonthlyAmount();
        if (monthlyAmount == null) return BigDecimal.ZERO;

        return monthlyAmount.multiply(coefficient).setScale(2, RoundingMode.HALF_UP);
    }

    private String generateBuyReason(FundInfo fundInfo, BigDecimal coefficient) {
        return String.format("当前温度%d℃，建议系数%.2f", fundInfo.getTemperature(), coefficient);
    }

    private List<String> generateBuyFactors(FundInfo fundInfo, BigDecimal coefficient) {
        List<String> factors = new ArrayList<>();
        factors.add(String.format("温度：%d℃", fundInfo.getTemperature()));
        factors.add(String.format("估值系数：%.2f", coefficient));
        return factors;
    }
}