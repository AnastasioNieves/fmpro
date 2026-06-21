package com.tmpro.model.dto;

public class StatisticsSummaryDTO {

    private int totalRecords;
    private int totalGoals;
    private int totalAssists;

    public StatisticsSummaryDTO() {
    }

    public StatisticsSummaryDTO(int totalRecords, int totalGoals, int totalAssists) {
        this.totalRecords = totalRecords;
        this.totalGoals = totalGoals;
        this.totalAssists = totalAssists;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
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
