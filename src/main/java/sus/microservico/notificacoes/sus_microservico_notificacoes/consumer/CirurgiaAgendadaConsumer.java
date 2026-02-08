package sus.microservico.notificacoes.sus_microservico_notificacoes.consumer;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import sus.microservico.notificacoes.sus_microservico_notificacoes.config.RabbitMQConfig;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.CirurgiaAtualizadaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.CirurgiaCanceladaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.CirurgiaCriadaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.CirurgiaNotificacao;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.CirurgiaNotificacaoRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CirurgiaAgendadaConsumer {
    
    private final Logger logger = LoggerFactory.getLogger(CirurgiaAgendadaConsumer.class);
    private final CirurgiaNotificacaoRepository repository;

    @RabbitListener(queues = RabbitMQConfig.CIRURGIA_CRIADA_QUEUE)
    public void receberCirurgiaCriada(CirurgiaCriadaEvent evento) {
        logger.info("Evento de criação recebido para paciente {}", evento.pacienteId());
        
        CirurgiaNotificacao notificacao = new CirurgiaNotificacao();
        notificacao.setPacienteId(evento.pacienteId());
        notificacao.setMedicoId(evento.medicoId());
        notificacao.setDataCirurgia(evento.dataCirurgia());
        notificacao.setHoraCirurgia(evento.horaCirurgia());
        notificacao.setLocal(evento.local());
        notificacao.setDataRecebimento(LocalDateTime.now());
        notificacao.setNotificacaoEnviada(false);
        
        CirurgiaNotificacao salva = repository.save(notificacao);
        logger.info("Cirurgia {} criada para paciente {}", salva.getId(), evento.pacienteId());
    }

    @RabbitListener(queues = RabbitMQConfig.CIRURGIA_ATUALIZADA_QUEUE)
    public void receberCirurgiaAtualizada(CirurgiaAtualizadaEvent evento) {
        logger.info("Evento de atualização recebido para cirurgia {}", evento.cirurgiaId());
        
        CirurgiaNotificacao notificacao = new CirurgiaNotificacao();
        notificacao.setPacienteId(evento.pacienteId());
        notificacao.setMedicoId(evento.medicoId());
        notificacao.setDataCirurgia(evento.dataCirurgia());
        notificacao.setHoraCirurgia(evento.horaCirurgia());
        notificacao.setLocal(evento.local());
        notificacao.setDataRecebimento(LocalDateTime.now());
        notificacao.setNotificacaoEnviada(false);
        
        CirurgiaNotificacao salva = repository.save(notificacao);
        logger.info("Atualização {} realizada com sucesso para paciente{}", salva.getId(), evento.pacienteId());
    }

    @RabbitListener(queues = RabbitMQConfig.CIRURGIA_CANCELADA_QUEUE)
    public void receberCirurgiaCancelada(CirurgiaCanceladaEvent evento) {
        logger.info("Evento de cancelamento recebido para cirurgia {}", evento.cirurgiaId());
        
        repository.findById(evento.cirurgiaId()).ifPresentOrElse(
            notificacao -> {
                repository.delete(notificacao);
                logger.info("Cancelamento da cirurgia {} realizado com sucesso", evento.cirurgiaId());
            },
            () -> logger.warn("Cancelamento não realizado para cirurgia {}", evento.cirurgiaId())
        );
    }
}
