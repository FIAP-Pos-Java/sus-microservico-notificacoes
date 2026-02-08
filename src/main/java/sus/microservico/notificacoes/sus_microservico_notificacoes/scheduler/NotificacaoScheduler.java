package sus.microservico.notificacoes.sus_microservico_notificacoes.scheduler;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.CirurgiaNotificacao;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.CirurgiaNotificacaoRepository;
import sus.microservico.notificacoes.sus_microservico_notificacoes.service.NotificacaoService;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class NotificacaoScheduler {

    private final Logger logger = LoggerFactory.getLogger(NotificacaoScheduler.class);
    private final CirurgiaNotificacaoRepository cirurgiaRepo;
    private final NotificacaoService notificacaoService;

    @Value("${notificacao.dias-antecedencia:10}")
    private int diasAntecedencia;

    @Scheduled(cron = "0 0 9 * * *") // Todo dia às 9h
    public void verificarCirurgiasProximas() {
        this.logger.info("========== Iniciando verificação de cirurgias próximas ==========");
        LocalDate dataLimite = LocalDate.now().plusDays(diasAntecedencia);
        this.logger.info("Buscando cirurgias até a data: {}", dataLimite);
        
        List<CirurgiaNotificacao> cirurgiasPendentes = cirurgiaRepo
                .findByNotificacaoEnviadaFalseAndDataCirurgiaLessThanEqual(dataLimite);
        
        this.logger.info("Encontradas {} cirurgias pendentes de notificação", cirurgiasPendentes.size());
        
        for (CirurgiaNotificacao cirurgia : cirurgiasPendentes) {
            try {
                notificacaoService.processarNotificacao(cirurgia);
            } catch (Exception e) {
                this.logger.error("Erro ao processar notificação da cirurgia {}: {}", 
                    cirurgia.getId(), e.getMessage());
            }
        }
        
        this.logger.info("========== Verificação de cirurgias concluída ==========");
    }

    // Método para testes - executa a cada 2 minutos (descomente se quiser testar)
    // @Scheduled(fixedDelay = 120000)
    // public void verificarCirurgiasProximasTeste() {
    //     this.logger.info("Executando verificação de teste...");
    //     verificarCirurgiasProximas();
    // }
}
