package hng_videoSuite_java.video.sevice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hng_videoSuite_java.video.dto.VideoPathDto;
import hng_videoSuite_java.video.enums.VideoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoUtils videoUtils;
    private final FfmpegService ffmpegService;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${rabbitmq.queue.concat}")
    public void handleVideoMergeJob(String message) {
        VideoPathDto videoPathDto;
        try {
            videoPathDto = objectMapper.readValue(message, VideoPathDto.class);
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse Video Path Dto: {}", ex.getMessage());
            return;
        }
        UUID jobId = videoPathDto.getJobId();
        if (jobId == null || videoPathDto.getVideo() == null) {
            log.error("invalid job ID: {}", jobId);
            return;
        }

        videoUtils.updateJobStatus(jobId, VideoStatus.PROCESSING);

        String resourcePath = getResourcePath();
        if (resourcePath == null) {
            log.error("Resource path could not be determined");
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
            return;
        }

        File mergeVideosDir = new File(resourcePath + "merge_videos");
        if (!mergeVideosDir.exists() && !mergeVideosDir.mkdirs()) {
            log.error("Failed to create merge videos directory");
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
            return;
        }

        String outputPath = mergeVideosDir.getAbsolutePath() + File.separator + jobId + "_merge_video.mp4";
        String[] videoFilePaths = new String[videoPathDto.getVideo().size()];

        int index = 0;
        try {
            for (Map.Entry<String, byte[]> entry : videoPathDto.getVideo().entrySet()) {
                String key = entry.getKey();
                byte[] videoData = entry.getValue();
                File videoFile = VideoUtils.byteArrayToFile(videoData, mergeVideosDir.getPath() + File.separator + key);
                videoFilePaths[index++] = videoFile.getAbsolutePath();
            }

            videoUtils.updateJobStatus(jobId, VideoStatus.PROCESSING);
            ffmpegService.mergeVideos(outputPath, jobId, videoFilePaths);
            videoUtils.updateJobStatus(jobId, VideoStatus.SUCCESS);
            videoUtils.updateJobProgress(jobId, 100);
        } catch (InterruptedException | IOException | ExecutionException ex) {
            log.error("Error during video processing: {}", ex.getMessage());
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
        }
    }

    private String getResourcePath() {
        try {
            String path = getClass().getClassLoader().getResource("").toURI().getPath();
            return path != null ? path : "";
        } catch (Exception ex) {
            log.error("Failed to get resource path: {}", ex.getMessage());
            return null;
        }
    }
}
