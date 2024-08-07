package hng_videoSuite_java.video.sevice;

import hng_videoSuite_java.notification.VideoPublisherService;
import hng_videoSuite_java.video.enums.VideoStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class FfmpegServiceTest {
    @MockBean
    private VideoPublisherService videoPublisherService;
    @MockBean
    private VideoUtils videoUtils;
    @Autowired
    private FfmpegService ffmpegService;

    @TempDir
    Path tempDir;

    @Test
    void testEncodeVideo() throws IOException, InterruptedException, ExecutionException {
        ClassPathResource resource1 = new ClassPathResource("videos/VID-1.mp4");

        // Create a temporary input file
        String tempInputFile = resource1.getFile().getAbsolutePath();
        Path tempOutputFile = tempDir.resolve("encoded_video.mp4");

        // Ensure the file is deleted after the test
        tempOutputFile.toFile().deleteOnExit();

        doNothing().when(videoUtils).updateJobProgress(anyString(), anyInt());
        // Define the totalDuration and jobId for the test
        double totalDuration = 34.0;
        String jobId = UUID.randomUUID().toString();

        // Call the encodeVideo method
        ffmpegService.encodeVideo(tempInputFile, tempOutputFile.toString(), totalDuration, jobId);

        // Verify that the output file was created
        File outputFile = tempOutputFile.toFile();
        assertTrue(outputFile.exists(), "Encoded video file should be created");
        assertTrue(outputFile.length() > 0, "Encoded video file should not be empty");
    }

    @Test
    public void testMergeVideos() throws Exception {
        ClassPathResource resource1 = new ClassPathResource("videos/VID-1.mp4");
        ClassPathResource resource2 = new ClassPathResource("videos/VID-2.mp4");

        Path tempOutputFile = tempDir.resolve("merged_video.mp4");

        tempOutputFile.toFile().deleteOnExit();

        doNothing().when(videoUtils).updateJobProgress(anyString(), anyInt());
        doNothing().when(videoUtils).updateJobStatus(anyString(), any(VideoStatus.class));

        String jobId = UUID.randomUUID().toString();

        String[] inputFiles = {
                resource1.getFile().getAbsolutePath(),
                resource2.getFile().getAbsolutePath()
        };

        ffmpegService.mergeVideos(tempOutputFile.toString(), jobId, inputFiles);

        File outputFile = tempOutputFile.toFile();
        assertTrue(outputFile.exists(), "Merged video file should be created");
        assertTrue(outputFile.length() > 0, "Merged video file should not be empty");

        verify(videoPublisherService).publishMergedVideo(eq(jobId), any(File.class));
    }
}