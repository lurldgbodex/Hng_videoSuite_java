package hng_videoSuite_java.video.dto;

import lombok.*;

import java.util.Map;

@Builder
@AllArgsConstructor
@RequiredArgsConstructor
@Setter
@Getter
public class VideoCompressDto {

    private String jobId;
    private String resolution;
    private String outputFormat;
    private String bitrate;
    private byte[] video;

}
