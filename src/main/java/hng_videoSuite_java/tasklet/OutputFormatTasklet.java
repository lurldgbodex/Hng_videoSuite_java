package hng_videoSuite_java.tasklet;

import com.rabbitmq.client.Return;
import hng_videoSuite_java.notification.VideoPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.File;
import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class OutputFormatTasklet implements Tasklet {
    private final VideoPublisherService videoPublisherService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        log.info("starting the outputFormatTasklet");

        String jobId = (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("jobId");
        String outputPath = (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("outputPath");

        if (jobId == null || outputPath == null) {
            log.error("Failed to get details from context");
            return RepeatStatus.FINISHED;
        }

        try {
            videoPublisherService.publishMergedVideo(jobId, new File(outputPath));
            File outputFile = new File(outputPath);
            if (outputFile.exists() && !outputFile.delete()) {
                log.error("Failed to delete output file: {}", outputFile);
            }
        } catch (IOException ex) {
            log.error("Failed to publish merged video: {}", ex.getMessage());
            return RepeatStatus.FINISHED;
        }
        return RepeatStatus.FINISHED;
    }
}
