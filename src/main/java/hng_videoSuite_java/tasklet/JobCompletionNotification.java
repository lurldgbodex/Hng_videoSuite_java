package hng_videoSuite_java.tasklet;

import hng_videoSuite_java.video.enums.VideoStatus;
import hng_videoSuite_java.video.sevice.VideoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JobCompletionNotification implements JobExecutionListener {
    private final VideoUtils videoUtils;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobId = jobExecution.getJobParameters().getString("jobId");
        if (jobId != null) {
            videoUtils.updateJobStatus(jobId, VideoStatus.PROCESSING);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobId = jobExecution.getJobParameters().getString("jobId");
        if (jobId != null) {
            if (jobExecution.getStatus().isUnsuccessful()) {
                videoUtils.updateJobStatus(jobId, VideoStatus.FAILED);
            } else {
                videoUtils.updateJobStatus(jobId, VideoStatus.SUCCESS);
            }
        }
    }
}
