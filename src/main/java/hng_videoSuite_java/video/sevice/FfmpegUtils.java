package hng_videoSuite_java.video.sevice;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Component
@RequiredArgsConstructor
public class FfmpegUtils {
    private final VideoUtils videoUtils;

    Process executeCommand(String command) throws IOException {
        log.info("Executing command: {}", command);
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true);
        return processBuilder.start();
    }

    void handleProcessOutput(Process process, double totalDuration,
                                     String jobId) throws ExecutionException, InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        AtomicReference<Double> elapsedTime = new AtomicReference<>(0.0);

        try {
            Future<?> outputFuture = executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("FFmpeg output: {}", line);
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
                throw new RuntimeException("FFmpeg process failed with exit code: " + exitCode);
            }
        } finally {
            executor.shutdown();
        }
    }

    double getVideoDuration(String videoFilePath, String ffprobe) throws IOException {
        // Command to get video duration
        String command = String.format("%s -v error -select_streams v:0 -show_entries stream=duration -of csv=p=0 %s",
                ffprobe, videoFilePath);
        Process process = executeCommand(command);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            log.info("Duration: {}", line);
            if (line != null) {
                return Double.parseDouble(line.trim());
            }
        } catch (NumberFormatException ex) {
            log.error("Error parsing duration output: {}", ex.getMessage());
            throw new IOException("Invalid Duration Format " + ex.getMessage());
        }

        throw new IOException("Unable to get video duration");
    }

    double parseDuration(String durationStr) {
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
