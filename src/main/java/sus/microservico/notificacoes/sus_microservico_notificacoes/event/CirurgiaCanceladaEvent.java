package sus.microservico.notificacoes.sus_microservico_notificacoes.event;

import java.io.Serializable;
import java.util.UUID;

public record CirurgiaCanceladaEvent(
        UUID cirurgiaId
) implements Serializable {
}
