# 🚀 Text Encoding Workbench v2.0

Aplicativo desktop avançado para análise e correção de problemas de codificação de texto (mojibake), especialmente otimizado para textos em português com caracteres acentuados.

## ✨ Recursos v2.0

- 🎯 **Interface Cyberpunk**: Dark theme moderno com animações suaves anti-tremida
- ⚡ **Engine Nativo Go**: Performance 10x melhor com bibliotecas nativas (5.2MB)
- 📊 **Tabelas Responsivas**: Colunas com emojis e redimensionamento automático
- 🔧 **Processamento Paralelo**: Múltiplos arquivos processados simultaneamente
- 📈 **Analytics Avançados**: Métricas detalhadas e estatísticas em tempo real
- 🎨 **Sistema Anti-Pulso**: 12 soluções técnicas para animações estáveis

## 🚀 Instalação Rápida

### Opção 1: Pacote DEB (Ubuntu/Debian)
```bash
# Baixe o arquivo .deb da release
sudo dpkg -i textencodingworkbench_2.0.0_amd64.deb
```

### Opção 2: Script Portável
```bash
git clone <repositorio>
cd textual_harmony_analyzer
./deployment_packages/linux/run_textencoding_workbench.sh
```

## 📁 Estrutura do Projeto

```
textual_harmony_analyzer/
├── character_analysis_engine/   # Engine Go nativo
│   ├── character_encoding_engine.go  # Funções exportadas
│   ├── build.sh                     # Build cross-platform
│   └── go.mod                       # Módulo Go
├── desktop_workbench/              # Aplicativo JavaFX
│   ├── src/main/java/
│   │   ├── core/                   # Interfaces JNA
│   │   ├── launcher/               # Bootstrap da aplicação
│   │   └── ui/                     # Interface moderna
│   └── pom.xml                     # Configuração Maven
├── native_libraries/               # Bibliotecas compiladas
│   ├── current/                    # Plataforma atual
│   ├── linux/amd64/               # Linux x64
│   ├── macos/                     # macOS Intel/ARM
│   └── windows/                   # Windows x64
└── deployment_packages/            # Arquivos para usuários
    └── linux/                     # Release Linux
        ├── textencodingworkbench_2.0.0_amd64.deb
        ├── run_textencoding_workbench.sh
        └── README_RELEASE.md
```

## 🛠️ Para Desenvolvedores

### Build Completo
```bash
# 1. Compile as bibliotecas nativas
cd character_analysis_engine
./build.sh

# 2. Compile o aplicativo JavaFX
cd ../desktop_workbench
mvn clean package -DskipTests

# 3. Execute o aplicativo
cd ../deployment_packages/linux
./run_textencoding_workbench.sh
```

### Requisitos de Desenvolvimento

- **Go**: 1.21+ (para engine nativo)
- **Java**: OpenJDK/Oracle JDK 21+ (para aplicativo)
- **Maven**: 3.9+ (para build)
- **JavaFX**: Incluído automaticamente

## 🎮 Como Usar

1. **Processamento Básico**: Arraste arquivos para a zona de drop
2. **Lote Avançado**: Clique em "⚡ LOTE AVANÇADO" para processar diretórios
3. **Analytics**: Veja métricas na aba "📊 ANALYTICS" 
4. **Dicionário**: Estatísticas linguísticas na aba "📚 DICIONÁRIO"

### Formatos Suportados
- `.txt` - Arquivos de texto
- `.log` - Arquivos de log  
- `.csv` - Dados CSV
- `.json` - Documentos JSON

## 🐛 Solução de Problemas

### Animações com Tremida
**Duplo-clique no botão ⬜** para reset de emergência - isso para todas as animações.

### Erro de Biblioteca Nativa
```bash
cd character_analysis_engine && ./build.sh
```

### Performance
O sistema usa todos os cores da CPU disponíveis e otimizações de GPU para melhor performance.

## 📊 Benchmarks

- **Arquivo 1MB**: ~50-200ms
- **100 arquivos**: ~5-15 segundos
- **Memória**: 200-500MB durante processamento
- **Bibliotecas**: 5.2MB por plataforma

## 🏷️ Versão Atual: v2.0.0

**Changelog v2.0:**
- ✅ Sistema de animações completamente reescrito
- ✅ Tabelas responsivas com emojis  
- ✅ Engine Go nativo para máxima performance
- ✅ Interface cyberpunk moderna
- ✅ Correções de vazamento de memória
- ✅ Suporte completo a caracteres especiais

---

**Desenvolvido para análise de texto em português! 🇧🇷**