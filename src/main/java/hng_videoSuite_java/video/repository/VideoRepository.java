package hng_videoSuite_java.video.repository;

import hng_videoSuite_java.video.entity.VideoSuite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VideoRepository extends JpaRepository<VideoSuite, UUID> {
}
