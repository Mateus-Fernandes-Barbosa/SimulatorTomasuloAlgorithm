package simulador;

import java.io.*;
import java.util.*;

/**
 * Parser responsável por ler e converter instruções MIPS de um arquivo texto
 */
public class InstructionParser {
    
    /**
     * Lê instruções de um arquivo texto
     * @param nomeArquivo Nome do arquivo a ser lido
     * @return Lista de instruções parseadas
     * @throws IOException Se houver erro na leitura do arquivo
     */
    public static List<Instrucao> lerInstrucoes(String nomeArquivo) throws IOException {
        List<Instrucao> instrucoes = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(nomeArquivo))) {
            String linha;
            int numeroLinha = 0;
            
            while ((linha = reader.readLine()) != null) {
                numeroLinha++;
                linha = linha.trim();
                
                // Ignora linhas vazias e comentários
                if (linha.isEmpty() || linha.startsWith("#") || linha.startsWith("//")) {
                    continue;
                }
                
                try {
                    Instrucao instrucao = parsearInstrucao(linha);
                    instrucoes.add(instrucao);
                } catch (Exception e) {
                    System.err.println("Erro ao parsear linha " + numeroLinha + ": " + linha);
                    System.err.println("Erro: " + e.getMessage());
                }
            }
        }
        
        return instrucoes;
    }
    
    /**
     * Parseia uma única linha de instrução
     * @param linha Linha de texto contendo a instrução
     * @return Instrução parseada
     */
    public static Instrucao parsearInstrucao(String linha) {
        linha = linha.trim().toUpperCase();
        
        // Remove vírgulas extras e espaços
        // Antes: ADD  R1,  R2, R3
        linha = linha.replaceAll("\\s+", " "); 
        // Depois: ADD R1, R2, R3


        String[] partes = linha.split("\\s+", 2);
        if (partes.length < 2) {
            throw new IllegalArgumentException("Formato de instrução inválido: " + linha);
        }
        //Parte 0: ADD
        String mnemonico = partes[0];
        //Parte 1: R1, R2, R3
        String operandos = partes[1];
        
        OpCode op = OpCode.fromString(mnemonico);
        
        // Remove espaços ao redor das vírgulas
        // Antes: R1,  R2,  R3
        operandos = operandos.replaceAll("\\s*,\\s*", ",");
        // Depois: R1,R2,R3
        
        switch (op) {
            case LOAD:
                return parsearLoad(op, operandos, linha);
            case STORE:
                return parsearStore(op, operandos, linha);
            case BEQ:
                return parsearBranch(op, operandos, linha);
            case ADDI:
            case SUBI:
            case MULI:
            case DIVI:
                return parsearImediato(op, operandos, linha);
            case ADD:
            case SUB:
            case MUL:
            case DIV:
                return parsearRegistrador(op, operandos, linha);
            default:
                throw new IllegalArgumentException("Operação não suportada: " + mnemonico);
        }
    }
    
    /**
     * Parseia instruções LOAD (ex: LOAD R1,100(R2))
     */
    private static Instrucao parsearLoad(OpCode op, String operandos, String linha) {
        String[] partes = operandos.split(",", 2);
        if (partes.length != 2) {
            throw new IllegalArgumentException("Formato LOAD inválido: " + linha);
        }
        
        String destino = partes[0].trim();
        String enderecoStr = partes[1].trim();
        
        // Parseia formato: imediato(registrador)
        if (enderecoStr.contains("(") && enderecoStr.contains(")")) {
            int abreParenteses = enderecoStr.indexOf('(');
            int fechaParenteses = enderecoStr.indexOf(')');
            
            String imediatoStr = enderecoStr.substring(0, abreParenteses);
            String rs = enderecoStr.substring(abreParenteses + 1, fechaParenteses);
            
            int imediato = Integer.parseInt(imediatoStr);
            return new Instrucao(op, destino, rs, imediato, linha);
        } else {
            throw new IllegalArgumentException("Formato de endereço LOAD inválido: " + linha);
        }
    }
    
    /**
     * Parseia instruções STORE (ex: STORE R1,100(R2))
     */
    private static Instrucao parsearStore(OpCode op, String operandos, String linha) {
        String[] partes = operandos.split(",", 2);
        if (partes.length != 2) {
            throw new IllegalArgumentException("Formato STORE inválido: " + linha);
        }
        
        String rs = partes[0].trim(); // Registrador com o valor a ser armazenado
        String enderecoStr = partes[1].trim();
        
        // Parseia formato: imediato(registrador)
        if (enderecoStr.contains("(") && enderecoStr.contains(")")) {
            int abreParenteses = enderecoStr.indexOf('(');
            int fechaParenteses = enderecoStr.indexOf(')');
            
            String imediatoStr = enderecoStr.substring(0, abreParenteses);
            String rt = enderecoStr.substring(abreParenteses + 1, fechaParenteses);
            
            int imediato = Integer.parseInt(imediatoStr);
            return new Instrucao(op, null, rs, rt, imediato, linha); // STORE não tem destino
        } else {
            throw new IllegalArgumentException("Formato de endereço STORE inválido: " + linha);
        }
    }
    
    /**
     * Parseia instruções de branch (ex: BEQ R1,R2,100)
     */
    private static Instrucao parsearBranch(OpCode op, String operandos, String linha) {
        String[] partes = operandos.split(",");
        if (partes.length != 3) {
            throw new IllegalArgumentException("Formato BEQ inválido: " + linha);
        }
        
        String rs = partes[0].trim();
        String rt = partes[1].trim();
        int imediato = Integer.parseInt(partes[2].trim());
        
        return new Instrucao(op, null, rs, rt, imediato, linha); // BEQ não tem destino
    }
    
    /**
     * Parseia instruções com valor imediato (ex: ADDI R1,R2,100)
     */
    private static Instrucao parsearImediato(OpCode op, String operandos, String linha) {
        String[] partes = operandos.split(",");
        if (partes.length != 3) {
            throw new IllegalArgumentException("Formato de instrução imediata inválido: " + linha);
        }
        
        String destino = partes[0].trim();
        String rs = partes[1].trim();
        int imediato = Integer.parseInt(partes[2].trim());
        
        return new Instrucao(op, destino, rs, imediato, linha);
    }
    
    /**
     * Parseia instruções com três registradores (ex: ADD R1,R2,R3)
     */
    private static Instrucao parsearRegistrador(OpCode op, String operandos, String linha) {
        String[] partes = operandos.split(",");
        if (partes.length != 3) {
            throw new IllegalArgumentException("Formato de instrução de registrador inválido: " + linha);
        }
        
        String destino = partes[0].trim();
        String rs = partes[1].trim();
        String rt = partes[2].trim();
        
        return new Instrucao(op, destino, rs, rt, linha);
    }
    
    /**
     * Valida se uma string é um registrador válido (formato Rx)
     */
    private static boolean isRegistradorValido(String reg) {
        return reg.matches("R\\d+");
    }
}
