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
    // Adiciona campo para controle de estado de execução
    private int estadoExecucao = 0; // -1: pulada, 0: nenhuma, 1: lida, 2: executada, 3: resultado escrito, 4: commitada
    private int qtdeExecucoes = 0;
    
    public Instrucao(OpCode op, String destino, String reg1, String reg2, int imediato, String instrucaoOriginal) {
        this.op = op;
        this.destino = destino;
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.imediato = imediato;
        this.instrucaoOriginal = instrucaoOriginal;
        switch (op) {
                case ADD:
                    this.ciclosDuracao = 1;
                    break;
                case SUB:
                    this.ciclosDuracao = 1;
                    break;
                case ADDI:
                    this.ciclosDuracao = 1;
                    break;
                case SUBI:
                    this.ciclosDuracao = 1;
                    break;
                case BEQ:
                    this.ciclosDuracao = 2;
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
        this.estadoExecucao = 0;
    }
    
    // Construtor para instruções sem valor imediato
    public Instrucao(OpCode op, String destino, String reg1, String reg2, String instrucaoOriginal) {
        this(op, destino, reg1, reg2, 0, instrucaoOriginal);
    }
    
    // Construtor para instruções com valor imediato
    public Instrucao(OpCode op, String destino, String reg1, int imediato, String instrucaoOriginal) {
        this(op, destino, reg1, null, imediato, instrucaoOriginal);
    }
    
    // Gettereg1 e Settereg1
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
    
    public int getEstadoExecucao() {
        return estadoExecucao;
    }
    public void setEstadoExecucao(int estadoExecucao) {
        if ( estadoExecucao > this.estadoExecucao + 2){ // Verifica se o novo estado é válido
            return;
        }
        this.estadoExecucao = estadoExecucao;
    }

    public void addExecucao() {
        this.qtdeExecucoes++;
    }

    public void resetExecucoes() {
        this.qtdeExecucoes = 0;
    }

    public int getQtdeExecucoes() {
        return qtdeExecucoes;
    }
    
    @Override
    public String toString() {
        return instrucaoOriginal != null ? instrucaoOriginal : 
               String.format("%s %s,%s,%s", op.getNome(), destino, reg1, reg2);
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
