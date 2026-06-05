# Windows Installer

This package deploys Product Show System on Windows Server by using:

Windows Server -> WSL2 Ubuntu -> Docker Compose -> Linux containers.

The installer text is intentionally written in English ASCII to avoid PowerShell encoding issues on Windows Server.

## Requirements

- Windows Server 2022/2025 is recommended.
- Virtualization must be enabled by the cloud provider.
- Cloud security group must allow `80` and `443`.
- Windows Firewall must allow `80` and `443`.
- Two domains must point to this server public IP:
  - API domain, for example `api.example.com`
  - Admin domain, for example `admin.example.com`

If you can reinstall the server OS, Ubuntu Server 22.04/24.04 is still the simplest production option.

## Important Upgrade Note

If an older installer failed with `ParserError`, delete the old extracted folder first:

```text
C:\Users\Administrator\Desktop\product-show-windows-installer
```

Then extract the new zip package again.

## How To Run

1. Extract `product-show-windows-installer.zip` on the Windows server.
2. Right-click `start-installer.cmd`.
3. Choose "Run as administrator".
4. Follow the prompts.

If Windows blocks script execution, open PowerShell as Administrator in this folder and run:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\install.ps1
```

The installer will:

- Check WSL2.
- Install or reuse `Ubuntu-22.04`.
- Install Git and Docker inside WSL Ubuntu.
- Clone `HUBO0926/yingyebu`.
- Generate server `.env`.
- Run `docker compose up --build -d`.
- Print API and admin URLs.

If WSL asks for a restart, restart the server and run `start-installer.cmd` again.

## Build The Zip Package

From the repository root:

```powershell
.\deploy\windows\package.ps1
```

Output:

```text
dist/product-show-windows-installer.zip
```

## Useful Commands After Deployment

Enter WSL:

```powershell
wsl -d Ubuntu-22.04
```

Open project directory:

```bash
cd ~/product-show-system
```

Show containers:

```bash
sudo docker compose ps
```

View backend logs:

```bash
sudo docker compose logs -f backend
```

Restart services:

```bash
sudo docker compose restart
```

Update deployment:

```bash
git pull
sudo docker compose up --build -d
```

Do not run `docker compose down -v` on production. It deletes MySQL data volumes.
