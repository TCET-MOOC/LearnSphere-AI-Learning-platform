package com.MOOC.OnlineLearningPlatfrom.Dto;

/** Tab counts for the admin user list (W7-12); computed over every account, not one page. */
public record UserCountsDto(long all, long students, long teachers, long flagged, long blacklisted) {
}
