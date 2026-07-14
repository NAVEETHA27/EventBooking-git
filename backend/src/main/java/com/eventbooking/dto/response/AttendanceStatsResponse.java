package com.eventbooking.dto.response;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AttendanceStatsResponse {
    private long registeredParticipants;
    private long checkedInParticipants;
    private long absentParticipants;
    private double attendancePercentage;
    private long liveAttendanceCount;
}
