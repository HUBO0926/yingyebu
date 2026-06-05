$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..\..")
$outDir = Join-Path $root "dist"
$packageDir = Join-Path $outDir "product-show-windows-installer"
$zipPath = Join-Path $outDir "product-show-windows-installer.zip"

if (Test-Path $packageDir) { Remove-Item -Recurse -Force $packageDir }
New-Item -ItemType Directory -Force -Path $packageDir | Out-Null

Copy-Item (Join-Path $PSScriptRoot "install.ps1") $packageDir
Copy-Item (Join-Path $PSScriptRoot "start-installer.cmd") $packageDir
Copy-Item (Join-Path $PSScriptRoot "README.md") $packageDir

if (Test-Path $zipPath) { Remove-Item -Force $zipPath }
Compress-Archive -Path (Join-Path $packageDir "*") -DestinationPath $zipPath

Write-Host "Installer package created: $zipPath"
