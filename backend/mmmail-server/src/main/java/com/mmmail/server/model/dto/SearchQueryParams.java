package com.mmmail.server.model.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public class SearchQueryParams {
    private String q;
    private String types;
    private Integer page;
    private Integer size;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime from;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime to;
    private Long orgId;

    public SearchQueryCriteria toCriteria(int defaultPage, int defaultSize) {
        return new SearchQueryCriteria(
                q,
                types,
                page == null ? defaultPage : page,
                size == null ? defaultSize : size,
                from,
                to,
                orgId
        );
    }

    public String getQ() { return q; }
    public void setQ(String q) { this.q = q; }
    public String getTypes() { return types; }
    public void setTypes(String types) { this.types = types; }
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    public LocalDateTime getFrom() { return from; }
    public void setFrom(LocalDateTime from) { this.from = from; }
    public LocalDateTime getTo() { return to; }
    public void setTo(LocalDateTime to) { this.to = to; }
    public Long getOrgId() { return orgId; }
    public void setOrgId(Long orgId) { this.orgId = orgId; }
}
