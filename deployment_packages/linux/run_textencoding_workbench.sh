#!/bin/bash
# Text Encoding Workbench v2.0 - Launcher Script
# Para executar o aplicativo sem instalação

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$(dirname "$SCRIPT_DIR")")"

echo "🚀 Iniciando Text Encoding Workbench v2.0"
echo "📍 Diretório do projeto: $PROJECT_ROOT"

# Verifica se o Java está disponível
if ! command -v java &> /dev/null; then
    echo "❌ Java não encontrado. Por favor, instale Java 21 ou superior."
    exit 1
fi

# Verifica a versão do Java
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F '.' '{print $1}')
if [ "$JAVA_VERSION" -lt 21 ]; then
    echo "❌ Java 21+ requerido. Versão atual: $JAVA_VERSION"
    exit 1
fi

echo "✅ Java $JAVA_VERSION detectado"

# Configura o classpath e bibliotecas nativas
JAR_PATH="$PROJECT_ROOT/desktop_workbench/target/demojibake-desktop-2.0.0.jar"
NATIVE_LIB_PATH="$PROJECT_ROOT/native_libraries/current"

if [ ! -f "$JAR_PATH" ]; then
    echo "❌ JAR não encontrado: $JAR_PATH"
    echo "Execute 'mvn clean package' no diretório desktop_workbench primeiro."
    exit 1
fi

if [ ! -d "$NATIVE_LIB_PATH" ]; then
    echo "❌ Bibliotecas nativas não encontradas: $NATIVE_LIB_PATH"
    echo "Execute './build.sh' no diretório character_analysis_engine primeiro."
    exit 1
fi

echo "✅ Arquivos do aplicativo encontrados"
echo "🔧 Iniciando aplicativo..."

# Executa o aplicativo com otimizações
java \
    -Djava.library.path="$NATIVE_LIB_PATH" \
    -Dprism.order=d3d,es2,sw \
    -Dprism.vsync=true \
    -Djavafx.animation.fullspeed=true \
    -Djavafx.animation.pulse=60 \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=10 \
    --module-path /usr/share/openjfx/lib \
    --add-modules javafx.controls,javafx.fxml \
    -jar "$JAR_PATH" "$@"

echo "👋 Text Encoding Workbench finalizado"
