package sus.microservico.notificacoes.sus_microservico_notificacoes.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.NotificacaoCirurgiaAtualizadaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.NotificacaoCirurgiaCanceladaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.event.NotificacaoCirurgiaCriadaEvent;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.AssistenteSocial;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.Paciente;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.TarefaAssistenteSocial;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.enums.StatusTarefa;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.AssistenteSocialRepository;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.PacienteRepository;
import sus.microservico.notificacoes.sus_microservico_notificacoes.repository.TarefaAssistenteSocialRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class NotificacaoService {

    private final Logger logger = LoggerFactory.getLogger(NotificacaoService.class);
    private final PacienteRepository pacienteRepository;
    private final TarefaAssistenteSocialRepository tarefaRepository;
    private final AssistenteSocialRepository assistenteSocialRepository;
    private final JavaMailSender mailSender;
    
    @Value("${twilio.account.sid}")
    private String twilioAccountSid;
    
    @Value("${twilio.auth.token}")
    private String twilioAuthToken;
    
    @Value("${twilio.phone.number}")
    private String twilioPhoneNumber;
    
    @Value("${spring.mail.username}")
    private String emailFrom;
    
    public NotificacaoService(PacienteRepository pacienteRepository, 
                             TarefaAssistenteSocialRepository tarefaRepository,
                             AssistenteSocialRepository assistenteSocialRepository,
                             JavaMailSender mailSender) {
        this.pacienteRepository = pacienteRepository;
        this.tarefaRepository = tarefaRepository;
        this.assistenteSocialRepository = assistenteSocialRepository;
        this.mailSender = mailSender;
    }
    
    @PostConstruct
    public void initTwilio() {
        if (twilioAccountSid != null && !twilioAccountSid.isBlank() && 
            twilioAuthToken != null && !twilioAuthToken.isBlank()) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
            logger.info("Twilio inicializado com sucesso");
        } else {
            logger.warn("Credenciais do Twilio não configuradas - SMS não será enviado");
        }
    }

    public void processarNotificacaoCriacao(NotificacaoCirurgiaCriadaEvent evento) {
        try {
            logger.info("==========================================================");
            logger.info("PROCESSANDO NOTIFICAÇÃO DE CRIAÇÃO");
            logger.info("Cirurgia ID: {}", evento.cirurgiaId());
            logger.info("Paciente ID: {}", evento.pacienteId());
            logger.info("==========================================================");
            
            Paciente paciente = pacienteRepository.findById(evento.pacienteId()).orElse(null);
            
            if (paciente == null) {
                logger.error("==========================================================");
                logger.error("PACIENTE NÃO ENCONTRADO");
                logger.error("Paciente ID: {}", evento.pacienteId());
                logger.error("Tabela: tb_usuario_paciente");
                logger.error("Verifique se o paciente foi cadastrado corretamente!");
                logger.error("==========================================================");
                return;
            }
            
            logger.info("Paciente encontrado: {}", paciente.getNome());
            logger.info("E-mail: {}", paciente.getEmail() != null ? paciente.getEmail() : "(não possui)");
            logger.info("Telefone: {}", paciente.getTelefone() != null ? paciente.getTelefone() : "(não possui)");
            
            String assunto = "Confirmação de Agendamento de Cirurgia";
            String mensagem = criarMensagemAgendamento(paciente.getNome(), evento);
            
            enviarNotificacoes(paciente, assunto, mensagem);
            
            logger.info("==========================================================");
            logger.info("✓ NOTIFICAÇÃO PROCESSADA COM SUCESSO");
            logger.info("==========================================================");
        } catch (Exception e) {
            logger.error("==========================================================");
            logger.error("ERRO AO PROCESSAR NOTIFICAÇÃO DE CRIAÇÃO");
            logger.error("Cirurgia ID: {}", evento.cirurgiaId());
            logger.error("Paciente ID: {}", evento.pacienteId());
            logger.error("Erro: {}", e.getMessage());
            logger.error("Stack trace:", e);
            logger.error("==========================================================");
            throw e;
        }
    }

    public void processarNotificacaoAtualizacao(NotificacaoCirurgiaAtualizadaEvent evento) {
        logger.info("Processando notificação de atualização para cirurgia {}", evento.cirurgiaId());
        
        Paciente paciente = pacienteRepository.findById(evento.pacienteId()).orElse(null);
        
        if (paciente == null) {
            logger.warn("Paciente {} não encontrado", evento.pacienteId());
            return;
        }
        
        String assunto = "Atualização no Agendamento da sua Cirurgia";
        String mensagem = criarMensagemAtualizacao(paciente.getNome(), evento);
        
        enviarNotificacoes(paciente, assunto, mensagem);
    }

    public void processarNotificacaoCancelamento(NotificacaoCirurgiaCanceladaEvent evento) {
        logger.info("Processando notificação de cancelamento para cirurgia {}", evento.cirurgiaId());
        
        Paciente paciente = pacienteRepository.findById(evento.pacienteId()).orElse(null);
        
        if (paciente == null) {
            logger.warn("Paciente {} não encontrado", evento.pacienteId());
            return;
        }
        
        String assunto = "Cancelamento de Cirurgia";
        String mensagem = criarMensagemCancelamento(paciente.getNome(), evento);
        
        enviarNotificacoes(paciente, assunto, mensagem);
    }

    private void enviarNotificacoes(Paciente paciente, String tipo, String mensagem) {
        logger.info("----------------------------------------------------------");
        logger.info("INICIANDO ENVIO DE NOTIFICAÇÕES");
        logger.info("Paciente: {}", paciente.getNome());
        logger.info("----------------------------------------------------------");
        
        boolean pacienteNotificado = false;
        
        // Notificar paciente por e-mail
        if (paciente.getEmail() != null && !paciente.getEmail().isBlank()) {
            logger.info("📧 Paciente possui e-mail. Tentando enviar...");
            boolean emailEnviado = enviarEmail(paciente.getEmail(), tipo, mensagem);
            if (emailEnviado) {
                pacienteNotificado = true;
                logger.info("E-mail marcado como enviado");
            } else {
                logger.warn("E-mail NÃO foi enviado com sucesso");
            }
        } else {
            logger.info("Paciente NÃO possui e-mail cadastrado");
        }
        
        // Notificar paciente por SMS
        if (paciente.getTelefone() != null && !paciente.getTelefone().isBlank()) {
            logger.info("📱 Paciente possui telefone. Tentando enviar SMS...");
            boolean smsEnviado = enviarSMS(paciente.getTelefone(), mensagem);
            if (smsEnviado) {
                pacienteNotificado = true;
                logger.info("✓ SMS marcado como enviado");
            } else {
                logger.warn("⚠ SMS NÃO foi enviado com sucesso");
            }
        } else {
            logger.info("ℹ Paciente NÃO possui telefone cadastrado");
        }
        
        // Se paciente não tem contato, criar tarefa para assistente social
        if (!pacienteNotificado) {
            logger.warn("⚠ PACIENTE NÃO FOI NOTIFICADO (sem e-mail e sem telefone)");
            logger.info("Criando tarefa para Assistente Social...");
            criarTarefaAssistenteSocial(paciente.getId(), mensagem);
        } else {
            logger.info("✓ Paciente foi notificado com sucesso!");
        }
        
        logger.info("----------------------------------------------------------");
        logger.info("✓ ENVIO DE NOTIFICAÇÕES CONCLUÍDO");
        logger.info("Paciente notificado: {}", pacienteNotificado ? "SIM" : "NÃO (Tarefa criada para AS)");
        logger.info("----------------------------------------------------------");
    }

    private boolean enviarEmail(String email, String assunto, String mensagem) {
        try {
            logger.info("   → Verificando configuração de e-mail...");
            
            if (emailFrom == null || emailFrom.isBlank()) {
                logger.error("   ❌ E-MAIL DE ORIGEM NÃO CONFIGURADO!");
                logger.error("   Verifique a variável MAIL_USERNAME no .env");
                logger.error("   Valor atual: {}", emailFrom);
                return false;
            }
            
            logger.info("   ✓ E-mail de origem configurado: {}", emailFrom);
            logger.info("   → Criando mensagem de e-mail...");
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(email);
            message.setSubject("SusTech - " + assunto);
            message.setText(mensagem);
            
            logger.info("   → Enviando e-mail via JavaMailSender...");
            logger.info("   De: {}", emailFrom);
            logger.info("   Para: {}", email);
            logger.info("   Assunto: SusTech - {}", assunto);
            
            mailSender.send(message);
            
            logger.info("==========================================================");
            logger.info("✅ EMAIL ENVIADO COM SUCESSO!");
            logger.info("Destinatário: {}", email);
            logger.info("Assunto: {}", assunto);
            logger.info("==========================================================");
            return true;
        } catch (Exception e) {
            logger.error("==========================================================");
            logger.error("❌ ERRO AO ENVIAR E-MAIL");
            logger.error("Destinatário: {}", email);
            logger.error("E-mail de origem: {}", emailFrom);
            logger.error("Tipo de erro: {}", e.getClass().getSimpleName());
            logger.error("Mensagem de erro: {}", e.getMessage());
            logger.error("Stack trace:", e);
            logger.error("----------------------------------------------------------");
            logger.error("POSSÍVEIS CAUSAS:");
            logger.error("1. Credenciais do Gmail incorretas no .env");
            logger.error("2. Senha de app do Gmail não configurada");
            logger.error("3. Servidor SMTP não acessível (smtp.gmail.com:587)");
            logger.error("4. Autenticação de 2 fatores não habilitada no Gmail");
            logger.error("==========================================================");
            return false;
        }
    }
    
    private String criarMensagemAgendamento(String nomePaciente, NotificacaoCirurgiaCriadaEvent evento) {
        return String.format(
            "Olá, %s!\n\n" +
            "É com alegria que informamos que sua cirurgia foi agendada com sucesso.\n\n" +
            "Detalhes do Agendamento:\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📅 Data: %s\n" +
            "🕐 Horário: %s\n" +
            "📍 Local: %s\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Orientações Importantes:\n" +
            "• Chegue com 1 hora de antecedência\n" +
            "• Traga um acompanhante adulto\n" +
            "• Siga rigorosamente as orientações de jejum fornecidas pelo seu médico\n" +
            "• Traga seus documentos pessoais e cartão do SUS\n" +
            "• Leve seus exames médicos mais recentes\n\n" +
            "Em caso de dúvidas ou imprevistos, não hesite em nos contatar.\n" +
            "Estamos aqui para cuidar de você!\n\n" +
            "Atenciosamente,\n" +
            "Equipe SusTech\n" +
            "Sistema Único de Saúde",
            nomePaciente,
            evento.dataCirurgia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            evento.horaCirurgia().format(DateTimeFormatter.ofPattern("HH:mm")),
            evento.local()
        );
    }
    
    private String criarMensagemAtualizacao(String nomePaciente, NotificacaoCirurgiaAtualizadaEvent evento) {
        return String.format(
            "Olá, %s!\n\n" +
            "Informamos que houve uma alteração no agendamento da sua cirurgia.\n\n" +
            "Novos Detalhes do Agendamento:\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📅 Nova Data: %s\n" +
            "🕐 Novo Horário: %s\n" +
            "📍 Local: %s\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Por favor, atualize sua agenda com estas novas informações.\n\n" +
            "Orientações Importantes:\n" +
            "• Chegue com 1 hora de antecedência\n" +
            "• Traga um acompanhante adulto\n" +
            "• Siga rigorosamente as orientações de jejum fornecidas pelo seu médico\n" +
            "• Traga seus documentos pessoais e cartão do SUS\n" +
            "• Leve seus exames médicos mais recentes\n\n" +
            "Em caso de dúvidas, estamos à disposição para ajudá-lo(a).\n\n" +
            "Atenciosamente,\n" +
            "Equipe SusTech\n" +
            "Sistema Único de Saúde",
            nomePaciente,
            evento.dataCirurgia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            evento.horaCirurgia().format(DateTimeFormatter.ofPattern("HH:mm")),
            evento.local()
        );
    }
    
    private String criarMensagemCancelamento(String nomePaciente, NotificacaoCirurgiaCanceladaEvent evento) {
        return String.format(
            "Olá, %s,\n\n" +
            "Lamentamos informar que sua cirurgia foi cancelada.\n\n" +
            "Cirurgia Cancelada:\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📅 Data que estava agendada: %s\n" +
            "🕐 Horário: %s\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Pedimos desculpas pelo transtorno. O cancelamento pode ter ocorrido por diversos motivos,\n" +
            "incluindo questões administrativas, disponibilidade de recursos ou necessidades médicas.\n\n" +
            "Próximos Passos:\n" +
            "• Nossa equipe entrará em contato para reagendar sua cirurgia o mais breve possível\n" +
            "• Continue seguindo as orientações médicas fornecidas anteriormente\n" +
            "• Em caso de urgência ou sintomas preocupantes, procure atendimento imediato\n\n" +
            "Compreendemos a importância deste procedimento e estamos trabalhando para\n" +
            "encontrar uma nova data que atenda às suas necessidades.\n\n" +
            "Para mais informações ou dúvidas, entre em contato conosco.\n" +
            "Estamos aqui para apoiá-lo(a).\n\n" +
            "Atenciosamente,\n" +
            "Equipe SusTech\n" +
            "Sistema Único de Saúde",
            nomePaciente,
            evento.dataCirurgia().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
            evento.horaCirurgia().format(DateTimeFormatter.ofPattern("HH:mm"))
        );
    }

    private boolean enviarSMS(String telefone, String mensagem) {
        try {
            logger.info("   → Verificando configuração do Twilio...");
            
            if (twilioPhoneNumber == null || twilioPhoneNumber.isBlank()) {
                logger.warn("   ⚠ TWILIO NÃO CONFIGURADO");
                logger.warn("   SMS não será enviado (isso é opcional)");
                logger.warn("   Para habilitar SMS, configure as variáveis TWILIO_* no .env");
                return false;
            }
            
            logger.info("   ✓ Twilio configurado");
            logger.info("   → Formatando número de telefone...");
            
            // formato internacional
            String telefoneFormatado = telefone.startsWith("+") ? telefone : "+55" + telefone.replaceAll("[^0-9]", "");
            logger.info("   Número original: {}", telefone);
            logger.info("   Número formatado: {}", telefoneFormatado);
            
            logger.info("   → Enviando SMS via Twilio...");
            Message message = Message.creator(
                    new PhoneNumber(telefoneFormatado),
                    new PhoneNumber(twilioPhoneNumber),
                    mensagem
            ).create();
            
            logger.info("==========================================================");
            logger.info("✅ SMS ENVIADO COM SUCESSO!");
            logger.info("Destinatário: {}", telefone);
            logger.info("Twilio SID: {}", message.getSid());
            logger.info("Mensagem: {}", mensagem);
            logger.info("==========================================================");
            return true;
        } catch (Exception e) {
            logger.error("==========================================================");
            logger.error("❌ ERRO AO ENVIAR SMS");
            logger.error("Destinatário: {}", telefone);
            logger.error("Número Twilio: {}", twilioPhoneNumber);
            logger.error("Tipo de erro: {}", e.getClass().getSimpleName());
            logger.error("Mensagem de erro: {}", e.getMessage());
            logger.error("Stack trace:", e);
            logger.error("----------------------------------------------------------");
            logger.error("POSSÍVEIS CAUSAS:");
            logger.error("1. Credenciais do Twilio incorretas no .env");
            logger.error("2. Número de telefone do Twilio não verificado");
            logger.error("3. Saldo insuficiente na conta Twilio");
            logger.error("4. Número de destino inválido");
            logger.error("==========================================================");
            return false;
        }
    }

    private void criarTarefaAssistenteSocial(java.util.UUID pacienteId, String mensagem) {
        try {
            logger.info("   → Criando tarefa para Assistente Social...");
            
            TarefaAssistenteSocial tarefa = new TarefaAssistenteSocial();
            tarefa.setPacienteId(pacienteId);
            tarefa.setDescricao("Notificar paciente presencialmente: " + mensagem);
            tarefa.setDataCriacao(LocalDateTime.now());
            
            Optional<AssistenteSocial> assistenteDisponivel = encontrarAssistenteSocialMenosOcupada();
            
            if (assistenteDisponivel.isPresent()) {
                AssistenteSocial assistente = assistenteDisponivel.get();
                tarefa.setAssistenteSocialId(assistente.getId());
                tarefa.setStatus(StatusTarefa.EM_ANDAMENTO);
                
                logger.info("   Tarefa atribuída automaticamente à assistente social: {}", assistente.getNome());
                logger.info("   Matrícula: {}", assistente.getMatricula());
                logger.info("   E-mail: {}", assistente.getEmail());
            } else {
                tarefa.setStatus(StatusTarefa.PENDENTE);
                logger.warn("   ⚠ Nenhuma assistente social disponível no sistema");
                logger.warn("   Tarefa criada como PENDENTE para atribuição manual");
            }
            
            TarefaAssistenteSocial tarefaSalva = tarefaRepository.save(tarefa);
            
            logger.info("==========================================================");
            logger.info("✅ TAREFA CRIADA PARA ASSISTENTE SOCIAL");
            logger.info("Tarefa ID: {}", tarefaSalva.getId());
            logger.info("Paciente ID: {}", pacienteId);
            logger.info("Status: {}", tarefaSalva.getStatus());
            logger.info("Assistente Social: {}", tarefaSalva.getAssistenteSocialId() != null ? 
                       tarefaSalva.getAssistenteSocialId() : "Não atribuída");
            logger.info("Descrição: {}", tarefaSalva.getDescricao());
            logger.info("==========================================================");
        } catch (Exception e) {
            logger.error("==========================================================");
            logger.error("❌ ERRO AO CRIAR TAREFA PARA ASSISTENTE SOCIAL");
            logger.error("Paciente ID: {}", pacienteId);
            logger.error("Erro: {}", e.getMessage());
            logger.error("Stack trace:", e);
            logger.error("==========================================================");
            throw e;
        }
    }
    
    private Optional<AssistenteSocial> encontrarAssistenteSocialMenosOcupada() {
        
        List<AssistenteSocial> assistentes = assistenteSocialRepository.findAll();
        
        if (assistentes.isEmpty()) {
            logger.warn("   ⚠ Nenhuma assistente social cadastrada no sistema");
            return Optional.empty();
        }
        
        logger.info("{} assistente(s) social(is) encontrada(s) no sistema", assistentes.size());
        
        // Encontrar a assistente com menos tarefas ativas
        Optional<AssistenteSocial> assistenteMenosOcupada = assistentes.stream()
                .min(Comparator.comparingLong(assistente -> {
                    long tarefasAtivas = tarefaRepository.contarTarefasAtivasPorAssistente(assistente.getId());
                    logger.info("   - {} ({}): {} tarefa(s) ativa(s)", 
                               assistente.getNome(), 
                               assistente.getMatricula(), 
                               tarefasAtivas);
                    return tarefasAtivas;
                }));
        
        assistenteMenosOcupada.ifPresent(assistente -> {
            long tarefasAtivas = tarefaRepository.contarTarefasAtivasPorAssistente(assistente.getId());
            logger.info("Assistente selecionada: {} (atualmente com {} tarefa(s))", 
                       assistente.getNome(), 
                       tarefasAtivas);
        });
        
        return assistenteMenosOcupada;
    }
    
    public void enviarLembretePaciente(java.util.UUID pacienteId, String dataCirurgia, String horaCirurgia, String local) {
        Paciente paciente = pacienteRepository.findById(pacienteId).orElse(null);
        
        if (paciente == null) {
            logger.warn("Paciente {} não encontrado para envio de lembrete", pacienteId);
            return;
        }
        
        String assunto = "Lembrete: Sua Cirurgia se Aproxima";
        String mensagemEmail = criarMensagemLembretePaciente(paciente.getNome(), dataCirurgia, horaCirurgia, local);
        String mensagemSMS = String.format(
            "LEMBRETE SUSTECH: %s, sua cirurgia está agendada para %s às %s no %s. " +
            "Chegue com 1h de antecedência. Traga acompanhante e documentos.",
            paciente.getNome(),
            dataCirurgia,
            horaCirurgia,
            local
        );
        
        boolean notificado = false;
        
        if (paciente.getEmail() != null && !paciente.getEmail().isBlank()) {
            enviarEmail(paciente.getEmail(), assunto, mensagemEmail);
            logger.info("Lembrete enviado para paciente por email");
            notificado = true;
        }
        
        if (paciente.getTelefone() != null && !paciente.getTelefone().isBlank()) {
            enviarSMS(paciente.getTelefone(), mensagemSMS);
            logger.info("Lembrete enviado para paciente por SMS");
            notificado = true;
        }
        
        if (!notificado) {
            logger.warn("Paciente {} não possui e-mail ou telefone para receber lembrete", pacienteId);
        }
    }
    
    private String criarMensagemLembretePaciente(String nomePaciente, String dataCirurgia, String horaCirurgia, String local) {
        return String.format(
            "Olá, %s!\n\n" +
            "Este é um lembrete importante sobre sua cirurgia que está próxima.\n\n" +
            "Detalhes da sua Cirurgia:\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "📅 Data: %s (daqui a 7 dias)\n" +
            "🕐 Horário: %s\n" +
            "📍 Local: %s\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Checklist - Não se Esqueça:\n" +
            "✓ Confirme seu acompanhante adulto\n" +
            "✓ Separe seus documentos (RG, CPF e Cartão do SUS)\n" +
            "✓ Reúna todos os seus exames médicos\n" +
            "✓ Siga as orientações de jejum do seu médico\n" +
            "✓ Chegue com 1 hora de antecedência\n" +
            "✓ Use roupas confortáveis\n" +
            "✓ Evite usar joias, maquiagem ou esmalte\n\n" +
            "Importante:\n" +
            "Caso necessite remarcar ou tenha algum imprevisto, entre em contato\n" +
            "conosco o quanto antes. Sua saúde e bem-estar são nossa prioridade!\n\n" +
            "Se tiver qualquer dúvida, estamos à disposição para ajudá-lo(a).\n\n" +
            "Desejamos que tudo corra muito bem!\n\n" +
            "Atenciosamente,\n" +
            "Equipe SusTech\n" +
            "Sistema Único de Saúde",
            nomePaciente,
            dataCirurgia,
            horaCirurgia,
            local
        );
    }
    
    public void enviarLembreteAssistenteSocial(AssistenteSocial assistenteSocial, String nomePaciente, String dataCirurgia, String horaCirurgia, String local) {
        if (assistenteSocial == null) {
            logger.warn("Assistente social não encontrada");
            return;
        }
        
        String assunto = "Lembrete: Cirurgia de Paciente Próxima";
        String mensagemEmail = criarMensagemLembreteAssistenteSocial(assistenteSocial.getNome(), nomePaciente, dataCirurgia, horaCirurgia, local);
        String mensagemSMS = String.format(
            "LEMBRETE SUSTECH: Assistente %s, o paciente %s tem cirurgia em %s às %s no %s. Verificar contato se necessário.",
            assistenteSocial.getNome(),
            nomePaciente,
            dataCirurgia,
            horaCirurgia,
            local
        );
        
        boolean notificado = false;
        
        if (assistenteSocial.getEmail() != null && !assistenteSocial.getEmail().isBlank()) {
            enviarEmail(assistenteSocial.getEmail(), assunto, mensagemEmail);
            logger.info("Lembrete enviado para assistente social {} por email", assistenteSocial.getNome());
            notificado = true;
        }
        
        if (assistenteSocial.getTelefoneContato() != null && !assistenteSocial.getTelefoneContato().isBlank()) {
            enviarSMS(assistenteSocial.getTelefoneContato(), mensagemSMS);
            logger.info("Lembrete enviado para assistente social {} por SMS", assistenteSocial.getNome());
            notificado = true;
        }
        
        if (!notificado) {
            logger.warn("Assistente social {} não possui e-mail ou telefone para receber lembrete", 
                       assistenteSocial.getId());
        }
    }
    
    private String criarMensagemLembreteAssistenteSocial(String nomeAssistente, String nomePaciente, String dataCirurgia, String horaCirurgia, String local) {
        return String.format(
            "Olá, %s!\n\n" +
            "Este é um lembrete sobre uma cirurgia próxima de um paciente sob seus cuidados.\n\n" +
            "Informações do Paciente e Cirurgia:\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "👤 Paciente: %s\n" +
            "📅 Data: %s (daqui a 7 dias)\n" +
            "🕐 Horário: %s\n" +
            "📍 Local: %s\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n\n" +
            "Ações Recomendadas:\n" +
            "• Verificar se o paciente recebeu as orientações pré-operatórias\n" +
            "• Confirmar se o paciente possui acompanhante confirmado\n" +
            "• Verificar se há necessidade de suporte adicional (transporte, documentação, etc.)\n" +
            "• Entrar em contato com o paciente para confirmação\n\n" +
            "Caso identifique qualquer necessidade especial ou dificuldade do paciente,\n" +
            "por favor, tome as providências necessárias o quanto antes.\n\n" +
            "Conte com o apoio da equipe SusTech para melhor atender nossos pacientes!\n\n" +
            "Atenciosamente,\n" +
            "Sistema SusTech\n" +
            "Serviço Social - SUS",
            nomeAssistente,
            nomePaciente,
            dataCirurgia,
            horaCirurgia,
            local
        );
    }
}
