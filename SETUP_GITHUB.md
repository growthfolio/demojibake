# Setup GitHub SSH - Windows PowerShell

## Pré-requisitos
- Git instalado e no PATH
- PowerShell (Windows)
- Conta GitHub (felipemacedo1)

## Passo a Passo

### 1. Configurar SSH
```powershell
# Execute no PowerShell como Administrador
.\setup-git-ssh.ps1
```

**O script irá:**
- Configurar user.name e user.email do Git
- Gerar chave SSH RSA 4096 bits
- Iniciar ssh-agent
- Mostrar a chave pública para copiar

### 2. Adicionar Chave no GitHub
1. Acesse: https://github.com/settings/ssh
2. Clique em **"New SSH key"**
3. Title: `Windows - Demojibakelizador`
4. Cole a chave pública gerada
5. Clique em **"Add SSH key"**

### 3. Testar Conexão SSH
```powershell
ssh -T git@github.com
```
**Resultado esperado:**
```
Hi felipemacedo1! You've successfully authenticated, but GitHub does not provide shell access.
```

### 4. Criar Repositório no GitHub
1. Acesse: https://github.com/orgs/growthfolio
2. Clique em **"New repository"**
3. Repository name: `demojibake`
4. Description: `Ferramenta corporativa para detectar e corrigir mojibake em arquivos de texto`
5. **Public** (recomendado para open source)
6. **NÃO** marcar "Add a README file"
7. Clique em **"Create repository"**

### 5. Inicializar e Enviar Código
```powershell
# Execute no diretório do projeto
.\init-repository.ps1
```

**O script irá:**
- Inicializar repositório Git local
- Adicionar remote origin
- Fazer commit inicial
- Enviar para branch main

### 6. Verificar Upload
Acesse: https://github.com/growthfolio/demojibake

Deve mostrar todos os arquivos do projeto.

## Comandos Manuais (Alternativa)

Se preferir executar manualmente:

```powershell
# Configurar Git
git config --global user.name "felipemacedo1"
git config --global user.email "felipealexandrej@gmail.com"

# Gerar chave SSH
ssh-keygen -t rsa -b 4096 -C "felipealexandrej@gmail.com"

# Adicionar ao ssh-agent
ssh-add ~/.ssh/id_rsa

# Mostrar chave pública
Get-Content ~/.ssh/id_rsa.pub

# Inicializar repositório
git init
git remote add origin git@github.com:growthfolio/demojibake.git
git add .
git commit -m "feat: implementação inicial do Demojibakelizador"
git branch -M main
git push -u origin main
```

## Troubleshooting

### Erro: "Permission denied (publickey)"
- Verifique se a chave SSH foi adicionada no GitHub
- Teste: `ssh -T git@github.com`
- Verifique ssh-agent: `ssh-add -l`

### Erro: "Repository not found"
- Verifique se o repositório foi criado no GitHub
- Confirme o nome: `demojibake` (não `demojibakelizator`)
- Verifique se está na organização `growthfolio`

### Erro: "Git not found"
- Instale Git: https://git-scm.com/download/win
- Adicione ao PATH do Windows
- Reinicie PowerShell

## Próximos Passos

Após o setup:
1. Configure GitHub Actions secrets se necessário
2. Crie primeira release: `git tag v1.0.0 && git push origin v1.0.0`
3. Teste build: `make build` (requer Go instalado)
4. Configure branch protection rules
5. Adicione colaboradores se necessário