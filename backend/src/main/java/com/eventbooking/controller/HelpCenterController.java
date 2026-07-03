package com.eventbooking.controller;

import com.eventbooking.dto.request.FaqRequest;
import com.eventbooking.dto.request.TutorialVideoRequest;
import com.eventbooking.dto.response.ApiResponse;
import com.eventbooking.entity.Faq;
import com.eventbooking.entity.TutorialVideo;
import com.eventbooking.service.HelpCenterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/help")
@RequiredArgsConstructor
public class HelpCenterController {
    private final HelpCenterService helpCenterService;

    @GetMapping
    public ApiResponse<Map<String, Object>> help(@RequestParam(required = false) String search,
                                                  @RequestParam(required = false) String category) {
        return ApiResponse.success(Map.of(
                "faqs", helpCenterService.faqs(search, category),
                "videos", helpCenterService.videos(),
                "contactSupport", Map.of("email", "support@eventbooking.local", "hours", "Mon-Fri 09:00-18:00")
        ));
    }

    @GetMapping("/faqs")
    public ApiResponse<List<Faq>> faqs(@RequestParam(required = false) String search,
                                       @RequestParam(required = false) String category) {
        return ApiResponse.success(helpCenterService.faqs(search, category));
    }

    @PostMapping("/faqs")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Faq> createFaq(@Valid @RequestBody FaqRequest request) {
        return ApiResponse.success("FAQ saved", helpCenterService.saveFaq(null, request));
    }

    @PutMapping("/faqs/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Faq> updateFaq(@PathVariable Long id, @Valid @RequestBody FaqRequest request) {
        return ApiResponse.success("FAQ updated", helpCenterService.saveFaq(id, request));
    }

    @GetMapping("/videos")
    public ApiResponse<List<TutorialVideo>> videos() {
        return ApiResponse.success(helpCenterService.videos());
    }

    @PostMapping("/videos")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TutorialVideo> createVideo(@Valid @RequestBody TutorialVideoRequest request) {
        return ApiResponse.success("Tutorial video saved", helpCenterService.saveVideo(null, request));
    }

    @PutMapping("/videos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<TutorialVideo> updateVideo(@PathVariable Long id, @Valid @RequestBody TutorialVideoRequest request) {
        return ApiResponse.success("Tutorial video updated", helpCenterService.saveVideo(id, request));
    }

    @DeleteMapping("/videos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteVideo(@PathVariable Long id) {
        helpCenterService.deleteVideo(id);
        return ApiResponse.success("Tutorial video deleted", null);
    }
}
