package com.webapp.citizen_services_web_app_backend.dto;

import java.sql.Date;

public class ComplaintTrendResponse {

    private Date date;
    private Long count;

    public ComplaintTrendResponse(Date date, Long count) {
        this.date = date;
        this.count = count;
    }

    public Date getDate() {
        return date;
    }

    public Long getCount() {
        return count;
    }
}