package com.tempinvest.service;

import com.tempinvest.entity.FundInfo;
import com.tempinvest.mapper.FundInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class FundDataFetchService {

    @Autowired
    private FundInfoMapper fundInfoMapper;

    /**
     * 抓取基金净值数据（模拟实现）
     * 实际项目中应调用天天基金/蛋卷基金API
     */
    public List<FundInfo> fetchFundNetValues() {
        List<FundInfo> funds = fundInfoMapper.selectAll();
        List<FundInfo> updated = new ArrayList<>();

        for (FundInfo fund : funds) {
            // 模拟数据更新
            // 实际应调用API获取真实数据
            fund.setNetValue(generateMockNetValue(fund.getFundCode()));
            fund.setNetValueDate(java.time.LocalDate.now());
            fund.setUpdateTime(LocalDateTime.now());
            updated.add(fund);
        }

        return updated;
    }

    /**
     * 模拟净值数据
     */
    private BigDecimal generateMockNetValue(String fundCode) {
        // 模拟不同基金的净值
        switch (fundCode) {
            case "510300": return new BigDecimal("4.1234");
            case "510500": return new BigDecimal("7.5678");
            case "159915": return new BigDecimal("0.9876");
            case "513180": return new BigDecimal("1.2345");
            case "513100": return new BigDecimal("2.3456");
            default: return new BigDecimal("1.0000");
        }
    }

    /**
     * 批量更新基金信息
     */
    public void batchUpdateFunds(List<FundInfo> funds) {
        for (FundInfo fund : funds) {
            fundInfoMapper.updateById(fund);
        }
    }
}