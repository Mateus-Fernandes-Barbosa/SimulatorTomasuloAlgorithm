package gui;

import simulador.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Interface gráfica principal do simulador Tomasulo
 */
public class SimuladorMain extends JFrame {
    private Simulador simulador;
    
    // Componentes da interface
    private JTable tabelaInstrucoes;
    private JTable tabelaEstacoes;
    private JTable tabelaROB;
    private JTable tabelaRegistradores;
    private JTextArea areaLog;
    private JLabel labelCiclo;
    private JLabel labelIPC;
    private JLabel labelInstrucoes;
    private JLabel labelCiclosBolha;
    private JButton btnCarregar;
    private JButton btnProximoCiclo;
    private JButton btnExecutarCompleto;
    private JButton btnReiniciar;
    private JProgressBar progressBar;
    
    // Modelos das tabelas
    private DefaultTableModel modeloInstrucoes;
    private DefaultTableModel modeloEstacoes;
    private DefaultTableModel modeloROB;
    private DefaultTableModel modeloRegistradores;
    
    public SimuladorMain() {
        simulador = new Simulador();
        inicializarInterface();
        atualizarInterface();
    }
    
    private void inicializarInterface() {
        setTitle("Simulador Tomasulo - Arquitetura de Computadores");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        // Painel superior com controles
        JPanel painelControles = criarPainelControles();
        add(painelControles, BorderLayout.NORTH);
        
        // Painel central com tabelas
        JPanel painelCentral = criarPainelCentral();
        add(painelCentral, BorderLayout.CENTER);
        
        // Painel inferior com log
        JPanel painelLog = criarPainelLog();
        add(painelLog, BorderLayout.SOUTH);
        
        // Configurações da janela
        setSize(1400, 900);
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
    
    private JPanel criarPainelControles() {
        JPanel painel = new JPanel(new FlowLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Controles"));
        
        btnCarregar = new JButton("Carregar Arquivo");
        btnCarregar.addActionListener(e -> carregarArquivo());
        
        btnProximoCiclo = new JButton("Próximo Ciclo");
        btnProximoCiclo.addActionListener(e -> proximoCiclo());
        btnProximoCiclo.setEnabled(false);
        
        btnExecutarCompleto = new JButton("Executar Completo");
        btnExecutarCompleto.addActionListener(e -> executarCompleto());
        btnExecutarCompleto.setEnabled(false);
        
        btnReiniciar = new JButton("Reiniciar");
        btnReiniciar.addActionListener(e -> reiniciar());
        btnReiniciar.setEnabled(false);
        
        // Labels de estatísticas
        labelCiclo = new JLabel("Ciclo: 0");
        labelIPC = new JLabel("IPC: 0.00");
        labelInstrucoes = new JLabel("Instruções: 0/0");
        labelCiclosBolha = new JLabel("Ciclos Bolha: 0");
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("0%");
        
        painel.add(btnCarregar);
        painel.add(new JSeparator(SwingConstants.VERTICAL));
        painel.add(btnProximoCiclo);
        painel.add(btnExecutarCompleto);
        painel.add(btnReiniciar);
        painel.add(new JSeparator(SwingConstants.VERTICAL));
        painel.add(labelCiclo);
        painel.add(labelIPC);
        painel.add(labelInstrucoes);
        painel.add(labelCiclosBolha);
        painel.add(progressBar);
        
        return painel;
    }
    
    private JPanel criarPainelCentral() {
        JPanel painel = new JPanel(new GridLayout(2, 2, 5, 5));
        painel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Tabela de Status das Instruções
        painel.add(criarPainelInstrucoes());
        
        // Tabela de Estações de Reserva
        painel.add(criarPainelEstacoes());
        
        // Tabela do ROB
        painel.add(criarPainelROB());
        
        // Tabela de Registradores
        painel.add(criarPainelRegistradores());
        
        return painel;
    }
    
    private JPanel criarPainelInstrucoes() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(new TitledBorder("Status das Instruções"));
        
        String[] colunas = {"Instrução", "Issue", "Execute", "Write Result", "Commit"};
        modeloInstrucoes = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tabelaInstrucoes = new JTable(modeloInstrucoes);
        tabelaInstrucoes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaInstrucoes.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane scroll = new JScrollPane(tabelaInstrucoes);
        painel.add(scroll, BorderLayout.CENTER);
        
        return painel;
    }
    
    private JPanel criarPainelEstacoes() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(new TitledBorder("Estações de Reserva"));
        
        String[] colunas = {"Nome", "Busy", "Op", "Vj", "Vk", "Qj", "Qk", "Dest"};
        modeloEstacoes = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tabelaEstacoes = new JTable(modeloEstacoes);
        tabelaEstacoes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaEstacoes.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane scroll = new JScrollPane(tabelaEstacoes);
        painel.add(scroll, BorderLayout.CENTER);
        
        return painel;
    }
    
    private JPanel criarPainelROB() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(new TitledBorder("Reorder Buffer"));
        
        String[] colunas = {"Entry", "Busy", "Instrução", "Estado", "Destino", "Valor"};
        modeloROB = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tabelaROB = new JTable(modeloROB);
        tabelaROB.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaROB.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane scroll = new JScrollPane(tabelaROB);
        painel.add(scroll, BorderLayout.CENTER);
        
        return painel;
    }
    
    private JPanel criarPainelRegistradores() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(new TitledBorder("Status dos Registradores"));
        
        String[] colunas = {"Registrador", "Busy", "Físico", "Valor"};
        modeloRegistradores = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tabelaRegistradores = new JTable(modeloRegistradores);
        tabelaRegistradores.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaRegistradores.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane scroll = new JScrollPane(tabelaRegistradores);
        painel.add(scroll, BorderLayout.CENTER);
        
        return painel;
    }
    
    private JPanel criarPainelLog() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(new TitledBorder("Log de Execução"));
        painel.setPreferredSize(new Dimension(0, 200));
        
        areaLog = new JTextArea(10, 0);
        areaLog.setEditable(false);
        areaLog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        areaLog.setBackground(Color.BLACK);
        areaLog.setForeground(Color.GREEN);
        
        JScrollPane scroll = new JScrollPane(areaLog);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        painel.add(scroll, BorderLayout.CENTER);
        
        return painel;
    }
    
    private void carregarArquivo() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
            }
            
            @Override
            public String getDescription() {
                return "Arquivos de texto (*.txt)";
            }
        });
        
        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            File arquivo = fileChooser.getSelectedFile();
            try {
                simulador.carregarInstrucoes(arquivo.getAbsolutePath());
                btnProximoCiclo.setEnabled(true);
                btnExecutarCompleto.setEnabled(true);
                btnReiniciar.setEnabled(true);
                atualizarInterface();
                JOptionPane.showMessageDialog(this, 
                    "Arquivo carregado com sucesso!\n" + 
                    simulador.getTotalInstrucoes() + " instruções carregadas.",
                    "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, 
                    "Erro ao carregar arquivo:\n" + e.getMessage(),
                    "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void proximoCiclo() {
        simulador.proximoCiclo();
        atualizarInterface();
        
        if (simulador.isSimulacaoCompleta()) {
            btnProximoCiclo.setEnabled(false);
            btnExecutarCompleto.setEnabled(false);
            JOptionPane.showMessageDialog(this,
                String.format("Simulação completa!\n\nEstatísticas:\n" +
                    "• Total de Ciclos: %d\n" +
                    "• Instruções Executadas: %d\n" +
                    "• Ciclos de Bolha: %d\n" +
                    "• IPC: %.2f",
                    simulador.getTotalCiclos(),
                    simulador.getInstrucoesExecutadas(),
                    simulador.getCiclosBolha(),
                    simulador.calcularIPC()),
                "Simulação Completa", JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void executarCompleto() {
        btnProximoCiclo.setEnabled(false);
        btnExecutarCompleto.setEnabled(false);
        
        // Executa em thread separada para não bloquear a UI
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                simulador.executarCompleto();
                return null;
            }
            
            @Override
            protected void done() {
                atualizarInterface();
                JOptionPane.showMessageDialog(SimuladorMain.this,
                    String.format("Simulação completa!\n\nEstatísticas:\n" +
                        "• Total de Ciclos: %d\n" +
                        "• Instruções Executadas: %d\n" +
                        "• Ciclos de Bolha: %d\n" +
                        "• IPC: %.2f",
                        simulador.getTotalCiclos(),
                        simulador.getInstrucoesExecutadas(),
                        simulador.getCiclosBolha(),
                        simulador.calcularIPC()),
                    "Simulação Completa", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        worker.execute();
    }
    
    private void reiniciar() {
        simulador.reiniciar();
        btnProximoCiclo.setEnabled(true);
        btnExecutarCompleto.setEnabled(true);
        btnReiniciar.setEnabled(true);
        atualizarInterface();
    }
    
    private void atualizarInterface() {
        atualizarTabelaInstrucoes();
        atualizarTabelaEstacoes();
        atualizarTabelaROB();
        atualizarTabelaRegistradores();
        atualizarEstatisticas();
        atualizarLog();
    }
    
    private void atualizarTabelaInstrucoes() {
        modeloInstrucoes.setRowCount(0);
        
        List<ReorderBufferSlot> rob = simulador.getReorderBufferState();
        List<Instrucao> instrucoes = simulador.getInstrucoes();
        
        // Cria um mapa para facilitar a busca de informações por instrução
        for (int i = 0; i < simulador.getTotalInstrucoes(); i++) {
            Instrucao instrucao = instrucoes.get(i);
            String instrucaoString = (i + 1) + ": " + instrucao.toString() + ((instrucao.getQtdeExecucoes() == 0) ? "" : " (" + instrucao.getQtdeExecucoes() + ")");
            String issue = "-";
            String execute = "-";
            String writeResult = "-";
            String commit = "-";
            
            // if (i < simulador.getPc()) {
            //     issue = "✓";
            //     if (i < simulador.getInstrucoesExecutadas()) {
            //         execute = "✓";
            //         writeResult = "✓";
            //         commit = "✓";
            //     }
            // }

            if (instrucao.getEstadoExecucao() == -1) {
                instrucaoString += " (Pulada)";
            } else {
                if(instrucao.getEstadoExecucao() >= 1) {
                    issue = "✓";
                }
                if(instrucao.getEstadoExecucao() >= 2) {
                    execute = "✓";
                }
                if(instrucao.getEstadoExecucao() >= 3) {
                    writeResult = "✓";
                }
                if(instrucao.getEstadoExecucao() >= 4) {
                    commit = "✓";
                }
            }
            
            
            modeloInstrucoes.addRow(new Object[]{instrucaoString, issue, execute, writeResult, commit});
        }
    }
    
    private void atualizarTabelaEstacoes() {
        modeloEstacoes.setRowCount(0);
        
        List<EstacaoDeReserva> estacoes = simulador.getReservationStationsState();
        for (EstacaoDeReserva estacao : estacoes) {
            Object[] linha = {
                estacao.getNome(),
                estacao.isBusy() ? "Sim" : "Não",
                estacao.getOp() != null ? estacao.getOp().getNome() : "-",
                estacao.getVj() != null ? String.format("%.2f", estacao.getVj()) : "-",
                estacao.getVk() != null ? String.format("%.2f", estacao.getVk()) : "-",
                estacao.getQj() != null ? estacao.getQj() : "-",
                estacao.getQk() != null ? estacao.getQk() : "-",
                estacao.getDest() != null ? estacao.getDest() : "-"
            };
            modeloEstacoes.addRow(linha);
        }
    }
    
    private void atualizarTabelaROB() {
        modeloROB.setRowCount(0);
        
        List<ReorderBufferSlot> rob = simulador.getReorderBufferState();
        for (int i = 0; i < rob.size(); i++) {
            ReorderBufferSlot slot = rob.get(i);
            Object[] linha = {
                i,
                slot.isBusy() ? "Sim" : "Não",
                // slot.getInstrucao() != null ? slot.getInstrucao().toString() + ((slot.getInstrucao().getQtdeExecucoes() == 0) ? "" : " (" + slot.getInstrucao().getQtdeExecucoes() + ")") + "-" : "-",
                slot.getInstrucao() != null ? slot.getInstrucao().toString() : "-",
                slot.isBusy() ? slot.getEstado().getDescricao() : "-",
                slot.getRegistradorPublico() != null ? slot.getRegistradorPublico() : "-",
                slot.isPronto() && slot.getResultado() != null ? 
                    String.format("%.2f", slot.getResultado()) : "-"
            };
            modeloROB.addRow(linha);
        }
    }
    
    @SuppressWarnings("unchecked")
    private void atualizarTabelaRegistradores() {
        modeloRegistradores.setRowCount(0);
        
        Map<String, Object> status = simulador.getRegisterStatus();
        Map<String, Float> publico = (Map<String, Float>) status.get("publico");
        Map<String, String> mapeamento = (Map<String, String>) status.get("mapeamento");
        Map<String, Float> privado = (Map<String, Float>) status.get("privado");
        
        for (String reg : publico.keySet()) {
            String regFisico = mapeamento.get(reg);
            Float valor = publico.get(reg);
            
            // Verifica se há instrução pendente para este registrador
            boolean busy = false;
            List<ReorderBufferSlot> rob = simulador.getReorderBufferState();
            for (ReorderBufferSlot slot : rob) {
                if (slot.isBusy() && reg.equals(slot.getRegistradorPublico()) && !slot.isPronto()) {
                    busy = true;
                    break;
                }
            }
            
            Object[] linha = {
                reg,
                busy ? "Sim" : "Não",
                regFisico,
                String.format("%.2f", valor)
            };
            modeloRegistradores.addRow(linha);
        }
    }
    
    private void atualizarEstatisticas() {
        labelCiclo.setText("Ciclo: " + simulador.getCicloAtual());
        labelIPC.setText(String.format("IPC: %.2f", simulador.calcularIPC()));
        labelInstrucoes.setText(String.format("Instruções: %d/%d", 
            simulador.getInstrucoesExecutadas(), simulador.getTotalInstrucoes()));
        labelCiclosBolha.setText("Ciclos Bolha: " + simulador.getCiclosBolha());
        
        // Atualiza barra de progresso
        if (simulador.getTotalInstrucoes() > 0) {
            int progresso = (simulador.getInstrucoesExecutadas() * 100) / simulador.getTotalInstrucoes();
            progressBar.setValue(progresso);
            progressBar.setString(progresso + "%");
        }
    }
    
    private void atualizarLog() {
        StringBuilder log = new StringBuilder();
        List<String> logs = simulador.getLogExecucao();
        
        // Mostra apenas as últimas 100 linhas para não sobrecarregar a interface
        int inicio = Math.max(0, logs.size() - 100);
        for (int i = inicio; i < logs.size(); i++) {
            log.append(logs.get(i)).append("\n");
        }
        
        areaLog.setText(log.toString());
        areaLog.setCaretPosition(areaLog.getDocument().getLength());
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SimuladorMain().setVisible(true);
        });
    }
}
