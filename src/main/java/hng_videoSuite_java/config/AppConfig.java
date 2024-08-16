package hng_videoSuite_java.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Value("${rabbitmq.queue.concat:videoConcat}")
    private String videoConcat;

    @Value("${rabbitmq.queue.finishedConcat:finishedConcat}")
    private String finishedConcat;

    @Value("${rabbitmq.queue.compress:videoCompress}")
    private String videoCompress;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public Queue concatQueue() {
        return new Queue(videoConcat, true);
    }

    @Bean
    public Queue finishedConcat() {
        return new Queue(finishedConcat, true);
    }

    @Bean
    public Queue videoCompressQueue(){
        return new Queue(videoCompress, true);
    }

}