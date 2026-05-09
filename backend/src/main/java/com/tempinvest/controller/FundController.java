package com.tempinvest.controller;

import com.tempinvest.entity.FundInfo;
import com.tempinvest.mapper.FundInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fund")
@CrossOrigin
public class FundController {

    @Autowired
    private FundInfoMapper fundInfoMapper;

    @GetMapping("/list")
    public Map<String, Object> list() {
        List<FundInfo> funds = fundInfoMapper.selectAll();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", funds);
        return result;
    }

    @GetMapping("/{code}/detail")
    public Map<String, Object> detail(@PathVariable String code) {
        FundInfo fund = fundInfoMapper.selectByCode(code);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", fund);
        return result;
    }
}