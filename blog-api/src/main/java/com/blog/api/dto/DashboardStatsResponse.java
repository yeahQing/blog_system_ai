package com.blog.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long articleTotal;
    private long articlePublished;
    private long articleDrafts;
    private long totalViews;
    private long categories;
    private long tags;
    private long comments;
    private long guestbookMessages;
    private long friendLinksTotal;
    private long friendLinksEnabled;
}
