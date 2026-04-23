package com.campus.trade.item.service;

import com.campus.trade.item.config.RecommendProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecommendationRuleScheduler implements ApplicationRunner {

    private final RecommendationRuleService recommendationRuleService;
    private final RecommendProperties recommendProperties;

    public RecommendationRuleScheduler(RecommendationRuleService recommendationRuleService,
                                       RecommendProperties recommendProperties) {
        this.recommendationRuleService = recommendationRuleService;
        this.recommendProperties = recommendProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (recommendProperties.getSchedule().isRebuildRulesOnStartup()) {
            recommendationRuleService.rebuildRules();
        }
    }

    @Scheduled(fixedDelayString = "#{@recommendProperties.schedule.ruleRefreshInterval.toMillis()}")
    public void refreshRules() {
        recommendationRuleService.rebuildRules();
    }
}
