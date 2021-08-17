package com.qiniu.android.http.metrics;

import com.qiniu.android.utils.Utils;

import java.util.Date;

public class UploadMetrics {

    protected Date startDate = null;
    protected Date endDate = null;

    public void start() {
        startDate = new Date();
    }

    public void end() {
        endDate = new Date();
    }

    public Date getStartDate() {
        return startDate;
    }

    public long totalElapsedTime(){
        if (startDate == null || endDate == null) {
            return 0;
        }
        return Utils.dateDuration(startDate, endDate);
    }
}
