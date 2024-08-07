package hng_videoSuite_java.video.repository;

import hng_videoSuite_java.video.entity.VideoSuite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<VideoSuite, String> {
}
