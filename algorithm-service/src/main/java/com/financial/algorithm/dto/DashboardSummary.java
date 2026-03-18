package com.financial.algorithm.dto;

import com.financial.algorithm.entity.RiskRecord;

import java.time.LocalDateTime;
import java.util.Map;

public record DashboardSummary(
        int totalAssets,
        long totalSimilarityPairs,
        LocalDateTime lastAnalysisAt,
        Map<String, Long> riskDistribution,
        RiskRecord topRiskAsset,
        RiskRecord lowestRiskAsset
) {}
