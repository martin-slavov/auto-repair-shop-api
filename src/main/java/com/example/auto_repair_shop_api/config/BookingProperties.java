package com.example.auto_repair_shop_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Booking rules for service visits. The defaults below apply when nothing is configured; each value can be
 * overridden with an {@code app.booking.*} property (e.g. {@code app.booking.work-end=18:00}).
 * Times are in the server's local time.
 */
@ConfigurationProperties(prefix = "app.booking")
public class BookingProperties {

    private Set<DayOfWeek> workingDays = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
    private LocalTime workStart = LocalTime.of(9, 0);
    private LocalTime workEnd = LocalTime.of(17, 0);
    private int slotMinutes = 30;
    private int visitDurationMinutes = 60;

    public Set<DayOfWeek> getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(Set<DayOfWeek> workingDays) {
        this.workingDays = workingDays;
    }

    public LocalTime getWorkStart() {
        return workStart;
    }

    public void setWorkStart(LocalTime workStart) {
        this.workStart = workStart;
    }

    public LocalTime getWorkEnd() {
        return workEnd;
    }

    public void setWorkEnd(LocalTime workEnd) {
        this.workEnd = workEnd;
    }

    public int getSlotMinutes() {
        return slotMinutes;
    }

    public void setSlotMinutes(int slotMinutes) {
        this.slotMinutes = slotMinutes;
    }

    public int getVisitDurationMinutes() {
        return visitDurationMinutes;
    }

    public void setVisitDurationMinutes(int visitDurationMinutes) {
        this.visitDurationMinutes = visitDurationMinutes;
    }

    public Duration visitDuration() {
        return Duration.ofMinutes(visitDurationMinutes);
    }

    /** Latest start time so that the visit still ends within working hours (16:00 with the defaults). */
    public LocalTime lastStart() {
        return workEnd.minus(visitDuration());
    }
}
