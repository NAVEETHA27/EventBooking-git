package com.eventbooking.service;

import com.eventbooking.dto.request.FaqRequest;
import com.eventbooking.dto.request.TutorialVideoRequest;
import com.eventbooking.entity.Faq;
import com.eventbooking.entity.TutorialVideo;
import com.eventbooking.repository.FaqRepository;
import com.eventbooking.repository.TutorialVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HelpCenterService {
    private final FaqRepository faqRepository;
    private final TutorialVideoRepository tutorialVideoRepository;

    @Transactional(readOnly = true)
    public List<Faq> faqs(String search, String category) {
        if (StringUtils.hasText(search)) {
            return faqRepository.findByActiveTrueAndQuestionContainingIgnoreCaseOrActiveTrueAndAnswerContainingIgnoreCase(search, search);
        }
        if (StringUtils.hasText(category)) {
            return faqRepository.findByActiveTrueAndCategoryIgnoreCaseOrderByQuestionAsc(category);
        }
        return faqRepository.findByActiveTrueOrderByCategoryAscQuestionAsc();
    }

    @Transactional
    public Faq saveFaq(Long id, FaqRequest request) {
        Faq faq = id == null ? new Faq() : faqRepository.findById(id).orElseThrow();
        faq.setCategory(request.getCategory());
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setActive(request.isActive());
        return faqRepository.save(faq);
    }

    @Transactional(readOnly = true)
    public List<TutorialVideo> videos() {
        return tutorialVideoRepository.findByActiveTrueOrderByCreatedAtDesc();
    }

    @Transactional
    public TutorialVideo saveVideo(Long id, TutorialVideoRequest request) {
        TutorialVideo video = id == null ? new TutorialVideo() : tutorialVideoRepository.findById(id).orElseThrow();
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setVideoUrl(request.getVideoUrl());
        video.setCategory(request.getCategory());
        video.setActive(request.isActive());
        return tutorialVideoRepository.save(video);
    }

    @Transactional
    public void deleteVideo(Long id) {
        tutorialVideoRepository.deleteById(id);
    }
}
