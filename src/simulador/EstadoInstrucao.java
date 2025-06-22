package simulador;

/**
 * Enum que representa os possíveis estados de uma instrução no pipeline
 */
public enum EstadoInstrucao {
    PROCESSANDO("Processando"),
    EXECUTANDO("Executando"),
    ESCRITA("Escrita"),
    CANCELADA("Cancelada"),
    CONCLUIDA("Concluída");

    
    private final String descricao;
    
    EstadoInstrucao(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
    
    @Override
    public String toString() {
        return descricao;
    }
}
