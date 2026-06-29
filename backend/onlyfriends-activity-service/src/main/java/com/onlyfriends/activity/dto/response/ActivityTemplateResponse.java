package com.onlyfriends.activity.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class ActivityTemplateResponse {
    private Long templateId;
    private String name;
    private String category;
    private String description;
    private List<String> defaultTags;
    private Integer defaultDuration;
    private Integer defaultMaxParticipants;
    private String safetyNotes;
    private String coverUrl;
}
