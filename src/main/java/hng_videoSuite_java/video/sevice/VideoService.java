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

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {
    private final VideoUtils videoUtils;
    private final ObjectMapper objectMapper;
    private final JobLauncher jobLauncher;
    private final Job videoProcessingJob;

    @RabbitListener(queues = "${rabbitmq.queue.concat:videoConcat}")
    public void handleJobLaunch(String message) {
        String jobId;

        if (message == null || message.isEmpty()) {
            log.error("Received empty or null message");
            return;
        }

        try {
            VideoPathDto videoPathDto = objectMapper.readValue(message, VideoPathDto.class);
            jobId = videoPathDto.getJobId();
        } catch (JsonProcessingException ex) {
            log.error("Failed to parse Video Path Dto: {}", ex.getMessage());
            return;
        }
        if (jobId == null) {
            log.error("invalid job ID");
            return;
        }

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobId", jobId)
                .addString("videoPathDto", message)
                .toJobParameters();

        try {
            jobLauncher.run(videoProcessingJob, jobParameters);
        } catch (Exception ex) {
            log.error("Error launching video processing job: {}", ex.getMessage());
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
        }
    }
}
