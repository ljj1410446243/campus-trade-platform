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
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TradeServiceImpl implements TradeService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;

    public TradeServiceImpl(TradeRepository tradeRepository,
                            UserRepository userRepository,
                            ItemRepository itemRepository) {
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
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
        if (!"ON_SALE".equals(item.getStatus())) {
            throw new BusinessException("商品当前不可下单");
        }

        item.setStatus("RESERVED");
        item.setUpdatedAt(new Date());
        itemRepository.save(item);

        Trade trade = new Trade();
        trade.setTradeNo(generateTradeNo());
        trade.setItemId(request.getItemId());
        trade.setBuyerId(buyerId);
        trade.setSellerId(item.getSellerId());
        trade.setPrice(item.getPrice() == null ? request.getPrice() : item.getPrice().doubleValue());
        trade.setStatus("WAIT_PAY");
        trade.setPayStatus("WAIT_PAY");
        trade.setPayChannel("MOCK");
        trade.setOutTradeNo(generateOutTradeNo());

        Date now = new Date();
        trade.setPayExpireAt(new Date(now.getTime() + 30L * 60L * 1000L));
        trade.setCreatedAt(now);
        trade.setUpdatedAt(now);

        Trade saved = tradeRepository.save(trade);
        return saved.getId();
    }

    @Override
    public InitiatePaymentResponse initiatePayment(String buyerId, String tradeId, InitiatePaymentRequest request) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException("交易不存在"));

        if (!buyerId.equals(trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以发起支付");
        }
        if (!"WAIT_PAY".equals(trade.getStatus()) || !"WAIT_PAY".equals(trade.getPayStatus())) {
            throw new BusinessException("当前状态不可支付");
        }

        String channel = normalizeChannel(request == null ? null : request.getChannel());
        trade.setPayChannel(channel);
        if (trade.getTradeNo() == null || trade.getTradeNo().isBlank()) {
            trade.setTradeNo(generateTradeNo());
        }
        if (trade.getOutTradeNo() == null || trade.getOutTradeNo().isBlank()) {
            trade.setOutTradeNo(generateOutTradeNo());
        }
        if (trade.getPayExpireAt() == null) {
            trade.setPayExpireAt(new Date(System.currentTimeMillis() + 30L * 60L * 1000L));
        }
        trade.setUpdatedAt(new Date());
        tradeRepository.save(trade);

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
        paymentData.put("mode", "MOCK");
        paymentData.put("message", "调用 mock 支付接口完成本地联调");
        paymentData.put("mockAction", "/trades/" + trade.getId() + "/mock-pay");
        response.setPaymentData(paymentData);
        return response;
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
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException("交易不存在"));

        if (!buyerId.equals(trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以模拟支付");
        }
        if ("PAID".equals(trade.getPayStatus()) || "WAIT_CONFIRM".equals(trade.getStatus()) || "COMPLETED".equals(trade.getStatus())) {
            return toPaymentStatusResponse(trade);
        }
        if (!"WAIT_PAY".equals(trade.getStatus()) || !"WAIT_PAY".equals(trade.getPayStatus())) {
            throw new BusinessException("当前状态不可模拟支付");
        }

        Date now = new Date();
        trade.setStatus("PAID");
        trade.setPayStatus("PAID");
        trade.setProviderTradeNo("MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20));
        trade.setPaidAt(now);
        trade.setUpdatedAt(now);
        tradeRepository.save(trade);

        return toPaymentStatusResponse(trade);
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
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException("交易不存在"));

        if (!userId.equals(trade.getBuyerId()) &&
                !userId.equals(trade.getSellerId())) {
            throw new BusinessException(403, "无权限操作该交易");
        }

        if (!"WAIT_PAY".equals(trade.getStatus()) || !"WAIT_PAY".equals(trade.getPayStatus())) {
            throw new BusinessException("当前状态不可取消");
        }

        ItemDocument item = itemRepository.findById(trade.getItemId()).orElse(null);
        if (item != null && "RESERVED".equals(item.getStatus())) {
            item.setStatus("ON_SALE");
            item.setUpdatedAt(new Date());
            itemRepository.save(item);
        }

        trade.setStatus("CANCELLED");
        trade.setPayStatus("CLOSED");
        trade.setCancelledAt(new Date());
        trade.setCancelReason("买家取消订单");
        trade.setUpdatedAt(new Date());

        tradeRepository.save(trade);
    }

    @Override
    public void completeTrade(String userId, String tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow(() -> new BusinessException("交易不存在"));

        if (!userId.equals(trade.getBuyerId())) {
            throw new BusinessException(403, "只有买家可以确认交易完成");
        }

        if (!"PAID".equals(trade.getStatus()) && !"WAIT_CONFIRM".equals(trade.getStatus())) {
            throw new BusinessException("当前状态不可完成");
        }

        ItemDocument item = itemRepository.findById(trade.getItemId()).orElse(null);
        if (item != null) {
            item.setStatus("SOLD");
            item.setUpdatedAt(new Date());
            itemRepository.save(item);
        }

        trade.setStatus("COMPLETED");
        trade.setConfirmedAt(new Date());
        trade.setCompletedAt(new Date());
        trade.setUpdatedAt(new Date());

        tradeRepository.save(trade);
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

    private String normalizeChannel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "MOCK";
        }
        return channel.trim().toUpperCase();
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
}
