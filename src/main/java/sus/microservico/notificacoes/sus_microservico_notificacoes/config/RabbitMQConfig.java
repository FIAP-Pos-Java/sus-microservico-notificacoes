package sus.microservico.notificacoes.sus_microservico_notificacoes.config;

import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    public static final String CIRURGIA_CRIADA_QUEUE = "notificacao.cirurgia.criada.queue";
    public static final String CIRURGIA_ATUALIZADA_QUEUE = "notificacao.cirurgia.atualizada.queue";
    public static final String CIRURGIA_CANCELADA_QUEUE = "notificacao.cirurgia.cancelada.queue";

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
