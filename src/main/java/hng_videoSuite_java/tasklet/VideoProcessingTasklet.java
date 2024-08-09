package hng_videoSuite_java.tasklet;

import hng_videoSuite_java.video.enums.VideoStatus;
import hng_videoSuite_java.video.sevice.FfmpegService;
import hng_videoSuite_java.video.sevice.VideoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessingTasklet implements Tasklet {
    private final VideoUtils videoUtils;
    private final FfmpegService ffmpegService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting job process execution");
        JobParameters jobParameters = contribution.getStepExecution().getJobParameters();
        String jobId = jobParameters.getString("jobId");
        String tempFilePath = jobParameters.getString("tempFilePath");
        String outputPath = jobParameters.getString("outputPath");

        if (jobId == null || tempFilePath == null || outputPath == null) {
            log.info("missing job id or files");
            return RepeatStatus.FINISHED;
        }
        List<String> videoFilePathsList = Files.readAllLines(Paths.get(tempFilePath));

        String[] videoFilePaths = videoFilePathsList.toArray(new String[0]);

        try {
            log.info("calling ffmpeg service");
            ffmpegService.mergeVideos(outputPath, jobId, videoFilePaths);
        } catch (InterruptedException | IOException | ExecutionException ex) {
            log.error("Error during video processing: {}", ex.getMessage());
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
        }
        return RepeatStatus.FINISHED;
    }
}
