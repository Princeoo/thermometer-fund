package com.tempinvest.service;

import com.tempinvest.entity.FundInfo;
import com.tempinvest.entity.UserFund;
import com.tempinvest.mapper.FundInfoMapper;
import com.tempinvest.mapper.UserFundMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FundDataUpdateService {

    @Autowired
    private FundDataFetchService fundDataFetchService;

    @Autowired
    private UserFundMapper userFundMapper;

    @Autowired
    private FundInfoMapper fundInfoMapper;

    /**
     * 每日收盘后更新数据
     * 工作日15:30执行
     */
    @Scheduled(cron = "0 30 15 ? * MON-FRI")
    public void updateDailyData() {
        // 1. 抓取基金净值数据
        List<FundInfo> funds = fundDataFetchService.fetchFundNetValues();

        // 2. 更新 fund_info 表
        fundDataFetchService.batchUpdateFunds(funds);

        // 3. 更新用户持仓的最高净值（移动止盈用）
        updateUserHighestPrices();
    }

    /**
     * 更新用户持仓的最高净值
     */
    private void updateUserHighestPrices() {
        List<UserFund> allHoldings = userFundMapper.selectAllActive();

        for (UserFund holding : allHoldings) {
            if (holding.getTrailingStopActive() != null && holding.getTrailingStopActive() == 1) {
                FundInfo fund = fundInfoMapper.selectByCode(holding.getFundCode());
                if (fund != null && fund.getNetValue() != null) {
                    if (holding.getHighestPrice() == null ||
                        fund.getNetValue().compareTo(holding.getHighestPrice()) > 0) {
                        holding.setHighestPrice(fund.getNetValue());
                        userFundMapper.updateById(holding);
                    }
                }
            }
        }
    }
}