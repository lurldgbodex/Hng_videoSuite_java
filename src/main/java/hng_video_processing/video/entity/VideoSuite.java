package hng_video_processing.video.entity;

import hng_video_processing.video.enums.VideoStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "video_suite")
public class VideoSuite {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private VideoStatus status;

    @Column(name = "job_type", nullable = false)
    private String jobType;

    @Column(name = "output_video_url")
    private String outputVideoUrl;

    @Column(name = "message")
    private String message;

    @Column(name = "progress")
    private int progress;
}
