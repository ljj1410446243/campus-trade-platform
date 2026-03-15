package com.campus.trade.item.dto;

import java.util.List;

/**
 * 搜索分页结果
 */
public class SearchItemPageResponse {

    private List<SearchItemResponse> list;
    private long total;

    public SearchItemPageResponse() {
    }

    public SearchItemPageResponse(List<SearchItemResponse> list, long total) {
        this.list = list;
        this.total = total;
    }

    public List<SearchItemResponse> getList() {
        return list;
    }

    public void setList(List<SearchItemResponse> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
