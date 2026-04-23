package com.campus.trade.item.service;

import com.campus.trade.item.config.RecommendProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecommendationHotScheduler implements ApplicationRunner {

    private final RecommendationHotService recommendationHotService;
    private final RecommendProperties recommendProperties;

    public RecommendationHotScheduler(RecommendationHotService recommendationHotService,
                                      RecommendProperties recommendProperties) {
        this.recommendationHotService = recommendationHotService;
        this.recommendProperties = recommendProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (recommendProperties.getSchedule().isRebuildHotOnStartup()) {
            recommendationHotService.rebuildHotCaches();
        }
    }

    @Scheduled(fixedDelayString = "#{@recommendProperties.schedule.hotRefreshInterval.toMillis()}")
    public void refreshHotCaches() {
        recommendationHotService.rebuildHotCaches();
    }
}
