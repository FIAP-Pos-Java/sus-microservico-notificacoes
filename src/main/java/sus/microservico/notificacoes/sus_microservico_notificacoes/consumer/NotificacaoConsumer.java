package sus.microservico.notificacoes.sus_microservico_notificacoes.consumer;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import sus.microservico.notificacoes.sus_microservico_notificacoes.config.RabbitMQConfig;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.NotificacaoCirurgiaAtualizadaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.NotificacaoCirurgiaCanceladaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.NotificacaoCirurgiaCriadaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.service.NotificacaoService;

@Component
@RequiredArgsConstructor
public class NotificacaoConsumer {
    
    private final Logger logger = LoggerFactory.getLogger(NotificacaoConsumer.class);
    private final NotificacaoService notificacaoService;

    @RabbitListener(queues = RabbitMQConfig.CIRURGIA_CRIADA_QUEUE)
    public void receberNotificacaoCirurgiaCriada(NotificacaoCirurgiaCriadaEvent evento) {
        logger.info("Evento de notificação de criação recebido para cirurgia {}", evento.cirurgiaId());
        notificacaoService.processarNotificacaoCriacao(evento);
    }

    @RabbitListener(queues = RabbitMQConfig.CIRURGIA_ATUALIZADA_QUEUE)
    public void receberNotificacaoCirurgiaAtualizada(NotificacaoCirurgiaAtualizadaEvent evento) {
        logger.info("Evento de notificação de atualização recebido para cirurgia {}", evento.cirurgiaId());
        notificacaoService.processarNotificacaoAtualizacao(evento);
    }

    @RabbitListener(queues = RabbitMQConfig.CIRURGIA_CANCELADA_QUEUE)
    public void receberNotificacaoCirurgiaCancelada(NotificacaoCirurgiaCanceladaEvent evento) {
        logger.info("Evento de notificação de cancelamento recebido para cirurgia {}", evento.cirurgiaId());
        notificacaoService.processarNotificacaoCancelamento(evento);
    }
}
