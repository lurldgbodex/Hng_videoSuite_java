package hng_video_processing.utils;

import hng_video_processing.video.entity.VideoSuite;
import hng_video_processing.video.enums.VideoStatus;
import hng_video_processing.video.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VideoUtilsTest {
    @Mock
    private VideoRepository videoRepository;
    @InjectMocks
    private VideoUtils underTest;

    @Test
    void testUpdateJobStatus() {
        UUID jobId = UUID.randomUUID();
        VideoSuite job = new VideoSuite();
        when(videoRepository.findById(any(UUID.class))).thenReturn(Optional.of(job));

        underTest.updateJobStatus(jobId, VideoStatus.SUCCESS);

        verify(videoRepository).save(any(VideoSuite.class));
        assertThat(job.getStatus()).isEqualTo(VideoStatus.SUCCESS);
    }

    @Test
    void testUpdateJobProgress() {
        UUID jobId = UUID.randomUUID();
        VideoSuite job = new VideoSuite();
        when(videoRepository.findById(any(UUID.class))).thenReturn(Optional.of(job));

        underTest.updateJobProgress(jobId, 50);

        verify(videoRepository).save(any(VideoSuite.class));
        assertThat(job.getProgress()).isEqualTo(50);
    }
}