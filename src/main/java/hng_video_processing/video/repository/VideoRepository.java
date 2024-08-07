package hng_video_processing.video.repository;

import hng_video_processing.video.entity.VideoSuite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VideoRepository extends JpaRepository<VideoSuite, UUID> {
}
