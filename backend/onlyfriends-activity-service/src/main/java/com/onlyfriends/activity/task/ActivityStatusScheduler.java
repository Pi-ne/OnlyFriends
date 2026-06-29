package com.onlyfriends.activity.task;

import com.onlyfriends.activity.service.ActivityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityStatusScheduler {
    private final ActivityService activityService;

    @Scheduled(fixedDelayString = "${app.activity-status-transition.fixed-delay:60000}")
    public void transitionStatuses() {
        int changed = activityService.transitionActivityStatuses();
        if (changed > 0) {
            log.info("Transitioned {} activity statuses", changed);
        }
    }
}
