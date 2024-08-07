package hng_video_processing.video.sevice;

import hng_video_processing.notification.VideoPublisherService;
import hng_video_processing.utils.VideoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
public class FfmpegServiceTest {
    private final VideoPublisherService videoPublisherService = mock(VideoPublisherService.class);
    private final VideoUtils videoUtils = mock(VideoUtils.class);
    private final FfmpegService ffmpegService = spy(new FfmpegService(videoPublisherService, videoUtils));


    @Test
    public void testHandleProcessOutput() throws Exception {
        // Mock process
        Process mockProcess = mock(Process.class);
        when(mockProcess.getInputStream()).thenReturn(new ByteArrayInputStream("time=00:00:10.00\n".getBytes()));
        when(mockProcess.getErrorStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
        when(mockProcess.waitFor()).thenReturn(0);

        ffmpegService.getClass().getDeclaredMethod("handleProcessOutput", Process.class, double.class, UUID.class)
                .invoke(ffmpegService, mockProcess, 100.0, UUID.randomUUID());

        // Verify progress update
        ArgumentCaptor<Integer> progressCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(videoUtils).updateJobProgress(any(UUID.class), progressCaptor.capture());
        assertEquals(10, progressCaptor.getValue());
    }
}
