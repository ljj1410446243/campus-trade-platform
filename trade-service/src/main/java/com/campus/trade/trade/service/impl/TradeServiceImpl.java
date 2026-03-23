package com.campus.trade.trade.service.impl;

import com.campus.trade.trade.dto.CreateTradeRequest;
import com.campus.trade.trade.dto.TradeDetailResponse;
import com.campus.trade.trade.dto.TradeListResponse;
import com.campus.trade.trade.exception.BusinessException;
import com.campus.trade.trade.model.Trade;
import com.campus.trade.trade.model.User;
import com.campus.trade.trade.repository.TradeRepository;
import com.campus.trade.trade.repository.UserRepository;
import com.campus.trade.trade.service.TradeService;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class TradeServiceImpl implements TradeService {

    private final TradeRepository tradeRepository;
    private final UserRepository userRepository;

    public TradeServiceImpl(TradeRepository tradeRepository, UserRepository userRepository) {
        this.tradeRepository = tradeRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String ping() {
        return "trade-service is running";
    }

    @Override
    public String createTrade(String buyerId, CreateTradeRequest request) {
        if (buyerId.equals(request.getSellerId())) {
            throw new BusinessException("买家和卖家不能是同一人");
        }

        Trade trade = new Trade();
        trade.setItemId(request.getItemId());
        trade.setBuyerId(buyerId);
        trade.setSellerId(request.getSellerId());
        trade.setPrice(request.getPrice());
        trade.setStatus("CREATED");

        Date now = new Date();
        trade.setCreatedAt(now);
        trade.setUpdatedAt(now);

        Trade saved = tradeRepository.save(trade);
        return saved.getId();
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
        response.setItemId(trade.getItemId());
        response.setBuyerId(trade.getBuyerId());
        response.setSellerId(trade.getSellerId());
        response.setPrice(trade.getPrice());
        response.setStatus(trade.getStatus());
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
        response.setItemId(trade.getItemId());
        response.setBuyerId(trade.getBuyerId());
        response.setSellerId(trade.getSellerId());
        response.setPrice(trade.getPrice());
        response.setStatus(trade.getStatus());
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

        if (!"CREATED".equals(trade.getStatus())) {
            throw new BusinessException("当前状态不可取消");
        }

        trade.setStatus("CANCELLED");
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

        if (!"CREATED".equals(trade.getStatus())) {
            throw new BusinessException("当前状态不可完成");
        }

        trade.setStatus("COMPLETED");
        trade.setUpdatedAt(new Date());

        tradeRepository.save(trade);
    }

    private String resolveDisplayName(User user) {
        if (user.getNickname() != null && !user.getNickname().isBlank()) {
            return user.getNickname();
        }
        return user.getUsername();
    }
}
