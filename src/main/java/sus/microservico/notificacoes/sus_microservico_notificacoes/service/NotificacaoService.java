package sus.microservico.notificacoes.sus_microservico_notificacoes.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.NotificacaoCirurgiaAtualizadaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.NotificacaoCirurgiaCanceladaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.NotificacaoCirurgiaCriadaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.Paciente;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.TarefaAssistenteSocial;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.enums.StatusTarefa;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.PacienteRepository;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.TarefaAssistenteSocialRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final Logger logger = LoggerFactory.getLogger(NotificacaoService.class);
    private final PacienteRepository pacienteRepository;
    private final TarefaAssistenteSocialRepository tarefaRepository;

    public void processarNotificacaoCriacao(NotificacaoCirurgiaCriadaEvent evento) {
        logger.info("Processando notificação de criação para cirurgia {}", evento.cirurgiaId());
        
        Paciente paciente = pacienteRepository.findById(evento.pacienteId()).orElse(null);
        
        if (paciente == null) {
            logger.warn("Paciente {} não encontrado", evento.pacienteId());
            return;
        }
        
        String mensagem = String.format(
                "Cirurgia agendada para %s às %s no local: %s",
                evento.dataCirurgia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                evento.horaCirurgia().format(DateTimeFormatter.ofPattern("HH:mm")),
                evento.local()
        );
        
        enviarNotificacoes(paciente, "AGENDAMENTO", mensagem);
    }

    public void processarNotificacaoAtualizacao(NotificacaoCirurgiaAtualizadaEvent evento) {
        logger.info("Processando notificação de atualização para cirurgia {}", evento.cirurgiaId());
        
        Paciente paciente = pacienteRepository.findById(evento.pacienteId()).orElse(null);
        
        if (paciente == null) {
            logger.warn("Paciente {} não encontrado", evento.pacienteId());
            return;
        }
        
        String mensagem = String.format(
                "Cirurgia ATUALIZADA para %s às %s no local: %s",
                evento.dataCirurgia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                evento.horaCirurgia().format(DateTimeFormatter.ofPattern("HH:mm")),
                evento.local()
        );
        
        enviarNotificacoes(paciente, "ATUALIZAÇÃO", mensagem);
    }

    public void processarNotificacaoCancelamento(NotificacaoCirurgiaCanceladaEvent evento) {
        logger.info("Processando notificação de cancelamento para cirurgia {}", evento.cirurgiaId());
        
        Paciente paciente = pacienteRepository.findById(evento.pacienteId()).orElse(null);
        
        if (paciente == null) {
            logger.warn("Paciente {} não encontrado", evento.pacienteId());
            return;
        }
        
        String mensagem = String.format(
                "Cirurgia CANCELADA que estava agendada para %s às %s",
                evento.dataCirurgia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                evento.horaCirurgia().format(DateTimeFormatter.ofPattern("HH:mm"))
        );
        
        enviarNotificacoes(paciente, "CANCELAMENTO", mensagem);
    }

    private void enviarNotificacoes(Paciente paciente, String tipo, String mensagem) {
        boolean pacienteNotificado = false;
        boolean medicoNotificado = false;
        
        // Notificar paciente
        if (paciente.getEmail() != null && !paciente.getEmail().isBlank()) {
            enviarEmail(paciente.getEmail(), tipo, mensagem);
            pacienteNotificado = true;
        }
        
        if (paciente.getTelefone() != null && !paciente.getTelefone().isBlank()) {
            enviarSMS(paciente.getTelefone(), mensagem);
            pacienteNotificado = true;
        }
        
        // Se paciente não tem contato, criar tarefa para assistente social
        if (!pacienteNotificado) {
            criarTarefaAssistenteSocial(paciente.getId(), mensagem);
        }
        
        logger.info("Notificações enviadas - Paciente: {}, Médico: {}", pacienteNotificado, medicoNotificado);
    }

    private void enviarEmail(String email, String tipo, String mensagem) {
        logger.info("==========================================================");
        logger.info("EMAIL enviado para: {}", email);
        logger.info("Tipo: {}", tipo);
        logger.info("Mensagem: {}", mensagem);
        logger.info("==========================================================");
    }

    private void enviarSMS(String telefone, String mensagem) {
        logger.info("==========================================================");
        logger.info("SMS enviado para: {}", telefone);
        logger.info("Mensagem: {}", mensagem);
        logger.info("==========================================================");
    }

    private void criarTarefaAssistenteSocial(java.util.UUID pacienteId, String mensagem) {
        TarefaAssistenteSocial tarefa = new TarefaAssistenteSocial();
        tarefa.setPacienteId(pacienteId);
        tarefa.setDescricao("Notificar paciente presencialmente: " + mensagem);
        tarefa.setStatus(StatusTarefa.PENDENTE);
        tarefa.setDataCriacao(LocalDateTime.now());
        
        tarefaRepository.save(tarefa);
        logger.info("Tarefa criada para assistente social notificar paciente {} presencialmente", pacienteId);
    }
    
    public void enviarLembretePaciente(java.util.UUID pacienteId, String mensagem) {
        Paciente paciente = pacienteRepository.findById(pacienteId).orElse(null);
        
        if (paciente == null) {
            logger.warn("Paciente {} não encontrado para envio de lembrete", pacienteId);
            return;
        }
        
        if (paciente.getEmail() != null && !paciente.getEmail().isBlank()) {
            enviarEmail(paciente.getEmail(), "LEMBRETE", mensagem);
            logger.warn("Lembrete enviado para paciente por email");
        }
        
        if (paciente.getTelefone() != null && !paciente.getTelefone().isBlank()) {
            enviarSMS(paciente.getTelefone(), mensagem);
            logger.warn("Lembrete enviado para paciente por SMS");
        }
    }
}
