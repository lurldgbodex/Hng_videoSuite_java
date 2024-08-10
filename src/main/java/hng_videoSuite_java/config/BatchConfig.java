package hng_videoSuite_java.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import hng_videoSuite_java.notification.JobCompletionNotification;
import hng_videoSuite_java.notification.VideoPublisherService;
import hng_videoSuite_java.tasklet.*;
import hng_videoSuite_java.video.sevice.FfmpegService;
import hng_videoSuite_java.video.sevice.VideoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class BatchConfig {
    private final VideoUtils videoUtils;
    private final FfmpegService ffmpegService;
    private final JobCompletionNotification listener;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager manager;
    private final ObjectMapper objectMapper;
    private final VideoPublisherService videoPublisherService;

    @Bean
    public Job videoProcessingJob() {
        return new JobBuilder("videoProcessingJob", jobRepository)
                .listener(listener)
                .start(inputConversionStep())
                .next(videoProcessingStep())
                .build();
    }

    @Bean
    public Step inputConversionStep() {
        return new StepBuilder("inputConversionStep", jobRepository)
                .tasklet(inputConversionTasklet(), manager)
                .build();
    }

    @Bean
    public Step videoProcessingStep() {
        return new StepBuilder("videoProcessingTasklet", jobRepository)
                .tasklet(encodeVideoTasklet(), manager)
                .build();
    }

    @Bean
    public Step outputFormatStep() {
        return new StepBuilder("outputFormatStep", jobRepository)
                .tasklet(outputFormatTasklet(), manager)
                .build();
    }

    @Bean
    public Tasklet inputConversionTasklet() {
        return new InputConversionTasklet(videoUtils, objectMapper);
    }

    @Bean
    public Tasklet encodeVideoTasklet() {
        return new VideoProcessingTasklet(ffmpegService, videoUtils);
    }

    @Bean
    public Tasklet outputFormatTasklet() {
        return new OutputFormatTasklet(videoPublisherService);
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }
}
