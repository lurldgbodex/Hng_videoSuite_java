package hng_video_processing.video.sevice;

import hng_video_processing.notification.VideoPublisherService;
import hng_video_processing.utils.VideoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class FfmpegService {
    private final VideoPublisherService videoPublisherService;
    private final VideoUtils videoUtils;
    private final String ffmpeg = "ffmpeg";
    private final String ffprobe = "ffprobe";

    public Process executeCommand(String command) throws IOException {
        log.info("Executing command: {}", command);
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
    }

    public void handleProcessOutput(Process process, double totalDuration,
                                     UUID jobId) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicReference<Double> elapsedTime = new AtomicReference<>(0.0);

        try {
            Future<?> outputFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.contains("time=")) {
                            String timeStr = line.split("time=")[1].split(" ")[0].trim();
                            elapsedTime.set(parseDuration(timeStr));
                            double progress = (elapsedTime.get() / totalDuration) * 100;
                            int progressPercentage = (int) Math.round(progress);
                            synchronized (this) {
                                videoUtils.updateJobProgress(jobId, progressPercentage);
                            }
                            log.info("Progress: {}%", progressPercentage);
                        }
                    }
                } catch (IOException ioe) {
                    log.error("Error reading process output: {}", ioe.getMessage());
                }
            });

            Future<?> errorFuture = executor.submit(() -> {
                try(BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.error("Error: {}", line);
                    }
                } catch (IOException ioe) {
                    log.error("Error reading process error stream: {}", ioe.getMessage());
                }
            });

            outputFuture.get();
            errorFuture.get();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg process failed with exit code: {}" +  exitCode);
            }
        } finally {
            executor.shutdown();
        }
    }

    public void encodeVideo(String inputFilePath, String outputFilePath,
                             double totalDuration, UUID jobId) throws IOException, ExecutionException, InterruptedException {
        log.info("Encoding video: {}", inputFilePath);
        String command = String.format("%s -i %s -c:v libx264 -c:a aac -strict experimental %s",
                ffmpeg, inputFilePath, outputFilePath);
        Process process = executeCommand(command);
        handleProcessOutput(process, totalDuration, jobId);
    }


    public void mergeVideos(String outputFilePath, UUID jobId,
                            String... inputFiles) throws IOException, InterruptedException, ExecutionException {
        double totalDuration = 0;

        log.info("Starting video merge process for output file: {}", outputFilePath);

        for (String inputFile: inputFiles) {
            totalDuration += getVideoDuration(inputFile);
        }

        // Re-encode all videos to ensure same format
        Path tempDir = Files.createTempDirectory("merge_videos");
        String[] encodedFiles = new String[inputFiles.length];
        for (int i = 0; i < inputFiles.length; i++) {
            Path encodedFilePath = tempDir.resolve("re-encoded_" + Paths.get(inputFiles[i]).getFileName());
            encodeVideo(inputFiles[i], encodedFilePath.toString(), totalDuration, jobId);
            encodedFiles[i] = encodedFilePath.toString();
        }

        // create a list file for the concatenation
        Path listFile = tempDir.resolve("input_files.txt");
        try (BufferedWriter writer = Files.newBufferedWriter(listFile)) {
            for (String encodedFile : encodedFiles) {
                writer.write("file '" + encodedFile + "'\n");
            }
        }

        // merge the re-encoded videos
        String command = String.format("%s -f concat -safe 0 -i %s -c copy %s",
                ffmpeg, listFile.toAbsolutePath(), outputFilePath);
        Process process = executeCommand(command);
        handleProcessOutput(process, totalDuration, jobId);

        // clean up tmp files
        Files.deleteIfExists(listFile);
        for (String encodedFile : encodedFiles) {
            Files.deleteIfExists(Paths.get(encodedFile));
        }
        Files.deleteIfExists(tempDir);

        try {
            videoPublisherService.publishMergedVideo(jobId, new File(outputFilePath));
        } catch (IOException ex) {
            log.error("Failed to publish merged video: {}", ex.getMessage());
        }
    }

    public double getVideoDuration(String videoFilePath) throws IOException {
        // Command to get video duration
        String command = String.format("%s -v error -select_streams v:0 -show_entries stream=duration -of csv=p=0 %s",
                ffprobe, videoFilePath);
        Process process = executeCommand(command);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line != null) {
                return Double.parseDouble(line.trim());
            }
        }

        throw new IOException("Unable to get video duration");
    }

    private double parseDuration(String durationStr) {
        String[] parts = durationStr.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid duration format");
        }

        double hours = Double.parseDouble(parts[0]);
        double minutes = Double.parseDouble(parts[1]);
        double seconds = Double.parseDouble(parts[2]);

        return hours * 3600 + minutes * 60 + seconds;
    }
}
