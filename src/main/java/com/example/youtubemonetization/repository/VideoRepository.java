package com.example.youtubemonetization.repository;

import com.example.youtubemonetization.entity.Video;
import com.example.youtubemonetization.enums.MonetizationStatus;
import com.example.youtubemonetization.enums.UploadStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByAuthorId(Long authorId);

    List<Video> findByUploadStatus(UploadStatus uploadStatus);

    List<Video> findByMonetizationStatus(MonetizationStatus monetizationStatus);

    @Query("select v from Video v where v.publishedAt is not null")
    List<Video> findPublishedVideos();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Video v where v.id = :id")
    Optional<Video> findByIdForUpdate(Long id);
}
