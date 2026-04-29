package com.tps.service;

import com.tps.dto.feedback.FeedbackRequest;
import com.tps.dto.feedback.FeedbackResponse;
import com.tps.entity.Feedback;
import com.tps.repository.FeedbackRepository;
import com.tps.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeedbackResponse create(Long userId, FeedbackRequest request) {
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setType(request.getType() == null || request.getType().isBlank() ? "GENERAL" : request.getType());
        feedback.setContent(request.getContent());
        feedback.setContact(request.getContact());
        return toResponse(feedbackRepository.save(feedback));
    }

    public Page<FeedbackResponse> my(Long userId, int page, int size) {
        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    public FeedbackResponse get(Long userId, Long id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
        if (!feedback.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权限查看此反馈");
        }
        return toResponse(feedback);
    }

    public Page<FeedbackResponse> adminList(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (status != null && !status.isBlank()) {
            return feedbackRepository.findByStatusOrderByCreatedAtDesc(Feedback.FeedbackStatus.valueOf(status), pageable)
                    .map(this::toResponse);
        }
        return feedbackRepository.findAllByOrderByCreatedAtDesc(pageable).map(this::toResponse);
    }

    @Transactional
    public FeedbackResponse reply(Long id, String reply) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
        feedback.setReply(reply);
        feedback.setStatus(Feedback.FeedbackStatus.DONE);
        return toResponse(feedbackRepository.save(feedback));
    }

    @Transactional
    public FeedbackResponse updateStatus(Long id, String status) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
        feedback.setStatus(Feedback.FeedbackStatus.valueOf(status));
        return toResponse(feedbackRepository.save(feedback));
    }

    private FeedbackResponse toResponse(Feedback feedback) {
        FeedbackResponse response = new FeedbackResponse();
        response.setId(feedback.getId());
        response.setUserId(feedback.getUserId());
        userRepository.findById(feedback.getUserId())
                .ifPresent(user -> response.setUserNickname(user.getNickname()));
        response.setType(feedback.getType());
        response.setContent(feedback.getContent());
        response.setContact(feedback.getContact());
        response.setStatus(feedback.getStatus().name());
        response.setReply(feedback.getReply());
        response.setCreatedAt(feedback.getCreatedAt());
        response.setUpdatedAt(feedback.getUpdatedAt());
        return response;
    }
}
