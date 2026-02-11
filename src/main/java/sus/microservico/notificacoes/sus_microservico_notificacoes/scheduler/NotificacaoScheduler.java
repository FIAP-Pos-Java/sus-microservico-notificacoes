package sus.microservico.notificacoes.sus_microservico_notificacoes.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.AssistenteSocial;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.Cirurgia;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.Paciente;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.TarefaAssistenteSocial;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.enums.StatusTarefa;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.AssistenteSocialRepository;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.CirurgiaRepository;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.PacienteRepository;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.TarefaAssistenteSocialRepository;
import sus.microservico.notificacoes.sus_microservico_notificacoes.service.NotificacaoService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificacaoScheduler {

    private final Logger logger = LoggerFactory.getLogger(NotificacaoScheduler.class);
    private final NotificacaoService notificacaoService;
    private final CirurgiaRepository cirurgiaRepository;
    private final PacienteRepository pacienteRepository;
    private final TarefaAssistenteSocialRepository tarefaRepository;
    private final AssistenteSocialRepository assistenteSocialRepository;
    
    private static final int DIAS_ANTECEDENCIA = 7;

    @Scheduled(cron = "0 0 9 * * *") // Todo dia às 9h
    public void verificarCirurgiasProximas() {
        logger.info("=== Iniciando verificação de cirurgias próximas ===");
        
        try {
            // Calcula a data daqui a 7 dias
            LocalDate dataAlvo = LocalDate.now().plusDays(DIAS_ANTECEDENCIA);
            logger.info("Buscando cirurgias para a data: {}", dataAlvo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            
            // Busca cirurgias marcadas para essa data que ainda não receberam lembrete
            List<Cirurgia> cirurgias = cirurgiaRepository.findByDataCirurgiaAndLembreteEnviadoFalse(dataAlvo);
            
            if (cirurgias == null || cirurgias.isEmpty()) {
                logger.info("Nenhuma cirurgia encontrada para {} dias a partir de hoje", DIAS_ANTECEDENCIA);
                return;
            }
            
            logger.info("Encontradas {} cirurgias para notificar", cirurgias.size());
            
            for (Cirurgia cirurgia : cirurgias) {
                try {
                    enviarLembreteCirurgia(cirurgia);
                    enviarLembreteAssistenteSocial(cirurgia);
                    
                    // Marcar lembrete como enviado
                    cirurgia.setLembreteEnviado(true);
                    cirurgiaRepository.save(cirurgia);
                    
                } catch (Exception e) {
                    logger.error("Erro ao enviar lembrete para cirurgia {}: {}", cirurgia.getId(), e.getMessage());
                }
            }
            
            logger.info("=== Verificação concluída com sucesso ===");
            
        } catch (Exception e) {
            logger.error("Erro inesperado ao verificar cirurgias próximas: {}", e.getMessage(), e);
        }
    }
    
    private void enviarLembreteCirurgia(Cirurgia cirurgia) {
        Paciente paciente = pacienteRepository.findById(cirurgia.getPacienteId()).orElse(null);
        
        if (paciente == null) {
            logger.warn("Paciente {} não encontrado para cirurgia {}", 
                       cirurgia.getPacienteId(), cirurgia.getId());
            return;
        }
        
        String dataCirurgia = cirurgia.getDataCirurgia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String horaCirurgia = cirurgia.getHoraCirurgia().format(DateTimeFormatter.ofPattern("HH:mm"));
        String local = cirurgia.getLocal();
        
        logger.info("Enviando lembrete para paciente {} ({}) - Cirurgia {}", 
                   paciente.getNome(), cirurgia.getPacienteId(), cirurgia.getId());
        
        notificacaoService.enviarLembretePaciente(cirurgia.getPacienteId(), dataCirurgia, horaCirurgia, local);
    }
    
    private void enviarLembreteAssistenteSocial(Cirurgia cirurgia) {
        List<TarefaAssistenteSocial> tarefas = tarefaRepository.findAll().stream()
                .filter(t -> t.getCirurgiaId() != null && t.getCirurgiaId().equals(cirurgia.getId()))
                .filter(t -> t.getStatus() == StatusTarefa.PENDENTE || t.getStatus() == StatusTarefa.EM_ANDAMENTO)
                .filter(t -> t.getAssistenteSocialId() != null)
                .toList();
        
        if (tarefas.isEmpty()) {
            logger.info("Nenhuma tarefa pendente/em andamento encontrada para cirurgia {}", cirurgia.getId());
            return;
        }
        
        logger.info("Encontradas {} tarefas para notificar assistentes sociais sobre cirurgia {}", 
                   tarefas.size(), cirurgia.getId());
        
        for (TarefaAssistenteSocial tarefa : tarefas) {
            try {
                AssistenteSocial assistente = assistenteSocialRepository.findById(tarefa.getAssistenteSocialId())
                        .orElse(null);
                
                if (assistente == null) {
                    logger.warn("Assistente social {} não encontrada para tarefa {}", 
                               tarefa.getAssistenteSocialId(), tarefa.getId());
                    continue;
                }
                
                Paciente paciente = pacienteRepository.findById(cirurgia.getPacienteId()).orElse(null);
                String nomePaciente = paciente != null ? paciente.getNome() : "Paciente ID: " + cirurgia.getPacienteId();
                String dataCirurgia = cirurgia.getDataCirurgia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String horaCirurgia = cirurgia.getHoraCirurgia().format(DateTimeFormatter.ofPattern("HH:mm"));
                String local = cirurgia.getLocal();
                
                logger.info("Enviando lembrete para assistente social {} ({}) sobre cirurgia {}", 
                           assistente.getNome(), assistente.getId(), cirurgia.getId());
                
                notificacaoService.enviarLembreteAssistenteSocial(assistente, nomePaciente, dataCirurgia, horaCirurgia, local);
                
            } catch (Exception e) {
                logger.error("Erro ao notificar assistente social sobre tarefa {}: {}", 
                            tarefa.getId(), e.getMessage());
            }
        }
    }

    // Método para testes - executa a cada 2 minutos (descomente se quiser testar)
    // @Scheduled(fixedDelay = 120000)
    // public void verificarCirurgiasProximasTeste() {
    //     this.logger.info("Executando verificação de teste...");
    //     verificarCirurgiasProximas();
    // }
}
