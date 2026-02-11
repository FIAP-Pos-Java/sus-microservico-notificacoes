package sus.microservico.notificacoes.sus_microservico_notificacoes.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.TarefaAssistenteSocial;
import sus.microservico.notificacoes.sus_microservico_notificacoes.model.enums.StatusTarefa;

import java.util.List;
import java.util.UUID;

@Repository
public interface TarefaAssistenteSocialRepository extends JpaRepository<TarefaAssistenteSocial, UUID> {
    List<TarefaAssistenteSocial> findByStatus(StatusTarefa status);
    
    @Query("SELECT COUNT(t) FROM TarefaAssistenteSocial t WHERE t.assistenteSocialId = :assistenteSocialId " +
           "AND (t.status = 'PENDENTE' OR t.status = 'EM_ANDAMENTO')")
    long contarTarefasAtivasPorAssistente(@Param("assistenteSocialId") UUID assistenteSocialId);
}
