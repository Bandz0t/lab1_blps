package com.example.youtubemonetization.controller.ui;

import com.example.youtubemonetization.dto.request.CopyrightCheckRequest;
import com.example.youtubemonetization.dto.request.EditVideoRequest;
import com.example.youtubemonetization.dto.request.MonetizationRequest;
import com.example.youtubemonetization.dto.request.MonthlyProcessRequest;
import com.example.youtubemonetization.dto.request.VideoCreateRequest;
import com.example.youtubemonetization.dto.response.AuthorStatsResponse;
import com.example.youtubemonetization.dto.response.MonthlyProcessResponse;
import com.example.youtubemonetization.dto.response.ProcessStateResponse;
import com.example.youtubemonetization.entity.Claim;
import com.example.youtubemonetization.entity.User;
import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.MonetizationType;
import com.example.youtubemonetization.exception.EntityNotFoundException;
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
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping
public class UiController {

    private static final Long DEMO_USER_ID = 1L;

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
    public String dashboard(@RequestParam(defaultValue = "1") Long userId, Model model) {
        User user = loadUser(userId);
        YearMonth currentPeriod = YearMonth.now();
        List<Video> videos = videoService.getVideos(userId);
        AuthorStatsResponse stats = statsService.getAuthorStats(userId, Optional.of(currentPeriod.getYear()), Optional.of(currentPeriod.getMonthValue()));
        model.addAttribute("currentPage", "dashboard");
        model.addAttribute("user", user);
        model.addAttribute("videos", videos);
        model.addAttribute("stats", stats);
        model.addAttribute("payouts", payoutDataService.getByUserId(userId));
        model.addAttribute("currentPeriod", currentPeriod);
        return "dashboard";
    }

    @GetMapping("/videos")
    public String videos(@RequestParam(defaultValue = "1") Long userId, Model model) {
        User user = loadUser(userId);
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", user);
        model.addAttribute("videos", videoService.getVideos(userId));
        return "videos";
    }

    @GetMapping("/videos/create")
    public String createVideoForm(@RequestParam(defaultValue = "1") Long userId, Model model) {
        User user = loadUser(userId);
        VideoCreateRequest form = new VideoCreateRequest();
        form.setAuthorId(userId);
        form.setFormat("mp4");
        form.setSizeBytes(52428800L);
        form.setDurationSeconds(120);
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", user);
        model.addAttribute("videoForm", form);
        return "video-create";
    }

    @PostMapping("/videos/create")
    public String createVideo(VideoCreateRequest request, RedirectAttributes redirectAttributes) {
        try {
            Video video = videoService.createVideo(request);
            redirectAttributes.addFlashAttribute("successMessage", "Видео успешно создано и BPMN-процесс запущен.");
            return "redirect:/videos/" + video.getId();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/videos/create?userId=" + (request.getAuthorId() == null ? DEMO_USER_ID : request.getAuthorId());
        }
    }

    @GetMapping("/videos/{id}")
    public String videoDetails(@PathVariable Long id, Model model) {
        Video video = videoService.getVideo(id);
        List<Claim> claims = claimDataService.getByVideoId(id);
        ProcessStateResponse processState = processService.getProcessState(id);
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", video.getAuthor());
        model.addAttribute("video", video);
        model.addAttribute("claims", claims);
        model.addAttribute("processState", processState);
        model.addAttribute("revenues", revenueService.getByVideo(id));
        model.addAttribute("copyrightForm", new CopyrightCheckRequest());
        return "video-details";
    }

    @PostMapping("/videos/{id}/copyright-check")
    public String copyrightCheck(@PathVariable Long id, CopyrightCheckRequest request, RedirectAttributes redirectAttributes) {
        try {
            copyrightService.processCopyrightCheck(id, request);
            redirectAttributes.addFlashAttribute("successMessage", request.isHasViolation()
                    ? "Нарушение зафиксировано. Видео отправлено на редактирование."
                    : "Проверка авторских прав завершена: нарушений не найдено.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/videos/" + id;
    }

    @GetMapping("/videos/{id}/edit")
    public String editVideoForm(@PathVariable Long id, Model model) {
        Video video = videoService.getVideo(id);
        EditVideoRequest form = new EditVideoRequest();
        form.setNewFilePath(video.getFilePath());
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", video.getAuthor());
        model.addAttribute("video", video);
        model.addAttribute("claims", claimDataService.getByVideoId(id));
        model.addAttribute("editForm", form);
        return "video-edit";
    }

    @PostMapping("/videos/{id}/edit")
    public String editVideo(@PathVariable Long id, EditVideoRequest request, RedirectAttributes redirectAttributes) {
        try {
            videoService.editVideo(id, request);
            redirectAttributes.addFlashAttribute("successMessage", "Видео обновлено. Теперь можно выполнить повторную copyright-check.");
            return "redirect:/videos/" + id;
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/videos/" + id + "/edit";
        }
    }

    @GetMapping("/videos/{id}/monetization")
    public String monetizationForm(@PathVariable Long id, Model model) {
        Video video = videoService.getVideo(id);
        MonetizationRequest form = new MonetizationRequest();
        form.setMonetizationType(video.getMonetizationType());
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", video.getAuthor());
        model.addAttribute("video", video);
        model.addAttribute("monetizationForm", form);
        model.addAttribute("monetizationTypes", MonetizationType.values());
        return "monetization";
    }

    @PostMapping("/videos/{id}/monetization")
    public String monetization(@PathVariable Long id, MonetizationRequest request, RedirectAttributes redirectAttributes) {
        try {
            monetizationService.chooseMonetization(id, request.getMonetizationType());
            redirectAttributes.addFlashAttribute("successMessage", "Монетизация обработана, статус публикации обновлен.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/videos/" + id;
    }

    @GetMapping("/revenues")
    public String revenues(
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model
    ) {
        User user = loadUser(userId);
        int actualYear = year == null ? YearMonth.now().getYear() : year;
        int actualMonth = month == null ? YearMonth.now().getMonthValue() : month;
        model.addAttribute("currentPage", "revenues");
        model.addAttribute("user", user);
        model.addAttribute("stats", statsService.getAuthorStats(userId, Optional.of(actualYear), Optional.of(actualMonth)));
        model.addAttribute("revenues", revenueService.getByAuthor(userId, actualYear, actualMonth));
        model.addAttribute("selectedYear", actualYear);
        model.addAttribute("selectedMonth", actualMonth);
        return "revenues";
    }

    @GetMapping("/payouts")
    public String payouts(@RequestParam(defaultValue = "1") Long userId, Model model) {
        User user = loadUser(userId);
        model.addAttribute("currentPage", "payouts");
        model.addAttribute("user", user);
        model.addAttribute("payouts", payoutDataService.getByUserId(userId));
        return "payouts";
    }

    @GetMapping("/processes/{videoId}")
    public String processDetails(@PathVariable Long videoId, Model model) {
        Video video = videoService.getVideo(videoId);
        model.addAttribute("currentPage", "processes");
        model.addAttribute("user", video.getAuthor());
        model.addAttribute("video", video);
        model.addAttribute("claims", claimDataService.getByVideoId(videoId));
        model.addAttribute("processState", processService.getProcessState(videoId));
        return "process-details";
    }

    @PostMapping("/processes/{videoId}/continue")
    public String continueProcess(@PathVariable Long videoId, RedirectAttributes redirectAttributes) {
        try {
            processService.continueProcess(videoId);
            redirectAttributes.addFlashAttribute("successMessage", "Системное продолжение процесса выполнено.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/processes/" + videoId;
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        MonthlyProcessRequest request = new MonthlyProcessRequest();
        request.setYear(YearMonth.now().getYear());
        request.setMonth(YearMonth.now().getMonthValue());
        model.addAttribute("currentPage", "admin");
        model.addAttribute("user", loadUser(DEMO_USER_ID));
        model.addAttribute("videos", videoService.getVideos(DEMO_USER_ID));
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
                    "Месячный процесс выполнен: videos=" + response.getProcessedVideos() + ", payouts=" + response.getCreatedPayouts()
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


    private User loadUser(Long userId) {
        return userDataService.getById(userId);
    }
}
