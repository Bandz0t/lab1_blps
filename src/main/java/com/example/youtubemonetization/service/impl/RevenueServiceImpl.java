package com.example.youtubemonetization.service.impl;

import com.example.youtubemonetization.entity.Revenue;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.exception.ConflictException;
import com.example.youtubemonetization.security.AccessGuard;
import com.example.youtubemonetization.security.SecurityPrivileges;
import com.example.youtubemonetization.service.RevenueDataService;
import com.example.youtubemonetization.service.RevenueService;
import com.example.youtubemonetization.service.VideoDataService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RevenueServiceImpl implements RevenueService {

    private final RevenueDataService revenueDataService;
    private final VideoDataService videoDataService;
    private final AccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public List<Revenue> getByAuthor(Long authorId, Integer year, Integer month) {
        accessGuard.requireSameUserOrAll(authorId, SecurityPrivileges.REVENUE_READ_OWN, SecurityPrivileges.REVENUE_READ_ALL);
        return revenueDataService.getByAuthorId(authorId).stream()
                .filter(revenue -> year == null || revenue.getPeriodYear().equals(year))
                .filter(revenue -> month == null || revenue.getPeriodMonth().equals(month))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Revenue> getByVideo(Long videoId) {
        Video video = videoDataService.getById(videoId);
        accessGuard.requireOwnOrAll(video.getAuthor().getId(), SecurityPrivileges.REVENUE_READ_OWN, SecurityPrivileges.REVENUE_READ_ALL);
        return revenueDataService.getByVideoId(videoId);
    }

    @Override
    public List<Revenue> calculateMonthlyRevenue(YearMonth period) {
        accessGuard.requirePrivilege(SecurityPrivileges.MONTHLY_PROCESS_RUN);
        List<Video> publishedVideos = videoDataService.getByUploadStatus(UploadStatus.PUBLISHED);
        List<Revenue> created = new ArrayList<>();
        for (Video video : publishedVideos) {
            revenueDataService.findByVideoIdAndPeriod(video.getId(), period.getYear(), period.getMonthValue())
                    .ifPresent(existing -> {
                        throw new ConflictException(
                                "Доход для videoId=" + video.getId() + " за " + period + " уже рассчитан"
                        );
                    });

            Revenue revenue = new Revenue();
            revenue.setVideo(video);
            revenue.setPeriodYear(period.getYear());
            revenue.setPeriodMonth(period.getMonthValue());

            long views = calculateViews(video, period);
            long monetizedViews = video.getMonetizationStatus() == MonetizationStatus.ENABLED
                    ? Math.max(0, Math.round(views * 0.72d))
                    : 0L;
            BigDecimal cpm = video.getMonetizationStatus() == MonetizationStatus.ENABLED
                    ? BigDecimal.valueOf(2.40 + (video.getId() % 5) * 0.55).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            BigDecimal amount = BigDecimal.valueOf(monetizedViews)
                    .divide(BigDecimal.valueOf(1000), 6, RoundingMode.HALF_UP)
                    .multiply(cpm)
                    .setScale(2, RoundingMode.HALF_UP);

            revenue.setViewsCount(views);
            revenue.setMonetizedViews(monetizedViews);
            revenue.setCpm(cpm);
            revenue.setAmount(amount);
            revenue.setCurrency("USD");
            created.add(revenueDataService.save(revenue));
        }
        return created;
    }

    private long calculateViews(Video video, YearMonth period) {
        long durationFactor = video.getDurationSeconds() == null ? 60 : video.getDurationSeconds();
        long titleFactor = video.getTitle().length() * 73L;
        long monthFactor = period.getMonthValue() * 1000L;
        return 1000L + (durationFactor * 9L) + titleFactor + monthFactor;
    }
}
