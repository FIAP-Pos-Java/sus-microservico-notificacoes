package sus.microservico.notificacoes.sus_microservico_notificacoes.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum StatusCirurgia {
    AGENDADA,
    REALIZADA,
    CANCELADA;

    @JsonCreator
    public static StatusCirurgia fromValue(String value) {
        return StatusCirurgia.valueOf(value.toUpperCase());
    }
}
