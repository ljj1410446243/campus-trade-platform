package com.campus.trade.trade.service.impl;

import com.campus.trade.trade.config.TradeProperties;
import com.campus.trade.trade.dto.CancelTradeRequest;
import com.campus.trade.trade.dto.CreateTradeRequest;
import com.campus.trade.trade.dto.CreateTradeResponse;
import com.campus.trade.trade.dto.InitiatePaymentRequest;
import com.campus.trade.trade.dto.InitiatePaymentResponse;
import com.campus.trade.trade.dto.PaymentCallbackRequest;
import com.campus.trade.trade.dto.PaymentStatusResponse;
import com.campus.trade.trade.dto.RefundTradeRequest;
import com.campus.trade.trade.dto.TradeDetailResponse;
import com.campus.trade.trade.dto.TradeListResponse;
import com.campus.trade.trade.enums.PayChannel;
import com.campus.trade.trade.enums.PayStatus;
import com.campus.trade.trade.enums.TradeLogAction;
import com.campus.trade.trade.enums.TradeStatus;
import com.campus.trade.trade.exception.BusinessException;
import com.campus.trade.trade.model.ItemDocument;
import com.campus.trade.trade.model.PaymentRecord;
import com.campus.trade.trade.model.Trade;
import com.campus.trade.trade.model.TradeLog;
import com.campus.trade.trade.model.User;
import com.campus.trade.trade.repository.ItemRepository;
import com.campus.trade.trade.repository.PaymentRecordRepository;
import com.campus.trade.trade.repository.TradeLogRepository;
import com.campus.trade.trade.repository.TradeRepository;
import com.campus.trade.trade.repository.UserRepository;
import com.campus.trade.trade.service.PaymentGateway;
import com.campus.trade.trade.service.PaymentGatewayRegistry;
import com.campus.trade.trade.service.TradeService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class TradeServiceImpl implements TradeService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final PaymentRecordRepository paymentRecordRepository;
    private final TradeLogRepository tradeLogRepository;
    private final MongoTemplate mongoTemplate;
    private final PaymentGatewayRegistry paymentGatewayRegistry;
    private final TradeProperties tradeProperties;

    public TradeServiceImpl(TradeRepository tradeRepository,
                            UserRepository userRepository,
                            ItemRepository itemRepository,
                            PaymentRecordRepository paymentRecordRepository,
                            TradeLogRepository tradeLogRepository,
                            MongoTemplate mongoTemplate,
                            PaymentGatewayRegistry paymentGatewayRegistry,
                            TradeProperties tradeProperties) {
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.paymentRecordRepository = paymentRecordRepository;
        this.tradeLogRepository = tradeLogRepository;
        this.mongoTemplate = mongoTemplate;
        this.paymentGatewayRegistry = paymentGatewayRegistry;
        this.tradeProperties = tradeProperties;
    }

    @Override
    public String ping() {
        return "trade-service is running";
    }

    @Override
    public CreateTradeResponse createTrade(String buyerId, CreateTradeRequest request) {
        ItemDocument item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new BusinessException("商品不存在"));

        if (Objects.equals(buyerId, item.getSellerId())) {
            throw new BusinessException("不能购买自己发布的商品");
        }
        if (!"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException("商品当前不可下单");
        }

        Date now = new Date();
        Date payExpireAt = Date.from(Instant.ofEpochMilli(now.getTime())
                .plus(tradeProperties.getPayTimeoutMinutes(), ChronoUnit.MINUTES));

        ItemDocument lockedItem = lockItemForOrder(item.getId(), now);

        Trade trade = new Trade();
        trade.setTradeNo(generateTradeNo());
        trade.setItemId(lockedItem.getId());
        trade.setBuyerId(buyerId);
        trade.setSellerId(lockedItem.getSellerId());
        trade.setAmount(lockedItem.getPrice() == null ? BigDecimal.ZERO : lockedItem.getPrice());
        trade.setStatus(TradeStatus.WAIT_PAY);
        trade.setPayStatus(PayStatus.WAIT_PAY);
        trade.setPayChannel(PayChannel.MOCK);
        trade.setOutTradeNo(generateOutTradeNo());
        trade.setPayExpireAt(payExpireAt);
        trade.setItemSnapshot(buildItemSnapshot(lockedItem));
        trade.setCreatedAt(now);
        trade.setUpdatedAt(now);

        try {
            Trade saved = tradeRepository.save(trade);
            saveLog(saved, TradeLogAction.ORDER_CREATED, null, TradeStatus.WAIT_PAY, buyerId, "BUYER", "订单创建成功");
            return toCreateTradeResponse(saved);
        } catch (RuntimeException ex) {
            releaseItem(lockedItem.getId(), now);
            throw ex;
        }
    }

    @Override
    public List<TradeListResponse> listBuyingTrades(String buyerId) {
        return tradeRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream()
                .map(this::closeExpiredTradeIfNeeded)
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public List<TradeListResponse> listSellingTrades(String sellerId) {
        return tradeRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::closeExpiredTradeIfNeeded)
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public TradeDetailResponse getTradeDetail(String userId, String tradeId) {
        Trade trade = closeExpiredTradeIfNeeded(loadTrade(tradeId));
        assertParticipant(userId, trade);

        TradeDetailResponse response = new TradeDetailResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setItemId(trade.getItemId());
        response.setAmount(trade.getAmount());
        response.setStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setPayChannel(trade.getPayChannel());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setProviderTradeNo(trade.getProviderTradeNo());
        response.setCancelReason(trade.getCancelReason());
        response.setRefundReason(trade.getRefundReason());
        response.setCreatedAt(trade.getCreatedAt());
        response.setUpdatedAt(trade.getUpdatedAt());
        response.setPayExpireAt(trade.getPayExpireAt());
        response.setPaidAt(trade.getPaidAt());
        response.setDeliveredAt(trade.getDeliveredAt());
        response.setConfirmedAt(trade.getConfirmedAt());
        response.setCompletedAt(trade.getCompletedAt());
        response.setCancelledAt(trade.getCancelledAt());
        response.setRefundingAt(trade.getRefundingAt());
        response.setRefundedAt(trade.getRefundedAt());
        response.setBuyer(buildPartyInfo(trade.getBuyerId()));
        response.setSeller(buildPartyInfo(trade.getSellerId()));
        response.setItem(buildItemDetail(trade));
        response.setPayment(buildPaymentInfo(trade));
        response.setLogs(tradeLogRepository.findByTradeIdOrderByCreatedAtAsc(trade.getId())
                .stream()
                .map(this::toTradeLogInfo)
                .toList());
        return response;
    }

    @Override
    public void cancelTrade(String buyerId, String tradeId, CancelTradeRequest request) {
        Trade trade = closeExpiredTradeIfNeeded(loadTrade(tradeId));
        if (!Objects.equals(buyerId, trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以取消订单");
        }
        if (trade.getStatus() == TradeStatus.CANCELLED) {
            return;
        }
        if (trade.getStatus() != TradeStatus.WAIT_PAY) {
            throw new BusinessException("已支付订单请走退款流程");
        }

        Date now = new Date();
        updateTradeState(trade.getId(), TradeStatus.WAIT_PAY, PayStatus.WAIT_PAY,
                TradeStatus.CANCELLED, PayStatus.CLOSED, update -> {
                    update.set("cancelledAt", now);
                    update.set("cancelReason", normalizeReason(request == null ? null : request.getReason(), "买家主动取消"));
                    update.set("updatedAt", now);
                });

        releaseItem(trade.getItemId(), now);
        Trade latest = loadTrade(tradeId);
        saveLog(latest, TradeLogAction.ORDER_CANCELLED, TradeStatus.WAIT_PAY, TradeStatus.CANCELLED,
                buyerId, "BUYER", latest.getCancelReason());
    }

    @Override
    public InitiatePaymentResponse initiatePayment(String buyerId, String tradeId, InitiatePaymentRequest request) {
        Trade trade = closeExpiredTradeIfNeeded(loadTrade(tradeId));
        if (!Objects.equals(buyerId, trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以发起支付");
        }
        if (trade.getStatus() != TradeStatus.WAIT_PAY || trade.getPayStatus() != PayStatus.WAIT_PAY) {
            throw new BusinessException("当前订单状态不可支付");
        }

        PayChannel channel = request == null || request.getChannel() == null ? PayChannel.MOCK : request.getChannel();
        PaymentGateway gateway = paymentGatewayRegistry.get(channel);
        if (gateway == null) {
            throw new BusinessException("支付渠道暂未接入");
        }

        trade.setPayChannel(channel);
        trade.setUpdatedAt(new Date());
        tradeRepository.save(trade);

        PaymentRecord paymentRecord = paymentRecordRepository.findByTradeId(tradeId)
                .orElseGet(PaymentRecord::new);
        paymentRecord.setTradeId(trade.getId());
        paymentRecord.setTradeNo(trade.getTradeNo());
        paymentRecord.setBuyerId(trade.getBuyerId());
        paymentRecord.setAmount(trade.getAmount());
        paymentRecord.setChannel(channel);
        paymentRecord.setStatus(PayStatus.WAIT_PAY);
        paymentRecord.setOutTradeNo(trade.getOutTradeNo());
        if (paymentRecord.getCreatedAt() == null) {
            paymentRecord.setCreatedAt(new Date());
        }
        paymentRecord.setUpdatedAt(new Date());
        paymentRecord.setExtension(buildPaymentExtension(channel));
        PaymentRecord savedPayment = paymentRecordRepository.save(paymentRecord);

        saveLog(trade, TradeLogAction.PAYMENT_INITIATED, trade.getStatus(), trade.getStatus(),
                buyerId, "BUYER", "发起支付，渠道：" + channel.name());
        return gateway.buildPaymentResponse(trade, savedPayment);
    }

    @Override
    public PaymentStatusResponse queryPaymentStatus(String userId, String tradeId) {
        Trade trade = closeExpiredTradeIfNeeded(loadTrade(tradeId));
        assertParticipant(userId, trade);
        return toPaymentStatus(trade);
    }

    @Override
    public PaymentStatusResponse mockPay(String buyerId, String tradeId) {
        Trade trade = closeExpiredTradeIfNeeded(loadTrade(tradeId));
        if (!Objects.equals(buyerId, trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以进行模拟支付");
        }
        if (trade.getStatus() == TradeStatus.CANCELLED) {
            throw new BusinessException("已取消订单不能支付");
        }

        return handlePaymentSuccess(
                trade,
                trade.getOutTradeNo(),
                "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20),
                PayChannel.MOCK,
                "mock payment success"
        );
    }

    @Override
    public void deliverTrade(String sellerId, String tradeId) {
        Trade trade = loadTrade(tradeId);
        if (!Objects.equals(sellerId, trade.getSellerId())) {
            throw new BusinessException(403, "只有卖家可以标记交付");
        }
        if (trade.getStatus() != TradeStatus.PAID) {
            throw new BusinessException("当前状态不可标记交付");
        }

        Date now = new Date();
        updateTradeState(trade.getId(), TradeStatus.PAID, PayStatus.PAID,
                TradeStatus.WAIT_CONFIRM, PayStatus.PAID, update -> {
                    update.set("deliveredAt", now);
                    update.set("updatedAt", now);
                });

        saveLog(loadTrade(tradeId), TradeLogAction.SELLER_DELIVERED, TradeStatus.PAID, TradeStatus.WAIT_CONFIRM,
                sellerId, "SELLER", "卖家已标记交付");
    }

    @Override
    public void confirmReceipt(String buyerId, String tradeId) {
        Trade trade = loadTrade(tradeId);
        if (!Objects.equals(buyerId, trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以确认收货");
        }
        if (trade.getStatus() != TradeStatus.WAIT_CONFIRM) {
            throw new BusinessException("当前状态不可确认收货");
        }

        Date now = new Date();
        updateTradeState(trade.getId(), TradeStatus.WAIT_CONFIRM, PayStatus.PAID,
                TradeStatus.COMPLETED, PayStatus.PAID, update -> {
                    update.set("confirmedAt", now);
                    update.set("completedAt", now);
                    update.set("updatedAt", now);
                });

        markItemSold(trade.getItemId(), now);
        saveLog(loadTrade(tradeId), TradeLogAction.BUYER_CONFIRMED, TradeStatus.WAIT_CONFIRM, TradeStatus.COMPLETED,
                buyerId, "BUYER", "买家已确认收货");
    }

    @Override
    public void refundTrade(String buyerId, String tradeId, RefundTradeRequest request) {
        Trade trade = loadTrade(tradeId);
        if (!Objects.equals(buyerId, trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以发起退款");
        }
        if (trade.getStatus() != TradeStatus.PAID && trade.getStatus() != TradeStatus.WAIT_CONFIRM) {
            throw new BusinessException("当前状态不可发起退款");
        }

        Date now = new Date();
        TradeStatus fromStatus = trade.getStatus();
        updateTradeState(trade.getId(), fromStatus, PayStatus.PAID,
                TradeStatus.REFUNDING, PayStatus.REFUNDING, update -> {
                    update.set("refundingAt", now);
                    update.set("refundReason", normalizeReason(request == null ? null : request.getReason(), "待渠道退款处理"));
                    update.set("updatedAt", now);
                });

        paymentRecordRepository.findByTradeId(tradeId).ifPresent(record -> {
            record.setStatus(PayStatus.REFUNDING);
            record.setUpdatedAt(now);
            paymentRecordRepository.save(record);
        });

        saveLog(loadTrade(tradeId), TradeLogAction.REFUND_REQUESTED, fromStatus, TradeStatus.REFUNDING,
                buyerId, "BUYER", normalizeReason(request == null ? null : request.getReason(), "发起退款"));
    }

    @Override
    public PaymentStatusResponse handlePaymentCallback(PaymentCallbackRequest request) {
        if (request == null) {
            throw new BusinessException("回调参数不能为空");
        }

        Trade trade = resolveTradeForCallback(request);
        saveLog(trade, TradeLogAction.PAYMENT_CALLBACK_RECEIVED, trade.getStatus(), trade.getStatus(),
                "SYSTEM", "SYSTEM", "收到支付回调：" + request.getEventType());

        if ("REFUND_SUCCESS".equalsIgnoreCase(request.getEventType())) {
            handleRefundSuccess(trade, request);
            return toPaymentStatus(loadTrade(trade.getId()));
        }

        return handlePaymentSuccess(
                trade,
                trade.getOutTradeNo(),
                request.getProviderTradeNo(),
                request.getChannel() == null ? trade.getPayChannel() : request.getChannel(),
                "payment callback"
        );
    }

    private PaymentStatusResponse handlePaymentSuccess(Trade trade,
                                                       String outTradeNo,
                                                       String providerTradeNo,
                                                       PayChannel channel,
                                                       String source) {
        if (trade.getStatus() == TradeStatus.CANCELLED) {
            throw new BusinessException("已取消订单不能支付成功");
        }
        if (trade.getStatus() == TradeStatus.PAID
                || trade.getStatus() == TradeStatus.WAIT_CONFIRM
                || trade.getStatus() == TradeStatus.COMPLETED) {
            return toPaymentStatus(trade);
        }
        if (trade.getStatus() != TradeStatus.WAIT_PAY || trade.getPayStatus() != PayStatus.WAIT_PAY) {
            throw new BusinessException("当前订单状态不可推进为已支付");
        }

        Date now = new Date();
        int updated = updateTradeState(trade.getId(), TradeStatus.WAIT_PAY, PayStatus.WAIT_PAY,
                TradeStatus.PAID, PayStatus.PAID, update -> {
                    update.set("payChannel", channel == null ? PayChannel.MOCK : channel);
                    update.set("providerTradeNo", providerTradeNo);
                    update.set("outTradeNo", outTradeNo);
                    update.set("paidAt", now);
                    update.set("updatedAt", now);
                });

        if (updated == 0) {
            Trade latest = loadTrade(trade.getId());
            if (latest.getStatus() == TradeStatus.PAID
                    || latest.getStatus() == TradeStatus.WAIT_CONFIRM
                    || latest.getStatus() == TradeStatus.COMPLETED) {
                return toPaymentStatus(latest);
            }
            if (latest.getStatus() == TradeStatus.CANCELLED) {
                throw new BusinessException("已取消订单不能支付成功");
            }
            throw new BusinessException("订单支付状态更新失败");
        }

        PaymentRecord paymentRecord = paymentRecordRepository.findByTradeId(trade.getId())
                .orElseGet(PaymentRecord::new);
        paymentRecord.setTradeId(trade.getId());
        paymentRecord.setTradeNo(trade.getTradeNo());
        paymentRecord.setBuyerId(trade.getBuyerId());
        paymentRecord.setAmount(trade.getAmount());
        paymentRecord.setChannel(channel == null ? PayChannel.MOCK : channel);
        paymentRecord.setStatus(PayStatus.PAID);
        paymentRecord.setOutTradeNo(outTradeNo);
        paymentRecord.setProviderTradeNo(providerTradeNo);
        paymentRecord.setPaidAt(now);
        if (paymentRecord.getCreatedAt() == null) {
            paymentRecord.setCreatedAt(now);
        }
        paymentRecord.setUpdatedAt(now);
        paymentRecordRepository.save(paymentRecord);

        Trade latest = loadTrade(trade.getId());
        saveLog(latest, TradeLogAction.PAYMENT_SUCCEEDED, TradeStatus.WAIT_PAY, TradeStatus.PAID,
                "SYSTEM", "SYSTEM", "支付成功，来源：" + source);
        return toPaymentStatus(latest);
    }

    private void handleRefundSuccess(Trade trade, PaymentCallbackRequest request) {
        if (trade.getStatus() == TradeStatus.REFUNDED) {
            return;
        }
        if (trade.getStatus() != TradeStatus.REFUNDING) {
            throw new BusinessException("当前订单状态不可退款完成");
        }

        Date now = new Date();
        updateTradeState(trade.getId(), TradeStatus.REFUNDING, PayStatus.REFUNDING,
                TradeStatus.REFUNDED, PayStatus.REFUNDED, update -> {
                    update.set("providerTradeNo", request.getProviderTradeNo());
                    update.set("refundedAt", now);
                    update.set("updatedAt", now);
                });

        paymentRecordRepository.findByTradeId(trade.getId()).ifPresent(record -> {
            record.setStatus(PayStatus.REFUNDED);
            record.setProviderTradeNo(request.getProviderTradeNo());
            record.setUpdatedAt(now);
            paymentRecordRepository.save(record);
        });

        releaseItem(trade.getItemId(), now);
        saveLog(loadTrade(trade.getId()), TradeLogAction.REFUND_COMPLETED, TradeStatus.REFUNDING, TradeStatus.REFUNDED,
                "SYSTEM", "SYSTEM", "退款完成");
    }

    private Trade resolveTradeForCallback(PaymentCallbackRequest request) {
        if (request.getTradeId() != null && !request.getTradeId().isBlank()) {
            return loadTrade(request.getTradeId());
        }
        if (request.getTradeNo() != null && !request.getTradeNo().isBlank()) {
            return tradeRepository.findByTradeNo(request.getTradeNo())
                    .orElseThrow(() -> new BusinessException("订单不存在"));
        }
        if (request.getOutTradeNo() != null && !request.getOutTradeNo().isBlank()) {
            return tradeRepository.findByOutTradeNo(request.getOutTradeNo())
                    .orElseThrow(() -> new BusinessException("订单不存在"));
        }
        throw new BusinessException("缺少订单标识");
    }

    private Trade loadTrade(String tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException("订单不存在"));
    }

    private void assertParticipant(String userId, Trade trade) {
        if (!Objects.equals(userId, trade.getBuyerId()) && !Objects.equals(userId, trade.getSellerId())) {
            throw new BusinessException(403, "无权限查看该订单");
        }
    }

    private Trade closeExpiredTradeIfNeeded(Trade trade) {
        if (trade.getStatus() != TradeStatus.WAIT_PAY || trade.getPayExpireAt() == null) {
            return trade;
        }
        if (!trade.getPayExpireAt().before(new Date())) {
            return trade;
        }

        Date now = new Date();
        int updated = updateTradeState(trade.getId(), TradeStatus.WAIT_PAY, PayStatus.WAIT_PAY,
                TradeStatus.CANCELLED, PayStatus.CLOSED, update -> {
                    update.set("cancelledAt", now);
                    update.set("cancelReason", "支付超时关闭");
                    update.set("updatedAt", now);
                });
        if (updated > 0) {
            releaseItem(trade.getItemId(), now);
            Trade latest = loadTrade(trade.getId());
            saveLog(latest, TradeLogAction.AUTO_CLOSED, TradeStatus.WAIT_PAY, TradeStatus.CANCELLED,
                    "SYSTEM", "SYSTEM", "订单支付超时自动关闭");
            return latest;
        }
        return loadTrade(trade.getId());
    }

    private ItemDocument lockItemForOrder(String itemId, Date now) {
        Query query = Query.query(Criteria.where("_id").is(itemId).and("status").is("ON_SALE"));
        Update update = new Update()
                .set("status", "RESERVED")
                .set("updatedAt", now);
        ItemDocument item = mongoTemplate.findAndModify(query, update, ItemDocument.class);
        if (item == null) {
            throw new BusinessException("商品已被锁定或不可售");
        }
        item.setStatus("RESERVED");
        item.setUpdatedAt(now);
        return item;
    }

    private void releaseItem(String itemId, Date now) {
        Query query = Query.query(Criteria.where("_id").is(itemId).and("status").is("RESERVED"));
        Update update = new Update()
                .set("status", "ON_SALE")
                .set("updatedAt", now);
        mongoTemplate.updateFirst(query, update, ItemDocument.class);
    }

    private void markItemSold(String itemId, Date now) {
        Query query = Query.query(Criteria.where("_id").is(itemId).and("status").is("RESERVED"));
        Update update = new Update()
                .set("status", "SOLD")
                .set("updatedAt", now);
        mongoTemplate.updateFirst(query, update, ItemDocument.class);
    }

    private int updateTradeState(String tradeId,
                                 TradeStatus expectedTradeStatus,
                                 PayStatus expectedPayStatus,
                                 TradeStatus nextTradeStatus,
                                 PayStatus nextPayStatus,
                                 Consumer<Update> customizer) {
        Query query = Query.query(Criteria.where("_id").is(tradeId)
                .and("status").is(expectedTradeStatus)
                .and("payStatus").is(expectedPayStatus));
        Update update = new Update()
                .set("status", nextTradeStatus)
                .set("payStatus", nextPayStatus);
        customizer.accept(update);
        return (int) mongoTemplate.updateFirst(query, update, Trade.class).getModifiedCount();
    }

    private Trade.ItemSnapshot buildItemSnapshot(ItemDocument item) {
        Trade.ItemSnapshot snapshot = new Trade.ItemSnapshot();
        snapshot.setItemId(item.getId());
        snapshot.setSellerId(item.getSellerId());
        snapshot.setTitle(item.getTitle());
        snapshot.setDescription(item.getDescription());
        snapshot.setCategoryId(item.getCategoryId());
        snapshot.setCategoryName(item.getCategoryName());
        snapshot.setPrice(item.getPrice());
        snapshot.setConditionStar(item.getConditionStar());
        snapshot.setImages(item.getImages());
        snapshot.setCoverImage(item.getImages() == null || item.getImages().isEmpty() ? null : item.getImages().get(0));
        snapshot.setTradeMode(item.getTradeMode());
        return snapshot;
    }

    private CreateTradeResponse toCreateTradeResponse(Trade trade) {
        CreateTradeResponse response = new CreateTradeResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setItemId(trade.getItemId());
        response.setAmount(trade.getAmount());
        response.setStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setDefaultPayChannel(trade.getPayChannel());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setPayExpireAt(trade.getPayExpireAt());
        response.setCreatedAt(trade.getCreatedAt());
        return response;
    }

    private TradeListResponse toListResponse(Trade trade) {
        TradeListResponse response = new TradeListResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setAmount(trade.getAmount());
        response.setStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setPayChannel(trade.getPayChannel());
        response.setCreatedAt(trade.getCreatedAt());
        response.setPaidAt(trade.getPaidAt());
        response.setPayExpireAt(trade.getPayExpireAt());

        TradeListResponse.TradeItemInfo itemInfo = new TradeListResponse.TradeItemInfo();
        itemInfo.setItemId(trade.getItemId());
        itemInfo.setTitle(trade.getItemSnapshot() == null ? null : trade.getItemSnapshot().getTitle());
        itemInfo.setCoverImage(trade.getItemSnapshot() == null ? null : trade.getItemSnapshot().getCoverImage());
        itemInfo.setPrice(trade.getItemSnapshot() == null ? trade.getAmount() : trade.getItemSnapshot().getPrice());
        itemInfo.setConditionStar(trade.getItemSnapshot() == null ? null : trade.getItemSnapshot().getConditionStar());
        response.setItem(itemInfo);

        response.setBuyer(toTradeUserInfo(userRepository.findById(trade.getBuyerId()).orElse(null), trade.getBuyerId()));
        response.setSeller(toTradeUserInfo(userRepository.findById(trade.getSellerId()).orElse(null), trade.getSellerId()));
        return response;
    }

    private TradeDetailResponse.TradePartyInfo buildPartyInfo(String userId) {
        User user = userRepository.findById(userId).orElse(null);
        TradeDetailResponse.TradePartyInfo partyInfo = new TradeDetailResponse.TradePartyInfo();
        partyInfo.setUserId(userId);
        if (user == null) {
            partyInfo.setNickname("用户");
            return partyInfo;
        }
        partyInfo.setNickname(resolveDisplayName(user));
        partyInfo.setAvatarUrl(user.getAvatarUrl());
        return partyInfo;
    }

    private TradeDetailResponse.TradeItemDetail buildItemDetail(Trade trade) {
        Trade.ItemSnapshot snapshot = trade.getItemSnapshot();
        TradeDetailResponse.TradeItemDetail detail = new TradeDetailResponse.TradeItemDetail();
        detail.setItemId(trade.getItemId());
        if (snapshot == null) {
            return detail;
        }
        detail.setTitle(snapshot.getTitle());
        detail.setDescription(snapshot.getDescription());
        detail.setCategoryId(snapshot.getCategoryId());
        detail.setCategoryName(snapshot.getCategoryName());
        detail.setPrice(snapshot.getPrice());
        detail.setConditionStar(snapshot.getConditionStar());
        detail.setImages(snapshot.getImages());
        detail.setCoverImage(snapshot.getCoverImage());
        detail.setTradeMode(snapshot.getTradeMode());
        return detail;
    }

    private TradeDetailResponse.PaymentInfo buildPaymentInfo(Trade trade) {
        TradeDetailResponse.PaymentInfo paymentInfo = new TradeDetailResponse.PaymentInfo();
        paymentInfo.setChannel(trade.getPayChannel());
        paymentInfo.setPayStatus(trade.getPayStatus());
        paymentInfo.setOutTradeNo(trade.getOutTradeNo());
        paymentInfo.setProviderTradeNo(trade.getProviderTradeNo());
        paymentInfo.setPayExpireAt(trade.getPayExpireAt());
        paymentInfo.setPaidAt(trade.getPaidAt());
        return paymentInfo;
    }

    private TradeDetailResponse.TradeLogInfo toTradeLogInfo(TradeLog log) {
        TradeDetailResponse.TradeLogInfo logInfo = new TradeDetailResponse.TradeLogInfo();
        logInfo.setAction(log.getAction());
        logInfo.setFromStatus(log.getFromStatus());
        logInfo.setToStatus(log.getToStatus());
        logInfo.setOperatorId(log.getOperatorId());
        logInfo.setOperatorRole(log.getOperatorRole());
        logInfo.setMessage(log.getMessage());
        logInfo.setCreatedAt(log.getCreatedAt());
        return logInfo;
    }

    private PaymentStatusResponse toPaymentStatus(Trade trade) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setTradeStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setChannel(trade.getPayChannel());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setProviderTradeNo(trade.getProviderTradeNo());
        response.setPayExpireAt(trade.getPayExpireAt());
        response.setPaidAt(trade.getPaidAt());
        response.setUpdatedAt(trade.getUpdatedAt());
        return response;
    }

    private TradeListResponse.TradeUserInfo toTradeUserInfo(User user, String userId) {
        TradeListResponse.TradeUserInfo info = new TradeListResponse.TradeUserInfo();
        info.setUserId(userId);
        if (user == null) {
            info.setNickname("用户");
            return info;
        }
        info.setNickname(resolveDisplayName(user));
        info.setAvatarUrl(user.getAvatarUrl());
        return info;
    }

    private String resolveDisplayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername() == null || user.getUsername().isBlank() ? "用户" : user.getUsername();
    }

    private void saveLog(Trade trade,
                         TradeLogAction action,
                         TradeStatus fromStatus,
                         TradeStatus toStatus,
                         String operatorId,
                         String operatorRole,
                         String message) {
        TradeLog log = new TradeLog();
        log.setTradeId(trade.getId());
        log.setTradeNo(trade.getTradeNo());
        log.setAction(action);
        log.setFromStatus(fromStatus);
        log.setToStatus(toStatus);
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole);
        log.setMessage(message);
        log.setCreatedAt(new Date());
        tradeLogRepository.save(log);
    }

    private Map<String, Object> buildPaymentExtension(PayChannel channel) {
        Map<String, Object> extension = new LinkedHashMap<>();
        extension.put("channel", channel.name());
        extension.put("reserved", Boolean.TRUE);
        return extension;
    }

    private String generateTradeNo() {
        return "T" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private String generateOutTradeNo() {
        return "P" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String normalizeReason(String reason, String defaultReason) {
        if (reason == null || reason.isBlank()) {
            return defaultReason;
        }
        return reason.trim();
    }
}
