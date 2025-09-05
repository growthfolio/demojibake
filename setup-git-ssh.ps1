# Script para configurar Git SSH no Windows
# Execute como: .\setup-git-ssh.ps1

Write-Host "Configurando Git SSH para GitHub..." -ForegroundColor Green

# Configurar usuário e email do Git
git config --global user.name "felipemacedo1"
git config --global user.email "felipealexandrej@gmail.com"

Write-Host "Usuário Git configurado: felipemacedo1" -ForegroundColor Yellow
Write-Host "Email Git configurado: felipealexandrej@gmail.com" -ForegroundColor Yellow

# Verificar se já existe chave SSH
$sshDir = "$env:USERPROFILE\.ssh"
$keyPath = "$sshDir\id_rsa"

if (Test-Path $keyPath) {
    Write-Host "Chave SSH já existe em: $keyPath" -ForegroundColor Yellow
    $overwrite = Read-Host "Deseja sobrescrever? (y/N)"
    if ($overwrite -ne "y") {
        Write-Host "Usando chave SSH existente..." -ForegroundColor Green
        Get-Content "$keyPath.pub"
        Write-Host "`nCopie a chave acima e adicione em: https://github.com/settings/ssh" -ForegroundColor Cyan
        exit
    }
}

# Criar diretório .ssh se não existir
if (!(Test-Path $sshDir)) {
    New-Item -ItemType Directory -Path $sshDir -Force
}

# Gerar nova chave SSH
Write-Host "Gerando nova chave SSH..." -ForegroundColor Green
ssh-keygen -t rsa -b 4096 -C "felipealexandrej@gmail.com" -f $keyPath -N '""'

# Iniciar ssh-agent
Write-Host "Iniciando ssh-agent..." -ForegroundColor Green
Start-Service ssh-agent
ssh-add $keyPath

# Mostrar chave pública
Write-Host "`n=== CHAVE PÚBLICA SSH ===" -ForegroundColor Cyan
Get-Content "$keyPath.pub"
Write-Host "=========================" -ForegroundColor Cyan

# Copiar para clipboard se possível
try {
    Get-Content "$keyPath.pub" | Set-Clipboard
    Write-Host "`nChave copiada para clipboard!" -ForegroundColor Green
} catch {
    Write-Host "`nNão foi possível copiar automaticamente." -ForegroundColor Yellow
}

Write-Host "`nPróximos passos:" -ForegroundColor Green
Write-Host "1. Acesse: https://github.com/settings/ssh" -ForegroundColor White
Write-Host "2. Clique em 'New SSH key'" -ForegroundColor White
Write-Host "3. Cole a chave pública acima" -ForegroundColor White
Write-Host "4. Teste com: ssh -T git@github.com" -ForegroundColor White

# Testar conexão
$test = Read-Host "`nDeseja testar a conexão SSH agora? (y/N)"
if ($test -eq "y") {
    Write-Host "Testando conexão SSH..." -ForegroundColor Green
    ssh -T git@github.com
}
