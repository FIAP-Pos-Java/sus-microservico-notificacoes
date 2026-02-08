package sus.microservico.notificacoes.sus_microservico_notificacoes.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.Cirurgia;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.CirurgiaRepository;
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
            
            // Para cada cirurgia, enviar lembrete apenas para o paciente
            for (Cirurgia cirurgia : cirurgias) {
                try {
                    enviarLembreteCirurgia(cirurgia);
                    
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
        String mensagem = String.format(
                "LEMBRETE: Sua cirurgia está agendada para %s às %s no local: %s. " +
                "Por favor, siga as orientações médicas de preparo.",
                cirurgia.getDataCirurgia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                cirurgia.getHoraCirurgia().format(DateTimeFormatter.ofPattern("HH:mm")),
                cirurgia.getLocal()
        );
        
        logger.info("Enviando lembrete para paciente {} - Cirurgia {}", 
                cirurgia.getPacienteId(), cirurgia.getId());
        
        notificacaoService.enviarLembretePaciente(cirurgia.getPacienteId(), mensagem);
    }

    // Método para testes - executa a cada 2 minutos (descomente se quiser testar)
    // @Scheduled(fixedDelay = 120000)
    // public void verificarCirurgiasProximasTeste() {
    //     this.logger.info("Executando verificação de teste...");
    //     verificarCirurgiasProximas();
    // }
}
