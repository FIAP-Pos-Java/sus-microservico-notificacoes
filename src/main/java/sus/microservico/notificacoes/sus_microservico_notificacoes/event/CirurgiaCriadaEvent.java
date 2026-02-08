package sus.microservico.notificacoes.sus_microservico_notificacoes.event;

import sus.microservico.notificacoes.sus_microservico_notificacoes.model.enums.StatusCirurgia;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CirurgiaCriadaEvent(
        UUID pacienteId,
        UUID medicoId,
        LocalDate dataCirurgia,
        LocalTime horaCirurgia,
        String local,
        String descricao,
        StatusCirurgia status
) implements Serializable {
}
