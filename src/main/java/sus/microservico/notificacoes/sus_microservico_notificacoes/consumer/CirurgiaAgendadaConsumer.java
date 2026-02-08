package sus.microservico.notificacoes.sus_microservico_notificacoes.consumer;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.CirurgiaAgendadaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.CirurgiaNotificacao;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.CirurgiaNotificacaoRepository;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class CirurgiaAgendadaConsumer {
    
    private final Logger logger = LoggerFactory.getLogger(CirurgiaAgendadaConsumer.class);
    private final CirurgiaNotificacaoRepository repository;

    @RabbitListener(queues = "cirurgia.agendada.queue")
    public void receberCirurgiaAgendada(CirurgiaAgendadaEvent evento) {
        this.logger.info("Evento CirurgiaAgendadaEvent recebido para paciente {}", evento.pacienteId());
        
        CirurgiaNotificacao notificacao = new CirurgiaNotificacao();
        notificacao.setPacienteId(evento.pacienteId());
        notificacao.setMedicoId(evento.medicoId());
        notificacao.setDataCirurgia(evento.dataCirurgia());
        notificacao.setHoraCirurgia(evento.horaCirurgia());
        notificacao.setLocal(evento.local());
        notificacao.setDataRecebimento(LocalDateTime.now());
        notificacao.setNotificacaoEnviada(false);
        
        CirurgiaNotificacao salva = repository.save(notificacao);
        this.logger.info("CirurgiaNotificacao {} salva para paciente {}", salva.getId(), evento.pacienteId());
    }
}
