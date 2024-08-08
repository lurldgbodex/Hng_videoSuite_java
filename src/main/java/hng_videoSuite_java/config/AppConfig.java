package hng_videoSuite_java.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
    @Value("${rabbitmq.queue.concat}")
    private String videoConcat;

    @Value("${rabbitmq.queue.finishedConcat")
    private String finishedConcat;

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
    public DirectExchange exchange() {
        return new DirectExchange("myExchange");
    }

    @Bean
    public Binding binding(Queue concatQueue, DirectExchange exchange) {
        return BindingBuilder.bind(concatQueue).to(exchange).with("RoutingKey");
    }
}
