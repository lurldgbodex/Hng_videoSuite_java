package hng_videoSuite_java.video.sevice;

import hng_videoSuite_java.video.entity.VideoSuite;
import hng_videoSuite_java.video.enums.VideoStatus;
import hng_videoSuite_java.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VideoUtils {
    private final VideoRepository videoRepository;
    public static File byteArrayToFile(byte[] byteArray, String filePath) throws IOException {
        File file = new File(filePath);

        try(FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(byteArray);
        }
        return file;
    }

    public void updateJobStatus(UUID jobId, VideoStatus status) {
        VideoSuite job = videoRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("job not found with id"));
        job.setStatus(status);
        videoRepository.save(job);
    }

    @Transactional
    public void updateJobProgress(UUID jobId, int progress) {
        VideoSuite job = videoRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("job not found"));
        job.setProgress(progress);
        videoRepository.save(job);
    }
}
