package hng_videoSuite_java.video.sevice;

import com.fasterxml.jackson.databind.ObjectMapper;
import hng_videoSuite_java.video.dto.VideoCompressDto;
import hng_videoSuite_java.video.dto.VideoPathDto;
import hng_videoSuite_java.video.sevice.VideoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCompressorConsumer {

    private final ObjectMapper objectMapper;
    private final VideoUtils videoUtils;

    public static String UPLOAD_DIR = "videoFiles";

    @RabbitListener(queues = "${rabbitmq.queue.compress:videoCompress}")
    public void handleTask(String message) throws IOException {
        log.info("Video compressor queue started.");

        VideoCompressDto videoCompressDto = objectMapper.readValue(message, VideoCompressDto.class);
        String jobId = videoCompressDto.getJobId();
        String resolution = videoCompressDto.getResolution();
        String outputFormat = videoCompressDto.getOutputFormat();
        String bitrate = videoCompressDto.getBitrate();

        String inputFile = saveVideoToFile(videoCompressDto.getVideo());
        log.info("Video temporarily saved at: {}", inputFile);

        String outputFile = UPLOAD_DIR + File.separator + jobId + "_compressed.mp4";

        StringBuilder commandBuilder = new StringBuilder(String.format("ffmpeg -i %s -vcodec h264", inputFile));

        if (bitrate != null && !bitrate.isEmpty()) {
            switch (resolution.toUpperCase()) {
                case "HIGH":
                    bitrate = "5M";
                       break;
                case "MEDIUM":
                    bitrate = "2.5M";
                        break;
                case "LOW":
                    bitrate = "1M";
                       break;
                }
            commandBuilder.append(" -b:v ").append(bitrate);
            }

        commandBuilder.append(" -b:v 500K");

        if (resolution != null && !resolution.isEmpty()) {
            switch (resolution.toUpperCase()) {
                case "HIGH":
                    commandBuilder.append(" -vf scale=1920:1080");
                    break;
                case "MEDIUM":
                    commandBuilder.append(" -vf scale=1280:720");
                    break;
                case "LOW":
                    commandBuilder.append(" -vf scale=640:360");
                    break;
                default:
                    log.warn("Unknown resolution. Skipping resolution setting.");
            }
        }

        if (outputFormat == null || outputFormat.isEmpty()) {
            outputFormat = "mp4";
        }
        commandBuilder.append(" -f ").append(outputFormat);

        commandBuilder.append(" ").append(outputFile);

        String command = commandBuilder.toString();
        System.out.println("comm -- " + command);

        try {
            Process process = Runtime.getRuntime().exec(command);
            new Thread(() -> {try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info(line);
                    }
                } catch (IOException exception) {
                    log.error("Error reading ffmpeg output", exception);
                }
            }).start();

            new Thread(() -> {try (var reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.error(line);
                    }
                } catch (IOException exception) {
                    log.error("Error reading ffmpeg error output", exception);
                }
            }).start();

            int exitCode = process.waitFor();
            log.info("ffmpeg exit code: {}", exitCode);

            if (exitCode == 0) {
                File input = new File(inputFile);

                if (input.delete()) {
                    videoUtils.updateJobStatus(jobId, "SAVED");
                    log.info("Video compression successful. Original file deleted.");
                } else {
                    throw new RuntimeException("Failed to delete the original video file.");
                }
            } else {
                throw new RuntimeException("Video compression failed with exit code " + exitCode);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Video compression was interrupted", exception);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to execute video compression command", exception);
        }
    }




    public static String saveVideoToFile(byte[] videoByte) throws IOException {
        File dir = new File(UPLOAD_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String filename = "video_" + UUID.randomUUID() + ".mp4";
        String filePath = UPLOAD_DIR + File.separator + filename;
        File videoFile = new File(filePath);

        try (FileOutputStream outputStream = new FileOutputStream(videoFile)) {
            outputStream.write(videoByte);
        }
        return videoFile.getAbsolutePath();
    }
}
