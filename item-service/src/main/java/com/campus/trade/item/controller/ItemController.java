package com.campus.trade.item.controller;

import com.campus.trade.common.response.ApiResponse;
import com.campus.trade.item.dto.AddCommentRequest;
import com.campus.trade.item.dto.CreateItemRequest;
import com.campus.trade.item.dto.ItemDetailResponse;
import com.campus.trade.item.dto.ItemListResponse;
import com.campus.trade.item.dto.SearchItemPageResponse;
import com.campus.trade.item.dto.UpdateItemRequest;
import com.campus.trade.item.model.ItemComment;
import com.campus.trade.item.service.ItemService;
import com.campus.trade.item.util.LoginUserHelper;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class ItemController {

    private final ItemService itemService;
    private final LoginUserHelper loginUserHelper;

    public ItemController(ItemService itemService, LoginUserHelper loginUserHelper) {
        this.itemService = itemService;
        this.loginUserHelper = loginUserHelper;
    }

    @GetMapping("/items/ping")
    public ApiResponse<String> ping() {
        return ApiResponse.success(itemService.ping());
    }

    @PostMapping("/items")
    public ApiResponse<Map<String, String>> createItem(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody CreateItemRequest request) {

        String sellerId = loginUserHelper.getCurrentUserId(authorizationHeader);
        String itemId = itemService.createItem(sellerId, request);

        return ApiResponse.success(Map.of("itemId", itemId));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<ItemDetailResponse> updateItem(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateItemRequest request) {

        String sellerId = loginUserHelper.getCurrentUserId(authorizationHeader);
        return ApiResponse.success(itemService.updateItem(sellerId, itemId, request));
    }

    @PostMapping("/items/{itemId}/off-shelf")
    public ApiResponse<Void> offShelfItem(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String itemId) {

        String sellerId = loginUserHelper.getCurrentUserId(authorizationHeader);
        itemService.offShelfItem(sellerId, itemId);
        return ApiResponse.success();
    }

    @GetMapping("/items/{itemId}")
    public ApiResponse<ItemDetailResponse> getItemDetail(@PathVariable String itemId) {
        return ApiResponse.success(itemService.getItemDetail(itemId));
    }

    @GetMapping("/items/mine")
    public ApiResponse<List<ItemListResponse>> listMyItems(
            @RequestHeader("Authorization") String authorizationHeader) {

        return ApiResponse.success(itemService.listMyItems(loginUserHelper.getCurrentUserId(authorizationHeader)));
    }

    @GetMapping("/search/items")
    public ApiResponse<SearchItemPageResponse> searchItems(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Integer minCondition,
            @RequestParam(required = false) Integer maxCondition,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {

        return ApiResponse.success(
                itemService.searchItems(
                        q, categoryId, minPrice, maxPrice,
                        minCondition, maxCondition, sort, page, pageSize
                )
        );
    }

    @GetMapping("/recommend/items")
    public ApiResponse<SearchItemPageResponse> recommendItems(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {

        return ApiResponse.success(itemService.recommendItems(page, pageSize));
    }

    @PostMapping("/items/{itemId}/comments")
    public ApiResponse<Void> addItemComment(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable String itemId,
            @Valid @RequestBody AddCommentRequest request) {

        String userId = loginUserHelper.getCurrentUserId(authorizationHeader);
        itemService.addItemComment(userId, itemId, request.getComment(), request.getRating());

        return ApiResponse.success();
    }

    @GetMapping("/items/{itemId}/comments")
    public ApiResponse<List<ItemComment>> getItemComments(@PathVariable String itemId) {
        return ApiResponse.success(itemService.getItemComments(itemId));
    }
}
