package simulador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Simulador {
    // Configurações do simulador
    private static final int TAMANHO_ROB = 8;
    private static final int NUM_ESTACOES_ADD = 3;
    private static final int NUM_ESTACOES_MUL = 3;
    private static final int NUM_ESTACOES_LOAD = 3;
    private static final int NUM_ESTACOES_BRANCHES = 3;
    private static final int NUM_REGISTRADORES_PRIVADOS = 32;
    private static final int NUM_REGISTRADORES_PUBLICOS = 16;

    // Estruturas de dados principais
    private Map<String, Float> bancoRegistradores; // R1 -> valor
    private Map<String, Float> bancoPrivado; // P1 -> valor
    private Map<String, String> mapaRenomeacao; // R1 -> P5
    private Queue<String> filaRegistradoresLivres; // Registradores privados livres

    private List<ReorderBufferSlot> rob; // Buffer de Reordenamento
    private int robHead; // Cabeça do ROB (próximo a fazer commit)
    private int robTail; // Cauda do ROB (próximo slot livre)

    private List<EstacaoDeReserva> estacoesAdd; // Estações para ADD/SUB
    private List<EstacaoDeReserva> estacoesMul; // Estações para MUL/DIV
    private List<EstacaoDeReserva> estacoesLoad; // Estações para LOAD/STORE
    private List<EstacaoDeReserva> estacoesBranches; // Estações para branches

    private Map<Integer, Float> memoria;

    // Estado do simulador
    private List<Instrucao> instrucoes;
    private int pc; // Program Counter
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

    /*
     * Método de inicialização do simulador
     * Configura os registradores, buffer de reordenamento, estações de reserva e
     * memória.
     */
    private void inicializarSimulador() {
        // Inicializando Banco de Registradores
        bancoRegistradores = new HashMap<>();
        for (int i = 0; i <= NUM_REGISTRADORES_PUBLICOS; i++) {
            bancoRegistradores.put("R" + i, (float) i);
        }

        // Inicializando Banco de Registradores privados
        bancoPrivado = new HashMap<>();
        filaRegistradoresLivres = new LinkedList<>();
        for (int i = 0; i <= NUM_REGISTRADORES_PRIVADOS; i++) {
            bancoPrivado.put("P" + i, (float) i);
            filaRegistradoresLivres.offer("P" + i);
        }

        mapaRenomeacao = new HashMap<>();

        // Inicializando Buffer de Reordenamento
        rob = new ArrayList<>();
        for (int i = 0; i < TAMANHO_ROB; i++) {
            rob.add(new ReorderBufferSlot(i));
        }
        robHead = 0;
        robTail = 0;

        // Inicializando Estações de Reserva
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

        estacoesBranches = new ArrayList<>();
        for (int i = 0; i < NUM_ESTACOES_BRANCHES; i++) {
            estacoesBranches.add(new EstacaoDeReserva("Branch" + (i + 1)));
        }

        // Iniciando memória
        memoria = new HashMap<>();
        for (int i = 0; i < 1024; i++) {
            memoria.put(i, (float) i); // Inicializando memória com zeros
        }

        // Inicializando estado do simulador
        instrucoes = new ArrayList<>();
        pc = 0;
        cicloAtual = 0;
        totalCiclos = 0;
        ciclosBolha = 0;
        simulacaoCompleta = false;
        instrucoesExecutadas = 0;
        logExecucao = new ArrayList<>();
    }

    public void reiniciar() {

        for (int i = 0; i < instrucoes.size(); i++) {
            instrucoes.get(i).setEstadoExecucao(0);
            instrucoes.get(i).resetExecucoes();
        }

        // Inicializando Banco de Registradores
        bancoRegistradores = new HashMap<>();
        for (int i = 0; i <= NUM_REGISTRADORES_PUBLICOS; i++) {
            bancoRegistradores.put("R" + i, (float) i);
        }

        // Inicializando Banco de Registradores privados
        bancoPrivado = new HashMap<>();
        filaRegistradoresLivres = new LinkedList<>();
        for (int i = 0; i <= NUM_REGISTRADORES_PRIVADOS; i++) {
            bancoPrivado.put("P" + i, (float) i);
            filaRegistradoresLivres.offer("P" + i);
        }

        mapaRenomeacao = new HashMap<>();

        // Inicializando Buffer de Reordenamento
        rob = new ArrayList<>(TAMANHO_ROB);
        for (int i = 0; i < TAMANHO_ROB; i++) {
            rob.add(new ReorderBufferSlot(i));
        }
        robHead = 0;
        robTail = 0;

        // Inicializando Estações de Reserva
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

        estacoesBranches = new ArrayList<>();
        for (int i = 0; i < NUM_ESTACOES_BRANCHES; i++) {
            estacoesBranches.add(new EstacaoDeReserva("Branch" + (i + 1)));
        }

        // Iniciando memória
        memoria = new HashMap<>();
        for (int i = 0; i < 1024; i++) {
            memoria.put(i, (float) i); // Inicializando memória com zeros
        }

        // Inicializando estado do simulador
        pc = 0;
        cicloAtual = 0;
        totalCiclos = 0;
        ciclosBolha = 0;
        simulacaoCompleta = false;
        instrucoesExecutadas = 0;
        logExecucao = new ArrayList<>();
    }

    public void proximoCiclo() {
        //System.out.println("Executando ciclo: " + cicloAtual);
        if (!simulacaoCompleta) {

            writeResult();

            execute();

            issue();

            commit();

            if (pc == instrucoes.size() && robVazio()) {
                simulacaoCompleta = true;
                logExecucao.add("Simulação completa. Total de ciclos gastos: " + totalCiclos);
                totalCiclos = cicloAtual - 1;
            }
            cicloAtual++;
            confereSituacaoROB();
        }
    }

    public void confereSituacaoROB() {
        for (int i = robHead; i != robTail; i = (i + 1) % TAMANHO_ROB) {
            ReorderBufferSlot slot = rob.get(i);
            //System.out.println("Slot " + i + ": " + slot.getInstrucao() + ", Busy: " + slot.isBusy());
        }
    }

    void writeResult() {
        // Verifica estações de reserva que terminaram a execução
        List<EstacaoDeReserva> todasEstacoes = new ArrayList<>();
        todasEstacoes.addAll(estacoesAdd);
        todasEstacoes.addAll(estacoesMul);
        todasEstacoes.addAll(estacoesLoad);
        todasEstacoes.addAll(estacoesBranches);

        for (EstacaoDeReserva estacao : todasEstacoes) {
            // System.out.println(
            // "Ciclos restantes para a estação " + estacao.getNome() + ": " +
            // estacao.getCiclosRestantes());
            if (estacao.isBusy() && estacao.getCiclosRestantes() == 0) {
                String regPrivado = estacao.getDest();

                // Atualiza o slot do ROB
                ReorderBufferSlot slot = encontrarSlotROB(regPrivado);
                if (slot.getCicloEscrita() != cicloAtual) {
                    Float resultado = estacao.calcularResultado();
                    slot.setCicloEscrita(cicloAtual);
                    slot.setCicloCommit(cicloAtual);
                    slot.setEstado(EstadoInstrucao.ESCRITA);
                    slot.setPronto(true);
                    Instrucao inst = slot.getInstrucao();
                    if (inst != null) inst.setEstadoExecucao(3); // resultado escrito
                    if (estacao.getOp().isMemoryOperation()) {
                        // Para LOAD, lê da memória
                        if (estacao.getOp() == OpCode.LOAD) {
                            int endereco = resultado.intValue();
                            resultado = memoria.getOrDefault(endereco, 0.0f);

                            // Propaga resultado via CDB para estações de reserva que estavam esperando
                            propagarResultadoCDB(regPrivado, resultado);
                        } else { // STORE
                            int endereco = resultado.intValue();
                            Float valor = bancoPrivado.get(regPrivado);
                            memoria.put(endereco, valor);
                            resultado = valor; // Para STORE, o resultado é o valor armazenado
                        }
                    } else if (estacao.getOp().isBranch()) {
                        //System.out.println("Branch detected");
                        if (resultado == 1) {
                            executarBEQ(slot);
                        }
                    } else {
                        // Propaga resultado via CDB para estações de reserva que estavam esperando
                        propagarResultadoCDB(regPrivado, resultado);
                    }

                    slot.marcarResultadoPronto(resultado, cicloAtual);
                    logExecucao.add("Write Result: " + estacao.getNome() + " -> ROB" + regPrivado + " = " + resultado);
                    estacao.limpar();
                }

            }
        }
    }

    private void executarBEQ(ReorderBufferSlot slot) {
        for (int i = robHead; i != robTail; i = (i + 1) % TAMANHO_ROB) {
            if (!rob.get(i).equals(slot)) {
                if (rob.get(i).isBusy()) {
                    if (rob.get(i).getCicloIssue() != -1 && rob.get(i).getCicloIssue() >= slot.getCicloIssue()) {
                        Instrucao inst = rob.get(i).getInstrucao();
                        if (inst != null) {
                            logExecucao.add("BEQ executado, instrução cancelada: " + inst.toString());
                        }
                        rob.get(i).limpar();
                    }
                }
            }
        }
        pc = slot.getInstrucao().getImediato() - 1; // Atualiza o PC para o endereço do branch
    }

    /*
     * Função que encontra um slot do ROB baseado no registrador renomeado.
     * Se o registrador renomeado for encontrado, retorna o slot correspondente.
     * Caso contrário, retorna null.
     */
    ReorderBufferSlot encontrarSlotROB(String regPrivado) {
        ReorderBufferSlot slotEncontrado = null;
        for (ReorderBufferSlot slot : rob) {
            if (slot.isBusy() && slot.getRegistradorRenomeado().equals(regPrivado)) {
                slotEncontrado = slot;
            }
        }
        return slotEncontrado;
    }

    /**
     * Propaga resultado via Common Data Bus (CDB)
     */
    private void propagarResultadoCDB(String regPrivado, Float valor) {
        // Atualiza estações de reserva que estavam esperando este resultado
        List<EstacaoDeReserva> todasEstacoes = new ArrayList<>();
        todasEstacoes.addAll(estacoesAdd);
        todasEstacoes.addAll(estacoesMul);
        todasEstacoes.addAll(estacoesLoad);
        todasEstacoes.addAll(estacoesBranches);

        for (EstacaoDeReserva estacao : todasEstacoes) {
            if (estacao.isBusy()) {
                if (estacao.getQj() != null && estacao.getQj().equals(regPrivado)) {
                    estacao.setVj(valor);
                    estacao.setQj(null);
                }
                if (estacao.getQk() != null && estacao.getQk().equals(regPrivado)) {
                    estacao.setVk(valor);
                    estacao.setQk(null);
                }
            }
        }
    }

    /**
     * Fase de Execução: Inicia execução de operações prontas
     */
    private void execute() {

        List<EstacaoDeReserva> todasEstacoes = new ArrayList<>();
        todasEstacoes.addAll(estacoesAdd);
        todasEstacoes.addAll(estacoesMul);
        todasEstacoes.addAll(estacoesLoad);
        todasEstacoes.addAll(estacoesBranches);

        for (EstacaoDeReserva estacao : todasEstacoes) {
            if (estacao.isBusy()) {
                boolean pronta = estacao.prontaParaExecucao();
                if (estacao.getCiclosRestantes() > 0 && pronta) {
                    ReorderBufferSlot slot = encontrarSlotROB(estacao.getDest());
                    if (slot != null) {
                        slot.setEstado(EstadoInstrucao.EXECUTANDO);
                        if (slot.getCicloExecucao() == -1)
                            slot.setCicloExecucao(cicloAtual);
                        boolean terminou = estacao.executarCiclo();
                        if (terminou) {
                            slot.setCicloEscrita(cicloAtual);
                            logExecucao.add("Execute: " + estacao.getNome() + " completou execução");
                            Instrucao inst = slot.getInstrucao();
                            if (inst != null) inst.setEstadoExecucao(2); // executada
                        }
                    }
                }
                if (!pronta)
                    ciclosBolha++;
            }
        }

    }

    private void issue() {
        if (pc < instrucoes.size()) {
            if (!rob.get(robTail).isBusy()) {
                Instrucao inst = instrucoes.get(pc);
                EstacaoDeReserva estacao = encontrarEstacaoLivre(inst.getOp());
                if (estacao != null) {
                    if (inst.podeEscrever() && !filaRegistradoresLivres.isEmpty()) {
                        ReorderBufferSlot slot = rob.get(robTail);
                        slot.setBusy(true);
                        slot.setPronto(false);
                        slot.setInstrucao(inst);
                        slot.setEstado(EstadoInstrucao.PROCESSANDO);
                        slot.setCicloIssue(cicloAtual);
                        String reg1 = inst.getReg1();
                        String reg2 = inst.getReg2();
                        String regPublico = inst.getRd();
                        verificaDependenciaVDD(reg1, reg2, estacao);
                        String regPrivado = filaRegistradoresLivres.poll();
                        bancoPrivado.put(regPrivado, bancoRegistradores.get(regPublico));
                        slot.setRegistradorRenomeado(regPrivado);
                        slot.setRegistradorPublico(regPublico);
                        estacao.setDest(regPrivado);
                        mapaRenomeacao.put(inst.getRd(), regPrivado);
                        int imediato = inst.getImediato();
                        if (imediato != 0) {
                            estacao.setImediato(imediato);
                        }
                        estacao.setBusy(true);
                        estacao.setOp(inst.getOp());
                        estacao.setCiclosRestantes(inst.getCiclosDuracao());
                        robTail = (robTail + 1) % TAMANHO_ROB;
                    } else {
                        ReorderBufferSlot slot = rob.get(robTail);
                        slot.setBusy(true);
                        slot.setInstrucao(inst);
                        slot.setPronto(false);
                        slot.setEstado(EstadoInstrucao.PROCESSANDO);
                        slot.setCicloIssue(cicloAtual);
                        String reg1 = inst.getReg1();
                        String reg2 = inst.getReg2();
                        verificaDependenciaVDD(reg1, reg2, estacao);
                        int imediato = inst.getImediato();
                        if (imediato != 0) {
                            estacao.setImediato(imediato);
                        }
                        String regPrivado = filaRegistradoresLivres.poll();
                        estacao.setDest(regPrivado);
                        slot.setRegistradorRenomeado(regPrivado);
                        estacao.setBusy(true);
                        estacao.setOp(inst.getOp());
                        estacao.setCiclosRestantes(inst.getCiclosDuracao());
                        robTail = (robTail + 1) % TAMANHO_ROB;
                    }
                    pc++;
                    if (inst.getEstadoExecucao() > 0) {
                        inst.addExecucao();                        
                    }
                    inst.setEstadoExecucao(1); // lida
                } else {
                    logExecucao.add("Nenhuma estação de reserva disponível, não foi possível emitir a instrução: "
                            + inst.toString());
                    ciclosBolha++;
                }
            } else {
                logExecucao.add("ROB cheio, não foi possível emitir a instrução: " + instrucoes.get(pc).toString());
                ciclosBolha++;
            }
        }
    }

    /*
     * @brief Essa função verifica se há dependências de dados entre a instrução
     * atual e alguma instrução ROB
     * e devolve a posição no ROB em que há esse conflito.
     */
    private void verificaDependenciaVDD(String reg1, String reg2, EstacaoDeReserva estacao) {
        // System.out.println("Verificando dependência VDD para: " + reg1 + ", " +
        // reg2);
        // Verifica se a instrução depende de outra que ainda não foi completada
        ReorderBufferSlot conflito1 = null, conflito2 = null;
        for (int i = robHead; i != robTail; i = (i + 1) % TAMANHO_ROB) {
            if (rob.get(i).isBusy()) {
                String regPublico = rob.get(i).getRegistradorPublico();
                if (regPublico != null && regPublico.equals(reg1)) {
                    if (conflito1 != null) {
                        if (conflito1.getCicloIssue() < rob.get(i).getCicloIssue()) {
                            conflito1 = rob.get(i);
                        }
                    } else {
                        conflito1 = rob.get(i);
                    }
                } else if (regPublico != null && regPublico.equals(reg2)) {
                    if (conflito2 != null) {
                        if (conflito2.getCicloIssue() < rob.get(i).getCicloIssue()) {
                            conflito2 = rob.get(i);
                        }
                    } else {
                        conflito2 = rob.get(i);
                    }
                }
            }
        }
        if (conflito1 != null) {
            logExecucao.add("Conflito VDD encontrado: " + reg1 + " em " + conflito1.getInstrucao().toString());
            if (conflito1.isPronto()) {
                estacao.setVj(bancoPrivado.get(conflito1.getRegistradorRenomeado()));
            } else {
                estacao.setQj(conflito1.getRegistradorRenomeado());
            }
        } else {
            estacao.setVj(bancoRegistradores.get(reg1));
        }
        if (conflito2 != null) {
            logExecucao.add("Conflito VDD encontrado: " + reg2 + " em " + conflito2.getInstrucao().toString());
            if (conflito2.isPronto()) {
                estacao.setVk(bancoPrivado.get(conflito2.getRegistradorRenomeado()));
            } else {
                estacao.setQk(conflito2.getRegistradorRenomeado());
            }
        } else {
            estacao.setVk(bancoRegistradores.get(reg2));
        }
    }

    /**
     * Encontra uma estação de reserva livre para a operação
     */
    private EstacaoDeReserva encontrarEstacaoLivre(OpCode op) {
        List<EstacaoDeReserva> estacoes;

        // System.out.println("Encontrando estação livre para a operação: " + op);

        if (op.isMemoryOperation()) {
            estacoes = estacoesLoad;
        } else if (op.isMultiplyDivide()) {
            estacoes = estacoesMul;
        } else if (op.isBranch()) {
            estacoes = estacoesBranches;
        } else {
            estacoes = estacoesAdd;
        }

        for (EstacaoDeReserva estacao : estacoes) {
            // System.out.println("Verificando estação: " + estacao.getNome() + " - Busy: "
            // + estacao.isBusy());
            if (!estacao.isBusy()) {
                return estacao;
            }
        }

        return null;
    }
    

    /**
     * Fase de Commit: Retira instruções da cabeça do ROB
     */
    private void commit() {

        ReorderBufferSlot slot = rob.get(robHead);

        if (slot.isBusy() && slot.isPronto() && slot.getCicloCommit() != cicloAtual) {
            Instrucao inst = slot.getInstrucao();
            slot.setCicloCommit(cicloAtual);
            if (inst != null) inst.setEstadoExecucao(4); // commitada
            // Atualiza banco publico se a instrução escreve em registrador
            if (inst.podeEscrever() && slot.getRegistradorPublico() != null) {
                String regPub = slot.getRegistradorPublico();
                // String regPrivAntigo = mapaRenomeacao.get(regPub);
                String regPriv = slot.getRegistradorRenomeado();

                // Atualiza o valor no banco publico
                bancoRegistradores.put(regPub, slot.getResultado());
                filaRegistradoresLivres.offer(regPriv);
                mapaRenomeacao.remove(regPub);
                logExecucao.add("Commit: " + inst + " -> " + regPub + " = " + slot.getResultado());
            } else {
                logExecucao.add("Commit: " + inst);
            }

            slot.limpar();
            robHead = (robHead + 1) % TAMANHO_ROB;
            instrucoesExecutadas++;
        }

    }

    /**
     * Verifica se o ROB está vazio
     */
    private boolean robVazio() {
        boolean resultado = true;
        for (ReorderBufferSlot slot : rob) {
            if (slot.isBusy()) {
                resultado = false;
            }
        }
        return resultado;
    }

    // Funções Requisitadas pela GUI

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
     * Carrega instruções de um arquivo
     */
    public void carregarInstrucoes(String nomeArquivo) throws IOException {
        reiniciar();
        instrucoes = InstructionParser.lerInstrucoes(nomeArquivo);
        pc = 0;
        logExecucao.add("Carregadas " + instrucoes.size() + " instruções do arquivo: " + nomeArquivo);
    }

    /**
     * Calcula o IPC (Instructions Per Cycle)
     */
    public double calcularIPC() {
        if (totalCiclos == 0)
            return 0.0;
        return (double) instrucoesExecutadas / totalCiclos;
    }

    /*
     * Retorna o Buffer de Reordenamento
     */
    public List<ReorderBufferSlot> getReorderBufferState() {
        return rob;
    }

    /*
     * Retorna as Estações de Reserva
     */
    public List<EstacaoDeReserva> getReservationStationsState() {
        List<EstacaoDeReserva> todas = new ArrayList<>();
        todas.addAll(estacoesAdd);
        todas.addAll(estacoesMul);
        todas.addAll(estacoesLoad);
        return todas;
    }

    /*
     * Retorna os estatus dos registradores
     */
    public Map<String, Object> getRegisterStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("publico", new HashMap<>(bancoRegistradores));
        status.put("fisico", new HashMap<>(bancoPrivado));
        status.put("mapeamento", new HashMap<>(mapaRenomeacao));
        status.put("livres", new ArrayList<>(filaRegistradoresLivres));
        return status;
    }

    // Getters para estatísticas
    public int getCicloAtual() {
        return cicloAtual;
    }

    public int getTotalCiclos() {
        return totalCiclos;
    }

    public int getCiclosBolha() {
        return ciclosBolha;
    }

    public boolean isSimulacaoCompleta() {
        return simulacaoCompleta;
    }

    public int getInstrucoesExecutadas() {
        return instrucoesExecutadas;
    }

    public List<Instrucao> getInstrucoes() {
        return instrucoes;
    }

    public List<String> getLogExecucao() {
        return logExecucao;
    }

    public int getPc() {
        return pc;
    }

    public int getTotalInstrucoes() {
        return instrucoes.size();
    }

}
