package simulador;

import java.io.IOException;
import java.util.*;

/**
 * Classe principal do simulador do algoritmo de Tomasulo
 */
public class Simulador {
    // Configurações do simulador
    private static final int TAMANHO_ROB = 8;
    private static final int NUM_ESTACOES_ADD = 3;
    private static final int NUM_ESTACOES_MUL = 2;
    private static final int NUM_ESTACOES_LOAD = 2;
    private static final int NUM_REGISTRADORES_PRIVADOS = 32;
    private static final int NUM_REGISTRADORES_PUBLICOS = 16;
    
    // Estruturas de dados principais
    private Map<String, Float> bancoRegistradores;     // R1 -> valor
    private Map<String, Float> bancoPrivado;           // P1 -> valor
    private Map<String, String> mapaRenomeacao;       // R1 -> P5
    private Queue<String> filaRegistradoresLivres;    // Registradores privados livres
    
    private List<ReorderBufferSlot> rob;              // Buffer de Reordenamento
    private int robHead;                              // Cabeça do ROB (próximo a fazer commit)
    private int robTail;                              // Cauda do ROB (próximo slot livre)
    
    private List<EstacaoDeReserva> estacoesAdd;       // Estações para ADD/SUB
    private List<EstacaoDeReserva> estacoesMul;       // Estações para MUL/DIV
    private List<EstacaoDeReserva> estacoesLoad;      // Estações para LOAD/STORE
    
    private Map<Integer, Float> memoria;              // Memória principal
    
    // Estado do simulador
    private List<Instrucao> instrucoes;
    private int pc;                                   // Program Counter
    private int cicloAtual;
    private int totalCiclos;
    private int ciclosBolha;
    private boolean simulacaoCompleta;
    
    // Estatísticas
    private int instrucoesExecutadas;
    private List<String> logExecucao;
    
    /**
     * Construtor do simulador
     */
    public Simulador() {
        inicializarSimulador();
    }
    
    /**
     * Inicializa todas as estruturas do simulador
     */
    private void inicializarSimulador() {
        // Inicializa bancos de registradores
        bancoRegistradores = new HashMap<>();
        bancoPrivado = new HashMap<>();
        mapaRenomeacao = new HashMap<>();
        filaRegistradoresLivres = new LinkedList<>();
        
        // Inicializa registradores arquiteturais
        for (int i = 0; i < NUM_REGISTRADORES_PUBLICOS; i++) {
            String regPub = "R" + i;
            String regFis = "P" + i;
            bancoRegistradores.put(regPub, (float) i); //Inicializa com valores simples
            bancoPrivado.put(regFis, (float) 0.0);
            filaRegistradoresLivres.offer(regFis);
            mapaRenomeacao.put(regPub, regFis);
        }
        
        // Adiciona registradores privados livres
        for (int i = NUM_REGISTRADORES_PUBLICOS; i < NUM_REGISTRADORES_PRIVADOS; i++) {
        //for (int i = 0; i < NUM_REGISTRADORES_PRIVADOS; i++) {
            String regFis = "P" + i;
            bancoPrivado.put(regFis, (float) 0.0);
            filaRegistradoresLivres.offer(regFis);
        }
        
        // Inicializa ROB
        rob = new ArrayList<>();
        for (int i = 0; i < TAMANHO_ROB; i++) {
            rob.add(new ReorderBufferSlot(i));
        }
        robHead = 0;
        robTail = 0;
        
        // Inicializa estações de reserva
        estacoesAdd = new ArrayList<>();
        for (int i = 0; i < NUM_ESTACOES_ADD; i++) {
            estacoesAdd.add(new EstacaoDeReserva("Add" + (i + 1)));
        }
        
        estacoesMul = new ArrayList<>();
        for (int i = 0; i < NUM_ESTACOES_MUL; i++) {
            estacoesMul.add(new EstacaoDeReserva("Mult" + (i + 1)));
        }
        
        estacoesLoad = new ArrayList<>();
        for (int i = 0; i < NUM_ESTACOES_LOAD; i++) {
            estacoesLoad.add(new EstacaoDeReserva("Load" + (i + 1)));
        }
        
        // Inicializa memória
        memoria = new HashMap<>();
        for (int i = 0; i < 1000; i += 4) {
            memoria.put(i, (float) i); // Inicializa memória com valores simples
        }
        
        // Inicializa estado
        instrucoes = new ArrayList<>();
        pc = 0;
        cicloAtual = 0;
        totalCiclos = 0;
        ciclosBolha = 0;
        simulacaoCompleta = false;
        instrucoesExecutadas = 0;
        logExecucao = new ArrayList<>();
    }
    
    /**
     * Carrega instruções de um arquivo
     */
    public void carregarInstrucoes(String nomeArquivo) throws IOException {
        instrucoes = InstructionParser.lerInstrucoes(nomeArquivo);
        pc = 0;
        logExecucao.add("Carregadas " + instrucoes.size() + " instruções do arquivo: " + nomeArquivo);
    }
    
    /**
     * Executa um ciclo completo do simulador
     */
    public void proximoCiclo() {
        if (simulacaoCompleta) return;
        
        cicloAtual++;
        boolean progresso = false;
        
        logExecucao.add("=== Ciclo " + cicloAtual + " ===");
        
        // Fase 1: Commit (em ordem)
        progresso |= commit();
        
        // Fase 2: Write Result (através do CDB)
        progresso |= writeResult();
        
        // Fase 3: Execute
        progresso |= execute();
        
        // Fase 4: Issue (emissão)
        progresso |= issue();
        
        // Verifica se a simulação está completa
        if (!progresso && pc >= instrucoes.size() && robVazio()) {
            simulacaoCompleta = true;
            totalCiclos = cicloAtual;
            logExecucao.add("Simulação completa em " + totalCiclos + " ciclos");
        }
        
        // Conta ciclos de bolha
        if (!progresso) {
            ciclosBolha++;
        }
    }
    
    /**
     * Fase de Commit: Retira instruções da cabeça do ROB
     */
    private boolean commit() {
        boolean progresso = false;
        ReorderBufferSlot slot = rob.get(robHead);
        
        if (slot.isBusy() && slot.prontoParaCommit()) {
            Instrucao inst = slot.getInstrucao();
            
            // Atualiza banco publico se a instrução escreve em registrador
            if (inst.escreveRegistrador() && slot.getRegistradorPublico() != null) {
                String regPub = slot.getRegistradorPublico();
                //String regPrivAntigo = mapaRenomeacao.get(regPub);
                String regPriv = slot.getRegistradorRenomeado();
                
                // Atualiza o valor no banco publico
                bancoRegistradores.put(regPub, slot.getResultado());
                filaRegistradoresLivres.offer(regPriv);
                
                
                logExecucao.add("Commit: " + inst + " -> " + regPub + " = " + slot.getResultado());
            } else {
                logExecucao.add("Commit: " + inst);
            }
            
            slot.commit(cicloAtual);
            slot.limpar();
            robHead = (robHead + 1) % TAMANHO_ROB;
            instrucoesExecutadas++;
            progresso = true;
        }
        
        return progresso;
    }
    
    /**
     * Fase de Write Result: Propaga resultados via CDB
     */
    private boolean writeResult() {
        boolean progresso = false;
        
        // Verifica estações de reserva que terminaram a execução
        List<EstacaoDeReserva> todasEstacoes = new ArrayList<>();
        todasEstacoes.addAll(estacoesAdd);
        todasEstacoes.addAll(estacoesMul);
        todasEstacoes.addAll(estacoesLoad);
        
        for (EstacaoDeReserva estacao : todasEstacoes) {
            if (estacao.isBusy() && estacao.getCiclosRestantes() == 0) {
                // Calcula resultado
                Float resultado = estacao.calcularResultado();
                int destROB = estacao.getDest();
                
                // Atualiza o slot do ROB
                ReorderBufferSlot slot = rob.get(destROB);
                if (estacao.getOp().isMemoryOperation()) {
                    // Para LOAD, lê da memória
                    if (estacao.getOp() == OpCode.LOAD) {
                        int endereco = resultado.intValue();
                        resultado = memoria.getOrDefault(endereco, 0.0f);
                    } else { // STORE
                        int endereco = resultado.intValue();
                        Float valor = bancoPrivado.get(slot.getRegistradorRenomeado());
                        memoria.put(endereco, valor);
                        resultado = valor; // Para STORE, o resultado é o valor armazenado
                    }
                }
                
                slot.marcarResultadoPronto(resultado, cicloAtual);
                
                // Propaga resultado via CDB para estações de reserva que estavam esperando
                propagarResultadoCDB(destROB, resultado);
                
                logExecucao.add("Write Result: " + estacao.getNome() + " -> ROB" + destROB + " = " + resultado);
                
                estacao.limpar();
                progresso = true;
            }
        }
        
        return progresso;
    }
    
    /**
     * Propaga resultado via Common Data Bus (CDB)
     */
    private void propagarResultadoCDB(int robIndex, Float valor) {
        // Atualiza estações de reserva que estavam esperando este resultado
        List<EstacaoDeReserva> todasEstacoes = new ArrayList<>();
        todasEstacoes.addAll(estacoesAdd);
        todasEstacoes.addAll(estacoesMul);
        todasEstacoes.addAll(estacoesLoad);
        
        for (EstacaoDeReserva estacao : todasEstacoes) {
            if (estacao.isBusy()) {
                if (estacao.getQj() == robIndex) {
                    estacao.setVj(valor);
                    estacao.setQj(-1);
                }
                if (estacao.getQk() == robIndex) {
                    estacao.setVk(valor);
                    estacao.setQk(-1);
                }
            }
        }
    }
    
    /**
     * Fase de Execução: Inicia execução de operações prontas
     */
    private boolean execute() {
        boolean progresso = false;
        
        List<EstacaoDeReserva> todasEstacoes = new ArrayList<>();
        todasEstacoes.addAll(estacoesAdd);
        todasEstacoes.addAll(estacoesMul);
        todasEstacoes.addAll(estacoesLoad);
        
        for (EstacaoDeReserva estacao : todasEstacoes) {
            if (estacao.isBusy()) {
                if (estacao.getCiclosRestantes() > 0) {
                    // Continua execução
                    boolean terminou = estacao.executarCiclo();
                    if (terminou) {
                        logExecucao.add("Execute: " + estacao.getNome() + " completou execução");
                    }
                    progresso = true;
                } else if (estacao.prontaParaExecucao() && estacao.getCiclosRestantes() == 0) {
                    // Inicia execução
                    estacao.iniciarExecucao();
                    ReorderBufferSlot slot = rob.get(estacao.getDest());
                    slot.setCicloExecucao(cicloAtual);
                    logExecucao.add("Execute: " + estacao.getNome() + " iniciou execução");
                    progresso = true;
                }
            }
        }
        
        return progresso;
    }
    
    /**
     * Fase de Issue: Emite próxima instrução
     */
    private boolean issue() {
        if (pc >= instrucoes.size()) return false;
        
        // Verifica se há slot livre no ROB
        if (rob.get(robTail).isBusy()) {
            return false; // ROB cheio
        }
        
        Instrucao inst = instrucoes.get(pc);
        
        // Encontra estação de reserva livre
        EstacaoDeReserva estacaoLivre = encontrarEstacaoLivre(inst.getOp());
        if (estacaoLivre == null) {
            return false; // Não há estação livre
        }
        
        // Verifica se há registrador privado livre (se necessário)
        if (inst.escreveRegistrador() && filaRegistradoresLivres.isEmpty()) {
            return false; // Não há registrador privado livre
        }
        
        // Aloca slot no ROB
        ReorderBufferSlot robSlot = rob.get(robTail);
        robSlot.setBusy(true);
        robSlot.setInstrucao(inst);
        robSlot.setEstado(EstadoInstrucao.PROCESSANDO);
        robSlot.setCicloIssue(cicloAtual);
        
        // Renomeação de registradores
        String novoRegFisico = null;
        if (inst.escreveRegistrador()) {
            novoRegFisico = filaRegistradoresLivres.poll();
            robSlot.setRegistradorPublico(inst.getRd());
            robSlot.setRegistradorRenomeado(novoRegFisico);
            mapaRenomeacao.put(inst.getRd(), novoRegFisico);
        }
        
        // Configura estação de reserva
        configurarEstacaoReserva(estacaoLivre, inst, robTail);
        
        logExecucao.add("Issue: " + inst + " -> ROB" + robTail + 
                       (novoRegFisico != null ? " (" + inst.getRd() + " -> " + novoRegFisico + ")" : ""));
        
        robTail = (robTail + 1) % TAMANHO_ROB;
        pc++;
        
        return true;
    }
    
    /**
     * Encontra uma estação de reserva livre para a operação
     */
    private EstacaoDeReserva encontrarEstacaoLivre(OpCode op) {
        List<EstacaoDeReserva> estacoes;
        
        if (op.isMemoryOperation()) {
            estacoes = estacoesLoad;
        } else if (op.isMultiplyDivide()) {
            estacoes = estacoesMul;
        } else {
            estacoes = estacoesAdd;
        }
        
        for (EstacaoDeReserva estacao : estacoes) {
            if (!estacao.isBusy()) {
                return estacao;
            }
        }
        
        return null;
    }
    
    /**
     * Configura uma estação de reserva com uma instrução
     */
    private void configurarEstacaoReserva(EstacaoDeReserva estacao, Instrucao inst, int robIndex) {
        estacao.setBusy(true);
        estacao.setOp(inst.getOp());
        estacao.setDest(robIndex);
        estacao.setImediato(inst.getImediato());
        
        // Configura operando fonte 1 (rs)
        if (inst.getRs() != null) {
            String regFisicoRs = mapaRenomeacao.get(inst.getRs());
            Float valorRs = bancoPrivado.get(regFisicoRs);
            
            // Verifica se há dependência
            int robProdutorRs = encontrarProdutorROB(regFisicoRs);
            if (robProdutorRs != -1 && !rob.get(robProdutorRs).isPronto()) {
                estacao.setQj(robProdutorRs);
                estacao.setVj(null);
            } else {
                estacao.setQj(-1);
                estacao.setVj(valorRs);
            }
            
            // Para operações de memória, calcula endereço
            if (inst.getOp().isMemoryOperation()) {
                estacao.setEndereco(valorRs != null ? valorRs.intValue() : 0);
            }
        }
        
        // Configura operando fonte 2 (rt)
        if (inst.getRt() != null && !inst.getOp().hasImmediate()) {
            String regFisicoRt = mapaRenomeacao.get(inst.getRt());
            Float valorRt = bancoPrivado.get(regFisicoRt);
            
            // Verifica se há dependência
            int robProdutorRt = encontrarProdutorROB(regFisicoRt);
            if (robProdutorRt != -1 && !rob.get(robProdutorRt).isPronto()) {
                estacao.setQk(robProdutorRt);
                estacao.setVk(null);
            } else {
                estacao.setQk(-1);
                estacao.setVk(valorRt);
            }
        } else if (inst.getOp().hasImmediate()) {
            estacao.setQk(-1);
            estacao.setVk((float) inst.getImediato());
        }
    }
    
    /**
     * Encontra o slot do ROB que produzirá o valor para um registrador privado
     */
    private int encontrarProdutorROB(String regFisico) {
        // Procura no ROB um slot ocupado que ainda não terminou e que escreve neste registrador
        for (int i = 0; i < TAMANHO_ROB; i++) {
            ReorderBufferSlot slot = rob.get(i);
            if (slot.isBusy() && !slot.isPronto() && 
                regFisico.equals(slot.getRegistradorRenomeado())) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * Verifica se o ROB está vazio
     */
    private boolean robVazio() {
        for (ReorderBufferSlot slot : rob) {
            if (slot.isBusy()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Executa a simulação completa
     */
    public void executarCompleto() {
        while (!simulacaoCompleta) {
            proximoCiclo();
            
            // Proteção contra loop infinito
            if (cicloAtual > 10000) {
                System.err.println("Simulação interrompida: muitos ciclos");
                break;
            }
        }
    }
    
    /**
     * Calcula o IPC (Instructions Per Cycle)
     */
    public double calcularIPC() {
        if (totalCiclos == 0) return 0.0;
        return (double) instrucoesExecutadas / totalCiclos;
    }
    
    /**
     * Reinicia o simulador
     */
    public void reiniciar() {
        inicializarSimulador();
    }
    
    // Métodos para a interface gráfica
    public List<String> getInstructionStatus() {
        List<String> status = new ArrayList<>();
        for (int i = 0; i < instrucoes.size(); i++) {
            Instrucao inst = instrucoes.get(i);
            // Encontra informações da instrução no ROB
            String estado = "Não emitida";
            if (i < pc) {
                estado = "Commitada";
                // Verifica se ainda está no ROB
                for (ReorderBufferSlot slot : rob) {
                    if (slot.isBusy() && slot.getInstrucao() == inst) {
                        estado = slot.getEstado().getDescricao();
                        break;
                    }
                }
            }
            status.add(inst.toString() + " - " + estado);
        }
        return status;
    }
    
    public List<EstacaoDeReserva> getReservationStationsState() {
        List<EstacaoDeReserva> todas = new ArrayList<>();
        todas.addAll(estacoesAdd);
        todas.addAll(estacoesMul);
        todas.addAll(estacoesLoad);
        return todas;
    }
    
    public List<ReorderBufferSlot> getReorderBufferState() {
        return rob;
    }
    
    public Map<String, Object> getRegisterStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("publico", new HashMap<>(bancoRegistradores));
        status.put("fisico", new HashMap<>(bancoPrivado));
        status.put("mapeamento", new HashMap<>(mapaRenomeacao));
        status.put("livres", new ArrayList<>(filaRegistradoresLivres));
        return status;
    }
    
    // Getters para estatísticas
    public int getCicloAtual() { return cicloAtual; }
    public int getTotalCiclos() { return totalCiclos; }
    public int getCiclosBolha() { return ciclosBolha; }
    public boolean isSimulacaoCompleta() { return simulacaoCompleta; }
    public int getInstrucoesExecutadas() { return instrucoesExecutadas; }
    public List<String> getLogExecucao() { return logExecucao; }
    public int getPc() { return pc; }
    public int getTotalInstrucoes() { return instrucoes.size(); }
}
