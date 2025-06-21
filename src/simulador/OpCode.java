package simulador;

/**
 * Enum que representa as operações possíveis no simulador Tomasulo
 */
public enum OpCode {
    LOAD("LOAD"),
    STORE("STORE"),
    ADD("ADD"),
    SUB("SUB"),
    MUL("MUL"),
    DIV("DIV"),
    BEQ("BEQ"),
    ADDI("ADDI"),
    SUBI("SUBI"),
    MULI("MULI"),
    DIVI("DIVI");
    
    private final String nome;
    
    OpCode(String nome) {
        this.nome = nome;
    }
    
    public String getNome() {
        return nome;
    }
    
    /**
     * Converte uma string para o OpCode correspondente
     */
    public static OpCode fromString(String texto) {
        for (OpCode op : OpCode.values()) {
            if (op.nome.equalsIgnoreCase(texto)) {
                return op;
            }
        }
        throw new IllegalArgumentException("OpCode não reconhecido: " + texto);
    }
    
    /**
     * Verifica se a operação é de Load/Store
     */
    public boolean isMemoryOperation() {
        return this == LOAD || this == STORE;
    }
    
    /**
     * Verifica se a operação é de multiplicação/divisão (operações mais lentas)
     */
    public boolean isMultiplyDivide() {
        return this == MUL || this == DIV || this == MULI || this == DIVI;
    }
    
    /**
     * Verifica se a operação é um branch
     */
    public boolean isBranch() {
        return this == BEQ;
    }
    
    /**
     * Verifica se a operação usa valor imediato
     */
    public boolean hasImmediate() {
        return this == ADDI || this == SUBI || this == MULI || this == DIVI || this == LOAD || this == STORE;
    }
}
