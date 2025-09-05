# Script para inicializar repositório Git e fazer push inicial
# Execute após configurar SSH: .\init-repository.ps1

Write-Host "Inicializando repositório Git..." -ForegroundColor Green

# Inicializar repositório
git init

# Adicionar remote origin
git remote add origin git@github.com:growthfolio/demojibake.git

# Criar .gitignore se não existir
if (!(Test-Path ".gitignore")) {
    Write-Host "Criando .gitignore..." -ForegroundColor Yellow
}

# Adicionar todos os arquivos
git add .

# Commit inicial
git commit -m "feat: implementação inicial do Demojibakelizador

- CLI completo com detecção e conversão de encoding
- GUI usando Fyne para interface amigável  
- Heurística de correção de mojibake
- Suporte multi-plataforma (Windows, Linux, macOS)
- Docker e CI/CD com GitHub Actions
- Documentação completa e samples de teste"

# Criar e fazer push da branch main
git branch -M main
git push -u origin main

Write-Host "`nRepositório inicializado e enviado para GitHub!" -ForegroundColor Green
Write-Host "URL: https://github.com/growthfolio/demojibake" -ForegroundColor Cyan