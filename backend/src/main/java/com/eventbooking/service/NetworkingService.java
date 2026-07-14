package com.eventbooking.service;

import com.eventbooking.ai.AIProvider;
import com.eventbooking.entity.*;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Phase 10 — AI Networking.
 *
 * Recommends connections based on shared department, skills, interests, events attended.
 * Uses AI to generate personalised match reasons.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkingService {

    private final UserRepository               userRepository;
    private final UserInterestRepository       interestRepository;
    private final BookingRepository            bookingRepository;
    private final NetworkingConnectionRepository connectionRepository;
    private final List<AIProvider>             aiProviders;

    @Value("${ai.provider:gemini}")
    private String providerName;

    // ── Suggest connections ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggestConnections(Long userId) {
        log.debug("[Networking] Suggesting connections for userId={}", userId);

        UserInterest myInterest = interestRepository.findByUserId(userId).orElse(null);

        // Events the user attended
        Set<Long> myEventIds = bookingRepository
                .findByUserId(userId, PageRequest.of(0, 100))
                .stream()
                .filter(b -> b.getBookingStatus() == Booking.BookingStatus.CONFIRMED)
                .map(b -> b.getEvent().getId())
                .collect(Collectors.toSet());

        // Already connected user IDs (both directions)
        Set<Long> alreadyConnected = connectionRepository
                .findByRequesterId(userId).stream()
                .map(c -> c.getReceiver().getId())
                .collect(Collectors.toSet());
        alreadyConnected.addAll(connectionRepository
                .findByReceiverId(userId).stream()
                .map(c -> c.getRequester().getId())
                .collect(Collectors.toSet()));
        alreadyConnected.add(userId); // exclude self

        List<User> candidates = userRepository.findAll().stream()
                .filter(u -> !alreadyConnected.contains(u.getId()))
                .limit(200)
                .collect(Collectors.toList());

        List<Map<String, Object>> suggestions = new ArrayList<>();
        for (User candidate : candidates) {
            double score = computeMatchScore(userId, myInterest, myEventIds, candidate);
            if (score < 20) continue;

            UserInterest theirInterest = interestRepository.findByUserId(candidate.getId()).orElse(null);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId",       candidate.getId());
            m.put("name",         candidate.getName());
            m.put("college",      candidate.getOrganizationName());
            m.put("department",   theirInterest != null ? theirInterest.getDepartment() : null);
            m.put("skills",       theirInterest != null ? theirInterest.getSkills() : null);
            m.put("matchScore",   (int) Math.round(score));
            m.put("matchReason",  buildMatchReason(myInterest, candidate, score));
            suggestions.add(m);
        }

        suggestions.sort((a, b) ->
                Integer.compare((int) b.get("matchScore"), (int) a.get("matchScore")));
        return suggestions.stream().limit(10).collect(Collectors.toList());
    }

    // ── Send connection request ───────────────────────────────────────────

    @Transactional
    public Map<String, Object> sendConnectionRequest(Long requesterId, Long receiverId) {
        if (requesterId.equals(receiverId)) throw new IllegalArgumentException("Cannot connect with yourself");

        boolean exists = connectionRepository.existsByRequesterIdAndReceiverId(requesterId, receiverId)
                      || connectionRepository.existsByRequesterIdAndReceiverId(receiverId, requesterId);
        if (exists) throw new IllegalStateException("Connection already exists");

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new com.eventbooking.exception.ResourceNotFoundException("User not found"));
        User receiver  = userRepository.findById(receiverId)
                .orElseThrow(() -> new com.eventbooking.exception.ResourceNotFoundException("Recipient user not found"));

        UserInterest myInterest = interestRepository.findByUserId(requesterId).orElse(null);
        Set<Long> myEventIds = bookingRepository.findByUserId(requesterId, PageRequest.of(0, 100))
                .stream().map(b -> b.getEvent().getId()).collect(Collectors.toSet());
        double score = computeMatchScore(requesterId, myInterest, myEventIds, receiver);

        NetworkingConnection conn = NetworkingConnection.builder()
                .requester(requester)
                .receiver(receiver)
                .status(NetworkingConnection.ConnectionStatus.PENDING)
                .matchScore(score)
                .matchReason(buildMatchReason(myInterest, receiver, score))
                .build();
        connectionRepository.save(conn);

        return Map.of("message", "Connection request sent", "matchScore", (int) Math.round(score));
    }

    // ── Accept / Reject ───────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> respondToConnection(Long connectionId, Long receiverId,
                                    NetworkingConnection.ConnectionStatus status) {
        NetworkingConnection conn = connectionRepository.findById(connectionId)
                .orElseThrow(() -> new IllegalArgumentException("Connection not found"));
        if (!conn.getReceiver().getId().equals(receiverId))
            throw new IllegalStateException("Not authorised");
        conn.setStatus(status);
        conn.setUpdatedAt(LocalDateTime.now());
        NetworkingConnection saved = connectionRepository.save(conn);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("connectionId", saved.getId());
        response.put("status", saved.getStatus());
        if (saved.getStatus() == NetworkingConnection.ConnectionStatus.ACCEPTED) {
            response.put("connection", toConnectionMap(saved, receiverId));
        }
        return response;
    }

    // ── My connections ────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMyConnections(Long userId) {
        List<NetworkingConnection> all = new ArrayList<>();
        all.addAll(connectionRepository.findByRequesterId(userId));
        all.addAll(connectionRepository.findByReceiverId(userId));

        return all.stream()
                .filter(c -> c.getStatus() == NetworkingConnection.ConnectionStatus.ACCEPTED)
                .map(c -> toConnectionMap(c, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingRequests(Long userId) {
        return connectionRepository.findByReceiverId(userId).stream()
                .filter(c -> c.getStatus() == NetworkingConnection.ConnectionStatus.PENDING)
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("connectionId", c.getId());
                    m.put("fromUserId",   c.getRequester().getId());
                    m.put("fromName",     c.getRequester().getName());
                    m.put("matchScore",   c.getMatchScore() != null ? (int) Math.round(c.getMatchScore()) : 0);
                    m.put("matchReason",  c.getMatchReason());
                    m.put("createdAt",    c.getCreatedAt());
                    return m;
                })
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private double computeMatchScore(Long userId, UserInterest myInterest,
                                     Set<Long> myEventIds, User candidate) {
        double score = 0;
        UserInterest theirInterest = interestRepository.findByUserId(candidate.getId()).orElse(null);
        if (theirInterest == null || myInterest == null) return 20; // base score

        // Same department
        if (myInterest.getDepartment() != null && theirInterest.getDepartment() != null
                && myInterest.getDepartment().equalsIgnoreCase(theirInterest.getDepartment())) score += 30;

        // Same college
        if (myInterest.getCollege() != null && theirInterest.getCollege() != null
                && myInterest.getCollege().equalsIgnoreCase(theirInterest.getCollege())) score += 20;

        // Shared skills
        if (myInterest.getSkills() != null && theirInterest.getSkills() != null) {
            long shared = Arrays.stream(myInterest.getSkills().split(","))
                    .filter(s -> theirInterest.getSkills().toLowerCase()
                            .contains(s.trim().toLowerCase()))
                    .count();
            score += Math.min(shared * 10, 25);
        }

        // Shared interests
        if (myInterest.getFavoriteCategories() != null && theirInterest.getFavoriteCategories() != null) {
            long shared = Arrays.stream(myInterest.getFavoriteCategories().split(","))
                    .filter(c -> theirInterest.getFavoriteCategories().toLowerCase()
                            .contains(c.trim().toLowerCase()))
                    .count();
            score += Math.min(shared * 5, 15);
        }

        // Attended same events
        Set<Long> theirEventIds = bookingRepository.findByUserId(candidate.getId(), PageRequest.of(0, 100))
                .stream().map(b -> b.getEvent().getId()).collect(Collectors.toSet());
        long mutualEvents = myEventIds.stream().filter(theirEventIds::contains).count();
        score += Math.min(mutualEvents * 5, 10);

        return Math.min(score, 100);
    }

    private Map<String, Object> toConnectionMap(NetworkingConnection c, Long userId) {
        User other = c.getRequester().getId().equals(userId) ? c.getReceiver() : c.getRequester();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("connectionId", c.getId());
        m.put("userId",       other.getId());
        m.put("name",         other.getName());
        m.put("college",      other.getOrganizationName());
        m.put("matchScore",   c.getMatchScore() != null ? (int) Math.round(c.getMatchScore()) : 0);
        m.put("matchReason",  c.getMatchReason());
        m.put("status",       c.getStatus());
        return m;
    }

    private String buildMatchReason(UserInterest myInterest, User candidate, double score) {
        if (myInterest == null) return "Active event participant";
        UserInterest theirInterest = interestRepository.findByUserId(candidate.getId()).orElse(null);
        if (theirInterest == null) return "Similar event participation";

        List<String> reasons = new ArrayList<>();
        if (myInterest.getDepartment() != null && theirInterest.getDepartment() != null
                && myInterest.getDepartment().equalsIgnoreCase(theirInterest.getDepartment()))
            reasons.add("Same department: " + myInterest.getDepartment());
        if (myInterest.getCollege() != null && theirInterest.getCollege() != null
                && myInterest.getCollege().equalsIgnoreCase(theirInterest.getCollege()))
            reasons.add("Same college");
        if (reasons.isEmpty()) reasons.add("Shared event interests");
        return String.join(" · ", reasons);
    }
}
