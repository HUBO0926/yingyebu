param(
  [string]$RepoUrl = "https://github.com/HUBO0926/yingyebu.git",
  [string]$Distro = "Ubuntu-22.04",
  [string]$LinuxInstallDir = "$HOME/product-show-system"
)

$ErrorActionPreference = "Stop"

function Assert-Admin {
  $identity = [Security.Principal.WindowsIdentity]::GetCurrent()
  $principal = New-Object Security.Principal.WindowsPrincipal($identity)
  if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw "Please run PowerShell as Administrator and start this installer again."
  }
}

function New-Secret([int]$Length = 32) {
  $bytes = New-Object byte[] $Length
  [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
  $value = [Convert]::ToBase64String($bytes)
  return $value.Replace("+", "A").Replace("/", "B").Replace("=", "")
}

function Read-Value([string]$Prompt, [string]$Default = "", [bool]$AllowEmpty = $false) {
  if ($Default) {
    $value = Read-Host "$Prompt [$Default]"
    if ([string]::IsNullOrWhiteSpace($value)) { return $Default }
    return $value.Trim()
  }
  do {
    $value = Read-Host $Prompt
    if ($AllowEmpty) {
      if ($null -eq $value) { return "" }
      return $value.Trim()
    }
  } while ([string]::IsNullOrWhiteSpace($value))
  return $value.Trim()
}

function Test-Command([string]$Name) {
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

Assert-Admin

Write-Host ""
Write-Host "Product Show System - Windows Installer" -ForegroundColor Cyan
Write-Host "Deployment mode: Windows Server + WSL2 Ubuntu + Docker Compose"
Write-Host ""

if (-not (Test-Command "wsl.exe")) {
  throw "wsl.exe was not found. Please use Windows Server 2022/2025 with WSL2, or deploy on Ubuntu Server."
}

$installedDistros = (wsl.exe -l -q) | ForEach-Object { $_.Trim([char]0xFEFF).Trim() } | Where-Object { $_ }
if ($installedDistros -notcontains $Distro) {
  Write-Host "$Distro was not found. Installing WSL distribution now." -ForegroundColor Yellow
  Write-Host "If Windows asks for a restart, restart the server and run this installer again." -ForegroundColor Yellow
  wsl.exe --install -d $Distro
  Write-Host "WSL install command finished. Restart if required, then run this installer again." -ForegroundColor Yellow
  exit 0
}

wsl.exe --set-default-version 2 | Out-Null
try {
  wsl.exe --set-version $Distro 2 | Out-Null
} catch {
  Write-Host "Skipped WSL version conversion: $($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Please enter deployment settings. Domains must already point to this server public IP." -ForegroundColor Cyan
$apiDomain = Read-Value "API domain, for example api.example.com" "api.example.com"
$adminDomain = Read-Value "Admin domain, for example admin.example.com" "admin.example.com"
$acmeEmail = Read-Value "TLS certificate email" "admin@example.com"
$wechatAppId = Read-Value "WeChat Mini Program AppID, optional" "" $true
$wechatSecret = Read-Value "WeChat Mini Program AppSecret, optional" "" $true

$mysqlPassword = New-Secret 24
$mysqlRootPassword = New-Secret 24
$saTokenSecret = New-Secret 48
$minioPassword = New-Secret 24

$envContent = @"
MYSQL_DATABASE=product_show
MYSQL_USER=product_show
MYSQL_PASSWORD=$mysqlPassword
MYSQL_ROOT_PASSWORD=$mysqlRootPassword
REDIS_PASSWORD=
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=$minioPassword
SA_TOKEN_SECRET=$saTokenSecret
WECHAT_APPID=$wechatAppId
WECHAT_SECRET=$wechatSecret
API_DOMAIN=$apiDomain
ADMIN_DOMAIN=$adminDomain
ACME_EMAIL=$acmeEmail
ADMIN_WEB_BASE_URL=https://$adminDomain
PUBLIC_API_BASE_URL=https://$apiDomain/api
AI_PROVIDER=
AI_CHAT_MODEL=
AI_EMBEDDING_MODEL=
DEEPSEEK_API_KEY=
GEMINI_API_KEY=
OPENAI_API_KEY=
"@

$tempDir = Join-Path $env:TEMP "product-show-installer"
New-Item -ItemType Directory -Force -Path $tempDir | Out-Null
$envFile = Join-Path $tempDir ".env"
$deployScript = Join-Path $tempDir "deploy-in-wsl.sh"
Set-Content -Path $envFile -Value $envContent -Encoding UTF8

$linuxScript = @'
#!/usr/bin/env bash
set -euo pipefail

REPO_URL="$1"
INSTALL_DIR="$2"
ENV_SOURCE="$3"

echo ""
echo "==> Installing base packages"
sudo apt-get update
sudo apt-get install -y git ca-certificates curl

if ! command -v docker >/dev/null 2>&1; then
  echo "==> Installing Docker Engine"
  curl -fsSL https://get.docker.com | sudo sh
fi

echo "==> Starting Docker"
if command -v systemctl >/dev/null 2>&1 && systemctl list-unit-files docker.service >/dev/null 2>&1; then
  sudo systemctl enable --now docker || sudo service docker start
else
  sudo service docker start || true
fi

if ! sudo docker version >/dev/null 2>&1; then
  echo "Docker did not start correctly. Check WSL2 and Windows firewall/security software."
  exit 1
fi

mkdir -p "$(dirname "$INSTALL_DIR")"
if [ -d "$INSTALL_DIR/.git" ]; then
  echo "==> Updating repository"
  git -C "$INSTALL_DIR" pull --ff-only
else
  echo "==> Cloning repository"
  git clone "$REPO_URL" "$INSTALL_DIR"
fi

cp "$ENV_SOURCE" "$INSTALL_DIR/.env"
chmod 600 "$INSTALL_DIR/.env"

echo "==> Building and starting services"
cd "$INSTALL_DIR"
sudo docker compose up --build -d

echo "==> Container status"
sudo docker compose ps

API_DOMAIN_VALUE="$(grep '^API_DOMAIN=' .env | cut -d= -f2)"
ADMIN_DOMAIN_VALUE="$(grep '^ADMIN_DOMAIN=' .env | cut -d= -f2)"

echo ""
echo "Deployment complete."
echo "API health check: https://${API_DOMAIN_VALUE}/api/health"
echo "Admin web:        https://${ADMIN_DOMAIN_VALUE}"
echo "Backend logs:     cd $INSTALL_DIR && sudo docker compose logs -f backend"
'@

Set-Content -Path $deployScript -Value $linuxScript -Encoding UTF8

$envFileLinux = (wsl.exe -d $Distro -- wslpath -a "$envFile").Trim()
$deployScriptLinux = (wsl.exe -d $Distro -- wslpath -a "$deployScript").Trim()

Write-Host ""
Write-Host "Entering WSL deployment. Docker image build may take several minutes." -ForegroundColor Cyan
wsl.exe -d $Distro -- bash "$deployScriptLinux" "$RepoUrl" "$LinuxInstallDir" "$envFileLinux"

Write-Host ""
Write-Host "Reminder:" -ForegroundColor Yellow
Write-Host "Open ports 80 and 443 in both cloud security group and Windows Firewall."
Write-Host "Mini Program API: https://$apiDomain/api"
Write-Host "Admin web:        https://$adminDomain"
