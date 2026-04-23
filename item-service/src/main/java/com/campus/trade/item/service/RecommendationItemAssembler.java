package com.campus.trade.item.service;

import com.campus.trade.item.dto.LocationDTO;
import com.campus.trade.item.dto.SearchItemResponse;
import com.campus.trade.item.model.FileDocument;
import com.campus.trade.item.model.Item;
import com.campus.trade.item.model.User;
import com.campus.trade.item.repository.FileRepository;
import com.campus.trade.item.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RecommendationItemAssembler {

    private static final String FILE_STATUS_DELETED = "DELETED";
    private static final int DEFAULT_CREDIT_SCORE = 0;
    private static final int DEFAULT_REVIEW_COUNT = 0;
    private static final double DEFAULT_AVERAGE_RATING = 0D;
    private static final String DEFAULT_CREDIT_LEVEL = "NEW";

    private final FileRepository fileRepository;
    private final UserRepository userRepository;

    public RecommendationItemAssembler(FileRepository fileRepository, UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.userRepository = userRepository;
    }

    public Map<String, FileDocument> loadFileDocumentMap(Collection<Item> items) {
        Set<String> imageUrls = new HashSet<>();
        for (Item item : items) {
            if (item == null) {
                continue;
            }
            if (item.getImages() != null) {
                item.getImages().stream()
                        .filter(this::hasText)
                        .forEach(imageUrls::add);
            }
            if (hasText(item.getCoverImage())) {
                imageUrls.add(item.getCoverImage());
            }
        }

        if (imageUrls.isEmpty()) {
            return Map.of();
        }

        Map<String, FileDocument> result = new HashMap<>();
        fileRepository.findByUrlIn(imageUrls).forEach(fileDocument -> {
            if (fileDocument == null || FILE_STATUS_DELETED.equalsIgnoreCase(fileDocument.getStatus())) {
                return;
            }
            if (hasText(fileDocument.getUrl())) {
                result.put(fileDocument.getUrl(), fileDocument);
            }
        });
        return result;
    }

    public Map<String, User> loadSellerMap(Collection<Item> items) {
        Set<String> sellerIds = new HashSet<>();
        for (Item item : items) {
            if (item != null && hasText(item.getSellerId())) {
                sellerIds.add(item.getSellerId());
            }
        }
        Map<String, User> result = new HashMap<>();
        for (String sellerId : sellerIds) {
            userRepository.findById(sellerId).ifPresent(user -> result.put(sellerId, user));
        }
        return result;
    }

    public SearchItemResponse toSearchItemResponse(Item item,
                                                   Double distanceMeters,
                                                   int hotScore,
                                                   String recommendReason,
                                                   List<String> sourceChannels,
                                                   SearchItemResponse.DebugScores debugScores,
                                                   Map<String, User> sellerMap,
                                                   Map<String, FileDocument> fileDocumentMap) {
        SearchItemResponse response = new SearchItemResponse();
        response.setItemId(item.getId());
        response.setTitle(item.getTitle());
        response.setPrice(item.getPrice());
        response.setConditionStar(item.getConditionStar());
        response.setCoverImage(resolveThumbnailCoverImage(item, fileDocumentMap));
        response.setHotScore(hotScore);
        response.setLocation(toLocationDto(item.getLocation()));
        response.setDistanceMeters(distanceMeters);
        response.setRecommendReason(recommendReason);
        response.setSourceChannels(sourceChannels);
        response.setDebugScores(debugScores);
        response.setSeller(buildSellerInfo(item.getSellerId(), sellerMap));
        return response;
    }

    private SearchItemResponse.SellerInfo buildSellerInfo(String sellerId, Map<String, User> sellerMap) {
        SearchItemResponse.SellerInfo sellerInfo = new SearchItemResponse.SellerInfo();
        sellerInfo.setUserId(sellerId);
        User seller = sellerMap.get(sellerId);
        if (seller == null) {
            sellerInfo.setCreditScore(DEFAULT_CREDIT_SCORE);
            sellerInfo.setCreditLevel(DEFAULT_CREDIT_LEVEL);
            sellerInfo.setReviewCount(DEFAULT_REVIEW_COUNT);
            sellerInfo.setAverageRating(DEFAULT_AVERAGE_RATING);
            return sellerInfo;
        }
        sellerInfo.setNickname(resolveDisplayName(seller));
        sellerInfo.setAvatarUrl(seller.getAvatarUrl());
        sellerInfo.setCreditScore(seller.getCreditScore() == null ? DEFAULT_CREDIT_SCORE : seller.getCreditScore());
        sellerInfo.setCreditLevel(!hasText(seller.getCreditLevel()) ? DEFAULT_CREDIT_LEVEL : seller.getCreditLevel());
        sellerInfo.setReviewCount(seller.getReviewCount() == null ? DEFAULT_REVIEW_COUNT : seller.getReviewCount());
        sellerInfo.setAverageRating(seller.getAverageRating() == null ? DEFAULT_AVERAGE_RATING : seller.getAverageRating());
        return sellerInfo;
    }

    private String resolveDisplayName(User seller) {
        if (hasText(seller.getNickname())) {
            return seller.getNickname();
        }
        if (hasText(seller.getUsername())) {
            return seller.getUsername();
        }
        return seller.getId();
    }

    private String resolveThumbnailCoverImage(Item item, Map<String, FileDocument> fileDocumentMap) {
        String coverImage = hasText(item.getCoverImage())
                ? item.getCoverImage()
                : (item.getImages() == null || item.getImages().isEmpty() ? null : item.getImages().get(0));
        if (!hasText(coverImage)) {
            return null;
        }
        FileDocument fileDocument = fileDocumentMap.get(coverImage);
        if (fileDocument == null) {
            return coverImage;
        }
        if (hasText(fileDocument.getThumbnailUrl())) {
            return fileDocument.getThumbnailUrl();
        }
        if (hasText(fileDocument.getPreviewUrl())) {
            return fileDocument.getPreviewUrl();
        }
        if (hasText(fileDocument.getOriginalUrl())) {
            return fileDocument.getOriginalUrl();
        }
        return coverImage;
    }

    private LocationDTO toLocationDto(Item.Location location) {
        if (location == null) {
            return null;
        }
        LocationDTO locationDTO = new LocationDTO();
        locationDTO.setLat(location.getLat());
        locationDTO.setLng(location.getLng());
        locationDTO.setAddress(location.getAddress());
        locationDTO.setPoiName(location.getPoiName());
        locationDTO.setCoordType(location.getCoordType());
        locationDTO.setSource(location.getSource());
        return locationDTO;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
