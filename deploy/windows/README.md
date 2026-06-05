# Windows 一键部署包

这个目录用于把系统部署到 Windows 服务器。实际运行方式是：

Windows Server -> WSL2 Ubuntu -> Docker Compose -> 项目容器。

## 适用环境

推荐：

- Windows Server 2022/2025
- 已开启虚拟化
- 云服务器安全组开放 `80`、`443`、远程登录端口
- 两个已解析到服务器公网 IP 的域名：
  - `api.example.com`
  - `admin.example.com`

如果可以重装系统，生产部署仍优先推荐 Ubuntu Server 22.04/24.04。

## 使用方式

1. 将 `install.ps1` 上传到 Windows 服务器。
2. 右键 PowerShell，选择“以管理员身份运行”。
3. 右键 `start-installer.cmd`，选择“以管理员身份运行”。

如果 Windows 拦截脚本执行，也可以进入脚本所在目录后手动执行：

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\install.ps1
```

脚本会自动：

- 检查 WSL2。
- 安装或使用 `Ubuntu-22.04`。
- 在 WSL Ubuntu 内安装 Git 和 Docker。
- 从 GitHub 拉取 `HUBO0926/yingyebu`。
- 生成服务器 `.env`。
- 执行 `docker compose up --build -d`。
- 输出 API 和后台访问地址。

如果安装 WSL 后提示重启，请重启服务器后再次运行 `install.ps1`。

## 生成 zip 安装包

在本地仓库根目录执行：

```powershell
.\deploy\windows\package.ps1
```

生成：

```text
dist/product-show-windows-installer.zip
```

## 部署后的常用命令

进入 WSL：

```powershell
wsl -d Ubuntu-22.04
```

进入项目目录：

```bash
cd ~/product-show-system
```

查看容器：

```bash
sudo docker compose ps
```

查看后端日志：

```bash
sudo docker compose logs -f backend
```

重启服务：

```bash
sudo docker compose restart
```

更新代码：

```bash
git pull
sudo docker compose up --build -d
```

## 注意事项

- 不要执行 `docker compose down -v`，这会删除 MySQL 数据卷。
- `.env` 只保存在服务器，不要提交到 Git。
- 微信小程序体验版必须使用 HTTPS API 域名。
- Windows 防火墙和云安全组都要放行 `80`、`443`。
