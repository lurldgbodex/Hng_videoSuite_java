package hng_videoSuite_java.video.sevice;

import hng_videoSuite_java.video.entity.VideoSuite;
import hng_videoSuite_java.video.enums.VideoStatus;
import hng_videoSuite_java.video.repository.VideoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        String jobId = UUID.randomUUID().toString();
        VideoSuite job = new VideoSuite();
        when(videoRepository.findById(anyString())).thenReturn(Optional.of(job));

        underTest.updateJobStatus(jobId, VideoStatus.SUCCESS);

        verify(videoRepository).save(any(VideoSuite.class));
        assertThat(job.getStatus()).isEqualTo(VideoStatus.SUCCESS);
    }

    @Test
    void testUpdateJobProgress() {
        String jobId = UUID.randomUUID().toString();
        VideoSuite job = new VideoSuite();
        when(videoRepository.findById(anyString())).thenReturn(Optional.of(job));

        underTest.updateJobProgress(jobId, 50);

        verify(videoRepository).save(any(VideoSuite.class));
        assertThat(job.getProgress()).isEqualTo(50);
    }
}