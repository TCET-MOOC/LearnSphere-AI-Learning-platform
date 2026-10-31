package com.MOOC.OnlineLearningPlatfrom.Dto;

import com.MOOC.OnlineLearningPlatfrom.Entity.LiveSession;

public class LiveSessionSourceDto {
    private LiveSession.SourceType sourceType;
    private String providerVideoId;
    private String sourceUrl;

    public LiveSessionSourceDto() {}

    public LiveSessionSourceDto(LiveSession.SourceType sourceType, String providerVideoId, String sourceUrl) {
        this.sourceType = sourceType;
        this.providerVideoId = providerVideoId;
        this.sourceUrl = sourceUrl;
    }

    public LiveSession.SourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(LiveSession.SourceType sourceType) {
        this.sourceType = sourceType;
    }

    public String getProviderVideoId() {
        return providerVideoId;
    }

    public void setProviderVideoId(String providerVideoId) {
        this.providerVideoId = providerVideoId;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
