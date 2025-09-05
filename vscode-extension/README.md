# Demojibakelizador - VS Code Extension

Extensão oficial do Demojibakelizador para Visual Studio Code. Detecta e corrige problemas de encoding (mojibake) diretamente no editor.

## 🚀 Funcionalidades

### ⚡ Detecção Automática
- **Status Bar**: Mostra encoding do arquivo atual
- **Auto-detecção**: Detecta encoding ao abrir arquivos
- **Indicadores visuais**: Ícones coloridos por status

### 🔧 Correção Rápida
- **Clique direito**: "Fix Encoding" no menu de contexto
- **Command Palette**: `Ctrl+Shift+P` → "Demojibakelizador"
- **Explorer**: Clique direito em arquivos/pastas

### 📊 Análise de Workspace
- **Scan completo**: Analisa todo o workspace
- **Tree View**: Lista arquivos com problemas
- **Relatórios HTML**: Gera relatórios detalhados

### 🔄 Conversão Reversa
- **UTF-8 → ISO-8859-1**: Para sistemas legados
- **Validação**: Verifica compatibilidade
- **Auto-fix**: Corrige caracteres incompatíveis

## 📋 Comandos Disponíveis

| Comando | Descrição | Atalho |
|---------|-----------|--------|
| `🔧 Fix Encoding` | Corrige encoding do arquivo atual | Menu contexto |
| `🔍 Detect Encoding` | Detecta encoding e mostra detalhes | Status bar |
| `📁 Scan Workspace` | Analisa todo o workspace | Command Palette |
| `🔄 Convert to ISO-8859-1` | Converte para sistema legado | Command Palette |
| `📊 Show Report` | Gera relatório HTML | Command Palette |
| `⚙️ Settings` | Abre configurações | Command Palette |

## ⚙️ Configurações

```json
{
  "demojibakelizador.binaryPath": "",
  "demojibakelizador.autoDetectOnOpen": true,
  "demojibakelizador.showStatusBar": true,
  "demojibakelizador.autoBackup": true,
  "demojibakelizador.backupSuffix": ".bak",
  "demojibakelizador.fileExtensions": [
    ".txt", ".md", ".java", ".js", ".ts", 
    ".html", ".css", ".xml", ".properties", ".csv"
  ],
  "demojibakelizador.excludeDirectories": [
    "node_modules", ".git", "target", "build", "dist"
  ],
  "demojibakelizador.fixMojibake": true,
  "demojibakelizador.stripBOM": true
}
```

## 🛠️ Instalação

### Método 1: VS Code Marketplace
1. Abra VS Code
2. Vá para Extensions (`Ctrl+Shift+X`)
3. Procure por "Demojibakelizador"
4. Clique em "Install"

### Método 2: VSIX Manual
1. Baixe o arquivo `.vsix` do [GitHub Releases](https://github.com/growthfolio/demojibake/releases)
2. No VS Code: `Ctrl+Shift+P` → "Extensions: Install from VSIX"
3. Selecione o arquivo baixado

### Método 3: Desenvolvimento
```bash
cd vscode-extension
npm install
npm run compile
# F5 para testar em nova janela do VS Code
```

## 📦 Pré-requisitos

A extensão precisa do binário `demojibake` instalado:

```bash
# Via Go
go install github.com/growthfolio/demojibake/cmd/demojibake@latest

# Ou baixar binário pré-compilado
# https://github.com/growthfolio/demojibake/releases
```

## 🎯 Uso Típico

### 1. **Arquivo Individual**
- Abra um arquivo
- Veja o encoding na status bar
- Se houver problema: clique direito → "Fix Encoding"

### 2. **Projeto Completo**
- `Ctrl+Shift+P` → "Demojibakelizador: Scan Workspace"
- Veja problemas no painel "Encoding Issues"
- Clique nos arquivos para abrir e corrigir

### 3. **Sistema Legado**
- Selecione arquivo UTF-8
- `Ctrl+Shift+P` → "Demojibakelizador: Convert to ISO-8859-1"
- Escolha modo (validar/converter/auto-fix)

## 🔍 Indicadores Visuais

### Status Bar
- `✅ UTF-8` - Arquivo OK
- `⚠️ ISO-8859-1` - Precisa conversão
- `❌ UNKNOWN` - Erro de detecção

### Tree View
- `⚠️` - Confiança alta (>80%)
- `❌` - Confiança baixa (<80%)

## 🚀 Workflow Corporativo

### Para Desenvolvedores
1. **Auto-detecção** ao abrir arquivos
2. **Fix rápido** via menu contexto
3. **Status visual** na barra inferior

### Para Tech Leads
1. **Scan de workspace** completo
2. **Relatórios HTML** para auditoria
3. **Configuração por projeto** via settings.json

### Para DevOps
1. **Validação pré-commit** via CLI
2. **Integração CI/CD** com exit codes
3. **Padronização** de encoding UTF-8

## 🐛 Troubleshooting

### Extensão não funciona
1. Verifique se o binário `demojibake` está instalado
2. Configure o caminho em `demojibakelizador.binaryPath`
3. Teste no terminal: `demojibake -h`

### Status bar não aparece
1. Verifique `demojibakelizador.showStatusBar: true`
2. Abra um arquivo suportado (.txt, .java, etc.)
3. Recarregue a janela (`Ctrl+Shift+P` → "Reload Window")

### Scan não encontra arquivos
1. Verifique `demojibakelizador.fileExtensions`
2. Confirme `demojibakelizador.excludeDirectories`
3. Teste no terminal com as mesmas configurações

## 📄 Licença

MIT License - veja [LICENSE](../LICENSE) para detalhes.

## 🤝 Contribuição

1. Fork o projeto
2. Crie uma branch: `git checkout -b feature/nova-funcionalidade`
3. Commit: `git commit -am 'Adiciona nova funcionalidade'`
4. Push: `git push origin feature/nova-funcionalidade`
5. Abra um Pull Request

## 📞 Suporte

- **Issues**: [GitHub Issues](https://github.com/growthfolio/demojibake/issues)
- **Documentação**: [README Principal](../README.md)
- **Releases**: [GitHub Releases](https://github.com/growthfolio/demojibake/releases)

---

**Demojibakelizador VS Code Extension** - Encoding perfeito, workflow perfeito! 🚀✨