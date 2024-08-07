package hng_video_processing.video.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class VideoPathDto {
    private UUID jobId;
    private Map<String, byte[]> video = new HashMap<>();
}
