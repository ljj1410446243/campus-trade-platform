package com.campus.trade.user.service.impl;

import com.campus.trade.user.client.NoticeServiceClient;
import com.campus.trade.user.dto.BanUserRequest;
import com.campus.trade.user.dto.CreateReportRequest;
import com.campus.trade.user.dto.InternalNotificationRequest;
import com.campus.trade.user.dto.ReportItemResponse;
import com.campus.trade.user.dto.ReviewReportRequest;
import com.campus.trade.user.exception.BusinessException;
import com.campus.trade.user.model.Item;
import com.campus.trade.user.model.Report;
import com.campus.trade.user.model.User;
import com.campus.trade.user.repository.ItemRepository;
import com.campus.trade.user.repository.ReportRepository;
import com.campus.trade.user.repository.UserRepository;
import com.campus.trade.user.service.ItemCacheInvalidationService;
import com.campus.trade.user.service.ReportService;
import com.campus.trade.user.util.UserAccessGuard;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ReportServiceImpl implements ReportService {

    private static final String REPORT_TYPE_ITEM = "ITEM";
    private static final String REPORT_TYPE_USER = "USER";
    private static final String REPORT_STATUS_PENDING = "PENDING";
    private static final String REPORT_STATUS_APPROVED = "APPROVED";
    private static final String REPORT_STATUS_REJECTED = "REJECTED";
    private static final String ITEM_STATUS_ON_SALE = "ON_SALE";
    private static final String ITEM_STATUS_OFF_SHELF = "OFF_SHELF";
    private static final String NOTICE_CATEGORY_REPORT_RESULT = "REPORT_RESULT";
    private static final String NOTICE_TITLE_REPORT_RESULT = "举报处理结果通知";
    private static final String ACTION_TYPE_MY_REPORTS = "MY_REPORTS";
    private static final String ACTION_TYPE_ITEM_DETAIL = "ITEM_DETAIL";

    private static final Set<String> ITEM_REASON_CODES = Set.of("ILLEGAL", "PORN", "SCAM", "SPAM", "OTHER");
    private static final Set<String> USER_REASON_CODES = Set.of("FRAUD", "HARASSMENT", "IMPERSONATION", "SPAM", "OTHER");
    private static final Map<String, String> ITEM_REASON_LABELS = Map.of(
            "ILLEGAL", "违法违规",
            "PORN", "色情低俗",
            "SCAM", "诈骗欺诈",
            "SPAM", "垃圾信息",
            "OTHER", "其他"
    );
    private static final Map<String, String> USER_REASON_LABELS = Map.of(
            "FRAUD", "欺诈",
            "HARASSMENT", "骚扰",
            "IMPERSONATION", "冒充",
            "SPAM", "垃圾信息",
            "OTHER", "其他"
    );

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final UserAccessGuard userAccessGuard;
    private final ItemCacheInvalidationService itemCacheInvalidationService;
    private final MongoTemplate mongoTemplate;
    private final NoticeServiceClient noticeServiceClient;

    public ReportServiceImpl(ReportRepository reportRepository,
                             UserRepository userRepository,
                             ItemRepository itemRepository,
                             UserAccessGuard userAccessGuard,
                             ItemCacheInvalidationService itemCacheInvalidationService,
                             MongoTemplate mongoTemplate,
                             NoticeServiceClient noticeServiceClient) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.userAccessGuard = userAccessGuard;
        this.itemCacheInvalidationService = itemCacheInvalidationService;
        this.mongoTemplate = mongoTemplate;
        this.noticeServiceClient = noticeServiceClient;
    }

    @Override
    public String createReport(String reporterId, CreateReportRequest request) {
        userAccessGuard.assertWritable(reporterId);

        String reportType = normalizeReportType(request.getReportType());
        String targetId = requireText(request.getTargetId(), "targetId不能为空");
        validateReasonCode(reportType, request.getReasonCode());
        ensureTargetExists(reportType, targetId);
        ensureNotSelfReport(reportType, reporterId, targetId);

        if (reportRepository.existsByReporterIdAndReportTypeAndTargetIdAndStatus(
                reporterId, reportType, targetId, REPORT_STATUS_PENDING)) {
            throw new BusinessException(409, "请勿重复提交待处理举报");
        }

        Date now = new Date();
        Report report = new Report();
        report.setReportType(reportType);
        report.setTargetId(targetId);
        report.setReporterId(reporterId);
        report.setReasonCode(normalizeReasonCode(request.getReasonCode()));
        report.setDescription(trimToNull(request.getDescription()));
        report.setStatus(REPORT_STATUS_PENDING);
        report.setCreatedAt(now);
        report.setUpdatedAt(now);
        return reportRepository.save(report).getId();
    }

    @Override
    public List<ReportItemResponse> listMyReports(String reporterId) {
        userAccessGuard.requireUser(reporterId);
        List<Report> reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId);
        return toResponses(reports);
    }

    @Override
    public List<ReportItemResponse> listReports(String adminUserId, String status) {
        userAccessGuard.assertAdmin(adminUserId);
        List<Report> reports;
        if (status == null || status.isBlank()) {
            reports = reportRepository.findAllByOrderByCreatedAtDesc();
        } else {
            reports = reportRepository.findByStatusOrderByCreatedAtDesc(normalizeReportStatus(status));
        }
        return toResponses(reports);
    }

    @Override
    public ReportItemResponse getReportDetail(String adminUserId, String reportId) {
        userAccessGuard.assertAdmin(adminUserId);
        return toResponses(List.of(getReportOrThrow(reportId))).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(404, "举报不存在"));
    }

    @Override
    public ReportItemResponse approveReport(String adminUserId, String reportId, ReviewReportRequest request) {
        userAccessGuard.assertAdmin(adminUserId);

        Report report = getPendingReportOrThrow(reportId);
        Date now = new Date();
        if (REPORT_TYPE_ITEM.equals(report.getReportType())) {
            offShelfItemInternal(report.getTargetId(), now);
        } else if (REPORT_TYPE_USER.equals(report.getReportType())) {
            banUserInternal(report.getTargetId(), trimToNull(request == null ? null : request.getReviewNote()), now);
        }

        report.setStatus(REPORT_STATUS_APPROVED);
        report.setReviewedBy(adminUserId);
        report.setReviewedAt(now);
        report.setReviewNote(trimToNull(request == null ? null : request.getReviewNote()));
        report.setUpdatedAt(now);
        reportRepository.save(report);
        createReviewNotifications(report);
        return getReportDetail(adminUserId, reportId);
    }

    @Override
    public ReportItemResponse rejectReport(String adminUserId, String reportId, ReviewReportRequest request) {
        userAccessGuard.assertAdmin(adminUserId);

        Report report = getPendingReportOrThrow(reportId);
        Date now = new Date();
        report.setStatus(REPORT_STATUS_REJECTED);
        report.setReviewedBy(adminUserId);
        report.setReviewedAt(now);
        report.setReviewNote(trimToNull(request == null ? null : request.getReviewNote()));
        report.setUpdatedAt(now);
        reportRepository.save(report);
        createReviewNotifications(report);
        return getReportDetail(adminUserId, reportId);
    }

    @Override
    public void banUser(String adminUserId, String targetUserId, BanUserRequest request) {
        userAccessGuard.assertAdmin(adminUserId);
        banUserInternal(requireText(targetUserId, "targetUserId不能为空"), trimToNull(request == null ? null : request.getReason()), new Date());
    }

    @Override
    public void unbanUser(String adminUserId, String targetUserId) {
        userAccessGuard.assertAdmin(adminUserId);
        User user = userAccessGuard.requireUser(requireText(targetUserId, "targetUserId不能为空"));
        user.setStatus(UserAccessGuard.STATUS_ACTIVE);
        user.setBannedAt(null);
        user.setBannedReason(null);
        userRepository.save(user);
    }

    @Override
    public void offShelfItem(String adminUserId, String itemId) {
        userAccessGuard.assertAdmin(adminUserId);
        offShelfItemInternal(requireText(itemId, "itemId不能为空"), new Date());
    }

    private void offShelfItemInternal(String itemId, Date now) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(404, "商品不存在"));
        if (ITEM_STATUS_ON_SALE.equals(item.getStatus())) {
            mongoTemplate.updateFirst(
                    Query.query(Criteria.where("_id").is(itemId).and("status").is(ITEM_STATUS_ON_SALE)),
                    new Update()
                            .set("status", ITEM_STATUS_OFF_SHELF)
                            .set("updatedAt", now),
                    "items"
            );
        }
        itemCacheInvalidationService.evictItemCaches(itemId);
    }

    private void banUserInternal(String targetUserId, String reason, Date now) {
        User user = userAccessGuard.requireUser(targetUserId);
        user.setStatus(UserAccessGuard.STATUS_BANNED);
        user.setBannedAt(now);
        user.setBannedReason(reason);
        userRepository.save(user);
    }

    private void ensureTargetExists(String reportType, String targetId) {
        if (REPORT_TYPE_ITEM.equals(reportType)) {
            if (!itemRepository.existsById(targetId)) {
                throw new BusinessException(404, "商品不存在");
            }
            return;
        }
        if (!userRepository.existsById(targetId)) {
            throw new BusinessException(404, "用户不存在");
        }
    }

    private void ensureNotSelfReport(String reportType, String reporterId, String targetId) {
        if (REPORT_TYPE_USER.equals(reportType) && reporterId.equals(targetId)) {
            throw new BusinessException(400, "不能举报自己");
        }
        if (REPORT_TYPE_ITEM.equals(reportType)) {
            itemRepository.findById(targetId).ifPresent(item -> {
                if (reporterId.equals(item.getSellerId())) {
                    throw new BusinessException(400, "不能举报自己发布的商品");
                }
            });
        }
    }

    private void validateReasonCode(String reportType, String reasonCode) {
        String normalizedReasonCode = normalizeReasonCode(reasonCode);
        Set<String> allowedReasonCodes = REPORT_TYPE_ITEM.equals(reportType) ? ITEM_REASON_CODES : USER_REASON_CODES;
        if (!allowedReasonCodes.contains(normalizedReasonCode)) {
            throw new BusinessException(400, "reasonCode不支持");
        }
    }

    private String normalizeReportType(String reportType) {
        String normalized = requireText(reportType, "reportType不能为空").toUpperCase(Locale.ROOT);
        if (!Arrays.asList(REPORT_TYPE_ITEM, REPORT_TYPE_USER).contains(normalized)) {
            throw new BusinessException(400, "reportType仅支持ITEM或USER");
        }
        return normalized;
    }

    private String normalizeReportStatus(String status) {
        String normalized = requireText(status, "status不能为空").toUpperCase(Locale.ROOT);
        if (!Arrays.asList(REPORT_STATUS_PENDING, REPORT_STATUS_APPROVED, REPORT_STATUS_REJECTED).contains(normalized)) {
            throw new BusinessException(400, "status不支持");
        }
        return normalized;
    }

    private String normalizeReasonCode(String reasonCode) {
        return requireText(reasonCode, "reasonCode不能为空").toUpperCase(Locale.ROOT);
    }

    private void createReviewNotifications(Report report) {
        noticeServiceClient.createNotification(buildReporterNotification(report));

        if (REPORT_STATUS_APPROVED.equals(report.getStatus())) {
            InternalNotificationRequest punishedNotification = buildPunishedNotification(report);
            if (punishedNotification != null) {
                noticeServiceClient.createNotification(punishedNotification);
            }
        }
    }

    private InternalNotificationRequest buildReporterNotification(Report report) {
        InternalNotificationRequest request = new InternalNotificationRequest();
        request.setRecipientUserId(report.getReporterId());
        request.setCategory(NOTICE_CATEGORY_REPORT_RESULT);
        request.setTitle(NOTICE_TITLE_REPORT_RESULT);
        request.setActionType(ACTION_TYPE_MY_REPORTS);
        request.setActionTargetId(report.getId());
        request.setContent(buildReporterContent(report));
        request.setExtra(buildExtra(report));
        return request;
    }

    private InternalNotificationRequest buildPunishedNotification(Report report) {
        if (REPORT_TYPE_ITEM.equals(report.getReportType())) {
            Item item = itemRepository.findById(report.getTargetId()).orElse(null);
            String sellerId = item == null ? null : trimToNull(item.getSellerId());
            if (sellerId == null) {
                return null;
            }

            InternalNotificationRequest request = new InternalNotificationRequest();
            request.setRecipientUserId(sellerId);
            request.setCategory(NOTICE_CATEGORY_REPORT_RESULT);
            request.setTitle(NOTICE_TITLE_REPORT_RESULT);
            request.setActionType(ACTION_TYPE_ITEM_DETAIL);
            request.setActionTargetId(report.getTargetId());
            request.setContent(buildPunishedItemContent(report, item));
            request.setExtra(buildExtra(report));
            return request;
        }

        InternalNotificationRequest request = new InternalNotificationRequest();
        request.setRecipientUserId(report.getTargetId());
        request.setCategory(NOTICE_CATEGORY_REPORT_RESULT);
        request.setTitle(NOTICE_TITLE_REPORT_RESULT);
        request.setActionType(ACTION_TYPE_MY_REPORTS);
        request.setActionTargetId(report.getId());
        request.setContent(buildPunishedUserContent(report));
        request.setExtra(buildExtra(report));
        return request;
    }

    private String buildReporterContent(Report report) {
        String subject = REPORT_TYPE_ITEM.equals(report.getReportType())
                ? "《" + resolveItemTitle(report) + "》"
                : "用户 " + resolveTargetName(report);

        String base;
        if (REPORT_STATUS_APPROVED.equals(report.getStatus())) {
            String resultText = REPORT_TYPE_ITEM.equals(report.getReportType()) ? "商品已下架。" : "用户已封禁。";
            base = "你提交的关于" + subject + "的举报已审核通过。处理结果：" + resultText;
        } else {
            base = "你提交的关于" + subject + "的举报未通过审核。";
        }
        return appendReviewNote(base, report.getReviewNote());
    }

    private String buildPunishedItemContent(Report report, Item item) {
        String itemTitle = resolveItemTitle(item, report.getTargetId());
        String content = "你发布的商品《" + itemTitle + "》因“" + resolveReasonLabel(report)
                + "”举报审核成立，已被下架。请修改后重新发布。";
        return appendReviewNote(content, report.getReviewNote());
    }

    private String buildPunishedUserContent(Report report) {
        String content = "关于你的举报因“" + resolveReasonLabel(report)
                + "”审核成立，当前已对账号执行封禁处理。请整改后联系管理员。";
        return appendReviewNote(content, report.getReviewNote());
    }

    private String appendReviewNote(String base, String reviewNote) {
        String normalizedNote = trimToNull(reviewNote);
        if (normalizedNote == null) {
            return base;
        }
        return base + " 审核备注：" + normalizedNote;
    }

    private Map<String, Object> buildExtra(Report report) {
        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("reportId", report.getId());
        extra.put("reportType", report.getReportType());
        extra.put("reasonCode", report.getReasonCode());
        extra.put("reasonLabel", resolveReasonLabel(report));
        extra.put("reviewResult", report.getStatus());
        extra.put("reviewNote", report.getReviewNote());
        extra.put("targetName", resolveTargetName(report));

        if (REPORT_TYPE_ITEM.equals(report.getReportType())) {
            extra.put("itemId", report.getTargetId());
            extra.put("itemTitle", resolveItemTitle(report));
        } else {
            extra.put("itemId", null);
            extra.put("itemTitle", null);
        }
        return extra;
    }

    private String resolveReasonLabel(Report report) {
        Map<String, String> labels = REPORT_TYPE_ITEM.equals(report.getReportType()) ? ITEM_REASON_LABELS : USER_REASON_LABELS;
        return labels.getOrDefault(report.getReasonCode(), report.getReasonCode());
    }

    private String resolveTargetName(Report report) {
        if (REPORT_TYPE_ITEM.equals(report.getReportType())) {
            Item item = itemRepository.findById(report.getTargetId()).orElse(null);
            String sellerId = item == null ? null : trimToNull(item.getSellerId());
            if (sellerId == null) {
                return report.getTargetId();
            }
            return resolveUserDisplayName(userRepository.findById(sellerId).orElse(null), sellerId);
        }
        return resolveUserDisplayName(userRepository.findById(report.getTargetId()).orElse(null), report.getTargetId());
    }

    private String resolveItemTitle(Report report) {
        return resolveItemTitle(itemRepository.findById(report.getTargetId()).orElse(null), report.getTargetId());
    }

    private String resolveItemTitle(Item item, String fallbackId) {
        if (item != null && trimToNull(item.getTitle()) != null) {
            return item.getTitle().trim();
        }
        return fallbackId;
    }

    private String resolveUserDisplayName(User user, String fallbackId) {
        if (user == null) {
            return fallbackId;
        }
        if (trimToNull(user.getNickname()) != null) {
            return user.getNickname().trim();
        }
        if (trimToNull(user.getUsername()) != null) {
            return user.getUsername().trim();
        }
        if (trimToNull(user.getRealName()) != null) {
            return user.getRealName().trim();
        }
        return fallbackId;
    }

    private Report getReportOrThrow(String reportId) {
        return reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(404, "举报不存在"));
    }

    private Report getPendingReportOrThrow(String reportId) {
        return reportRepository.findByIdAndStatus(reportId, REPORT_STATUS_PENDING)
                .orElseThrow(() -> new BusinessException(409, "举报已处理或不存在"));
    }

    private String requireText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new BusinessException(400, message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<ReportItemResponse> toResponses(List<Report> reports) {
        Map<String, Item> itemMap = buildItemMap(reports);
        Map<String, User> userMap = buildUserMap(reports, itemMap);
        return reports.stream()
                .map(report -> toResponse(report, userMap, itemMap))
                .toList();
    }

    private Map<String, Item> buildItemMap(List<Report> reports) {
        Set<String> itemIds = reports.stream()
                .filter(report -> REPORT_TYPE_ITEM.equals(report.getReportType()))
                .map(Report::getTargetId)
                .map(this::trimToNull)
                .filter(value -> value != null)
                .collect(java.util.stream.Collectors.toSet());

        if (itemIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Item> itemMap = new HashMap<>();
        for (Item item : itemRepository.findAllById(itemIds)) {
            if (item != null && trimToNull(item.getId()) != null) {
                itemMap.put(item.getId(), item);
            }
        }
        return itemMap;
    }

    private Map<String, User> buildUserMap(List<Report> reports, Map<String, Item> itemMap) {
        Set<String> userIds = new java.util.LinkedHashSet<>();
        for (Report report : reports) {
            addIfPresent(userIds, report.getReporterId());
            if (REPORT_TYPE_USER.equals(report.getReportType())) {
                addIfPresent(userIds, report.getTargetId());
            } else if (REPORT_TYPE_ITEM.equals(report.getReportType())) {
                Item item = itemMap.get(report.getTargetId());
                if (item != null) {
                    addIfPresent(userIds, item.getSellerId());
                }
            }
            addIfPresent(userIds, report.getReviewedBy());
        }

        if (userIds.isEmpty()) {
            return Map.of();
        }

        Map<String, User> userMap = new HashMap<>();
        for (User user : userRepository.findAllById(userIds)) {
            if (user != null && trimToNull(user.getId()) != null) {
                userMap.put(user.getId(), user);
            }
        }
        return userMap;
    }

    private void addIfPresent(Collection<String> ids, String value) {
        String normalized = trimToNull(value);
        if (normalized != null) {
            ids.add(normalized);
        }
    }

    private ReportItemResponse toResponse(Report report, Map<String, User> userMap, Map<String, Item> itemMap) {
        ReportItemResponse response = new ReportItemResponse();
        response.setReportId(report.getId());
        response.setReportType(report.getReportType());
        response.setTargetId(report.getTargetId());
        response.setReporterId(report.getReporterId());
        response.setReasonCode(report.getReasonCode());
        response.setDescription(report.getDescription());
        response.setStatus(report.getStatus());
        response.setReviewedBy(report.getReviewedBy());
        response.setReviewedAt(report.getReviewedAt());
        response.setReviewNote(report.getReviewNote());
        response.setCreatedAt(report.getCreatedAt());
        response.setUpdatedAt(report.getUpdatedAt());

        response.setReporter(toUserSummary(userMap.get(report.getReporterId())));

        if (REPORT_TYPE_USER.equals(report.getReportType())) {
            response.setTargetUserId(report.getTargetId());
            response.setItemId(null);
            response.setTargetUser(toUserSummary(userMap.get(report.getTargetId())));
            response.setTargetItem(null);
            return response;
        }

        if (REPORT_TYPE_ITEM.equals(report.getReportType())) {
            Item item = itemMap.get(report.getTargetId());
            response.setItemId(report.getTargetId());
            response.setTargetItem(toItemSummary(item, report.getTargetId()));
            String sellerId = item == null ? null : trimToNull(item.getSellerId());
            response.setTargetUserId(sellerId);
            response.setTargetUser(sellerId == null ? null : toUserSummary(userMap.get(sellerId)));
            return response;
        }

        return response;
    }

    private ReportItemResponse.ReportUserSummary toUserSummary(User user) {
        if (user == null) {
            return null;
        }
        ReportItemResponse.ReportUserSummary summary = new ReportItemResponse.ReportUserSummary();
        summary.setUserId(user.getId());
        summary.setUsername(trimToNull(user.getUsername()));
        summary.setNickname(trimToNull(user.getNickname()));
        return summary;
    }

    private ReportItemResponse.ReportItemSummary toItemSummary(Item item, String fallbackItemId) {
        if (item == null && trimToNull(fallbackItemId) == null) {
            return null;
        }
        ReportItemResponse.ReportItemSummary summary = new ReportItemResponse.ReportItemSummary();
        summary.setItemId(item == null ? trimToNull(fallbackItemId) : item.getId());
        summary.setTitle(item == null ? null : trimToNull(item.getTitle()));
        return summary;
    }
}
