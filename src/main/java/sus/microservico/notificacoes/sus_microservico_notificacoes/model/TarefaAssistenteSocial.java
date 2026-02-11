package sus.microservico.notificacoes.sus_microservico_notificacoes.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.enums.StatusTarefa;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_tarefa_assistente_social")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TarefaAssistenteSocial {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    private UUID pacienteId;
    private UUID cirurgiaId;
    
    @Column(length = 1000)
    private String descricao;
    
    @Enumerated(EnumType.STRING)
    private StatusTarefa status;
    
    private UUID assistenteSocialId;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataConclusao;
}
