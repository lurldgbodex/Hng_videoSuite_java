package hng_videoSuite_java.video.sevice;

import com.fasterxml.jackson.databind.ObjectMapper;
import hng_videoSuite_java.video.dto.VideoPathDto;
import hng_videoSuite_java.video.enums.VideoStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class VideoServiceTest {
    @Mock
    private FfmpegService ffmpegService;
    @Mock
    private VideoUtils videoUtils;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private VideoService underTest;

    @Test
    void testHandleVideoMergeJob_success() throws IOException, ExecutionException, InterruptedException {
        Map<String, byte[]> video = new HashMap<>();
        video.put("video1.mp4", new byte[]{1,2,3});
        video.put("video2.mp4", new byte[]{5,6,7});

        VideoPathDto videoPathDto = new VideoPathDto();
        videoPathDto.setJobId(UUID.randomUUID().toString());
        videoPathDto.setVideo(video);
        String jsonMessage = new ObjectMapper().writeValueAsString(videoPathDto);

        when(objectMapper.readValue(anyString(), eq(VideoPathDto.class))).thenReturn(videoPathDto);
        doNothing().when(videoUtils).updateJobStatus(anyString(), any(VideoStatus.class));
        doNothing().when(videoUtils).updateJobProgress(anyString(), anyInt());
        doNothing().when(ffmpegService).mergeVideos(anyString(), anyString(), any(String[].class));

        underTest.handleVideoMergeJob(jsonMessage);

        verify(ffmpegService).mergeVideos(anyString(), anyString(), any(String[].class));
        verify(videoUtils, times(3)).updateJobStatus(anyString(), any(VideoStatus.class));
        verify(videoUtils, times(1)).updateJobProgress(anyString(), anyInt());
    }

    @Test
    void testHandleVideoMergeJob_Failure() throws IOException, ExecutionException, InterruptedException {
        String jobId = UUID.randomUUID().toString();
        VideoPathDto videoPathDto = new VideoPathDto();
        videoPathDto.setJobId(jobId);
        Map<String, byte[]> videoMap = new HashMap<>();
        videoMap.put("video1.mp4", new byte[]{1, 2, 3});
        videoPathDto.setVideo(videoMap);

        String jsonMessage = new ObjectMapper().writeValueAsString(videoPathDto);

        when(objectMapper.readValue(anyString(), eq(VideoPathDto.class))).thenReturn(videoPathDto);
        doNothing().when(videoUtils).updateJobStatus(anyString(), any(VideoStatus.class));
        doThrow(new IOException("FFmpeg failed")).when(ffmpegService).mergeVideos(anyString(), anyString(), any(String[].class));

        // Test
        underTest.handleVideoMergeJob(jsonMessage);
        verify(ffmpegService).mergeVideos(anyString(), anyString(), any(String[].class));
    }

}