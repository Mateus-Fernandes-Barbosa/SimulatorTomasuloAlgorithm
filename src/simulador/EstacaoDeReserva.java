package simulador;

/**
 * Classe que representa uma Estação de Reserva no algoritmo de Tomasulo
 */
public class EstacaoDeReserva {
    private String nome;
    private boolean busy;
    private OpCode op;
    private Float vj; // Valor do operando j
    private Float vk; // Valor do operando k
    private String qj; // Nome do registrador privado que produzirá o operando j (-1 se não há
                       // dependência)
    private String qk; // Nome do registrador privado que produzirá o operando k (-1 se não há
                       // dependência)
    private String dest; // Nome do registrador privado de destino
    private int imediato; // Valor imediato // Endereço de memória para LOAD/STORE
    private int ciclosRestantes; // Ciclos restantes para completar a operação

    public EstacaoDeReserva(String nome) {
        this.nome = nome;
        this.busy = false;
        this.op = null;
        this.vj = null;
        this.vk = null;
        this.qj = null;
        this.qk = null;
        this.dest = null;
        this.imediato = 0;
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
        this.qj = null;
        this.qk = null;
        this.dest = null;
        this.imediato = 0;
        this.ciclosRestantes = 0;
    }

    /**
     * Verifica se a estação está pronta para execução
     */
    public boolean prontaParaExecucao() {
        boolean pronto = true;
        if (!busy) {
            if (op.isMemoryOperation()) {
                pronto = qj == null; // Apenas vj precisa estar pronto
            } else {
                // Para outras operações, verificamos se todos os operandos estão prontos
                boolean vjPronto = (qj == null);
                boolean vkPronto = (qk == null) || op.hasImmediate() || (op.isBranch() && vk != null);
                pronto = vjPronto && vkPronto;
            }
        }
        else{
            pronto = false;
        }

        // Para operações de memória, precisamos apenas do endereço base
        return pronto;
    }

    /**
     * Inicia a execução da operação
     */
    public void iniciarExecucao() {
        if (prontaParaExecucao()) {
            // Define o número de ciclos baseado no tipo de operação
            switch (op) {
                case ADD:
                    this.ciclosRestantes = 3;
                    break;
                case SUB:
                    this.ciclosRestantes = 3;
                    break;
                case ADDI:
                    this.ciclosRestantes = 3;
                    break;
                case SUBI:
                    this.ciclosRestantes = 3;
                    break;
                case BEQ:
                    this.ciclosRestantes = 3;
                    break;
                case MUL:
                    this.ciclosRestantes = 3;
                    break;
                case MULI:
                    this.ciclosRestantes = 3;
                    break;
                case DIV:
                    this.ciclosRestantes = 3;
                    break;
                case DIVI:
                    this.ciclosRestantes = 3;
                    break;
                case LOAD:
                    this.ciclosRestantes = 5;
                    break;
                case STORE:
                    this.ciclosRestantes = 5;
                    break;
                default:
                    this.ciclosRestantes = 1;
            }
        }
    }

    /**
     * Executa um ciclo da operação
     * 
     * @return true se a operação foi completada
     */
    public boolean executarCiclo() {
        boolean terminou = false;
        if (ciclosRestantes > 0) {
            ciclosRestantes--;
            if (ciclosRestantes == 0)
                terminou = true;
        }
        return terminou;
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
                return (float) (vj + imediato); // Retorna o endereço calculado
            case STORE:
                return (float) (vj + imediato); // Retorna o endereço calculado
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

    public String getQj() {
        return qj;
    }

    public void setQj(String qj) {
        this.qj = qj;
    }

    public String getQk() {
        return qk;
    }

    public void setQk(String qk) {
        this.qk = qk;
    }

    public String getDest() {
        return dest;
    }

    public void setDest(String dest) {
        this.dest = dest;
    }

    public int getImediato() {
        return imediato;
    }

    public void setImediato(int imediato) {
        this.imediato = imediato;
    }

    public int getCiclosRestantes() {
        return ciclosRestantes;
    }

    public void setCiclosRestantes(int ciclosRestantes) {
        this.ciclosRestantes = ciclosRestantes;
    }
}
