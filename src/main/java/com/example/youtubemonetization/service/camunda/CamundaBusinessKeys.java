package com.example.youtubemonetization.service.camunda;

import java.time.YearMonth;

public final class CamundaBusinessKeys {

    public static final String VIDEO_PROCESS_KEY = "youtube_monetization_process";
    public static final String MONTHLY_PAYOUT_PROCESS_KEY = "monthly_payout_process";

    private CamundaBusinessKeys() {
    }

    public static String video(Long videoId) {
        return "video-" + videoId;
    }

    public static String monthlyPayout(YearMonth period) {
        return "monthly-payout-" + period;
    }
}
