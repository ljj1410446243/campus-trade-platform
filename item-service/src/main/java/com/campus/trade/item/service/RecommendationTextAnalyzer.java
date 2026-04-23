package com.campus.trade.item.service;

import com.campus.trade.item.model.Item;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RecommendationTextAnalyzer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHan}A-Za-z0-9]{2,}");

    private static final Map<String, String> KEYWORD_SYNONYMS = Map.ofEntries(
            Map.entry("教材", "图书教材"),
            Map.entry("书籍", "图书教材"),
            Map.entry("考研", "考研资料"),
            Map.entry("笔记本", "笔记本电脑"),
            Map.entry("电脑", "笔记本电脑"),
            Map.entry("手机", "手机数码"),
            Map.entry("耳机", "耳机音频"),
            Map.entry("平板", "平板设备"),
            Map.entry("宿舍", "宿舍用品"),
            Map.entry("日用", "生活用品")
    );

    private static final Set<String> STOP_WORDS = Set.of(
            "出售", "转让", "全新", "自用", "闲置", "包邮", "可刀", "急出", "一个", "两个",
            "非常", "真的", "这个", "那个", "支持", "需要", "同学", "学弟", "学妹", "校园",
            "二手", "商品", "物品", "交易", "当天", "联系", "价格", "面议"
    );

    public List<String> extractKeywords(Item item) {
        if (item == null) {
            return List.of();
        }
        return extractKeywords(item.getTitle(), item.getDescription(), item.getCategoryName());
    }

    public List<String> extractKeywords(String title, String description, String categoryName) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        collectKeywords(title, keywords);
        collectKeywords(description, keywords);
        collectKeywords(categoryName, keywords);
        return new ArrayList<>(keywords);
    }

    private void collectKeywords(String text, Set<String> keywords) {
        if (text == null || text.isBlank()) {
            return;
        }
        String normalizedText = text.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : KEYWORD_SYNONYMS.entrySet()) {
            if (normalizedText.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                keywords.add(entry.getValue());
            }
        }
        Matcher matcher = TOKEN_PATTERN.matcher(normalizedText);
        while (matcher.find()) {
            String token = normalizeKeyword(matcher.group());
            if (token != null) {
                keywords.add(token);
            }
        }
    }

    public String normalizeKeyword(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() < 2 || STOP_WORDS.contains(normalized)) {
            return null;
        }
        for (Map.Entry<String, String> entry : KEYWORD_SYNONYMS.entrySet()) {
            if (normalized.contains(entry.getKey().toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }
        return normalized;
    }

    public String resolvePriceBucket(BigDecimal price) {
        if (price == null) {
            return null;
        }
        if (price.compareTo(BigDecimal.valueOf(50)) <= 0) {
            return "0-50";
        }
        if (price.compareTo(BigDecimal.valueOf(100)) <= 0) {
            return "50-100";
        }
        if (price.compareTo(BigDecimal.valueOf(300)) <= 0) {
            return "100-300";
        }
        if (price.compareTo(BigDecimal.valueOf(800)) <= 0) {
            return "300-800";
        }
        return "800+";
    }

    public double computeDecay(Date occurredAt, int halfLifeDays) {
        if (occurredAt == null) {
            return 1D;
        }
        long days = Math.max(0, Duration.between(occurredAt.toInstant(), Instant.now()).toDays());
        return computeDecay(days, halfLifeDays);
    }

    public double computeDecay(LocalDateTime occurredAt, int halfLifeDays) {
        if (occurredAt == null) {
            return 1D;
        }
        Instant instant = occurredAt.atZone(ZoneId.systemDefault()).toInstant();
        long days = Math.max(0, Duration.between(instant, Instant.now()).toDays());
        return computeDecay(days, halfLifeDays);
    }

    private double computeDecay(long days, int halfLifeDays) {
        int safeHalfLife = Math.max(1, halfLifeDays);
        double lambda = Math.log(2D) / safeHalfLife;
        return Math.exp(-lambda * days);
    }

    public <T> List<Map.Entry<T, Double>> topEntries(Map<T, Double> source, int limit) {
        return source.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0D)
                .sorted((left, right) -> Double.compare(right.getValue(), left.getValue()))
                .limit(limit)
                .toList();
    }

    public Map<String, Double> toStringWeightMap(Collection<Map.Entry<String, Double>> entries) {
        Map<String, Double> result = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : entries) {
            result.put(entry.getKey(), round(entry.getValue()));
        }
        return result;
    }

    public double round(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}
