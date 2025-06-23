# Simulador do Algoritmo de Tomasulo

Este projeto implementa um simulador didático do algoritmo de Tomasulo para arquitetura superescalar com execução fora de ordem.

## Características Implementadas

### Algoritmo de Tomasulo
- **Execução fora de ordem** com commit em ordem
- **Buffer de Reordenamento (ROB)** para garantir a ordem de commit
- **Renomeação de registradores** usando banco de registradores físicos
- **Estações de Reserva** para diferentes tipos de operações
- **Common Data Bus (CDB)** para propagação de resultados

### Tipos de Operações Suportadas
- **Aritméticas**: ADD, SUB, MUL, DIV
- **Imediatas**: ADDI, SUBI, MULI, DIVI
- **Memória**: LOAD, STORE
- **Controle**: BEQ (Branch if Equal)

### Estrutura do Simulador
- **16 registradores arquiteturais**: R0 a R15
- **32 registradores físicos**: P0 a P31
- **8 slots no ROB**: Buffer de reordenamento
- **Estações de Reserva**:
  - 3 estações para ADD/SUB
  - 2 estações para MUL/DIV
  - 2 estações para LOAD/STORE

## Como Usar

### 1. Compilação
```bash
javac -d bin src/simulador/*.java src/gui/*.java
```

### 2. Execução
```bash
java -cp bin gui.SimuladorMain
```

### 3. Interface Gráfica
A interface é dividida em seções:

#### Controles
- **Carregar Arquivo**: Carrega instruções de um arquivo .txt
- **Próximo Ciclo**: Executa apenas um ciclo
- **Executar Completo**: Executa até o fim
- **Reiniciar**: Reinicia o simulador

#### Tabelas de Monitoramento
1. **Status das Instruções**: Mostra o pipeline de cada instrução
2. **Estações de Reserva**: Estado das estações de reserva
3. **Reorder Buffer**: Conteúdo do ROB
4. **Status dos Registradores**: Mapeamento e valores

#### Estatísticas
- **Ciclo atual**: Número do ciclo sendo executado
- **IPC**: Instructions Per Cycle
- **Instruções**: Executadas/Total
- **Ciclos de Bolha**: Ciclos sem progresso

### 4. Formato das Instruções

As instruções devem seguir o formato MIPS:

```
# Operações aritméticas (3 registradores)
ADD R1,R2,R3     # R1 = R2 + R3
SUB R1,R2,R3     # R1 = R2 - R3
MUL R1,R2,R3     # R1 = R2 * R3
DIV R1,R2,R3     # R1 = R2 / R3

# Operações com imediato
ADDI R1,R2,100   # R1 = R2 + 100
SUBI R1,R2,50    # R1 = R2 - 50
MULI R1,R2,5     # R1 = R2 * 5
DIVI R1,R2,2     # R1 = R2 / 2

# Operações de memória
LOAD R1,100(R2)  # R1 = Mem[R2 + 100]
STORE R1,100(R2) # Mem[R2 + 100] = R1

# Branch condicional
BEQ R1,R2,10     # if (R1 == R2) PC += 10
```

### 5. Exemplo de Uso

## Com java runtime
1. Compile o programa com `javac -d bin src/simulador/*.java src/gui/*.java`
2. Abra o simulador executando `java -cp bin gui.SimuladorMain`
3. Clique em "Carregar Arquivo" e selecione `exemplo.txt`
4. Use "Próximo Ciclo" para ver a execução passo a passo
5. Ou use "Executar Completo" para ver o resultado final

## Arquitetura do Código

### Pacote `simulador`
- `OpCode.java`: Enum com as operações suportadas
- `EstadoInstrucao.java`: Estados das instruções no pipeline
- `Instrucao.java`: Representação de uma instrução MIPS
- `EstacaoDeReserva.java`: Estação de reserva do algoritmo
- `ReorderBufferSlot.java`: Slot do buffer de reordenamento
- `InstructionParser.java`: Parser de instruções MIPS
- `Simulador.java`: Classe principal com a lógica do algoritmo

### Pacote `gui`
- `SimuladorGUI.java`: Interface gráfica em Java Swing

## Algoritmo Implementado

### Ciclo Principal (em ordem inversa)

1. **Commit**: 
   - Verifica a cabeça do ROB
   - Se concluída, atualiza banco arquitetural
   - Libera registrador físico antigo

2. **Write Result**:
   - Unidades funcionais terminadas enviam resultado via CDB
   - ROB e estações de reserva escutam o CDB
   - Propaga resultados para dependências

3. **Execute**:
   - Verifica estações prontas (operandos disponíveis)
   - Inicia/continua execução baseada no tipo de operação

4. **Issue**:
   - Pega próxima instrução
   - Aloca slot no ROB e estação de reserva
   - Renomeia registrador de destino
   - Resolve dependências dos operandos

### Latências das Operações
- **ADD/SUB/ADDI/SUBI/BEQ**: 1 ciclo
- **MUL/MULI**: 3 ciclos
- **DIV/DIVI**: 5 ciclos
- **LOAD/STORE**: 2 ciclos

## Métricas de Desempenho

- **IPC (Instructions Per Cycle)**: Instruções executadas / Total de ciclos
- **Ciclos de Bolha**: Ciclos sem progresso no pipeline
- **Throughput**: Instruções completadas por ciclo

## Limitações Conhecidas

1. Especulação de branch não implementada completamente
2. Cache de memória não simulado (acesso em tempo constante)
3. Hazards estruturais limitados ao número de estações de reserva
4. Modelo de memória simplificado

## Arquivos de Exemplo

- `exemplo.txt`: Programa exemplo com diferentes tipos de instruções
- Crie seus próprios arquivos seguindo o formato especificado

Este simulador foi desenvolvido para fins didáticos e permite a visualização clara do funcionamento do algoritmo de Tomasulo, facilitando o entendimento de conceitos como execução fora de ordem, renomeação de registradores e especulação.
This is a simulator made in Java to show how Tomasulo's Algorithm works in a simple processor
