package simulador;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

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
    private List<List<Instrucao>> instrucoesInstancias;
    private int pc; // Program Counter
    private int cicloAtual;
    private int totalCiclos;
    private int ciclosBolha;
    private boolean simulacaoCompleta;

    // Estatísticas
    private int instrucoesExecutadas;
    private List<String> logExecucao;

    // Adicione este atributo à sua classe Simulador:
    private Stack<EstadoSimulador> historicoCiclos = new Stack<>();

    // Classe para armazenar o estado do simulador
    private static class EstadoSimulador {
        Map<String, Float> bancoRegistradores;
        Map<String, Float> bancoPrivado;
        Map<String, String> mapaRenomeacao;
        Queue<String> filaRegistradoresLivres;
        List<ReorderBufferSlot> rob;
        int robHead;
        int robTail;
        List<EstacaoDeReserva> estacoesAdd;
        List<EstacaoDeReserva> estacoesMul;
        List<EstacaoDeReserva> estacoesLoad;
        List<EstacaoDeReserva> estacoesBranches;
        Map<Integer, Float> memoria;
        List<List<Instrucao>> instrucoesInstancias;
        int pc;
        int cicloAtual;
        int totalCiclos;
        int ciclosBolha;
        boolean simulacaoCompleta;
        int instrucoesExecutadas;
        List<String> logExecucao;

        // Construtor copia profunda
        EstadoSimulador(Simulador sim) {
            this.bancoRegistradores = new HashMap<>(sim.bancoRegistradores);
            this.bancoPrivado = new HashMap<>(sim.bancoPrivado);
            this.mapaRenomeacao = new HashMap<>(sim.mapaRenomeacao);
            this.filaRegistradoresLivres = new LinkedList<>(sim.filaRegistradoresLivres);
            this.rob = new ArrayList<>();
            for (ReorderBufferSlot slot : sim.rob) {
                ReorderBufferSlot novoSlot = new ReorderBufferSlot(slot);
                // deep copy da instrução associada ao slot
                if (slot.getInstrucao() != null) {
                    novoSlot.setInstrucao(new Instrucao(slot.getInstrucao()));
                }
                this.rob.add(novoSlot);
            }
            this.robHead = sim.robHead;
            this.robTail = sim.robTail;
            this.estacoesAdd = new ArrayList<>();
            for (EstacaoDeReserva e : sim.estacoesAdd)
                this.estacoesAdd.add(new EstacaoDeReserva(e));
            this.estacoesMul = new ArrayList<>();
            for (EstacaoDeReserva e : sim.estacoesMul)
                this.estacoesMul.add(new EstacaoDeReserva(e));
            this.estacoesLoad = new ArrayList<>();
            for (EstacaoDeReserva e : sim.estacoesLoad)
                this.estacoesLoad.add(new EstacaoDeReserva(e));
            this.estacoesBranches = new ArrayList<>();
            for (EstacaoDeReserva e : sim.estacoesBranches)
                this.estacoesBranches.add(new EstacaoDeReserva(e));
            this.memoria = new HashMap<>(sim.memoria);
            // Cópia profunda das instâncias de instruções
            this.instrucoesInstancias = new ArrayList<>();
            for (List<Instrucao> lista : sim.instrucoesInstancias) {
                List<Instrucao> novaLista = new ArrayList<>();
                for (Instrucao inst : lista) {
                    novaLista.add(new Instrucao(inst)); // Usa o copy constructor
                }
                this.instrucoesInstancias.add(novaLista);
            }
            this.pc = sim.pc;
            this.cicloAtual = sim.cicloAtual;
            this.totalCiclos = sim.totalCiclos;
            this.ciclosBolha = sim.ciclosBolha;
            this.simulacaoCompleta = sim.simulacaoCompleta;
            this.instrucoesExecutadas = sim.instrucoesExecutadas;
            this.logExecucao = new ArrayList<>(sim.logExecucao);
        }

        // Método para restaurar o estado no simulador
        void restaurar(Simulador sim) {
            sim.bancoRegistradores = new HashMap<>(this.bancoRegistradores);
            sim.bancoPrivado = new HashMap<>(this.bancoPrivado);
            sim.mapaRenomeacao = new HashMap<>(this.mapaRenomeacao);
            sim.filaRegistradoresLivres = new LinkedList<>(this.filaRegistradoresLivres);
            sim.rob = new ArrayList<>();
            for (ReorderBufferSlot slot : this.rob) {
                ReorderBufferSlot novoSlot = new ReorderBufferSlot(slot);
                if (slot.getInstrucao() != null) {
                    novoSlot.setInstrucao(new Instrucao(slot.getInstrucao()));
                }
                sim.rob.add(novoSlot);
            }
            sim.robHead = this.robHead;
            sim.robTail = this.robTail;
            sim.estacoesAdd = new ArrayList<>();
            for (EstacaoDeReserva e : this.estacoesAdd)
                sim.estacoesAdd.add(new EstacaoDeReserva(e));
            sim.estacoesMul = new ArrayList<>();
            for (EstacaoDeReserva e : this.estacoesMul)
                sim.estacoesMul.add(new EstacaoDeReserva(e));
            sim.estacoesLoad = new ArrayList<>();
            for (EstacaoDeReserva e : this.estacoesLoad)
                sim.estacoesLoad.add(new EstacaoDeReserva(e));
            sim.estacoesBranches = new ArrayList<>();
            for (EstacaoDeReserva e : this.estacoesBranches)
                sim.estacoesBranches.add(new EstacaoDeReserva(e));
            sim.memoria = new HashMap<>(this.memoria);
            // Cópia profunda das instâncias de instruções
            sim.instrucoesInstancias = new ArrayList<>();
            for (List<Instrucao> lista : this.instrucoesInstancias) {
                List<Instrucao> novaLista = new ArrayList<>();
                for (Instrucao inst : lista) {
                    novaLista.add(new Instrucao(inst)); // Usa o copy constructor
                }
                sim.instrucoesInstancias.add(novaLista);
            }
            sim.pc = this.pc;
            sim.cicloAtual = this.cicloAtual;
            sim.totalCiclos = this.totalCiclos;
            sim.ciclosBolha = this.ciclosBolha;
            sim.simulacaoCompleta = this.simulacaoCompleta;
            sim.instrucoesExecutadas = this.instrucoesExecutadas;
            sim.logExecucao = new ArrayList<>(this.logExecucao);
        }
    }

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
        instrucoesInstancias = new ArrayList<>();
        pc = 0;
        cicloAtual = 0;
        totalCiclos = 0;
        ciclosBolha = 0;
        simulacaoCompleta = false;
        instrucoesExecutadas = 0;
        logExecucao = new ArrayList<>();
        // Limpa instruções do ROB e das estações de reserva
        for (ReorderBufferSlot slot : rob) {
            slot.limpar();
        }
        for (EstacaoDeReserva e : estacoesAdd)
            e.limpar();
        for (EstacaoDeReserva e : estacoesMul)
            e.limpar();
        for (EstacaoDeReserva e : estacoesLoad)
            e.limpar();
        for (EstacaoDeReserva e : estacoesBranches)
            e.limpar();
    }

    public void reiniciar() {

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
        // Limpa instruções do ROB e das estações de reserva
        for (ReorderBufferSlot slot : rob) {
            slot.limpar();
        }
        for (EstacaoDeReserva e : estacoesAdd)
            e.limpar();
        for (EstacaoDeReserva e : estacoesMul)
            e.limpar();
        for (EstacaoDeReserva e : estacoesLoad)
            e.limpar();
        for (EstacaoDeReserva e : estacoesBranches)
            e.limpar();
    }

    public void proximoCiclo() {
        if (!simulacaoCompleta) {
            historicoCiclos.push(new EstadoSimulador(this)); // Salva o estado atual

            writeResult();

            execute();

            issue();

            commit();

            if (todasInstrucoesFinalizadas() && robVazio()) {
                simulacaoCompleta = true;
                logExecucao.add("Simulação completa. Total de ciclos gastos: " + totalCiclos);
                totalCiclos = cicloAtual - 1;
            }
            cicloAtual++;
        }
    }

    void writeResult() {
        List<EstacaoDeReserva> todasEstacoes = new ArrayList<>();
        todasEstacoes.addAll(estacoesAdd);
        todasEstacoes.addAll(estacoesMul);
        todasEstacoes.addAll(estacoesLoad);
        todasEstacoes.addAll(estacoesBranches);
        for (EstacaoDeReserva estacao : todasEstacoes) {
            if (estacao.isBusy() && estacao.getCiclosRestantes() == 0) {
                String regPrivado = estacao.getDest();
                ReorderBufferSlot slot = encontrarSlotROB(regPrivado);
                if (slot.getCicloEscrita() != cicloAtual) {
                    Float resultado = estacao.calcularResultado();
                    slot.setCicloEscrita(cicloAtual);
                    slot.setCicloCommit(cicloAtual);
                    slot.setEstado(EstadoInstrucao.ESCRITA);
                    slot.setPronto(true);
                    // Atualiza estado da instrução
                    if (slot.getInstrucao() != null)
                        slot.getInstrucao().setEstado(3); // Write result
                    if (estacao.getOp().isMemoryOperation()) {
                        if (estacao.getOp() == OpCode.LOAD) {
                            int endereco = resultado.intValue();
                            resultado = memoria.getOrDefault(endereco, 0.0f);
                            propagarResultadoCDB(regPrivado, resultado);
                        } else {
                            int endereco = resultado.intValue();
                            Float valor = bancoPrivado.get(regPrivado);
                            memoria.put(endereco, valor);
                            resultado = valor;
                        }
                    } else if (estacao.getOp().isBranch()) {
                        if (resultado == 1) {
                            int branchIndex = pc - 1;
                            for (int i = branchIndex + 1; i < instrucoesInstancias.size(); i++) {
                                Instrucao atual = getInstrucaoAtual(i);
                                if (atual.getEstado() > 0) {
                                    Instrucao nova = new Instrucao(atual);
                                    nova.setEstado(0);
                                    nova.setInstanciaId(atual.getInstanciaId() + 1);
                                    instrucoesInstancias.get(i).add(nova);
                                }
                                // Marque todas as instâncias antigas como puladas
                                List<Instrucao> lista = instrucoesInstancias.get(i);
                                for (int j = 0; j < lista.size() - 1; j++) {
                                    lista.get(j).setEstado(-1);
                                }
                            }
                        }
                    } else {
                        propagarResultadoCDB(regPrivado, resultado);
                    }
                    slot.marcarResultadoPronto(resultado, cicloAtual);
                    logExecucao.add("Write Result: " + estacao.getNome() + " -> ROB" + regPrivado + " = " + resultado);
                    estacao.limpar();
                }
            }
        }
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
                            // Atualiza estado da instrução
                            if (slot.getInstrucao() != null)
                                slot.getInstrucao().setEstado(2); // Executada
                        }
                    } else {
                        estacao.limpar();
                    }
                }
                if (!pronta)
                    ciclosBolha++;
            }
        }
    }

    private void issue() {
        if (pc < instrucoesInstancias.size()) {
            Instrucao inst = getInstrucaoAtual(pc);
            if (!rob.get(robTail).isBusy()) {
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
                        inst.setEstado(1); // Issue
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
                        inst.setEstado(1); // Issue
                        robTail = (robTail + 1) % TAMANHO_ROB;
                    }
                    pc++;
                } else {
                    logExecucao.add("Nenhuma estação de reserva disponível, não foi possível emitir a instrução: " + inst.toString());
                    ciclosBolha++;
                }
            } else {
                logExecucao.add("ROB cheio, não foi possível emitir a instrução: " + inst.toString());
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
        System.out.println("Verificando dependência VDD para: " + reg1 + ", " + reg2);
        // Verifica se a instrução depende de outra que ainda não foi completada
        ReorderBufferSlot conflito1 = null, conflito2 = null;
        for (int i = robHead; i != robTail; i = (i + 1) % TAMANHO_ROB) {
            if (rob.get(i).isBusy()) {
                String regPublico = rob.get(i).getRegistradorPublico();
                if (reg1 != null && regPublico != null && regPublico.equals(reg1)) {
                    if (conflito1 != null) {
                        if (conflito1.getCicloIssue() < rob.get(i).getCicloIssue()) {
                            conflito1 = rob.get(i);
                        }
                    } else {
                        conflito1 = rob.get(i);
                    }
                } else if (reg2 != null && regPublico != null && regPublico.equals(reg2)) {
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
        } else if (reg1 != null) {
            estacao.setVj(bancoRegistradores.get(reg1));
        }
        if (conflito2 != null) {
            logExecucao.add("Conflito VDD encontrado: " + reg2 + " em " + conflito2.getInstrucao().toString());
            if (conflito2.isPronto()) {
                estacao.setVk(bancoPrivado.get(conflito2.getRegistradorRenomeado()));
            } else {
                estacao.setQk(conflito2.getRegistradorRenomeado());
            }
        } else if (reg2 != null) {
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
     * Encontra um registrador com base no seu nome e se é privado ou não.
     */
    /*
     * private String EncontraRegistrador(String nome, boolean privado) {
     * String reg = null;
     * if (privado) {
     * for (String r : bancoPrivado) {
     * if (r.getNome().equals(nome)) {
     * reg = r;
     * break;
     * }
     * }
     * } else {
     * for (Registrador r : bancoRegistradores) {
     * if (r.getNome().equals(nome)) {
     * reg = r;
     * break;
     * }
     * }
     * }
     * return reg;
     * }
     */

    /**
     * Fase de Commit: Retira instruções da cabeça do ROB
     */
    private void commit() {
        ReorderBufferSlot slot = rob.get(robHead);
        if (slot.isBusy() && slot.isPronto() && slot.getCicloCommit() != cicloAtual) {
            Instrucao inst = slot.getInstrucao();
            slot.setCicloCommit(cicloAtual);
            if (inst.podeEscrever() && slot.getRegistradorPublico() != null) {
                String regPub = slot.getRegistradorPublico();
                String regPriv = slot.getRegistradorRenomeado();
                bancoRegistradores.put(regPub, slot.getResultado());
                filaRegistradoresLivres.offer(regPriv);
                mapaRenomeacao.remove(regPub);
            }
            slot.limpar();
            robHead = (robHead + 1) % TAMANHO_ROB;
            instrucoesExecutadas++;
            // Atualiza estado da instrução
            if (inst != null)
                inst.setEstado(4); // Commit
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
        List<Instrucao> lidas = InstructionParser.lerInstrucoes(nomeArquivo);
        instrucoesInstancias = new ArrayList<>();
        for (Instrucao inst : lidas) {
            List<Instrucao> lista = new ArrayList<>();
            lista.add(new Instrucao(inst)); // deep copy
            instrucoesInstancias.add(lista);
        }
        pc = 0;
        logExecucao.add("Carregadas " + instrucoesInstancias.size() + " instruções do arquivo: " + nomeArquivo);
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

    public List<List<Instrucao>> getInstrucoesInstancias() {
        return instrucoesInstancias;
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

    public List<String> getLogExecucao() {
        return logExecucao;
    }

    public int getPc() {
        return pc;
    }

    public int getTotalInstrucoes() {
        return instrucoesInstancias.size();
    }

    // Novo método para acessar a instância atual de uma instrução
    private Instrucao getInstrucaoAtual(int idx) {
        List<Instrucao> lista = instrucoesInstancias.get(idx);
        return lista.get(lista.size() - 1);
    }

    // Novo método para verificar se todas as instruções estão finalizadas
    private boolean todasInstrucoesFinalizadas() {
        for (List<Instrucao> instancias : instrucoesInstancias) {
            Instrucao atual = instancias.get(instancias.size() - 1);
            if (atual.getEstado() != 4 && atual.getEstado() != -1) {
                return false;
            }
        }
        return true;
    }

    // Função para voltar um ciclo:
    public void cicloAnterior() {
        if (!historicoCiclos.isEmpty()) {
            EstadoSimulador estadoAnterior = historicoCiclos.pop();
            estadoAnterior.restaurar(this);
        }
    }
}
