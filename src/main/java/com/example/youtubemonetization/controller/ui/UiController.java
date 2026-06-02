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
import com.example.youtubemonetization.service.VideoMetadataService;
import com.example.youtubemonetization.service.VideoService;
import com.example.youtubemonetization.service.storage.VideoStorageService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;
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
    private final VideoStorageService videoStorageService;
    private final VideoMetadataService videoMetadataService;

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
        model.addAttribute("currentPage", "videos");
        model.addAttribute("user", user);
        model.addAttribute("videoForm", form);
        return "video-create";
    }

    @PostMapping("/videos/create")
    public String createVideo(
            Authentication authentication,
            VideoCreateRequest request,
            @RequestParam("videoFile") MultipartFile videoFile,
            RedirectAttributes redirectAttributes
    ) {
        User user = currentUser(authentication);
        if (videoFile == null || videoFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Выберите видеофайл для загрузки.");
            return "redirect:/videos/create";
        }
        try {
            VideoCreateRequest createRequest = buildServerSideCreateRequest(request, user, videoFile);
            Video video = videoService.createVideo(createRequest);
            redirectAttributes.addFlashAttribute("successMessage", "Видео успешно создано. Запущен цикл публикации.");
            return "redirect:/videos/" + video.getId();
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/videos/create";
        }
    }

    private VideoCreateRequest buildServerSideCreateRequest(VideoCreateRequest form, User user, MultipartFile videoFile) {
        if (user.getId() == null) {
            throw new IllegalStateException("Current user is not persisted");
        }
        VideoCreateRequest request = new VideoCreateRequest();
        request.setAuthorId(user.getId());
        request.setTitle(form.getTitle());
        request.setDescription(form.getDescription());
        request.setSizeBytes(videoFile.getSize());
        request.setFormat(resolveFormat(videoFile));
        request.setDurationSeconds(videoMetadataService.extractDurationSeconds(videoFile));
        request.setFilePath(videoStorageService.upload(videoFile));
        return request;
    }

    private String resolveFormat(MultipartFile videoFile) {
        String originalName = videoFile.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            return "mp4";
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
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
        return "video-details";
    }

    @PostMapping("/moderation/videos/{id}/copyright-check")
    public String copyrightCheck(
            Authentication authentication,
            @PathVariable Long id,
            CopyrightCheckRequest request,
            RedirectAttributes redirectAttributes
    ) {
        User user = currentUser(authentication);
        assertModeratorOrAdmin(user);
        try {
            copyrightService.processCopyrightCheck(id, request);
            redirectAttributes.addFlashAttribute("successMessage", request.isHasViolation()
                    ? "Обнаружено нарушение. Отправили ролик на доработку."
                    : "Проверка авторских прав завершена: нарушений не найдено.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/moderation";
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
    public String editVideo(
            @PathVariable Long id,
            EditVideoRequest request,
            @RequestParam("videoFile") MultipartFile videoFile,
            RedirectAttributes redirectAttributes
    ) {
        try {
            if (videoFile != null && !videoFile.isEmpty()) {
                request.setNewFilePath(videoStorageService.upload(videoFile));
            }
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
    public String revenues(
            Authentication authentication,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model
    ) {
        User user = currentUser(authentication);
        YearMonth selectedPeriod = resolveRevenuePeriod(user.getId(), year, month);
        int actualYear = selectedPeriod.getYear();
        int actualMonth = selectedPeriod.getMonthValue();
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

    @GetMapping("/moderation")
    public String moderation(Authentication authentication, Model model) {
        User user = currentUser(authentication);
        assertModeratorOrAdmin(user);
        model.addAttribute("currentPage", "moderation");
        model.addAttribute("user", user);
        model.addAttribute("videos", videoService.getAllVideos());
        model.addAttribute("copyrightForm", new CopyrightCheckRequest());
        return "moderation";
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

    private void assertModeratorOrAdmin(User user) {
        if ("ADMIN".equals(user.getRole()) || "MODERATOR".equals(user.getRole())) {
            return;
        }
        throw new IllegalStateException("Доступ только для модератора или администратора");
    }

    private YearMonth resolveRevenuePeriod(Long userId, Integer year, Integer month) {
        if (year != null && month != null) {
            return YearMonth.of(year, month);
        }
        return payoutDataService.getByUserId(userId).stream()
                .map(payout -> YearMonth.of(payout.getPeriodYear(), payout.getPeriodMonth()))
                .max(YearMonth::compareTo)
                .orElse(YearMonth.now());
    }
}
