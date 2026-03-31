package com.example.youtubemonetization.controller.ui;

import com.example.youtubemonetization.dto.request.CopyrightCheckRequest;
import com.example.youtubemonetization.dto.request.EditVideoRequest;
import com.example.youtubemonetization.dto.request.MonetizationRequest;
import com.example.youtubemonetization.dto.request.MonthlyProcessRequest;
import com.example.youtubemonetization.dto.request.VideoCreateRequest;
import com.example.youtubemonetization.dto.response.AuthorStatsResponse;
import com.example.youtubemonetization.dto.response.MonthlyProcessResponse;
import com.example.youtubemonetization.entity.Claim;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.MonetizationType;
import com.example.youtubemonetization.service.ClaimDataService;
import com.example.youtubemonetization.service.CopyrightService;
import com.example.youtubemonetization.service.MonetizationService;
import com.example.youtubemonetization.service.PayoutDataService;
import com.example.youtubemonetization.service.ProcessService;
import com.example.youtubemonetization.service.RevenueService;
import com.example.youtubemonetization.service.StatsService;
import com.example.youtubemonetization.service.UserDataService;
import com.example.youtubemonetization.service.VideoService;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class UiController {

    private final UserDataService userDataService;
    private final VideoService videoService;
    private final RevenueService revenueService;
    private final PayoutDataService payoutDataService;
    private final StatsService statsService;
    private final ProcessService processService;
    private final ClaimDataService claimDataService;
    private final CopyrightService copyrightService;
    private final MonetizationService monetizationService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        YearMonth currentPeriod = YearMonth.now();
        List<Video> videos = videoService.getVideos(user.getId());
        AuthorStatsResponse stats = statsService.getAuthorStats(user.getId(), Optional.of(currentPeriod.getYear()), Optional.of(currentPeriod.getMonthValue()));
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("user", user);
        model.addAttribute("videos", videos);
        model.addAttribute("stats", stats);
        model.addAttribute("payouts", payoutDataService.getByUserId(user.getId()));
        model.addAttribute("currentPeriod", currentPeriod);
        return "dashboard";
    }

    @GetMapping("/videos")
    public String videos(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", user);
        model.addAttribute("videos", "ADMIN".equals(user.getRole()) ? videoService.getAllVideos() : videoService.getVideos(user.getId()));
        return "videos";
    }

    @GetMapping("/videos/create")
    public String createVideoForm(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        VideoCreateRequest form = new VideoCreateRequest();
        form.setAuthorId(user.getId());
        form.setFormat("mp4");
        form.setSizeBytes(52428800L);
        form.setDurationSeconds(120);
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", user);
        model.addAttribute("videoForm", form);
        return "video-create";
    }

    @PostMapping("/videos/create")
    public String createVideo(Authentication authentication, VideoCreateRequest request, RedirectAttributes redirectAttributes) {
        User user = currentUser(authentication);
        request.setAuthorId(user.getId());
        try {
            Video video = videoService.createVideo(request);
            redirectAttributes.addFlashAttribute("successMessage", "Видео успешно создано. Запущен цикл публикации.");
            return "redirect:/videos/" + video.getId();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/videos/create";
        }
    }

    @GetMapping("/videos/{id}")
    public String videoDetails(Authentication authentication, @PathVariable Long id, Model model) {
        User user = currentUser(authentication);
        Video video = videoService.getVideo(id);
        assertVideoOwnerOrPrivileged(video, user);
        List<Claim> claims = claimDataService.getByVideoId(id);
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", user);
        model.addAttribute("video", video);
        model.addAttribute("claims", claims);
        model.addAttribute("processState", processService.getProcessState(id));
        model.addAttribute("revenues", revenueService.getByVideo(id));
        model.addAttribute("copyrightForm", new CopyrightCheckRequest());
        return "video-details";
    }

    @PostMapping("/videos/{id}/copyright-check")
    public String copyrightCheck(@PathVariable Long id, CopyrightCheckRequest request, RedirectAttributes redirectAttributes) {
        try {
            copyrightService.processCopyrightCheck(id, request);
            redirectAttributes.addFlashAttribute("successMessage", request.isHasViolation()
                    ? "Обнаружено нарушение. Отправили ролик на доработку."
                    : "Проверка авторских прав завершена: нарушений не найдено.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/videos/" + id;
    }

    @GetMapping("/videos/{id}/edit")
    public String editVideoForm(Authentication authentication, @PathVariable Long id, Model model) {
        User user = currentUser(authentication);
        Video video = videoService.getVideo(id);
        assertVideoOwnerOrPrivileged(video, user);
        EditVideoRequest form = new EditVideoRequest();
        form.setNewFilePath(video.getFilePath());
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", user);
        model.addAttribute("video", video);
        model.addAttribute("claims", claimDataService.getByVideoId(id));
        model.addAttribute("editForm", form);
        return "video-edit";
    }

    @PostMapping("/videos/{id}/edit")
    public String editVideo(@PathVariable Long id, EditVideoRequest request, RedirectAttributes redirectAttributes) {
        try {
            videoService.editVideo(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Видео обновлено. Можно повторно запустить проверку прав.");
            return "redirect:/videos/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/videos/" + id + "/edit";
        }
    }

    @GetMapping("/videos/{id}/monetization")
    public String monetizationForm(Authentication authentication, @PathVariable Long id, Model model) {
        User user = currentUser(authentication);
        Video video = videoService.getVideo(id);
        assertVideoOwnerOrPrivileged(video, user);
        MonetizationRequest form = new MonetizationRequest();
        form.setMonetizationType(video.getMonetizationType());
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", user);
        model.addAttribute("video", video);
        model.addAttribute("monetizationForm", form);
        model.addAttribute("monetizationTypes", MonetizationType.values());
        return "monetization";
    }

    @PostMapping("/videos/{id}/monetization")
    public String monetization(@PathVariable Long id, MonetizationRequest request, RedirectAttributes redirectAttributes) {
        try {
            monetizationService.chooseMonetization(id, request.getMonetizationType());
            redirectAttributes.addFlashAttribute("successMessage", "Параметры монетизации применены, статус публикации обновлен.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/videos/" + id;
    }

    @GetMapping("/revenues")
    public String revenues(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        int actualYear = YearMonth.now().getYear();
        int actualMonth = YearMonth.now().getMonthValue();
        model.addAttribute("currentPage", "revenues");
        model.addAttribute("user", user);
        model.addAttribute("stats", statsService.getAuthorStats(user.getId(), Optional.of(actualYear), Optional.of(actualMonth)));
        model.addAttribute("revenues", revenueService.getByAuthor(user.getId(), actualYear, actualMonth));
        model.addAttribute("selectedYear", actualYear);
        model.addAttribute("selectedMonth", actualMonth);
        return "revenues";
    }

    @GetMapping("/payouts")
    public String payouts(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        model.addAttribute("currentPage", "payouts");
        model.addAttribute("user", user);
        model.addAttribute("payouts", payoutDataService.getByUserId(user.getId()));
        return "payouts";
    }

    @GetMapping("/processes/{videoId}")
    public String processDetails(Authentication authentication, @PathVariable Long videoId, Model model) {
        User user = currentUser(authentication);
        Video video = videoService.getVideo(videoId);
        assertVideoOwnerOrPrivileged(video, user);
        model.addAttribute("currentPage", "processes");
        model.addAttribute("user", user);
        model.addAttribute("video", video);
        model.addAttribute("claims", claimDataService.getByVideoId(videoId));
        model.addAttribute("processState", processService.getProcessState(videoId));
        return "process-details";
    }

    @PostMapping("/processes/{videoId}/continue")
    public String continueProcess(@PathVariable Long videoId, RedirectAttributes redirectAttributes) {
        try {
            processService.continueProcess(videoId);
            redirectAttributes.addFlashAttribute("successMessage", "Действие обработки успешно выполнено.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/processes/" + videoId;
    }

    @GetMapping("/admin")
    public String admin(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        MonthlyProcessRequest request = new MonthlyProcessRequest();
        request.setYear(YearMonth.now().getYear());
        request.setMonth(YearMonth.now().getMonthValue());
        model.addAttribute("currentPage", "admin");
        model.addAttribute("user", user);
        model.addAttribute("videos", videoService.getAllVideos());
        model.addAttribute("monthlyProcessForm", request);
        return "admin";
    }

    @PostMapping("/admin/process-monthly")
    public String processMonthly(MonthlyProcessRequest request, RedirectAttributes redirectAttributes) {
        try {
            MonthlyProcessResponse response = processService.runMonthlyRevenueProcess(
                    Optional.ofNullable(request.getYear()),
                    Optional.ofNullable(request.getMonth())
            );
            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Расчёт завершен: videos=" + response.getProcessedVideos() + ", payouts=" + response.getCreatedPayouts()
            );
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin";
    }

    @GetMapping("/ui")
    public String uiEntry() {
        return "redirect:/dashboard";
    }

    private User currentUser(Authentication authentication) {
        return userDataService.findByUsername(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Текущий пользователь не найден"));
    }

    private void assertVideoOwnerOrPrivileged(Video video, User user) {
        if (video.getAuthor().getId().equals(user.getId())) {
            return;
        }
        if ("ADMIN".equals(user.getRole()) || "MODERATOR".equals(user.getRole())) {
            return;
        }
        throw new IllegalStateException("Недостаточно прав для просмотра этого видео");
    }
}
