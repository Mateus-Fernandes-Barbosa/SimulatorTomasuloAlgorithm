@echo off
echo Compilando Simulador Tomasulo...

if not exist bin mkdir bin

javac -d bin src/simulador/*.java src/gui/*.java

if %ERRORLEVEL% == 0 (
    echo Compilacao concluida com sucesso!
    echo Executando simulador...
    java -cp bin gui.SimuladorMain
) else (
    echo Erro na compilacao!
    pause
)
