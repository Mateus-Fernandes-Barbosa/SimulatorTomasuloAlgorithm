package simulador;

/**
 * Classe que representa uma instrução MIPS
 */
public class Instrucao {
    private OpCode op;
    private String destino;  // Registrador de destino
    private String reg1;  // Primeiro registrador fonte
    private String reg2;  // Segundo registrador fonte
    private int imediato; // Valor imediato
    private String instrucaoOriginal; // Texto original da instrução
    private int ciclosDuracao; // Duração em ciclos da instrução
    // --- Novos campos para controle de execução ---
    private int estado = 0; // -1=pulada, 0=nenhuma, 1=issue, 2=exec, 3=write, 4=commit
    private int instanciaId = 0; // Para múltiplas execuções da mesma instrução

    public Instrucao(OpCode op, String destino, String reg1, String reg2, int imediato, String instrucaoOriginal) {
        this.op = op;
        this.destino = destino;
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.imediato = imediato;
        this.instrucaoOriginal = instrucaoOriginal;
        switch (op) {
                case ADD:
                    this.ciclosDuracao = 3;
                    break;
                case SUB:
                    this.ciclosDuracao = 3;
                    break;
                case ADDI:
                    this.ciclosDuracao = 3;
                    break;
                case SUBI:
                    this.ciclosDuracao = 3;
                    break;
                case BEQ:
                    this.ciclosDuracao = 3;
                    break;
                case MUL:
                    this.ciclosDuracao = 3;
                    break;
                case MULI:
                    this.ciclosDuracao = 3;
                    break;
                case DIV:
                    this.ciclosDuracao = 3;
                    break;
                case DIVI:
                    this.ciclosDuracao = 3;
                    break;
                case LOAD:
                    this.ciclosDuracao = 5;
                    break;
                case STORE:
                    this.ciclosDuracao = 5;
                    break;
                default:
                    this.ciclosDuracao = 1;
            }
    }

    // Construtor de cópia profunda
    public Instrucao(Instrucao outra) {
        this.op = outra.op;
        this.destino = outra.destino;
        this.reg1 = outra.reg1;
        this.reg2 = outra.reg2;
        this.imediato = outra.imediato;
        this.instrucaoOriginal = outra.instrucaoOriginal;
        this.ciclosDuracao = outra.ciclosDuracao;
        this.estado = outra.estado;
        this.instanciaId = outra.instanciaId;
    }

    // Construtor para instruções sem valor imediato
    public Instrucao(OpCode op, String destino, String reg1, String reg2, String instrucaoOriginal) {
        this(op, destino, reg1, reg2, 0, instrucaoOriginal);
    }

    // Construtor para instruções com valor imediato
    public Instrucao(OpCode op, String destino, String reg1, int imediato, String instrucaoOriginal) {
        this(op, destino, reg1, null, imediato, instrucaoOriginal);
    }

    // Getters e Setters
    public OpCode getOp() {
        return op;
    }

    public void setOp(OpCode op) {
        this.op = op;
    }

    public String getRd() {
        return destino;
    }

    public void setRd(String destino) {
        this.destino = destino;
    }

    public String getReg1() {
        return reg1;
    }

    public void setReg1(String reg1) {
        this.reg1 = reg1;
    }

    public String getReg2() {
        return reg2;
    }

    public void setReg2(String reg2) {
        this.reg2 = reg2;
    }

    public int getImediato() {
        return imediato;
    }

    public void setImediato(int imediato) {
        this.imediato = imediato;
    }

    public String getInstrucaoOriginal() {
        return instrucaoOriginal;
    }

    public void setInstrucaoOriginal(String instrucaoOriginal) {
        this.instrucaoOriginal = instrucaoOriginal;
    }

    public int getCiclosDuracao() {
        return ciclosDuracao;
    }

    // --- Novos métodos para controle de execução ---
    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    public int getInstanciaId() {
        return instanciaId;
    }

    public void setInstanciaId(int instanciaId) {
        this.instanciaId = instanciaId;
    }

    @Override
    public String toString() {
        String base = instrucaoOriginal != null ? instrucaoOriginal : 
               String.format("%s %s,%s,%s", op.getNome(), destino, reg1, reg2);
        return base + " [instancia " + instanciaId + ", estado " + estado + "]";
    }

    /**
     * Verifica se a instrução escreve em um registrador
     */
    public boolean podeEscrever() {
        return destino != null && !op.isBranch() && op != OpCode.STORE;
    }

    /**
     * Verifica se a instrução usa dois registradores fonte
     */
    public boolean usaDoisStringesFonte() {
        return reg2 != null && !op.hasImmediate();
    }
}
