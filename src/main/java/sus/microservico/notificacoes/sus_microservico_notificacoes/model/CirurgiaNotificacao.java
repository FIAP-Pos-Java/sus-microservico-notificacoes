package sus.microservico.notificacoes.sus_microservico_notificacoes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "tb_cirurgia_notificacao")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CirurgiaNotificacao {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID pacienteId;
    private UUID medicoId;
    private LocalDate dataCirurgia;
    private LocalTime horaCirurgia;
    private String local;
    private LocalDateTime dataRecebimento;
    private boolean notificacaoEnviada;
    private LocalDateTime dataNotificacao;
}
