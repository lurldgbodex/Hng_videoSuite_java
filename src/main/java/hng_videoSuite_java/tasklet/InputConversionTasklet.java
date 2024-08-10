package hng_videoSuite_java.tasklet;

import com.fasterxml.jackson.databind.ObjectMapper;
import hng_videoSuite_java.video.dto.VideoPathDto;
import hng_videoSuite_java.video.enums.VideoStatus;
import hng_videoSuite_java.video.sevice.VideoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class InputConversionTasklet implements Tasklet {
    private final VideoUtils videoUtils;
    private final ObjectMapper objectMapper;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting conversion task");

        String messagePath = contribution.getStepExecution().getJobParameters().getString("inputFilePath");

        if (messagePath == null || messagePath.isEmpty()) {
            log.info("Received empty or null message");
            throw new IllegalArgumentException("Received empty or null message");
        }

        String message;
        try {
            log.info("Extracting message from path");
            message = new String(Files.readAllBytes(Paths.get(messagePath)));
        } catch (IOException ex) {
            log.error("Error reading file from path: {}", ex.getMessage());
            return RepeatStatus.FINISHED;
        }

        if (message.isEmpty()) {
            log.info("Received empty or null message after reading file");
            throw new IllegalArgumentException("Received empty or null message after reading file");
        }

        String jobId = null;
        String outputPath;
        String[] videoFilePaths;

        try {
            log.info("Reading and mapping file to videoDto");
            VideoPathDto videoPathDto = objectMapper.readValue(message, VideoPathDto.class);
            jobId = videoPathDto.getJobId();
            String resourcePath = getResourcePath();

            if (resourcePath == null) {
                log.error("Failed to get resource path");
                videoUtils.updateJobStatus(jobId, VideoStatus.FAILED.toString());
                return RepeatStatus.FINISHED;
            }

            File mergeVideosDir = new File(resourcePath + "merge_videos");
            if (!mergeVideosDir.exists() && !mergeVideosDir.mkdirs()) {
                log.error("Failed to create or find mergeVideoDir");
                videoUtils.updateJobStatus(jobId, VideoStatus.FAILED.toString());
                return RepeatStatus.FINISHED;
            }

            outputPath = mergeVideosDir.getAbsolutePath() + File.separator + jobId + "_merge_video.mp4";
            videoFilePaths = new String[videoPathDto.getVideo().size()];

            int index = 0;
            for (Map.Entry<String, byte[]> entry : videoPathDto.getVideo().entrySet()) {
                log.info("Getting each video byte from the videoPathDto and add it to videoFilePath");
                String key = entry.getKey();
                byte[] videoData = entry.getValue();
                File videoFile = VideoUtils.byteArrayToFile(videoData, mergeVideosDir.getPath() + File.separator + key);
                videoFilePaths[index++] = videoFile.getAbsolutePath();
            }

        } catch (IOException ex) {
            log.error("Conversion Task Failed: {}", ex.getMessage());
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED.toString());
            return RepeatStatus.FINISHED;
        }

        log.info("creating a temp file to hold the videoPaths");
        File tempFile = File.createTempFile("videoPaths", ".txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
            for (String filePath : videoFilePaths) {
                writer.write(filePath);
                writer.newLine();
            }
        }

        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("jobId", jobId);
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("outputPath", outputPath);
        chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().put("tempFilePath", tempFile.getAbsolutePath());

        return RepeatStatus.FINISHED;
    }

    private String getResourcePath() {
        try {
            String path = getClass().getClassLoader().getResource("").toURI().getPath();
            return path != null ? path : "";
        } catch (Exception ex) {
            log.error("Error getting resourcePath: {}", ex.getMessage());
            return null;
        }
    }
}
