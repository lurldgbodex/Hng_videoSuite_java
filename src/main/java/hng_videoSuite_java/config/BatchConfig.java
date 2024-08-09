package hng_videoSuite_java.config;

import hng_videoSuite_java.tasklet.JobCompletionNotification;
import hng_videoSuite_java.tasklet.VideoProcessingTasklet;
import hng_videoSuite_java.video.sevice.FfmpegService;
import hng_videoSuite_java.video.sevice.VideoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
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
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {
    private final VideoUtils videoUtils;
    private final FfmpegService ffmpegService;
    private final JobCompletionNotification listener;

    @Bean
    public Job videoProcessingJob(JobRepository jobRepository, Step videoProcessingStep) {
        return new JobBuilder("videoProcessingJob", jobRepository)
                .listener(listener)
                .start(videoProcessingStep)
                .build();
    }

    @Bean
    public Step videoProcessingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("videoProcessingStep", jobRepository)
                .tasklet(tasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet tasklet() {
        return new VideoProcessingTasklet(videoUtils, ffmpegService);
    }

    @Bean
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }
}
