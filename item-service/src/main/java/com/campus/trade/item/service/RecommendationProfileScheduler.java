package com.campus.trade.item.service;

import com.campus.trade.item.config.RecommendProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecommendationProfileScheduler implements ApplicationRunner {

    private final RecommendationProfileService recommendationProfileService;
    private final RecommendProperties recommendProperties;

    public RecommendationProfileScheduler(RecommendationProfileService recommendationProfileService,
                                          RecommendProperties recommendProperties) {
        this.recommendationProfileService = recommendationProfileService;
        this.recommendProperties = recommendProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (recommendProperties.getSchedule().isRebuildProfilesOnStartup()) {
            recommendationProfileService.rebuildRecentProfiles();
        }
    }

    @Scheduled(fixedDelayString = "#{@recommendProperties.schedule.profileRefreshInterval.toMillis()}")
    public void refreshProfiles() {
        recommendationProfileService.rebuildRecentProfiles();
    }
}
