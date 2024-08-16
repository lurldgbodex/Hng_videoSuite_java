package hng_videoSuite_java.video.sevice;

import com.fasterxml.jackson.databind.ObjectMapper;
import hng_videoSuite_java.video.dto.VideoCompressDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoCompressorConsumer {

    private final ObjectMapper objectMapper;
    private final VideoUtils videoUtils;

    private final RabbitTemplate rabbitTemplate;

    public static String UPLOAD_DIR = "videoFiles";

    @Value("${rabbitmq.queue.save.compress.video:savedVideo}")
    private String saveCompressedVideos;

    @RabbitListener(queues = "${rabbitmq.queue.compress:videoCompress}")
    public void handleTask(String message) throws IOException {
        log.info("Video compressor queue started.");

        VideoCompressDto videoCompressDto = objectMapper.readValue(message, VideoCompressDto.class);
        String jobId = videoCompressDto.getJobId(); String resolution = videoCompressDto.getResolution();
        String outputFormat = videoCompressDto.getOutputFormat(); String bitrate = videoCompressDto.getBitrate();

        String inputFile = saveVideoToFile(videoCompressDto.getVideo());
        log.info("Video temporarily saved at: {}", inputFile);

        String outputFile = UPLOAD_DIR + File.separator + jobId + "_compressed.mp4";
        StringBuilder commandBuilder = new StringBuilder(String.format("ffmpeg -i %s -vcodec h264", inputFile));

        setBitrateIfNotNull(resolution, bitrate, commandBuilder);
        commandBuilder.append(" -b:v 500K");
        setResolutionIfNotNull(resolution, commandBuilder);
        outputFormat = setOutputFormatToDefaultValue(outputFormat);
        commandBuilder.append(" -f ").append(outputFormat);
        commandBuilder.append(" ").append(outputFile);
        String command = commandBuilder.toString();
        runFFmpegCommand(jobId, inputFile, outputFile, command);
    }

    private static String setOutputFormatToDefaultValue(String outputFormat) {
        if (outputFormat == null || outputFormat.isEmpty()) {
            outputFormat = "mp4";
        }
        return outputFormat;
    }

    private static void setResolutionIfNotNull(String resolution, StringBuilder commandBuilder) {
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
    }

    private static void setBitrateIfNotNull(String resolution, String bitrate, StringBuilder commandBuilder) {
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
    }


    private void runFFmpegCommand(String jobId, String inputFile, String outputFile, String command) {
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

                    byte[] compressedVideoBytes = Files.readAllBytes(Paths.get(outputFile));
                    String encodedVideo = Base64.getEncoder().encodeToString(compressedVideoBytes);

                    Map<String, Object> videoMessage = new HashMap<>();
                    videoMessage.put("jobId", jobId);
                    videoMessage.put("filename", jobId + "_compressed.mp4");
                    videoMessage.put("video", encodedVideo);

                    String jsonString = objectMapper.writeValueAsString(videoMessage);
                    rabbitTemplate.convertAndSend(saveCompressedVideos, jsonString);

                    log.info("Original file deleted.");
                    log.info("Video compression successful");
                    log.info("Compressed video details sent to the queue.");
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
