package hng_videoSuite_java.video.sevice;

import com.fasterxml.jackson.core.JsonProcessingException;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {
    private final JobLauncher jobLauncher;
    private final Job videoProcessingJob;
    private final ObjectMapper objectMapper;
    private final VideoUtils videoUtils;

    @RabbitListener(queues = "${rabbitmq.queue.concat:videoConcat}")
    public void handleJobLaunch(String message) {
        log.info("Queue request gotten");

        if (message == null || message.isEmpty()) {
            log.error("message is empty");
            throw new RuntimeException("Empty message received from queue");
        }

        String jobId;
        try {
            VideoPathDto dto = objectMapper.readValue(message, VideoPathDto.class);
            jobId = dto.getJobId();

            if (jobId == null || jobId.isEmpty()) {
                log.info("invalid video data sent");
                return;
            }
        } catch (JsonProcessingException ex) {
            log.error("Invalid videoPathDto data");
            return;
        }

        String tempFilePath = createTempFileForMessage(message);
        log.info("File path: {}", tempFilePath);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("inputFilePath", tempFilePath)
                .addString("jobId", jobId)
                .toJobParameters();

        try {
            log.info("Launching Job process");
            jobLauncher.run(videoProcessingJob, jobParameters);
            log.info("Job process completed successfully");
        } catch (Exception ex) {
            log.error("Error launching video processing job: {}", ex.getMessage());
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED.name());
        }
    }

    private String createTempFileForMessage(String message) {
        File tempFile = null;
        try {
            tempFile = File.createTempFile("videoMessage", ".json");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile))) {
                writer.write(message);
            }
        } catch (IOException ex) {
           log.error("Error creating temp File {}", ex.getMessage());
        }
        return tempFile != null ? tempFile.getAbsolutePath() : "";
    }
}


