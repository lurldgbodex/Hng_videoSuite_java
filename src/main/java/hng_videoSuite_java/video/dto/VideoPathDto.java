package hng_videoSuite_java.video.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Data
public class VideoPathDto {
    private String jobId;
    private Map<String, byte[]> video = new HashMap<>();
}
