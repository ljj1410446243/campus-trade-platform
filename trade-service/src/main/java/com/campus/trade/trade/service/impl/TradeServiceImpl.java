package com.campus.trade.trade.service.impl;

import com.campus.trade.trade.dto.CreateTradeRequest;
import com.campus.trade.trade.dto.InitiatePaymentRequest;
import com.campus.trade.trade.dto.InitiatePaymentResponse;
import com.campus.trade.trade.dto.PaymentInitiationResult;
import com.campus.trade.trade.dto.PaymentStatusResponse;
import com.campus.trade.trade.dto.PaymentStatusQueryResult;
import com.campus.trade.trade.dto.TradeDetailResponse;
import com.campus.trade.trade.dto.TradeListResponse;
import com.campus.trade.trade.exception.BusinessException;
import com.campus.trade.trade.model.ItemDocument;
import com.campus.trade.trade.model.PickupPoint;
import com.campus.trade.trade.model.Trade;
import com.campus.trade.trade.model.User;
import com.campus.trade.trade.repository.ItemRepository;
import com.campus.trade.trade.repository.TradeRepository;
import com.campus.trade.trade.repository.UserRepository;
import com.campus.trade.trade.service.PaymentGateway;
import com.campus.trade.trade.service.PickupPointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import com.campus.trade.trade.service.TradeService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import com.campus.trade.trade.util.UserAccessGuard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class TradeServiceImpl implements TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeServiceImpl.class);

    private static final String ITEM_STATUS_ON_SALE = "ON_SALE";
    private static final String ITEM_STATUS_RESERVED = "RESERVED";
    private static final String ITEM_STATUS_SOLD = "SOLD";
    private static final String ITEM_TRADE_MODE_ONLINE = "ONLINE";

    private static final String TRADE_STATUS_WAIT_PAY = "WAIT_PAY";
    private static final String TRADE_STATUS_PAID = "PAID";
    private static final String TRADE_STATUS_WAIT_CONFIRM = "WAIT_CONFIRM";
    private static final String TRADE_STATUS_COMPLETED = "COMPLETED";
    private static final String TRADE_STATUS_CANCELLED = "CANCELLED";

    private static final String PAY_STATUS_WAIT_PAY = "WAIT_PAY";
    private static final String PAY_STATUS_PAID = "PAID";
    private static final String PAY_STATUS_CLOSED = "CLOSED";
    private static final String PROFILE_PROD = "prod";
    private static final String PROFILE_PRODUCTION = "production";
    private static final String DELIVERY_MODE_DIRECT = "DIRECT";
    private static final String DELIVERY_MODE_PICKUP_POINT = "PICKUP_POINT";

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final MongoTemplate mongoTemplate;
    private final Environment environment;
    private final PaymentGateway paymentGateway;
    private final PickupPointService pickupPointService;
    private final UserAccessGuard userAccessGuard;

    public TradeServiceImpl(TradeRepository tradeRepository,
                            UserRepository userRepository,
                            ItemRepository itemRepository,
                            MongoTemplate mongoTemplate,
                            Environment environment,
                            PaymentGateway paymentGateway,
                            PickupPointService pickupPointService,
                            UserAccessGuard userAccessGuard) {
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.mongoTemplate = mongoTemplate;
        this.environment = environment;
        this.paymentGateway = paymentGateway;
        this.pickupPointService = pickupPointService;
        this.userAccessGuard = userAccessGuard;
    }

    @Override
    public String ping() {
        return "trade-service is running";
    }

    @Override
    public String createTrade(String buyerId, CreateTradeRequest request) {
        userAccessGuard.assertWritable(buyerId);

        ItemDocument item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new BusinessException("商品不存在"));

        if (buyerId.equals(item.getSellerId())) {
            throw new BusinessException("买家和卖家不能是同一人");
        }
        if (request.getSellerId() != null && !request.getSellerId().isBlank()
                && !Objects.equals(request.getSellerId(), item.getSellerId())) {
            throw new BusinessException("卖家信息不匹配");
        }

        Date now = new Date();
        boolean reserved = updateItemStatus(item.getId(), item.getSellerId(), ITEM_STATUS_ON_SALE, ITEM_STATUS_RESERVED, now);
        if (!reserved) {
            throw buildItemUnavailableException(loadItemOrThrow(item.getId()));
        }

        Trade trade = new Trade();
        trade.setTradeNo(generateTradeNo());
        trade.setItemId(request.getItemId());
        trade.setBuyerId(buyerId);
        trade.setSellerId(item.getSellerId());
        applyDeliveryInfo(trade, item, request);
        trade.setPrice(resolveTradePrice(item.getPrice(), request.getPrice()));
        trade.setStatus(TRADE_STATUS_WAIT_PAY);
        trade.setPayStatus(PAY_STATUS_WAIT_PAY);
        trade.setPayChannel(paymentGateway.getChannel());
        trade.setOutTradeNo(generateOutTradeNo());

        trade.setPayExpireAt(new Date(now.getTime() + 30L * 60L * 1000L));
        trade.setCreatedAt(now);
        trade.setUpdatedAt(now);

        try {
            Trade saved = tradeRepository.save(trade);
            return saved.getId();
        } catch (RuntimeException e) {
            updateItemStatus(item.getId(), item.getSellerId(), ITEM_STATUS_RESERVED, ITEM_STATUS_ON_SALE, new Date());
            throw e;
        }
    }

    @Override
    public InitiatePaymentResponse initiatePayment(String buyerId, String tradeId, InitiatePaymentRequest request) {
        userAccessGuard.assertWritable(buyerId);
        Trade trade = getTradeOrThrow(tradeId);

        if (!buyerId.equals(trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以发起支付");
        }
        if (isPaidState(trade) || TRADE_STATUS_COMPLETED.equals(trade.getStatus())) {
            return toInitiatePaymentResponse(trade, paymentGateway.initiate(trade, request));
        }
        if (!isWaitPayState(trade)) {
            throw conflict("当前状态不可支付");
        }

        String channel = normalizeChannel(request == null ? null : request.getChannel());
        Date now = new Date();

        Update update = new Update()
                .set("updatedAt", now)
                .set("tradeNo", defaultIfBlank(trade.getTradeNo(), generateTradeNo()))
                .set("outTradeNo", defaultIfBlank(trade.getOutTradeNo(), generateOutTradeNo()))
                .set("payExpireAt", trade.getPayExpireAt() == null ? new Date(now.getTime() + 30L * 60L * 1000L) : trade.getPayExpireAt());

        if (shouldUpdatePayChannel(trade.getPayChannel(), channel)) {
            update.set("payChannel", channel);
        }

        long modified = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(tradeId)
                        .and("status").is(TRADE_STATUS_WAIT_PAY)
                        .and("payStatus").is(PAY_STATUS_WAIT_PAY)),
                update,
                Trade.class
        ).getModifiedCount();

        if (modified == 0) {
            Trade latestTrade = getTradeOrThrow(tradeId);
            if (isWaitPayState(latestTrade) || isPaidState(latestTrade) || TRADE_STATUS_COMPLETED.equals(latestTrade.getStatus())) {
                return toInitiatePaymentResponse(latestTrade, paymentGateway.initiate(latestTrade, request));
            }
            throw conflict("当前状态不可支付");
        }

        Trade latestTrade = getTradeOrThrow(tradeId);
        return toInitiatePaymentResponse(latestTrade, paymentGateway.initiate(latestTrade, request));
    }

    @Override
    public PaymentStatusResponse queryPaymentStatus(String userId, String tradeId) {
        Trade trade = getTradeOrThrow(tradeId);

        if (!userId.equals(trade.getBuyerId()) && !userId.equals(trade.getSellerId())) {
            throw new BusinessException(403, "无权限查看支付状态");
        }
        return toPaymentStatusResponse(trade, paymentGateway.query(trade));
    }

    @Override
    public TradeDetailResponse mockPaySuccess(String buyerId, String tradeId) {
        ensureMockPaySuccessAllowed();
        userAccessGuard.assertWritable(buyerId);

        Trade trade = getTradeOrThrow(tradeId);
        String beforeStatus = trade.getStatus();
        String beforePayStatus = trade.getPayStatus();

        if (!buyerId.equals(trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以发起模拟支付");
        }

        if (isMockPaySuccessIdempotent(trade)) {
            TradeDetailResponse response = getTradeDetail(tradeId);
            logMockPaySuccess(buyerId, tradeId, beforeStatus, beforePayStatus,
                    response.getStatus(), response.getPayStatus(), true);
            return response;
        }

        if (!isWaitPayState(trade)) {
            throw conflict("当前订单状态不允许支付");
        }

        Date now = new Date();
        long modified = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(tradeId)
                        .and("buyerId").is(buyerId)
                        .and("status").is(TRADE_STATUS_WAIT_PAY)
                        .and("payStatus").is(PAY_STATUS_WAIT_PAY)),
                new Update()
                        .set("status", TRADE_STATUS_PAID)
                        .set("payStatus", PAY_STATUS_PAID)
                        .set("paidAt", trade.getPaidAt() == null ? now : trade.getPaidAt())
                        .set("payChannel", paymentGateway.getChannel())
                        .set("outTradeNo", defaultIfBlank(trade.getOutTradeNo(), generateMockSuccessOutTradeNo(tradeId, now)))
                        .set("providerTradeNo", defaultIfBlank(trade.getProviderTradeNo(), paymentGateway.generateProviderTradeNo()))
                        .set("updatedAt", now),
                Trade.class
        ).getModifiedCount();

        if (modified == 0) {
            Trade latestTrade = getTradeOrThrow(tradeId);
            if (isMockPaySuccessIdempotent(latestTrade)) {
                TradeDetailResponse response = getTradeDetail(tradeId);
                logMockPaySuccess(buyerId, tradeId, beforeStatus, beforePayStatus,
                        response.getStatus(), response.getPayStatus(), true);
                return response;
            }
            throw conflict("当前订单状态不允许支付");
        }

        TradeDetailResponse response = getTradeDetail(tradeId);
        logMockPaySuccess(buyerId, tradeId, beforeStatus, beforePayStatus,
                response.getStatus(), response.getPayStatus(), false);
        return response;
    }

    @Override
    public PaymentStatusResponse mockPay(String buyerId, String tradeId) {
        userAccessGuard.assertWritable(buyerId);
        Trade trade = getTradeOrThrow(tradeId);

        if (!buyerId.equals(trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以模拟支付");
        }
        if (isPaidState(trade) || TRADE_STATUS_COMPLETED.equals(trade.getStatus())) {
            return toPaymentStatusResponse(trade, paymentGateway.query(trade));
        }
        if (!isWaitPayState(trade)) {
            throw conflict("当前状态不可模拟支付");
        }

        Date now = new Date();
        long modified = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(tradeId)
                        .and("buyerId").is(buyerId)
                        .and("status").is(TRADE_STATUS_WAIT_PAY)
                        .and("payStatus").is(PAY_STATUS_WAIT_PAY)),
                new Update()
                        .set("status", TRADE_STATUS_PAID)
                        .set("payStatus", PAY_STATUS_PAID)
                        .set("payChannel", paymentGateway.getChannel())
                        .set("providerTradeNo", defaultIfBlank(trade.getProviderTradeNo(), paymentGateway.generateProviderTradeNo()))
                        .set("paidAt", trade.getPaidAt() == null ? now : trade.getPaidAt())
                        .set("updatedAt", now),
                Trade.class
        ).getModifiedCount();

        if (modified == 0) {
            Trade latestTrade = getTradeOrThrow(tradeId);
            if (isPaidState(latestTrade) || TRADE_STATUS_COMPLETED.equals(latestTrade.getStatus())) {
                return toPaymentStatusResponse(latestTrade, paymentGateway.query(latestTrade));
            }
            throw conflict("当前状态不可模拟支付");
        }

        Trade latestTrade = getTradeOrThrow(tradeId);
        return toPaymentStatusResponse(latestTrade, paymentGateway.query(latestTrade));
    }

    @Override
    public List<TradeListResponse> listBuyingTrades(String buyerId) {
        return tradeRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public List<TradeListResponse> listSellingTrades(String sellerId) {
        return tradeRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public TradeDetailResponse getTradeDetail(String tradeId) {
        Trade trade = getTradeOrThrow(tradeId);

        TradeDetailResponse response = new TradeDetailResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setItemId(trade.getItemId());
        response.setBuyerId(trade.getBuyerId());
        response.setSellerId(trade.getSellerId());
        response.setDeliveryMode(trade.getDeliveryMode());
        response.setPickupPointId(trade.getPickupPointId());
        response.setPickupPointName(trade.getPickupPointName());
        response.setPickupAddress(trade.getPickupAddress());
        response.setPickupLat(trade.getPickupLat());
        response.setPickupLng(trade.getPickupLng());
        response.setPickupContactPhone(trade.getPickupContactPhone());
        response.setPickupBusinessHours(trade.getPickupBusinessHours());
        response.setPrice(trade.getPrice());
        response.setStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setPayChannel(trade.getPayChannel());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setProviderTradeNo(trade.getProviderTradeNo());
        response.setPayExpireAt(trade.getPayExpireAt());
        response.setPaidAt(trade.getPaidAt());
        response.setDeliveredAt(trade.getDeliveredAt());
        response.setConfirmedAt(trade.getConfirmedAt());
        response.setCompletedAt(trade.getCompletedAt());
        response.setCancelledAt(trade.getCancelledAt());
        response.setCancelReason(trade.getCancelReason());
        response.setRefundReason(trade.getRefundReason());
        response.setCreatedAt(trade.getCreatedAt());

        // 补买家昵称
        userRepository.findById(trade.getBuyerId()).ifPresent(user -> {
            response.setBuyerNickname(resolveDisplayName(user));
        });

        // 补卖家昵称
        userRepository.findById(trade.getSellerId()).ifPresent(user -> {
            response.setSellerNickname(resolveDisplayName(user));
        });

        return response;
    }

    private TradeListResponse toListResponse(Trade trade) {
        TradeListResponse response = new TradeListResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setItemId(trade.getItemId());
        response.setBuyerId(trade.getBuyerId());
        response.setSellerId(trade.getSellerId());
        response.setPrice(trade.getPrice());
        response.setDeliveryMode(trade.getDeliveryMode());
        response.setPickupPointId(trade.getPickupPointId());
        response.setPickupPointName(trade.getPickupPointName());
        response.setStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setPayChannel(trade.getPayChannel());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setPayExpireAt(trade.getPayExpireAt());
        response.setPaidAt(trade.getPaidAt());
        response.setCreatedAt(trade.getCreatedAt());
        return response;
    }

    @Override
    public void cancelTrade(String userId, String tradeId) {
        userAccessGuard.assertWritable(userId);
        Trade trade = getTradeOrThrow(tradeId);

        if (!userId.equals(trade.getBuyerId()) &&
                !userId.equals(trade.getSellerId())) {
            throw new BusinessException(403, "无权限操作该交易");
        }

        if (TRADE_STATUS_CANCELLED.equals(trade.getStatus())) {
            restoreItemAfterCancel(trade);
            return;
        }

        if (!isWaitPayState(trade)) {
            throw conflict("当前状态不可取消");
        }

        Date now = new Date();
        long modified = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(tradeId)
                        .and("status").is(TRADE_STATUS_WAIT_PAY)
                        .and("payStatus").is(PAY_STATUS_WAIT_PAY)),
                new Update()
                        .set("status", TRADE_STATUS_CANCELLED)
                        .set("payStatus", PAY_STATUS_CLOSED)
                        .set("cancelledAt", now)
                        .set("cancelReason", resolveCancelReason(userId, trade))
                        .set("updatedAt", now),
                Trade.class
        ).getModifiedCount();

        if (modified == 0) {
            Trade latestTrade = getTradeOrThrow(tradeId);
            if (TRADE_STATUS_CANCELLED.equals(latestTrade.getStatus())) {
                restoreItemAfterCancel(latestTrade);
                return;
            }
            throw conflict("当前状态不可取消");
        }

        restoreItemAfterCancel(getTradeOrThrow(tradeId));
    }

    @Override
    public void deliverTrade(String userId, String tradeId) {
        userAccessGuard.assertWritable(userId);
        Trade trade = getTradeOrThrow(tradeId);

        if (!userId.equals(trade.getSellerId())) {
            throw new BusinessException(403, "只有卖家可以发货");
        }

        if (TRADE_STATUS_WAIT_CONFIRM.equals(trade.getStatus())) {
            backfillDeliveredAtIfMissing(tradeId, trade);
            return;
        }

        if (TRADE_STATUS_COMPLETED.equals(trade.getStatus())) {
            return;
        }

        if (!canDeliverTrade(trade)) {
            throw buildDeliverStateException(trade);
        }

        Date now = new Date();
        long modified = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(tradeId)
                        .and("sellerId").is(userId)
                        .and("status").is(TRADE_STATUS_PAID)
                        .and("payStatus").is(PAY_STATUS_PAID)),
                new Update()
                        .set("status", TRADE_STATUS_WAIT_CONFIRM)
                        .set("deliveredAt", trade.getDeliveredAt() == null ? now : trade.getDeliveredAt())
                        .set("updatedAt", now),
                Trade.class
        ).getModifiedCount();

        if (modified == 0) {
            Trade latestTrade = getTradeOrThrow(tradeId);
            if (TRADE_STATUS_WAIT_CONFIRM.equals(latestTrade.getStatus())) {
                backfillDeliveredAtIfMissing(tradeId, latestTrade);
                return;
            }
            if (TRADE_STATUS_COMPLETED.equals(latestTrade.getStatus())) {
                return;
            }
            throw buildDeliverStateException(latestTrade);
        }
    }

    @Override
    public void completeTrade(String userId, String tradeId) {
        userAccessGuard.assertWritable(userId);
        String tradeStatus = null;
        String buyerId = null;
        boolean allowedToConfirm = false;

        try {
            Trade trade = getTradeOrThrow(tradeId);
            tradeStatus = trade.getStatus();
            buyerId = trade.getBuyerId();
            allowedToConfirm = canCompleteTrade(trade);

            if (!userId.equals(buyerId)) {
                throw new BusinessException(403, "只有买家可以确认收货");
            }

            if (TRADE_STATUS_COMPLETED.equals(tradeStatus)) {
                ensureItemSold(trade);
                logConfirmTradeSuccess(tradeId, userId, buyerId, tradeStatus, false, true);
                return;
            }

            if (!allowedToConfirm) {
                throw buildCompleteStateException(trade);
            }

            ensureItemSold(trade);

            Date now = new Date();
            long modified = mongoTemplate.updateFirst(
                    Query.query(Criteria.where("id").is(tradeId)
                            .and("buyerId").is(userId)
                            .and("status").is(TRADE_STATUS_WAIT_CONFIRM)
                            .and("payStatus").is(PAY_STATUS_PAID)),
                    new Update()
                            .set("status", TRADE_STATUS_COMPLETED)
                            .set("confirmedAt", now)
                            .set("completedAt", now)
                            .set("updatedAt", now),
                    Trade.class
            ).getModifiedCount();

            if (modified == 0) {
                Trade latestTrade = getTradeOrThrow(tradeId);
                if (TRADE_STATUS_COMPLETED.equals(latestTrade.getStatus())) {
                    ensureItemSold(latestTrade);
                    logConfirmTradeSuccess(tradeId, userId, buyerId, latestTrade.getStatus(), false, true);
                    return;
                }
                throw buildCompleteStateException(latestTrade);
            }

            logConfirmTradeSuccess(tradeId, userId, buyerId, tradeStatus, true, false);
        } catch (RuntimeException e) {
            logConfirmTradeFailure(tradeId, userId, buyerId, tradeStatus, allowedToConfirm, e);
            throw e;
        }
    }

    private PaymentStatusResponse toPaymentStatusResponse(Trade trade, PaymentStatusQueryResult queryResult) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setPayChannel(queryResult == null ? trade.getPayChannel() : queryResult.getPayChannel());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setProviderTradeNo(queryResult == null ? trade.getProviderTradeNo() : queryResult.getProviderTradeNo());
        response.setPayExpireAt(trade.getPayExpireAt());
        response.setPaidAt(trade.getPaidAt());
        response.setUpdatedAt(trade.getUpdatedAt());
        return response;
    }

    private InitiatePaymentResponse toInitiatePaymentResponse(Trade trade, PaymentInitiationResult initiationResult) {
        InitiatePaymentResponse response = new InitiatePaymentResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setPayChannel(initiationResult == null ? trade.getPayChannel() : initiationResult.getPayChannel());
        response.setAmount(trade.getPrice());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setPayExpireAt(trade.getPayExpireAt());
        response.setPaymentData(initiationResult == null ? new LinkedHashMap<>() : initiationResult.getPaymentData());
        return response;
    }

    private String normalizeChannel(String channel) {
        return paymentGateway.getChannel();
    }

    private String generateTradeNo() {
        return "T" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private String generateOutTradeNo() {
        return "P" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String resolveDisplayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private Trade getTradeOrThrow(String tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException(404, "订单不存在"));
    }

    private ItemDocument loadItemOrThrow(String itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException("商品不存在"));
    }

    private boolean isWaitPayState(Trade trade) {
        return TRADE_STATUS_WAIT_PAY.equals(trade.getStatus()) && PAY_STATUS_WAIT_PAY.equals(trade.getPayStatus());
    }

    private boolean isPaidState(Trade trade) {
        return PAY_STATUS_PAID.equals(trade.getPayStatus())
                && (TRADE_STATUS_PAID.equals(trade.getStatus()) || TRADE_STATUS_WAIT_CONFIRM.equals(trade.getStatus()));
    }

    private boolean canDeliverTrade(Trade trade) {
        return TRADE_STATUS_PAID.equals(trade.getStatus()) && PAY_STATUS_PAID.equals(trade.getPayStatus());
    }

    private boolean canCompleteTrade(Trade trade) {
        return TRADE_STATUS_WAIT_CONFIRM.equals(trade.getStatus()) && PAY_STATUS_PAID.equals(trade.getPayStatus());
    }

    private boolean shouldUpdatePayChannel(String existingChannel, String requestedChannel) {
        if (requestedChannel == null || requestedChannel.isBlank()) {
            return false;
        }
        if (existingChannel == null || existingChannel.isBlank()) {
            return true;
        }
        return !requestedChannel.equalsIgnoreCase(existingChannel);
    }

    private boolean updateItemStatus(String itemId, String sellerId, String fromStatus, String toStatus, Date updatedAt) {
        return mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(itemId)
                        .and("sellerId").is(sellerId)
                        .and("status").is(fromStatus)),
                new Update()
                        .set("status", toStatus)
                        .set("updatedAt", updatedAt),
                ItemDocument.class
        ).getModifiedCount() > 0;
    }

    private void restoreItemAfterCancel(Trade trade) {
        Date now = new Date();
        boolean restored = updateItemStatus(trade.getItemId(), trade.getSellerId(), ITEM_STATUS_RESERVED, ITEM_STATUS_ON_SALE, now);
        if (restored) {
            return;
        }

        ItemDocument item = loadItemOrThrow(trade.getItemId());
        if (ITEM_STATUS_ON_SALE.equals(item.getStatus())) {
            return;
        }
        throw new BusinessException("交易已取消，但商品状态恢复失败");
    }

    private void backfillDeliveredAtIfMissing(String tradeId, Trade trade) {
        if (trade.getDeliveredAt() != null) {
            return;
        }

        Date now = new Date();
        mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(tradeId)
                        .and("status").is(TRADE_STATUS_WAIT_CONFIRM)
                        .and("payStatus").is(PAY_STATUS_PAID)
                        .and("deliveredAt").is(null)),
                new Update()
                        .set("deliveredAt", now)
                        .set("updatedAt", now),
                Trade.class
        );
    }

    private void ensureItemSold(Trade trade) {
        Date now = new Date();
        boolean sold = updateItemStatus(trade.getItemId(), trade.getSellerId(), ITEM_STATUS_RESERVED, ITEM_STATUS_SOLD, now);
        if (sold) {
            return;
        }

        ItemDocument item = loadItemOrThrow(trade.getItemId());
        if (ITEM_STATUS_SOLD.equals(item.getStatus())) {
            return;
        }
        throw new BusinessException("商品状态异常，无法完成交易");
    }

    private BusinessException buildItemUnavailableException(ItemDocument item) {
        if (ITEM_STATUS_RESERVED.equals(item.getStatus())) {
            return new BusinessException("商品已被其他用户占用");
        }
        if (ITEM_STATUS_SOLD.equals(item.getStatus())) {
            return new BusinessException("商品已售出");
        }
        return new BusinessException("商品当前不可下单");
    }

    private String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private void applyDeliveryInfo(Trade trade, ItemDocument item, CreateTradeRequest request) {
        String deliveryMode = normalizeDeliveryMode(request == null ? null : request.getDeliveryMode());
        trade.setDeliveryMode(deliveryMode);

        if (DELIVERY_MODE_DIRECT.equals(deliveryMode)) {
            clearPickupSnapshot(trade);
            return;
        }

        if (ITEM_TRADE_MODE_ONLINE.equalsIgnoreCase(item.getTradeMode())) {
            throw new BusinessException(409, "线上交易商品不支持自提点");
        }

        String pickupPointId = request == null ? null : request.getPickupPointId();
        if (pickupPointId == null || pickupPointId.isBlank()) {
            throw new BusinessException(400, "pickupPointId不能为空");
        }

        PickupPoint pickupPoint = pickupPointService.requireEnabledPickupPoint(pickupPointId.trim());
        trade.setPickupPointId(pickupPoint.getId());
        trade.setPickupPointName(pickupPoint.getName());
        trade.setPickupAddress(pickupPoint.getAddress());
        trade.setPickupLat(pickupPoint.getLat());
        trade.setPickupLng(pickupPoint.getLng());
        trade.setPickupContactPhone(pickupPoint.getContactPhone());
        trade.setPickupBusinessHours(pickupPoint.getBusinessHours());
    }

    private String normalizeDeliveryMode(String deliveryMode) {
        if (deliveryMode == null || deliveryMode.isBlank()) {
            return DELIVERY_MODE_DIRECT;
        }
        String normalized = deliveryMode.trim().toUpperCase();
        if (!DELIVERY_MODE_DIRECT.equals(normalized) && !DELIVERY_MODE_PICKUP_POINT.equals(normalized)) {
            throw new BusinessException(400, "deliveryMode仅支持DIRECT或PICKUP_POINT");
        }
        return normalized;
    }

    private void clearPickupSnapshot(Trade trade) {
        trade.setPickupPointId(null);
        trade.setPickupPointName(null);
        trade.setPickupAddress(null);
        trade.setPickupLat(null);
        trade.setPickupLng(null);
        trade.setPickupContactPhone(null);
        trade.setPickupBusinessHours(null);
    }

    private double resolveTradePrice(BigDecimal itemPrice, Double requestPrice) {
        return itemPrice == null ? requestPrice : itemPrice.doubleValue();
    }

    private String resolveCancelReason(String userId, Trade trade) {
        if (userId.equals(trade.getSellerId())) {
            return "卖家取消订单";
        }
        return "买家取消订单";
    }

    private BusinessException buildDeliverStateException(Trade trade) {
        if (PAY_STATUS_WAIT_PAY.equals(trade.getPayStatus()) || TRADE_STATUS_WAIT_PAY.equals(trade.getStatus())) {
            return conflict("订单未支付，不能发货");
        }
        return conflict("当前状态不可发货");
    }

    private BusinessException buildCompleteStateException(Trade trade) {
        if (TRADE_STATUS_WAIT_PAY.equals(trade.getStatus())
                || (TRADE_STATUS_PAID.equals(trade.getStatus()) && PAY_STATUS_PAID.equals(trade.getPayStatus()))) {
            return conflict("当前订单未发货，不能确认收货");
        }
        return conflict("当前状态不可确认收货");
    }

    private BusinessException conflict(String message) {
        return new BusinessException(409, message);
    }

    private void ensureMockPaySuccessAllowed() {
        if (Arrays.stream(environment.getActiveProfiles())
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(profile -> PROFILE_PROD.equals(profile) || PROFILE_PRODUCTION.equals(profile))) {
            throw new BusinessException(403, "生产环境不允许调用模拟支付接口");
        }
    }

    private boolean isMockPaySuccessIdempotent(Trade trade) {
        return TRADE_STATUS_PAID.equals(trade.getStatus())
                || TRADE_STATUS_WAIT_CONFIRM.equals(trade.getStatus())
                || TRADE_STATUS_COMPLETED.equals(trade.getStatus());
    }

    private String generateMockSuccessOutTradeNo(String tradeId, Date now) {
        return "MOCK_" + tradeId + "_" + Instant.ofEpochMilli(now.getTime()).toEpochMilli();
    }

    private void logMockPaySuccess(String userId,
                                   String tradeId,
                                   String beforeStatus,
                                   String beforePayStatus,
                                   String afterStatus,
                                   String afterPayStatus,
                                   boolean idempotent) {
        log.info("mock-pay-success userId={} tradeId={} beforeStatus={} beforePayStatus={} afterStatus={} afterPayStatus={} idempotent={} at={}",
                userId,
                tradeId,
                beforeStatus,
                beforePayStatus,
                afterStatus,
                afterPayStatus,
                idempotent,
                Instant.now());
    }

    private void logConfirmTradeSuccess(String tradeId,
                                        String userId,
                                        String buyerId,
                                        String status,
                                        boolean allowedToConfirm,
                                        boolean idempotent) {
        log.info("confirm-trade success tradeId={} userId={} buyerId={} status={} allowedToConfirm={} idempotent={} at={}",
                tradeId,
                userId,
                buyerId,
                status,
                allowedToConfirm,
                idempotent,
                Instant.now());
    }

    private void logConfirmTradeFailure(String tradeId,
                                        String userId,
                                        String buyerId,
                                        String status,
                                        boolean allowedToConfirm,
                                        RuntimeException exception) {
        log.error("confirm-trade failed tradeId={} userId={} buyerId={} status={} allowedToConfirm={} exceptionType={}",
                tradeId,
                userId,
                buyerId,
                status,
                allowedToConfirm,
                exception.getClass().getName(),
                exception);
    }
}
