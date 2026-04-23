package com.campus.trade.item.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索分页结果
 */
public class SearchItemPageResponse {

    private String requestId;
    private List<SearchItemResponse> list;
    private long total;

    public SearchItemPageResponse() {
    }

    public SearchItemPageResponse(List<SearchItemResponse> list, long total) {
        this.list = list == null ? new ArrayList<>() : new ArrayList<>(list);
        this.total = total;
    }

    public SearchItemPageResponse(String requestId, List<SearchItemResponse> list, long total) {
        this.requestId = requestId;
        this.list = list == null ? new ArrayList<>() : new ArrayList<>(list);
        this.total = total;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<SearchItemResponse> getList() {
        return list;
    }

    public void setList(List<SearchItemResponse> list) {
        this.list = list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
