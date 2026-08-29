package com.sunrisedental.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardStats {

    private int todayAppointments;
    private int scheduled;
    private int completedToday;
    private int cancelledToday;
    private BigDecimal todayRevenue = BigDecimal.ZERO;
    private BigDecimal monthRevenue = BigDecimal.ZERO;
    private int registeredPatients;
    private int unreadNotifications;
    private List<Appointment> upcoming = new ArrayList<>();
    private List<Map<String, Object>> treatmentMix = new ArrayList<>();
    private List<Map<String, Object>> dentistLoad = new ArrayList<>();

    public int getTodayAppointments() {
        return todayAppointments;
    }

    public void setTodayAppointments(int todayAppointments) {
        this.todayAppointments = todayAppointments;
    }

    public int getScheduled() {
        return scheduled;
    }

    public void setScheduled(int scheduled) {
        this.scheduled = scheduled;
    }

    public int getCompletedToday() {
        return completedToday;
    }

    public void setCompletedToday(int completedToday) {
        this.completedToday = completedToday;
    }

    public int getCancelledToday() {
        return cancelledToday;
    }

    public void setCancelledToday(int cancelledToday) {
        this.cancelledToday = cancelledToday;
    }

    public BigDecimal getTodayRevenue() {
        return todayRevenue;
    }

    public void setTodayRevenue(BigDecimal todayRevenue) {
        this.todayRevenue = todayRevenue;
    }

    public BigDecimal getMonthRevenue() {
        return monthRevenue;
    }

    public void setMonthRevenue(BigDecimal monthRevenue) {
        this.monthRevenue = monthRevenue;
    }

    public int getRegisteredPatients() {
        return registeredPatients;
    }

    public void setRegisteredPatients(int registeredPatients) {
        this.registeredPatients = registeredPatients;
    }

    public int getUnreadNotifications() {
        return unreadNotifications;
    }

    public void setUnreadNotifications(int unreadNotifications) {
        this.unreadNotifications = unreadNotifications;
    }

    public List<Appointment> getUpcoming() {
        return upcoming;
    }

    public void setUpcoming(List<Appointment> upcoming) {
        this.upcoming = upcoming;
    }

    public List<Map<String, Object>> getTreatmentMix() {
        return treatmentMix;
    }

    public void setTreatmentMix(List<Map<String, Object>> treatmentMix) {
        this.treatmentMix = treatmentMix;
    }

    public List<Map<String, Object>> getDentistLoad() {
        return dentistLoad;
    }

    public void setDentistLoad(List<Map<String, Object>> dentistLoad) {
        this.dentistLoad = dentistLoad;
    }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("todayAppointments", todayAppointments);
        map.put("scheduled", scheduled);
        map.put("completedToday", completedToday);
        map.put("cancelledToday", cancelledToday);
        map.put("todayRevenue", todayRevenue);
        map.put("monthRevenue", monthRevenue);
        map.put("registeredPatients", registeredPatients);
        map.put("unreadNotifications", unreadNotifications);
        map.put("upcoming", upcoming);
        map.put("treatmentMix", treatmentMix);
        map.put("dentistLoad", dentistLoad);
        return map;
    }
}
