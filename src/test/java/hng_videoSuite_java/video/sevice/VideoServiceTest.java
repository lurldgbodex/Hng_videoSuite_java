package hng_videoSuite_java.video.sevice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import hng_videoSuite_java.video.dto.VideoPathDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoServiceTest {
    @Mock
    private VideoUtils videoUtils;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private JobLauncher jobLauncher;
    @Mock
    private Job videoProcessingJob;
    @InjectMocks
    private VideoService underTest;

    @Test
    void testJobLaunch_success()
            throws IOException, JobInstanceAlreadyCompleteException,
            JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

        // Setup test data
        Map<String, byte[]> video = new HashMap<>();
        video.put("video1.mp4", new byte[]{1, 2, 3});
        video.put("video2.mp4", new byte[]{5, 6, 7});
        VideoPathDto videoPathDto = new VideoPathDto();
        videoPathDto.setJobId(UUID.randomUUID().toString());
        videoPathDto.setVideo(video);
        String jsonMessage = new ObjectMapper().writeValueAsString(videoPathDto);

        // Mocking
        when(objectMapper.readValue(anyString(), eq(VideoPathDto.class))).thenReturn(videoPathDto);
        JobExecution dummyJobExecution = mock(JobExecution.class);
        when(jobLauncher.run(any(Job.class), any(JobParameters.class))).thenReturn(dummyJobExecution);

        // Call method under test
        underTest.handleJobLaunch(jsonMessage);

        // Capturing JobParameters
        ArgumentCaptor<JobParameters> jobParametersCaptor = ArgumentCaptor.forClass(JobParameters.class);
        verify(jobLauncher).run(eq(videoProcessingJob), jobParametersCaptor.capture());

        // Verify JobParameters
        JobParameters jobParameters = jobParametersCaptor.getValue();
        assertThat(videoPathDto.getJobId()).isEqualTo(jobParameters.getString("jobId"));
    }

    @Test
    void testHandleJobLaunch_Failure_dueToParsingException()
            throws IOException, JobParametersInvalidException,
            JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {

        // Setup test data
        String jsonMessage = "{\"invalidJson\":true}";

        // Mocking
        when(objectMapper.readValue(anyString(), eq(VideoPathDto.class)))
                .thenThrow(new JsonProcessingException("Parsing Error") {});

        // Call method under test
        underTest.handleJobLaunch(jsonMessage);

        // Verify jobLauncher was not called
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));

        // Verify that the status update method was not called
        verify(videoUtils, never()).updateJobStatus(anyString(), any());
    }
    @Test
    void testHandleJobLaunch_Failure_dueToNullOrEmptyMessage() throws JobInstanceAlreadyCompleteException,
            JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException, IOException {
        // Call method under test with null message
        assertThatThrownBy(() -> underTest.handleJobLaunch(null))
                .isInstanceOf(RuntimeException.class);

        // Verify jobLauncher was not called
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));

        // Call method under test with empty message
        assertThatThrownBy(() -> underTest.handleJobLaunch(""))
                .isInstanceOf(RuntimeException.class);

        // Verify jobLauncher was not called
        verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
    }
}