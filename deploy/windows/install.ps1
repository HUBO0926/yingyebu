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
    throw "请用管理员身份运行 PowerShell，然后重新执行本安装脚本。"
  }
}

function New-Secret([int]$Length = 32) {
  $bytes = New-Object byte[] $Length
  [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
  return [Convert]::ToBase64String($bytes).Replace("+", "A").Replace("/", "B").Replace("=", "")
}

function Read-Value([string]$Prompt, [string]$Default = "") {
  if ($Default) {
    $value = Read-Host "$Prompt [$Default]"
    if ([string]::IsNullOrWhiteSpace($value)) { return $Default }
    return $value.Trim()
  }
  do {
    $value = Read-Host $Prompt
  } while ([string]::IsNullOrWhiteSpace($value))
  return $value.Trim()
}

function Test-Command([string]$Name) {
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

Assert-Admin

Write-Host ""
Write-Host "产品展示与智能报价系统 - Windows 一键部署" -ForegroundColor Cyan
Write-Host "本脚本会使用 WSL2 Ubuntu + Docker Compose 部署 Linux 容器。"
Write-Host ""

if (-not (Test-Command "wsl.exe")) {
  throw "当前系统没有 wsl.exe。请确认 Windows Server 版本支持 WSL2，或改用 Ubuntu 服务器部署。"
}

$installedDistros = (wsl.exe -l -q) | ForEach-Object { $_.Trim([char]0xFEFF).Trim() } | Where-Object { $_ }
if ($installedDistros -notcontains $Distro) {
  Write-Host "未发现 $Distro，开始安装。安装完成后如果系统提示重启，请重启后再次运行本脚本。" -ForegroundColor Yellow
  wsl.exe --install -d $Distro
  Write-Host "WSL 安装命令已执行。如果刚才提示重启，请重启服务器后重新运行本脚本。" -ForegroundColor Yellow
  exit 0
}

wsl.exe --set-default-version 2 | Out-Null
try {
  wsl.exe --set-version $Distro 2 | Out-Null
} catch {
  Write-Host "WSL 版本设置跳过：$($_.Exception.Message)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "请填写部署域名和微信配置。域名必须已经解析到本服务器公网 IP。" -ForegroundColor Cyan
$apiDomain = Read-Value "API 域名，例如 api.example.com" "api.example.com"
$adminDomain = Read-Value "后台域名，例如 admin.example.com" "admin.example.com"
$acmeEmail = Read-Value "证书申请邮箱" "admin@example.com"
$wechatAppId = Read-Value "微信小程序 AppID，可先留空" ""
$wechatSecret = Read-Value "微信小程序 AppSecret，可先留空" ""

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
echo "==> 安装基础依赖"
sudo apt-get update
sudo apt-get install -y git ca-certificates curl

if ! command -v docker >/dev/null 2>&1; then
  echo "==> 安装 Docker Engine"
  curl -fsSL https://get.docker.com | sudo sh
fi

echo "==> 启动 Docker"
if command -v systemctl >/dev/null 2>&1 && systemctl list-unit-files docker.service >/dev/null 2>&1; then
  sudo systemctl enable --now docker || sudo service docker start
else
  sudo service docker start || true
fi

if ! sudo docker version >/dev/null 2>&1; then
  echo "Docker 未能正常启动。请确认 WSL2 可用，并在 Windows 防火墙/安全软件中允许 Docker。"
  exit 1
fi

mkdir -p "$(dirname "$INSTALL_DIR")"
if [ -d "$INSTALL_DIR/.git" ]; then
  echo "==> 更新代码"
  git -C "$INSTALL_DIR" pull --ff-only
else
  echo "==> 克隆代码"
  git clone "$REPO_URL" "$INSTALL_DIR"
fi

cp "$ENV_SOURCE" "$INSTALL_DIR/.env"
chmod 600 "$INSTALL_DIR/.env"

echo "==> 构建并启动服务"
cd "$INSTALL_DIR"
sudo docker compose up --build -d

echo "==> 容器状态"
sudo docker compose ps

echo ""
echo "部署完成。"
echo "API 健康检查：    https://$(grep '^API_DOMAIN=' .env | cut -d= -f2)/api/health"
echo "管理后台地址：    https://$(grep '^ADMIN_DOMAIN=' .env | cut -d= -f2)"
echo "查看后端日志：    cd $INSTALL_DIR && sudo docker compose logs -f backend"
'@

Set-Content -Path $deployScript -Value $linuxScript -Encoding UTF8

$envFileLinux = (wsl.exe -d $Distro -- wslpath -a "$envFile").Trim()
$deployScriptLinux = (wsl.exe -d $Distro -- wslpath -a "$deployScript").Trim()

Write-Host ""
Write-Host "开始进入 WSL 部署，请等待 Docker 镜像构建完成。" -ForegroundColor Cyan
wsl.exe -d $Distro -- bash "$deployScriptLinux" "$RepoUrl" "$LinuxInstallDir" "$envFileLinux"

Write-Host ""
Write-Host "Windows 防火墙提醒：" -ForegroundColor Yellow
Write-Host "请确认云服务器安全组和 Windows 防火墙已开放 80、443。"
Write-Host "小程序体验版 API 地址：https://$apiDomain/api"
Write-Host "管理后台地址：https://$adminDomain"
