package com.tempinvest.controller;

import com.tempinvest.dto.InvestSuggestion;
import com.tempinvest.service.InvestDecisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/decision")
@CrossOrigin
public class DecisionController {

    @Autowired
    private InvestDecisionService investDecisionService;

    @GetMapping("/{fundCode}")
    public Map<String, Object> getSuggestion(@PathVariable String fundCode) {
        // 暂时使用userId=1L（单用户模式）
        InvestSuggestion suggestion = investDecisionService.calculateSuggestion(1L, fundCode);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", suggestion);
        return result;
    }
}