param(
    [string]$VpsIp = "57.156.60.79",
    [string]$VpsUser = "julio"
)

$ErrorActionPreference = "Stop"
$ProyectoDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "=== Desplegando DEDTE Mail System (Grupo #11) en $VpsUser@$VpsIp ===" -ForegroundColor Cyan

# 1. Configurar llave SSH si no existe (para pedir la contraseña solo una vez)
$sshKey = "$env:USERPROFILE\.ssh\id_ed25519"
if (-not (Test-Path "$sshKey.pub")) {
    New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.ssh" | Out-Null
    ssh-keygen -t ed25519 -N '""' -f $sshKey -q
}

Write-Host "`n[1/4] Instalando clave SSH en el VPS (ingresa tu contraseña de Azure una sola vez)..." -ForegroundColor Yellow
$pubKey = Get-Content "$sshKey.pub" -Raw
ssh -o StrictHostKeyChecking=accept-new "$VpsUser@$VpsIp" "mkdir -p ~/.ssh ~/dedte-mail/sql && chmod 700 ~/.ssh && echo '$($pubKey.Trim())' >> ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys"

Write-Host "`n[2/4] Subiendo JAR ejecutable, config.properties y scripts..." -ForegroundColor Yellow
scp "$ProyectoDir\target\dedte-mail-system-1.0.0-jar-with-dependencies.jar" `
    "$ProyectoDir\config.properties" `
    "$ProyectoDir\setup_vps.sh" `
    "$ProyectoDir\test_8_cus.sh" `
    "$VpsUser@${VpsIp}:~/dedte-mail/"

scp "$ProyectoDir\sql\01_schema_dedte.sql" `
    "$ProyectoDir\sql\02_seed_dedte.sql" `
    "$VpsUser@${VpsIp}:~/dedte-mail/sql/"

Write-Host "`n[3/4] Normalizando saltos de línea y ejecutando setup + pruebas de los 8 CUs en el VPS..." -ForegroundColor Yellow
ssh "$VpsUser@$VpsIp" "sed -i 's/\r$//' ~/dedte-mail/setup_vps.sh ~/dedte-mail/test_8_cus.sh && bash ~/dedte-mail/setup_vps.sh"

Write-Host "`n[4/4] Despliegue y pruebas en VPS completados." -ForegroundColor Green
