package com.onlyfriends.ai.dto;

import lombok.Data;

import java.util.List;

@Data
public class AiActivityPlanResponse {
    private String title;
    private String description;
    private List<String> tags;
    private String locationSuggestion;
    private Integer suggestedDurationHours;
    private Integer suggestedMaxParticipants;
    private List<String> safetyNotes;
    private List<String> agenda;
}
