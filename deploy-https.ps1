# Deploy via HTTPS para GitHub
Write-Host "Enviando código via HTTPS..." -ForegroundColor Green

# Inicializar Git
git init
git branch -M main

# Adicionar remote via HTTPS
git remote add origin https://github.com/growthfolio/demojibake.git

# Adicionar arquivos
git add .

# Commit inicial
git commit -m "feat: implementação completa do Demojibakelizador

✨ Features:
- CLI robusto com detecção automática de encoding
- GUI amigável usando Fyne
- Correção inteligente de mojibake (UTF-8 → Latin-1)
- Processamento em lote com concorrência
- Operações atômicas com backup automático
- Suporte multi-plataforma (Windows/Linux/macOS)
- Docker e CI/CD pipeline
- Documentação completa + samples de teste

🛠️ Tecnologias:
- Go 1.21+
- Fyne v2 (GUI)
- saintfish/chardet (detecção)
- golang.org/x/text (conversão)

📦 Pronto para produção corporativa!"

# Push para GitHub (vai pedir usuário/senha)
Write-Host "Fazendo push... (será solicitado usuário e senha/token)" -ForegroundColor Yellow
git push -u origin main

Write-Host "✅ Código enviado para: https://github.com/growthfolio/demojibake" -ForegroundColor Green