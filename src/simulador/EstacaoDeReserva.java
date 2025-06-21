package simulador;

/**
 * Classe que representa uma Estação de Reserva no algoritmo de Tomasulo
 */
public class EstacaoDeReserva {
    private String nome;
    private boolean busy;
    private OpCode op;
    private Float vj;  // Valor do operando j
    private Float vk;  // Valor do operando k
    private int qj;    // Índice do slot no ROB que produzirá o operando j (-1 se não há dependência)
    private int qk;    // Índice do slot no ROB que produzirá o operando k (-1 se não há dependência)
    private int dest;  // Índice do slot no ROB de destino
    private int imediato; // Valor imediato
    private int endereco; // Endereço de memória para LOAD/STORE
    private int ciclosRestantes; // Ciclos restantes para completar a operação
    
    public EstacaoDeReserva(String nome) {
        this.nome = nome;
        this.busy = false;
        this.op = null;
        this.vj = null;
        this.vk = null;
        this.qj = -1;
        this.qk = -1;
        this.dest = -1;
        this.imediato = 0;
        this.endereco = 0;
        this.ciclosRestantes = 0;
    }
    
    /**
     * Limpa a estação de reserva
     */
    public void limpar() {
        this.busy = false;
        this.op = null;
        this.vj = null;
        this.vk = null;
        this.qj = -1;
        this.qk = -1;
        this.dest = -1;
        this.imediato = 0;
        this.endereco = 0;
        this.ciclosRestantes = 0;
    }
    
    /**
     * Verifica se a estação está pronta para execução
     */
    public boolean prontaParaExecucao() {
        if (!busy) return false;
        
        // Para operações de memória, precisamos apenas do endereço base
        if (op.isMemoryOperation()) {
            return qj == -1; // Apenas rs precisa estar pronto
        }
        
        // Para outras operações, verificamos se todos os operandos estão prontos
        boolean vjPronto = (qj == -1);
        boolean vkPronto = (qk == -1) || op.hasImmediate() || (op.isBranch() && vk != null);
        
        return vjPronto && vkPronto;
    }
    
    /**
     * Inicia a execução da operação
     */
    public void iniciarExecucao() {
        if (prontaParaExecucao()) {
            // Define o número de ciclos baseado no tipo de operação
            switch (op) {
                case ADD:
                    ciclosRestantes = 3;
                    break;
                case SUB:
                    ciclosRestantes = 3;
                    break;
                case ADDI:
                    ciclosRestantes = 3;
                    break;
                case SUBI:
                    ciclosRestantes = 3;
                    break;
                case BEQ:
                    ciclosRestantes = 3;
                    break;
                case MUL:
                    ciclosRestantes = 3;
                    break;
                case MULI:
                    ciclosRestantes = 3;
                    break;
                case DIV:
                    ciclosRestantes = 3;
                    break;
                case DIVI:
                    ciclosRestantes = 3;
                    break;
                case LOAD:
                    ciclosRestantes = 5;
                    break;
                case STORE:
                    ciclosRestantes = 5;
                    break;
                default:
                    ciclosRestantes = 1;
            }
        }
    }
    
    /**
     * Executa um ciclo da operação
     * @return true se a operação foi completada
     */
    public boolean executarCiclo() {
        if (ciclosRestantes > 0) {
            ciclosRestantes--;
            return ciclosRestantes == 0;
        }
        return false;
    }
    
    /**
     * Calcula o resultado da operação
     */
    public Float calcularResultado() {
        if (op == null || !prontaParaExecucao()) {
            return null;
        }
        
        switch (op) {
            case ADD:
                return vj + (vk != null ? vk : 0);
            case SUB:
                return vj - (vk != null ? vk : 0);
            case MUL:
                return vj * (vk != null ? vk : 0);
            case DIV:
                if (vk != null && vk != 0) {
                    return vj / vk;
                }
                throw new RuntimeException("Divisão por zero!");
            case ADDI:
                return vj + imediato;
            case SUBI:
                return vj - imediato;
            case MULI:
                return vj * imediato;
            case DIVI:
                if (imediato != 0) {
                    return vj / imediato;
                }
                throw new RuntimeException("Divisão por zero!");
            case LOAD:
                return (float) (endereco + imediato); // Retorna o endereço calculado
            case STORE:
                return (float) (endereco + imediato); // Retorna o endereço calculado
            case BEQ:
                return vj.equals(vk) ? 1.0f : 0.0f; // 1 se iguais, 0 se diferentes
            default:
                return 0.0f;
        }
    }
    
    // Getters e Setters
    public String getNome() {
        return nome;
    }
    
    public void setNome(String nome) {
        this.nome = nome;
    }
    
    public boolean isBusy() {
        return busy;
    }
    
    public void setBusy(boolean busy) {
        this.busy = busy;
    }
    
    public OpCode getOp() {
        return op;
    }
    
    public void setOp(OpCode op) {
        this.op = op;
    }
    
    public Float getVj() {
        return vj;
    }
    
    public void setVj(Float vj) {
        this.vj = vj;
    }
    
    public Float getVk() {
        return vk;
    }
    
    public void setVk(Float vk) {
        this.vk = vk;
    }
    
    public int getQj() {
        return qj;
    }
    
    public void setQj(int qj) {
        this.qj = qj;
    }
    
    public int getQk() {
        return qk;
    }
    
    public void setQk(int qk) {
        this.qk = qk;
    }
    
    public int getDest() {
        return dest;
    }
    
    public void setDest(int dest) {
        this.dest = dest;
    }
    
    public int getImediato() {
        return imediato;
    }
    
    public void setImediato(int imediato) {
        this.imediato = imediato;
    }
    
    public int getEndereco() {
        return endereco;
    }
    
    public void setEndereco(int endereco) {
        this.endereco = endereco;
    }
    
    public int getCiclosRestantes() {
        return ciclosRestantes;
    }
    
    public void setCiclosRestantes(int ciclosRestantes) {
        this.ciclosRestantes = ciclosRestantes;
    }
}
