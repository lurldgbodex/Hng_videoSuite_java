package hng_videoSuite_java.video.sevice;

import com.fasterxml.jackson.databind.ObjectMapper;
import hng_videoSuite_java.video.dto.VideoPathDto;
import hng_videoSuite_java.video.enums.VideoStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoUtils videoUtils;
    private final ObjectMapper objectMapper;
    private final JobLauncher jobLauncher;
    private final Job videoProcessingJob;

    @RabbitListener(queues = "${rabbitmq.queue.concat:videoConcat}")
    public void handleJobLaunch(String message) throws IOException {
        String jobId;

        log.info("Getting message from queues: {}", message);
        if (message == null || message.isEmpty()) {
            log.error("Received empty or null message");
            return;
        }

        String outputPath;
        String[] videoFilePaths;
        try {
            VideoPathDto videoPathDto = objectMapper.readValue(message, VideoPathDto.class);
            jobId = videoPathDto.getJobId();
            String resourcePath = getResourcePath();

            if (resourcePath == null) {
                videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
                return;
            }

            log.info("Creating video directory");
            File mergeVideosDir = new File(resourcePath + "merge_videos");
            if (!mergeVideosDir.exists() && !mergeVideosDir.mkdirs()) {
                log.error("Failed to create merge videos directory");
                videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
                return;
            }
            log.info("creating output path");
            outputPath = mergeVideosDir.getAbsolutePath() + File.separator + jobId + "_merge_video.mp4";
            videoFilePaths = new String[videoPathDto.getVideo().size()];

            int index = 0;
            log.info("getting byte values");
            for (Map.Entry<String, byte[]> entry : videoPathDto.getVideo().entrySet()) {
                String key = entry.getKey();
                byte[] videoData = entry.getValue();
                log.info("getting video file");
                File videoFile = VideoUtils.byteArrayToFile(videoData, mergeVideosDir.getPath() + File.separator + key);
                log.info("successfully gotten video file");
                videoFilePaths[index++] = videoFile.getAbsolutePath();
            }
        } catch (IOException ex) {
            log.error("Failed to parse Video Path Dto: {}", ex.getMessage());
            return;
        }

        if (jobId == null) {
            log.error("invalid job ID");
            return;
        }

        File tempFile = File.createTempFile("videoPaths", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (String filePath : videoFilePaths) {
                writer.write(filePath);
                writer.newLine();
            }
        }


        log.info("created job parameter");
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", jobId)
                .addString("outputPath", outputPath)
                .addString("tempFilePath", tempFile.getAbsolutePath())
                .toJobParameters();

        try {
            log.info("Launching job processing");
            jobLauncher.run(videoProcessingJob, jobParameters);
            log.info("successfully launch job processing");
        } catch (Exception ex) {
            log.error("Error launching video processing job: {}", ex.getMessage());
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


