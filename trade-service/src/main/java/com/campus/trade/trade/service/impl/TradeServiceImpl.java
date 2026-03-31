package com.campus.trade.trade.service.impl;

import com.campus.trade.trade.dto.CreateTradeRequest;
import com.campus.trade.trade.dto.InitiatePaymentRequest;
import com.campus.trade.trade.dto.InitiatePaymentResponse;
import com.campus.trade.trade.dto.PaymentStatusResponse;
import com.campus.trade.trade.dto.TradeDetailResponse;
import com.campus.trade.trade.dto.TradeListResponse;
import com.campus.trade.trade.exception.BusinessException;
import com.campus.trade.trade.model.ItemDocument;
import com.campus.trade.trade.model.Trade;
import com.campus.trade.trade.model.User;
import com.campus.trade.trade.repository.ItemRepository;
import com.campus.trade.trade.repository.TradeRepository;
import com.campus.trade.trade.repository.UserRepository;
import com.campus.trade.trade.service.TradeService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class TradeServiceImpl implements TradeService {

    private static final String ITEM_STATUS_ON_SALE = "ON_SALE";
    private static final String ITEM_STATUS_RESERVED = "RESERVED";
    private static final String ITEM_STATUS_SOLD = "SOLD";

    private static final String TRADE_STATUS_WAIT_PAY = "WAIT_PAY";
    private static final String TRADE_STATUS_WAIT_CONFIRM = "WAIT_CONFIRM";
    private static final String TRADE_STATUS_COMPLETED = "COMPLETED";
    private static final String TRADE_STATUS_CANCELLED = "CANCELLED";

    private static final String PAY_STATUS_WAIT_PAY = "WAIT_PAY";
    private static final String PAY_STATUS_PAID = "PAID";
    private static final String PAY_STATUS_CLOSED = "CLOSED";
    private static final String PAY_CHANNEL_MOCK = "MOCK";

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final MongoTemplate mongoTemplate;

    public TradeServiceImpl(TradeRepository tradeRepository,
                            UserRepository userRepository,
                            ItemRepository itemRepository,
                            MongoTemplate mongoTemplate) {
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public String ping() {
        return "trade-service is running";
    }

    @Override
    public String createTrade(String buyerId, CreateTradeRequest request) {
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
        trade.setPrice(resolveTradePrice(item.getPrice(), request.getPrice()));
        trade.setStatus(TRADE_STATUS_WAIT_PAY);
        trade.setPayStatus(PAY_STATUS_WAIT_PAY);
        trade.setPayChannel(PAY_CHANNEL_MOCK);
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
        Trade trade = getTradeOrThrow(tradeId);

        if (!buyerId.equals(trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以发起支付");
        }
        if (isPaidState(trade) || TRADE_STATUS_COMPLETED.equals(trade.getStatus())) {
            return toInitiatePaymentResponse(trade);
        }
        if (!isWaitPayState(trade)) {
            throw new BusinessException("当前状态不可支付");
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
                return toInitiatePaymentResponse(latestTrade);
            }
            throw new BusinessException("当前状态不可支付");
        }

        return toInitiatePaymentResponse(getTradeOrThrow(tradeId));
    }

    @Override
    public PaymentStatusResponse queryPaymentStatus(String userId, String tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException("交易不存在"));

        if (!userId.equals(trade.getBuyerId()) && !userId.equals(trade.getSellerId())) {
            throw new BusinessException(403, "无权限查看支付状态");
        }
        return toPaymentStatusResponse(trade);
    }

    @Override
    public PaymentStatusResponse mockPay(String buyerId, String tradeId) {
        Trade trade = getTradeOrThrow(tradeId);

        if (!buyerId.equals(trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以模拟支付");
        }
        if (isPaidState(trade) || TRADE_STATUS_COMPLETED.equals(trade.getStatus())) {
            return toPaymentStatusResponse(trade);
        }
        if (!isWaitPayState(trade)) {
            throw new BusinessException("当前状态不可模拟支付");
        }

        Date now = new Date();
        long modified = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(tradeId)
                        .and("buyerId").is(buyerId)
                        .and("status").is(TRADE_STATUS_WAIT_PAY)
                        .and("payStatus").is(PAY_STATUS_WAIT_PAY)),
                new Update()
                        .set("status", TRADE_STATUS_WAIT_CONFIRM)
                        .set("payStatus", PAY_STATUS_PAID)
                        .set("providerTradeNo", defaultIfBlank(trade.getProviderTradeNo(), generateMockProviderTradeNo()))
                        .set("paidAt", trade.getPaidAt() == null ? now : trade.getPaidAt())
                        .set("updatedAt", now),
                Trade.class
        ).getModifiedCount();

        if (modified == 0) {
            Trade latestTrade = getTradeOrThrow(tradeId);
            if (isPaidState(latestTrade) || TRADE_STATUS_COMPLETED.equals(latestTrade.getStatus())) {
                return toPaymentStatusResponse(latestTrade);
            }
            throw new BusinessException("当前状态不可模拟支付");
        }

        return toPaymentStatusResponse(getTradeOrThrow(tradeId));
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
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException("交易不存在"));

        TradeDetailResponse response = new TradeDetailResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setItemId(trade.getItemId());
        response.setBuyerId(trade.getBuyerId());
        response.setSellerId(trade.getSellerId());
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
            throw new BusinessException("当前状态不可取消");
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
            throw new BusinessException("当前状态不可取消");
        }

        restoreItemAfterCancel(getTradeOrThrow(tradeId));
    }

    @Override
    public void completeTrade(String userId, String tradeId) {
        Trade trade = getTradeOrThrow(tradeId);

        if (!userId.equals(trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以确认交易完成");
        }

        if (TRADE_STATUS_COMPLETED.equals(trade.getStatus())) {
            ensureItemSold(trade);
            return;
        }

        if (!canCompleteTrade(trade)) {
            throw new BusinessException("当前状态不可完成");
        }

        ensureItemSold(trade);

        Date now = new Date();
        long modified = mongoTemplate.updateFirst(
                Query.query(Criteria.where("id").is(tradeId)
                        .and("buyerId").is(userId)
                        .and("status").in(TRADE_STATUS_WAIT_CONFIRM, PAY_STATUS_PAID)
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
                return;
            }
            throw new BusinessException("当前状态不可完成");
        }
    }

    private PaymentStatusResponse toPaymentStatusResponse(Trade trade) {
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setPayChannel(trade.getPayChannel());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setProviderTradeNo(trade.getProviderTradeNo());
        response.setPayExpireAt(trade.getPayExpireAt());
        response.setPaidAt(trade.getPaidAt());
        response.setUpdatedAt(trade.getUpdatedAt());
        return response;
    }

    private InitiatePaymentResponse toInitiatePaymentResponse(Trade trade) {
        InitiatePaymentResponse response = new InitiatePaymentResponse();
        response.setTradeId(trade.getId());
        response.setTradeNo(trade.getTradeNo());
        response.setStatus(trade.getStatus());
        response.setPayStatus(trade.getPayStatus());
        response.setPayChannel(trade.getPayChannel());
        response.setAmount(trade.getPrice());
        response.setOutTradeNo(trade.getOutTradeNo());
        response.setPayExpireAt(trade.getPayExpireAt());

        Map<String, Object> paymentData = new LinkedHashMap<>();
        paymentData.put("mode", PAY_CHANNEL_MOCK);
        paymentData.put("message", "调用 mock 支付接口完成本地联调");
        paymentData.put("mockAction", "/trades/" + trade.getId() + "/mock-pay");
        response.setPaymentData(paymentData);
        return response;
    }

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return PAY_CHANNEL_MOCK;
        }
        return channel.trim().toUpperCase();
    }

    private String generateTradeNo() {
        return "T" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
    }

    private String generateOutTradeNo() {
        return "P" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    private String generateMockProviderTradeNo() {
        return "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private String resolveDisplayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private Trade getTradeOrThrow(String tradeId) {
        return tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException("交易不存在"));
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
                && (TRADE_STATUS_WAIT_CONFIRM.equals(trade.getStatus()) || PAY_STATUS_PAID.equals(trade.getStatus()));
    }

    private boolean canCompleteTrade(Trade trade) {
        return PAY_STATUS_PAID.equals(trade.getPayStatus())
                && (TRADE_STATUS_WAIT_CONFIRM.equals(trade.getStatus()) || PAY_STATUS_PAID.equals(trade.getStatus()));
    }

    private boolean shouldUpdatePayChannel(String existingChannel, String requestedChannel) {
        if (requestedChannel == null || requestedChannel.isBlank()) {
            return false;
        }
        if (existingChannel == null || existingChannel.isBlank()) {
            return true;
        }
        return PAY_CHANNEL_MOCK.equals(existingChannel) && !PAY_CHANNEL_MOCK.equals(requestedChannel);
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

    private double resolveTradePrice(BigDecimal itemPrice, Double requestPrice) {
        return itemPrice == null ? requestPrice : itemPrice.doubleValue();
    }

    private String resolveCancelReason(String userId, Trade trade) {
        if (userId.equals(trade.getSellerId())) {
            return "卖家取消订单";
        }
        return "买家取消订单";
    }
}
