package hng_videoSuite_java.tasklet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hng_videoSuite_java.video.dto.VideoPathDto;
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

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoProcessingTasklet implements Tasklet {
    private final VideoUtils videoUtils;
    private final FfmpegService ffmpegService;
    private final ObjectMapper objectMapper;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        JobParameters jobParameters = contribution.getStepExecution().getJobParameters();
        String jobId = jobParameters.getString("jobId");
        String videoPathDtoJson = jobParameters.getString("videoPathDto");

        VideoPathDto videoPathDto;

        try {
            videoPathDto = objectMapper.readValue(videoPathDtoJson, VideoPathDto.class);
        } catch (JsonProcessingException ex) {
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
            return RepeatStatus.FINISHED;
        }

        if (jobId == null | videoPathDto == null) {
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
            return RepeatStatus.FINISHED;
        }

        String resourcePath = getResourcePath();
        if (resourcePath == null) {
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
            return RepeatStatus.FINISHED;
        }

        File mergeVideosDir = new File(resourcePath + "merge_videos");
        if (!mergeVideosDir.exists() && !mergeVideosDir.mkdirs()) {
            log.error("Failed to create merge videos directory");
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
            return RepeatStatus.FINISHED;
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

            ffmpegService.mergeVideos(outputPath, jobId, videoFilePaths);
        } catch (InterruptedException | IOException | ExecutionException ex) {
            log.error("Error during video processing: {}", ex.getMessage());
            videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
        }
        return RepeatStatus.FINISHED;
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
