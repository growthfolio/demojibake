# Demojibakelizador

Uma ferramenta corporativa para detectar e corrigir problemas de encoding (mojibake) em arquivos de texto, convertendo-os para UTF-8.

## Visão Geral

**Mojibake** (文字化け) é a corrupção de caracteres que ocorre quando texto é interpretado usando um encoding diferente do original. Exemplos comuns:

- `café` → `cafÃ©` (UTF-8 lido como Latin-1)
- `"aspas"` → `â€œaspasâ€` (UTF-8 lido como Windows-1252)
- `—` → `â€"` (travessão longo)
- `©` → `Â©` (símbolo de copyright)

O Demojibakelizador oferece:

- **Detecção automática** de encoding usando heurísticas avançadas
- **Conversão segura** para UTF-8 com backup automático
- **Correção de mojibake** através de heurísticas inteligentes
- **CLI robusto** para automação e CI/CD
- **GUI amigável** para uso manual
- **Processamento em lote** com concorrência
- **Operação segura** com dry-run e backups

## Instalação

### Via Go Install
```bash
go install github.com/growthfolio/demojibake/cmd/demojibake@latest
go install github.com/growthfolio/demojibake/cmd/demojibake-gui@latest
```

### Binários Pré-compilados
Baixe os binários para sua plataforma na [página de releases](https://github.com/growthfolio/demojibake/releases).

### Docker
```bash
docker pull demojibake:latest
docker run --rm -v $(pwd):/data demojibake -path /data
```

### Build Local
```bash
git clone https://github.com/growthfolio/demojibake.git
cd demojibake
make build
```

## Uso Rápido

### Detectar Problemas de Encoding
```bash
# Detectar em diretório específico
demojibake -path ./src -detect -ext ".java,.properties"

# Detectar recursivamente
demojibake -path . -detect -recursive

# Para CI: falhar se encontrar não-UTF-8
demojibake -path . -detect -fail-if-not-utf8
```

### Corrigir Arquivos
```bash
# Conversão básica com backup
demojibake -path ./src -in-place -backup-suffix ".bak"

# Dry-run (simular sem alterar)
demojibake -path ./src -dry-run

# Forçar encoding de origem
demojibake -path arquivo.txt -from iso-8859-1 -in-place

# Processar arquivo único para stdout
demojibake -path arquivo.txt -stdout
```

### Opções Avançadas
```bash
# Customizar extensões e exclusões
demojibake -path . -ext ".txt,.md,.java" -exclude-dirs "node_modules,.git"

# Controlar BOM UTF-8
demojibake -path . -strip-bom=false -add-bom

# Ajustar concorrência
demojibake -path . -workers 8

# Desabilitar correção de mojibake
demojibake -path . -fix-mojibake=false
```

## Interface Gráfica (GUI)

Execute `demojibake-gui` para abrir a interface gráfica:

![GUI Screenshot](assets/icons/app.png)

### Recursos da GUI:
- Seleção de arquivos e pastas via dialog
- Configuração visual de todas as opções
- Visualização em tempo real dos logs
- Barra de progresso durante processamento
- Cancelamento de operações em andamento

### Campos da GUI:
- **Caminho**: Arquivo ou pasta para processar
- **Modo**: Detectar apenas ou converter para UTF-8
- **Encoding origem**: Auto-detecção ou forçar encoding específico
- **Extensões**: Lista de extensões de arquivo (CSV)
- **Opções**: Recursivo, In-place, Dry-run, Fix Mojibake, BOM handling
- **Configurações**: Sufixo de backup, número de workers

## Heurística de Correção de Mojibake

O sistema implementa uma heurística inteligente para reverter mojibake comum:

### Como Funciona:
1. **Detecção de Padrões**: Identifica sequências típicas de mojibake (`Ã©`, `â€"`, `Â `)
2. **Round-trip Latin-1**: Tenta converter cada caractere ≤ 0xFF para byte e reinterpretar como UTF-8
3. **Scoring**: Avalia a qualidade do texto antes e depois da correção
4. **Aplicação Segura**: Só aplica a correção se melhorar o score do texto

### Limitações:
- Funciona melhor com mojibake UTF-8 → Latin-1/Windows-1252
- Pode não detectar todos os casos complexos
- Textos muito corrompidos podem não ser recuperáveis

## Encodings Suportados

| Encoding | Aliases | Descrição |
|----------|---------|-----------|
| UTF-8 | utf8 | Unicode padrão |
| ISO-8859-1 | latin1 | Europa Ocidental |
| ISO-8859-2 | latin2 | Europa Central |
| ISO-8859-15 | latin9 | Europa Ocidental + Euro |
| Windows-1252 | cp1252 | Windows Europa Ocidental |
| Macintosh | mac-roman | Mac OS clássico |
| CP850 | ibm850 | DOS Europa Ocidental |

*Suporte completo para ISO-8859-3 até ISO-8859-16 também disponível.*

## Diretórios Ignorados

Por padrão, os seguintes diretórios são ignorados:
- `.git`, `.svn`, `.hg` (controle de versão)
- `node_modules` (Node.js)
- `bin`, `target`, `dist`, `build`, `out` (build artifacts)
- `.idea`, `.vscode` (IDEs)

Customize com `-exclude-dirs "dir1,dir2,dir3"`.

## Segurança e Desempenho

### Segurança:
- **Escrita Atômica**: Usa arquivos temporários + rename para evitar corrupção
- **Backups Automáticos**: Cria `.bak` antes de modificar (configurável)
- **Detecção de Binários**: Ignora arquivos com bytes NUL
- **Preservação de Metadados**: Mantém permissões e timestamps
- **Dry-run**: Simula operações sem alterar arquivos

### Desempenho:
- **Streaming**: Processa arquivos grandes em chunks de 64KB
- **Concorrência**: Workers paralelos (padrão: NumCPU/2)
- **Sample-based Detection**: Analisa apenas primeiros 64KB para detecção
- **Cancelamento Graceful**: Respeita Ctrl+C e finaliza operações pendentes

## Roteiro de QA Manual

### Preparação:
```bash
# Build do projeto
make build

# Copiar samples para teste
cp -r assets/samples /tmp/test-samples
cd /tmp/test-samples
```

### Teste 1: Detecção
```bash
demojibake -path . -detect -v
```
**Resultado esperado**: Deve detectar encodings diferentes em cada arquivo sample.

### Teste 2: Dry-run
```bash
demojibake -path . -dry-run -in-place -backup-suffix ".bak"
```
**Resultado esperado**: Status `FIX` para arquivos com problemas, mas **nenhum arquivo alterado**.

### Teste 3: Conversão Real
```bash
demojibake -path . -in-place -backup-suffix ".bak"
```
**Resultado esperado**: 
- Arquivos `.bak` criados
- Mojibake corrigido nos arquivos originais
- Acentos e símbolos exibidos corretamente

### Teste 4: GUI
```bash
demojibake-gui
```
**Passos**:
1. Selecionar pasta `/tmp/test-samples`
2. Configurar "Converter p/ UTF-8"
3. Marcar "Dry-run"
4. Executar e verificar logs
5. Desmarcar "Dry-run" e executar novamente

### Teste 5: Cancelamento
```bash
# Em diretório grande
demojibake -path /usr/share -detect &
# Pressionar Ctrl+C após alguns segundos
```
**Resultado esperado**: Cancelamento limpo com mensagem de interrupção.

### Teste 6: CI Mode
```bash
# Criar arquivo não-UTF-8
echo "Ã©" > test-mojibake.txt
demojibake -path . -detect -fail-if-not-utf8
echo $?  # Deve retornar 1
```

## Integração com CI/CD

### GitHub Actions:
```yaml
- name: Check Encoding
  run: |
    go install github.com/growthfolio/demojibake/cmd/demojibake@latest
    demojibake -path . -detect -fail-if-not-utf8 -ext ".java,.properties,.xml"
```

### Pre-commit Hook:
```bash
# Instalar hook
./scripts/install_hooks.sh

# O hook verificará automaticamente arquivos staged
git add arquivo-com-mojibake.txt
git commit -m "test"  # Será bloqueado se houver problemas
```

## Flags Completas do CLI

```
-path <arquivo|diretorio>        Caminho para processar (default: .)
-ext ".txt,.md,..."              Extensões de arquivo (CSV)
-recursive <true|false>          Processar recursivamente (default: true)
-detect                          Apenas detectar, não converter
-from <encoding>                 Forçar encoding de origem
-in-place                        Modificar arquivos no local
-backup-suffix ".bak"           Sufixo de backup ("" para desabilitar)
-dry-run                         Simular sem fazer alterações
-workers <N>                     Número de workers (default: NumCPU/2)
-preserve-times <true|false>     Preservar timestamps (default: true)
-strip-bom <true|false>          Remover BOM UTF-8 (default: true)
-add-bom                         Adicionar BOM UTF-8
-fix-mojibake <true|false>       Tentar corrigir mojibake (default: true)
-stdout                          Saída para stdout (apenas 1 arquivo)
-fail-if-not-utf8                Falhar se encontrar não-UTF-8
-exclude-dirs "dir1,dir2"        Diretórios para ignorar (CSV)
-v                               Saída verbosa (DEBUG)
```

## Códigos de Saída

- `0`: Sucesso
- `1`: Erros durante processamento ou arquivos não-UTF-8 encontrados (com `-fail-if-not-utf8`)
- `2`: Uso inválido da ferramenta

## Changelog

### v1.0.0 (2024-01-XX)
- Implementação inicial
- CLI completo com todas as funcionalidades
- GUI usando Fyne
- Suporte multi-plataforma
- Heurística de correção de mojibake
- Docker e CI/CD integração
- Documentação completa

## Licença

MIT License - veja [LICENSE](LICENSE) para detalhes.

## Contribuição

1. Fork o projeto
2. Crie uma branch para sua feature (`git checkout -b feature/nova-funcionalidade`)
3. Commit suas mudanças (`git commit -am 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/nova-funcionalidade`)
5. Abra um Pull Request

## Suporte

- **Issues**: [GitHub Issues](https://github.com/growthfolio/demojibake/issues)
- **Documentação**: Este README e comentários no código
- **Exemplos**: Diretório `assets/samples/`

---

**Demojibakelizador** - Porque texto corrompido não deveria ser normal. 🔧✨