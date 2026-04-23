package com.campus.trade.item.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.recommend")
public class RecommendProperties {

    private Behavior behavior = new Behavior();
    private Candidate candidate = new Candidate();
    private SimilarUser similarUser = new SimilarUser();
    private Rule rule = new Rule();
    private Weight weight = new Weight();
    private Cache cache = new Cache();
    private Schedule schedule = new Schedule();
    private List<String> coldStartCategoryKeywords = new ArrayList<>(List.of("教材", "电子产品", "生活用品"));

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }

    public Candidate getCandidate() {
        return candidate;
    }

    public void setCandidate(Candidate candidate) {
        this.candidate = candidate;
    }

    public SimilarUser getSimilarUser() {
        return similarUser;
    }

    public void setSimilarUser(SimilarUser similarUser) {
        this.similarUser = similarUser;
    }

    public Rule getRule() {
        return rule;
    }

    public void setRule(Rule rule) {
        this.rule = rule;
    }

    public Weight getWeight() {
        return weight;
    }

    public void setWeight(Weight weight) {
        this.weight = weight;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public List<String> getColdStartCategoryKeywords() {
        return coldStartCategoryKeywords;
    }

    public void setColdStartCategoryKeywords(List<String> coldStartCategoryKeywords) {
        this.coldStartCategoryKeywords = coldStartCategoryKeywords;
    }

    public static class Behavior {
        private int browseWeight = 1;
        private int favoriteWeight = 3;
        private int chatWeight = 4;
        private int tradeWeight = 6;
        private int positiveReviewBonus = 2;
        private int browseStrongThreshold = 2;
        private int coldStartThreshold = 3;
        private int recentBehaviorDays = 90;
        private int transactionWindowDays = 7;
        private int positiveReviewThreshold = 4;
        private int halfLifeDays = 30;

        public int getBrowseWeight() {
            return browseWeight;
        }

        public void setBrowseWeight(int browseWeight) {
            this.browseWeight = browseWeight;
        }

        public int getFavoriteWeight() {
            return favoriteWeight;
        }

        public void setFavoriteWeight(int favoriteWeight) {
            this.favoriteWeight = favoriteWeight;
        }

        public int getChatWeight() {
            return chatWeight;
        }

        public void setChatWeight(int chatWeight) {
            this.chatWeight = chatWeight;
        }

        public int getTradeWeight() {
            return tradeWeight;
        }

        public void setTradeWeight(int tradeWeight) {
            this.tradeWeight = tradeWeight;
        }

        public int getPositiveReviewBonus() {
            return positiveReviewBonus;
        }

        public void setPositiveReviewBonus(int positiveReviewBonus) {
            this.positiveReviewBonus = positiveReviewBonus;
        }

        public int getBrowseStrongThreshold() {
            return browseStrongThreshold;
        }

        public void setBrowseStrongThreshold(int browseStrongThreshold) {
            this.browseStrongThreshold = browseStrongThreshold;
        }

        public int getColdStartThreshold() {
            return coldStartThreshold;
        }

        public void setColdStartThreshold(int coldStartThreshold) {
            this.coldStartThreshold = coldStartThreshold;
        }

        public int getRecentBehaviorDays() {
            return recentBehaviorDays;
        }

        public void setRecentBehaviorDays(int recentBehaviorDays) {
            this.recentBehaviorDays = recentBehaviorDays;
        }

        public int getTransactionWindowDays() {
            return transactionWindowDays;
        }

        public void setTransactionWindowDays(int transactionWindowDays) {
            this.transactionWindowDays = transactionWindowDays;
        }

        public int getPositiveReviewThreshold() {
            return positiveReviewThreshold;
        }

        public void setPositiveReviewThreshold(int positiveReviewThreshold) {
            this.positiveReviewThreshold = positiveReviewThreshold;
        }

        public int getHalfLifeDays() {
            return halfLifeDays;
        }

        public void setHalfLifeDays(int halfLifeDays) {
            this.halfLifeDays = halfLifeDays;
        }
    }

    public static class Candidate {
        private int poolSize = 200;
        private int queryLimit = 60;
        private int hotFallbackCount = 40;
        private int excludeRecentBrowseCount = 20;
        private int minResultCount = 6;
        private boolean allowRepeatExposureFallback = true;

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public int getQueryLimit() {
            return queryLimit;
        }

        public void setQueryLimit(int queryLimit) {
            this.queryLimit = queryLimit;
        }

        public int getHotFallbackCount() {
            return hotFallbackCount;
        }

        public void setHotFallbackCount(int hotFallbackCount) {
            this.hotFallbackCount = hotFallbackCount;
        }

        public int getExcludeRecentBrowseCount() {
            return excludeRecentBrowseCount;
        }

        public void setExcludeRecentBrowseCount(int excludeRecentBrowseCount) {
            this.excludeRecentBrowseCount = excludeRecentBrowseCount;
        }

        public int getMinResultCount() {
            return minResultCount;
        }

        public void setMinResultCount(int minResultCount) {
            this.minResultCount = minResultCount;
        }

        public boolean isAllowRepeatExposureFallback() {
            return allowRepeatExposureFallback;
        }

        public void setAllowRepeatExposureFallback(boolean allowRepeatExposureFallback) {
            this.allowRepeatExposureFallback = allowRepeatExposureFallback;
        }
    }

    public static class SimilarUser {
        private int topK = 10;
        private int maxBehaviorItemsPerUser = 30;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public int getMaxBehaviorItemsPerUser() {
            return maxBehaviorItemsPerUser;
        }

        public void setMaxBehaviorItemsPerUser(int maxBehaviorItemsPerUser) {
            this.maxBehaviorItemsPerUser = maxBehaviorItemsPerUser;
        }
    }

    public static class Rule {
        private double minSupport = 0.02D;
        private double minConfidence = 0.15D;
        private double minLift = 1.05D;
        private int maxAntecedentItems = 3;
        private int maxRulesPerSeed = 30;

        public double getMinSupport() {
            return minSupport;
        }

        public void setMinSupport(double minSupport) {
            this.minSupport = minSupport;
        }

        public double getMinConfidence() {
            return minConfidence;
        }

        public void setMinConfidence(double minConfidence) {
            this.minConfidence = minConfidence;
        }

        public double getMinLift() {
            return minLift;
        }

        public void setMinLift(double minLift) {
            this.minLift = minLift;
        }

        public int getMaxAntecedentItems() {
            return maxAntecedentItems;
        }

        public void setMaxAntecedentItems(int maxAntecedentItems) {
            this.maxAntecedentItems = maxAntecedentItems;
        }

        public int getMaxRulesPerSeed() {
            return maxRulesPerSeed;
        }

        public void setMaxRulesPerSeed(int maxRulesPerSeed) {
            this.maxRulesPerSeed = maxRulesPerSeed;
        }
    }

    public static class Weight {
        private double profile = 0.35D;
        private double behaviorSimilarity = 0.25D;
        private double association = 0.20D;
        private double hotness = 0.10D;
        private double freshness = 0.05D;
        private double distance = 0.05D;

        public double getProfile() {
            return profile;
        }

        public void setProfile(double profile) {
            this.profile = profile;
        }

        public double getBehaviorSimilarity() {
            return behaviorSimilarity;
        }

        public void setBehaviorSimilarity(double behaviorSimilarity) {
            this.behaviorSimilarity = behaviorSimilarity;
        }

        public double getAssociation() {
            return association;
        }

        public void setAssociation(double association) {
            this.association = association;
        }

        public double getHotness() {
            return hotness;
        }

        public void setHotness(double hotness) {
            this.hotness = hotness;
        }

        public double getFreshness() {
            return freshness;
        }

        public void setFreshness(double freshness) {
            this.freshness = freshness;
        }

        public double getDistance() {
            return distance;
        }

        public void setDistance(double distance) {
            this.distance = distance;
        }
    }

    public static class Cache {
        private Duration userTtl = Duration.ofMinutes(5);
        private Duration similarTtl = Duration.ofMinutes(5);
        private Duration requestTtl = Duration.ofHours(6);
        private Duration hotTtl = Duration.ofHours(3);

        public Duration getUserTtl() {
            return userTtl;
        }

        public void setUserTtl(Duration userTtl) {
            this.userTtl = userTtl;
        }

        public Duration getSimilarTtl() {
            return similarTtl;
        }

        public void setSimilarTtl(Duration similarTtl) {
            this.similarTtl = similarTtl;
        }

        public Duration getRequestTtl() {
            return requestTtl;
        }

        public void setRequestTtl(Duration requestTtl) {
            this.requestTtl = requestTtl;
        }

        public Duration getHotTtl() {
            return hotTtl;
        }

        public void setHotTtl(Duration hotTtl) {
            this.hotTtl = hotTtl;
        }
    }

    public static class Schedule {
        private boolean rebuildProfilesOnStartup = false;
        private boolean rebuildRulesOnStartup = false;
        private boolean rebuildHotOnStartup = false;
        private Duration profileRefreshInterval = Duration.ofHours(6);
        private Duration ruleRefreshInterval = Duration.ofHours(12);
        private Duration hotRefreshInterval = Duration.ofHours(2);

        public boolean isRebuildProfilesOnStartup() {
            return rebuildProfilesOnStartup;
        }

        public void setRebuildProfilesOnStartup(boolean rebuildProfilesOnStartup) {
            this.rebuildProfilesOnStartup = rebuildProfilesOnStartup;
        }

        public boolean isRebuildRulesOnStartup() {
            return rebuildRulesOnStartup;
        }

        public void setRebuildRulesOnStartup(boolean rebuildRulesOnStartup) {
            this.rebuildRulesOnStartup = rebuildRulesOnStartup;
        }

        public boolean isRebuildHotOnStartup() {
            return rebuildHotOnStartup;
        }

        public void setRebuildHotOnStartup(boolean rebuildHotOnStartup) {
            this.rebuildHotOnStartup = rebuildHotOnStartup;
        }

        public Duration getProfileRefreshInterval() {
            return profileRefreshInterval;
        }

        public void setProfileRefreshInterval(Duration profileRefreshInterval) {
            this.profileRefreshInterval = profileRefreshInterval;
        }

        public Duration getRuleRefreshInterval() {
            return ruleRefreshInterval;
        }

        public void setRuleRefreshInterval(Duration ruleRefreshInterval) {
            this.ruleRefreshInterval = ruleRefreshInterval;
        }

        public Duration getHotRefreshInterval() {
            return hotRefreshInterval;
        }

        public void setHotRefreshInterval(Duration hotRefreshInterval) {
            this.hotRefreshInterval = hotRefreshInterval;
        }
    }
}
