package com.tmpro.model.dto;

public class StatisticsSummaryDTO {

    private long totalRecords;
    private int totalGoals;
    private int totalAssists;

    public StatisticsSummaryDTO() {
    }

    public StatisticsSummaryDTO(long totalRecords, int totalGoals, int totalAssists) {
        this.totalRecords = totalRecords;
        this.totalGoals = totalGoals;
        this.totalAssists = totalAssists;
    }

    public long getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(long totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getTotalGoals() {
        return totalGoals;
    }

    public void setTotalGoals(int totalGoals) {
        this.totalGoals = totalGoals;
    }

    public int getTotalAssists() {
        return totalAssists;
    }

    public void setTotalAssists(int totalAssists) {
        this.totalAssists = totalAssists;
    }
}
