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
    
    public Instrucao(OpCode op, String destino, String reg1, String reg2, int imediato, String instrucaoOriginal) {
        this.op = op;
        this.destino = destino;
        this.reg1 = reg1;
        this.reg2 = reg2;
        this.imediato = imediato;
        this.instrucaoOriginal = instrucaoOriginal;
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
    
    public String getRs() {
        return reg1;
    }
    
    public void setRs(String reg1) {
        this.reg1 = reg1;
    }
    
    public String getRt() {
        return reg2;
    }
    
    public void setRt(String reg2) {
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
    
    @Override
    public String toString() {
        return instrucaoOriginal != null ? instrucaoOriginal : 
               String.format("%s %s,%s,%s", op.getNome(), destino, reg1, reg2);
    }
    
    /**
     * Verifica se a instrução escreve em um registrador
     */
    public boolean escreveRegistrador() {
        return destino != null && !op.isBranch() && op != OpCode.STORE;
    }
    
    /**
     * Verifica se a instrução usa dois registradores fonte
     */
    public boolean usaDoisRegistradoresFonte() {
        return reg2 != null && !op.hasImmediate();
    }
}
