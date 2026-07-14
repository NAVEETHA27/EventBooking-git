package com.eventbooking.repository;

import com.eventbooking.entity.NetworkingConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NetworkingConnectionRepository extends JpaRepository<NetworkingConnection, Long> {

    @Query("SELECT c FROM NetworkingConnection c WHERE c.requester.id = :userId OR c.receiver.id = :userId")
    Page<NetworkingConnection> findAllByUserId(@Param("userId") Long userId, Pageable pageable);

    Optional<NetworkingConnection> findByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    List<NetworkingConnection> findByRequesterId(Long requesterId);

    List<NetworkingConnection> findByReceiverId(Long receiverId);

    boolean existsByRequesterIdAndReceiverId(Long requesterId, Long receiverId);

    @Query("SELECT c FROM NetworkingConnection c WHERE c.receiver.id = :userId AND c.status = 'PENDING'")
    List<NetworkingConnection> findPendingRequestsForUser(@Param("userId") Long userId);

    @Query("SELECT c FROM NetworkingConnection c WHERE (c.requester.id = :userId OR c.receiver.id = :userId) AND c.status = 'ACCEPTED'")
    List<NetworkingConnection> findAcceptedConnections(@Param("userId") Long userId);
}
