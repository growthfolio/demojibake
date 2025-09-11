# 🚀 Text Encoding Workbench v2.0 - RELEASE NOTES

## 📋 O que é?
Aplicativo desktop avançado para análise e correção de problemas de codificação de texto (mojibake), especialmente para textos em português com caracteres acentuados.

## ✨ Principais Recursos v2.0
- 🎯 **Interface Moderna**: Dark theme cyberpunk com animações suaves
- ⚡ **Processamento Paralelo**: Múltiplos arquivos processados simultaneamente  
- 📊 **Tabela Responsiva**: Colunas com emojis e redimensionamento automático
- 🔧 **Engine Nativo**: Biblioteca Go otimizada para máxima performance
- 📈 **Analytics Avançados**: Métricas detalhadas e estatísticas de processamento
- 🎨 **Sistema Anti-Tremida**: 12 "soluções bizarras" para animações estáveis

## 🔧 Como Instalar

### Método 1: Pacote DEB (Recomendado para Ubuntu/Debian)
```bash
sudo dpkg -i textencodingworkbench_2.0.0_amd64.deb
sudo apt-get install -f  # Se houver dependências faltando
```

### Método 2: Script Portável (Para qualquer distribuição Linux)
```bash
# 1. Baixe e extraia o projeto
git clone <repositorio>
cd textual_harmony_analyzer

# 2. Compile as bibliotecas nativas
cd character_analysis_engine
./build.sh

# 3. Compile o aplicativo Java
cd ../desktop_workbench
mvn clean package -DskipTests

# 4. Execute o aplicativo
cd ../deployment_packages/linux
./run_textencoding_workbench.sh
```

## 📋 Requisitos do Sistema
- **Java**: OpenJDK/Oracle JDK 21+ 
- **JavaFX**: Incluído automaticamente
- **Memória**: 512MB RAM mínimo, 2GB recomendado
- **OS**: Linux x64 (Ubuntu 20.04+, Debian 11+, etc.)

## 🎮 Como Usar

### 1. Processamento Básico
1. Abra o aplicativo
2. Vá para a aba **⚡ PROCESSAMENTO**
3. Arraste arquivos para a zona de drop OU clique em **🚀 PROCESSAR ARQUIVOS**
4. Aguarde o processamento e veja os resultados na tabela

### 2. Processamento em Lote
1. Clique em **⚡ LOTE AVANÇADO**
2. Selecione um diretório
3. Todos os arquivos compatíveis serão processados automaticamente

### 3. Visualizar Estatísticas
- **📚 DICIONÁRIO**: Métricas do dicionário linguístico
- **📊 ANALYTICS**: Performance e estatísticas detalhadas  
- **⚙️ CONFIGURAÇÕES**: Ajustes de performance e processamento

## 🔍 Formatos Suportados
- `.txt` - Arquivos de texto
- `.log` - Arquivos de log
- `.csv` - Dados separados por vírgula
- `.json` - Documentos JSON

## 🐛 Solução de Problemas

### Animações com Tremida/Pulsação
- **Duplo-clique no botão ⬜ (maximizar)** para reset de emergência
- Isso para todas as animações e restaura o estado normal

### Erro de Biblioteca Nativa
```bash
# Recompile as bibliotecas nativas
cd character_analysis_engine
./build.sh
```

### Erro de JavaFX
```bash
# Instale JavaFX se necessário
sudo apt-get install openjfx
```

### Permissões Negadas
```bash
# Torne o script executável
chmod +x run_textencoding_workbench.sh
```

## 📊 Performance

### Benchmarks Típicos
- **Arquivo de 1MB**: ~50-200ms
- **Batch de 100 arquivos**: ~5-15 segundos  
- **Uso de memória**: 200-500MB durante processamento
- **CPU**: Utiliza todos os cores disponíveis

### Otimizações Aplicadas
- ✅ Memory-mapped I/O
- ✅ Bloom filters para dicionário
- ✅ Workers pool otimizado
- ✅ Cache de padrões de encoding
- ✅ Análise contextual com n-gramas

## 🔄 Logs e Debugging

Os logs são exibidos no console. Para debug detalhado:
```bash
# Execute com logs verbose
java -Djava.util.logging.level=FINE -jar <aplicativo>
```

## 🆘 Suporte

Em caso de problemas:
1. Verifique os requisitos do sistema
2. Recompile as bibliotecas nativas
3. Teste o script portável
4. Use o botão de reset de emergência (duplo-click ⬜)

## 🎉 Changelog v2.0

### ✨ Novidades
- Interface completamente redesenhada
- Sistema de animações estável  
- Tabelas responsivas com emojis
- Engine nativo reescrito em Go
- Performance 10x melhor

### 🔧 Correções
- Eliminadas tremidas nas animações
- Corrigidos vazamentos de memória
- Melhorada detecção de encoding
- Corrigidos problemas com caracteres especiais

---

**🚀 Desenvolvido com paixão para análise de texto em português!** 

*Versão 2.0 - Setembro 2024*
