package com.campus.trade.item.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "app.location")
public class LocationProperties {

    private Duration requestTimeout = Duration.ofSeconds(3);
    private Amap amap = new Amap();

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Amap getAmap() {
        return amap;
    }

    public void setAmap(Amap amap) {
        this.amap = amap;
    }

    public static class Amap {
        private String baseUrl = "https://restapi.amap.com";
        private String key = "YOUR_AMAP_WEB_API_KEY";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }
    }
}
