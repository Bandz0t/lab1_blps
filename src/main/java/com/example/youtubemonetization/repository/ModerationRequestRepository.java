package com.example.youtubemonetization.repository;

import com.example.youtubemonetization.entity.ModerationRequest;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationRequestRepository extends JpaRepository<ModerationRequest, Long> {

    Optional<ModerationRequest> findFirstByVideoIdAndActiveTrueOrderByCreatedAtDesc(Long videoId);
}
