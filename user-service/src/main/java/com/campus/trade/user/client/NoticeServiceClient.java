package com.campus.trade.user.client;

import com.campus.trade.user.dto.InternalNotificationRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NoticeServiceClient {

    private final RestClient restClient;

    public NoticeServiceClient(@Value("${notice.service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public void createNotification(InternalNotificationRequest request) {
        restClient.post()
                .uri("/internal/notifications")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
