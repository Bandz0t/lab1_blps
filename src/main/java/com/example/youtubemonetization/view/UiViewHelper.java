package com.example.youtubemonetization.view;

import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.CopyrightStatus;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.PayoutStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import com.example.youtubemonetization.enums.ValidationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component("ui")
public class UiViewHelper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String statusLabel(Enum<?> status) {
        if (status == null) {
            return "—";
        }
        return switch (status.name()) {
            case "PENDING" -> "PENDING";
            case "PASSED" -> "PASSED";
            case "FAILED" -> "FAILED";
            case "CLEARED" -> "CLEARED";
            case "VIOLATION_FOUND" -> "VIOLATION_FOUND";
            case "NEEDS_EDITING" -> "NEEDS_EDITING";
            case "ENABLED" -> "ENABLED";
            case "DISABLED" -> "DISABLED";
            case "UPLOADED" -> "UPLOADED";
            case "READY_FOR_REVIEW" -> "READY_FOR_REVIEW";
            case "REJECTED" -> "REJECTED";
            case "PUBLISHED" -> "PUBLISHED";
            case "PROCESSED" -> "PROCESSED";
            case "OPEN" -> "OPEN";
            case "RESOLVED" -> "RESOLVED";
            default -> status.name();
        };
    }

    public String badgeClass(Enum<?> status) {
        if (status == null) {
            return "badge badge-neutral";
        }
        return switch (status.name()) {
            case "PASSED", "CLEARED", "ENABLED", "PUBLISHED", "PROCESSED", "RESOLVED" -> "badge badge-success";
            case "FAILED", "DISABLED", "REJECTED", "OPEN", "NEEDS_EDITING" -> "badge badge-danger";
            case "READY_FOR_REVIEW" -> "badge badge-info";
            default -> "badge badge-warning";
        };
    }

    public String processStepClass(Video video, String step) {
        int currentIndex = processIndex(resolveCurrentStep(video));
        int targetIndex = processIndex(step);
        if (currentIndex > targetIndex) {
            return "timeline-step done";
        }
        if (currentIndex == targetIndex) {
            return "timeline-step active";
        }
        return "timeline-step";
    }

    public String resolveCurrentStep(Video video) {
        if (video.getUploadStatus() == UploadStatus.REJECTED || video.getValidationStatus() == ValidationStatus.FAILED) {
            return "REJECTED";
        }
        if (video.getPublishedAt() != null) {
            return "PUBLISHED";
        }
        if (video.getValidationStatus() == ValidationStatus.PENDING) {
            return "TECHNICAL_VALIDATION";
        }
        if (video.getValidationStatus() == ValidationStatus.PASSED && video.getCopyrightStatus() == CopyrightStatus.PENDING) {
            return "COPYRIGHT_CHECK";
        }
        if (video.getCopyrightStatus() == CopyrightStatus.NEEDS_EDITING) {
            return "EDIT_REQUIRED";
        }
        if (video.getCopyrightStatus() == CopyrightStatus.CLEARED && video.getMonetizationStatus() == MonetizationStatus.PENDING) {
            return "MONETIZATION";
        }
        if (video.getPublishedAt() != null && video.getMonetizationStatus() != null) {
            return "REVENUE";
        }
        return "IN_PROGRESS";
    }

    public String humanStep(String step) {
        if (step == null) {
            return "—";
        }
        return switch (step) {
            case "TECHNICAL_VALIDATION" -> "Техническая проверка";
            case "WAITING_FOR_COPYRIGHT_CHECK", "COPYRIGHT_CHECK" -> "Проверка авторских прав";
            case "EDIT_REQUIRED" -> "Требуется редактирование";
            case "WAITING_FOR_MONETIZATION_SELECTION", "MONETIZATION" -> "Выбор монетизации";
            case "PUBLISHED" -> "Видео опубликовано";
            case "REVENUE" -> "Начисление доходов";
            case "REJECTED" -> "Видео отклонено";
            default -> step;
        };
    }

    public String yesNo(boolean value) {
        return value ? "Да" : "Нет";
    }

    public String formatDateTime(LocalDateTime value) {
        return value == null ? "—" : value.format(DATE_TIME_FORMATTER);
    }

    public String money(BigDecimal value, String currency) {
        if (value == null) {
            return "0.00 " + (currency == null ? "USD" : currency);
        }
        return value.setScale(2, RoundingMode.HALF_UP) + " " + (currency == null ? "USD" : currency);
    }

    public long countPublished(List<Video> videos) {
        return videos.stream().filter(video -> video.getUploadStatus() == UploadStatus.PUBLISHED).count();
    }

    public long countMonetized(List<Video> videos) {
        return videos.stream().filter(video -> video.getMonetizationStatus() == MonetizationStatus.ENABLED).count();
    }

    public long countClaims(List<Video> videos) {
        return videos.stream().filter(video -> video.getCopyrightStatus() == CopyrightStatus.NEEDS_EDITING).count();
    }

    public String payoutBadgeClass(PayoutStatus status) {
        return badgeClass(status);
    }

    private int processIndex(String step) {
        return switch (step) {
            case "TECHNICAL_VALIDATION" -> 0;
            case "COPYRIGHT_CHECK", "WAITING_FOR_COPYRIGHT_CHECK" -> 1;
            case "EDIT_REQUIRED" -> 2;
            case "MONETIZATION", "WAITING_FOR_MONETIZATION_SELECTION" -> 3;
            case "PUBLISHED" -> 4;
            case "REVENUE" -> 5;
            case "REJECTED" -> 6;
            default -> 1;
        };
    }
}
