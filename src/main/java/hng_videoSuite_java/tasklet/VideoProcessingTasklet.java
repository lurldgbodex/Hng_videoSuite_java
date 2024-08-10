package hng_videoSuite_java.tasklet;

import hng_videoSuite_java.video.enums.VideoStatus;
import hng_videoSuite_java.video.sevice.FfmpegService;
import hng_videoSuite_java.video.sevice.VideoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Slf4j
@RequiredArgsConstructor
public class VideoProcessingTasklet implements Tasklet {
    private final FfmpegService ffmpegService;
    private final VideoUtils videoUtils;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Starting video processing");

        String jobId = (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("jobId");
        String outputPath = (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("outputPath");
        String tempInputPath = (String) chunkContext.getStepContext().getStepExecution().getJobExecution().getExecutionContext().get("tempFilePath");

        if (jobId == null || outputPath == null || tempInputPath == null) {
            log.info("Failed to extract details from context");
            return RepeatStatus.FINISHED;
        }

        List<String> videoFilePathsList = Files.readAllLines(Paths.get(tempInputPath));
        String[] videoFilePaths = videoFilePathsList.toArray(new String[0]);

        try {
            log.info("Running ffmpeg service");
            ffmpegService.mergeVideos(outputPath, jobId, videoFilePaths);
            log.info("successfully run ffmpegService");
        } catch (InterruptedException | IOException | ExecutionException ex) {
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED.name());
            return RepeatStatus.FINISHED;
        }

        return RepeatStatus.FINISHED;
    }
}
