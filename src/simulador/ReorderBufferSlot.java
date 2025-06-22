package simulador;

/**
 * Classe que representa um slot do Buffer de Reordenamento (ROB)
 */
public class ReorderBufferSlot {
    private boolean busy;
    private Instrucao instrucao;
    private EstadoInstrucao estado;
    private String registradorPublico;  // Registrador arquitetural a ser atualizado (ex: "R5")
    private String registradorRenomeado;        // Registrador físico alocado para o resultado (ex: "P14")
    private Float resultado;             // Valor do resultado
    private boolean pronto;              // true quando o campo resultado for válido
    private int indice;                  // Índice do slot no ROB
    private int cicloIssue;             // Ciclo em que a instrução foi emitida
    private int cicloExecucao;          // Ciclo em que a instrução começou a executar
    private int cicloEscrita;           // Ciclo em que o resultado foi escrito
    private int cicloCommit;            // Ciclo em que a instrução foi commitada
    
    public ReorderBufferSlot(int indice) {
        this.indice = indice;
        this.busy = false;
        this.instrucao = null;
        this.estado = EstadoInstrucao.PROCESSANDO;
        this.registradorPublico = null;
        this.registradorRenomeado = null;
        this.resultado = null;
        this.pronto = false;
        this.cicloIssue = -1;
        this.cicloExecucao = -1;
        this.cicloEscrita = -1;
        this.cicloCommit = -1;
    }

    public ReorderBufferSlot(ReorderBufferSlot outro) {
        this.busy = outro.busy;
        this.instrucao = outro.instrucao;
        this.estado = outro.estado;
        this.registradorPublico = outro.registradorPublico;
        this.registradorRenomeado = outro.registradorRenomeado;
        this.resultado = outro.resultado;
        this.pronto = outro.pronto;
        this.indice = outro.indice;
        this.cicloIssue = outro.cicloIssue;
        this.cicloExecucao = outro.cicloExecucao;
        this.cicloEscrita = outro.cicloEscrita;
        this.cicloCommit = outro.cicloCommit;
    }
    
    /**
     * Limpa o slot do ROB
     */
    public void limpar() {
        this.busy = false;
        this.instrucao = null;
        this.estado = EstadoInstrucao.PROCESSANDO;
        this.registradorPublico = null;
        this.registradorRenomeado = null;
        this.resultado = null;
        this.pronto = false;
        this.cicloIssue = -1;
        this.cicloExecucao = -1;
        this.cicloEscrita = -1;
        this.cicloCommit = -1;
    }
    
    /**
     * Verifica se o slot está pronto para commit
     */
    public boolean prontoParaCommit() {
        return busy && estado == EstadoInstrucao.CONCLUIDA;
    }
    
    /**
     * Marca o resultado como pronto
     */
    public void marcarResultadoPronto(Float valor, int ciclo) {
        setResultado(valor);
        this.pronto = true;
        this.estado = EstadoInstrucao.CONCLUIDA;
        this.cicloEscrita = ciclo;
    }
    
    
    // Getters e Setters
    public boolean isBusy() {
        return busy;
    }
    
    public void setBusy(boolean busy) {
        this.busy = busy;
    }
    
    public Instrucao getInstrucao() {
        return instrucao;
    }
    
    public void setInstrucao(Instrucao instrucao) {
        this.instrucao = instrucao;
    }
    
    public EstadoInstrucao getEstado() {
        return estado;
    }
    
    public void setEstado(EstadoInstrucao estado) {
        this.estado = estado;
    }
    
    public String getRegistradorPublico() {
        return registradorPublico;
    }
    
    public void setRegistradorPublico(String registradorPublico) {
        this.registradorPublico = registradorPublico;
    }
    
    public String getRegistradorRenomeado() {
        return registradorRenomeado;
    }
    
    public void setRegistradorRenomeado(String registradorRenomeado) {
        this.registradorRenomeado = registradorRenomeado;
    }
    
    public Float getResultado() {
        return resultado;
    }
    
    public void setResultado(Float resultado) {
        this.resultado = resultado;
    }
    
    public boolean isPronto() {
        return pronto;
    }
    
    public void setPronto(boolean pronto) {
        this.pronto = pronto;
    }
    
    public int getIndice() {
        return indice;
    }
    
    public void setIndice(int indice) {
        this.indice = indice;
    }
    
    public int getCicloIssue() {
        return cicloIssue;
    }
    
    public void setCicloIssue(int cicloIssue) {
        this.cicloIssue = cicloIssue;
    }
    
    public int getCicloExecucao() {
        return cicloExecucao;
    }
    
    public void setCicloExecucao(int cicloExecucao) {
        this.cicloExecucao = cicloExecucao;
    }
    
    public int getCicloEscrita() {
        return cicloEscrita;
    }
    
    public void setCicloEscrita(int cicloEscrita) {
        this.cicloEscrita = cicloEscrita;
    }
    
    public int getCicloCommit() {
        return cicloCommit;
    }
    
    public void setCicloCommit(int cicloCommit) {
        this.cicloCommit = cicloCommit;
    }
}
