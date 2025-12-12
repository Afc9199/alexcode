# 🚀 Ngrok 快速启动指南

## 📋 前置要求

1. **安装 ngrok**
   - 下载：https://ngrok.com/download
   - Windows: 下载 `ngrok.exe`，放到 PATH 环境变量中，或放在项目根目录
   - Mac/Linux: 下载对应版本，或使用包管理器安装

2. **注册 ngrok 账号（可选但推荐）**
   - 访问 https://dashboard.ngrok.com/signup
   - 注册后获取 authtoken
   - 运行：`ngrok config add-authtoken YOUR_TOKEN`

## 🎯 快速启动步骤

### 方法 1：使用启动脚本（推荐）

#### Windows:
```bash
# 双击运行或在 PowerShell 中执行
.\start-ngrok.bat
```

#### Mac/Linux:
```bash
# 添加执行权限
chmod +x start-ngrok.sh

# 运行脚本
./start-ngrok.sh
```

### 方法 2：手动启动

#### 1. 启动 Spring Boot 应用

在第一个终端窗口：

```bash
# 方式 A: 使用 Maven
mvn spring-boot:run

# 方式 B: 如果已打包
java -jar target/employee-management-0.0.1-SNAPSHOT.jar
```

确保应用运行在 **端口 8081**（默认配置）

#### 2. 启动 ngrok 隧道

在第二个终端窗口：

```bash
ngrok http 8081
```

#### 3. 获取公共 URL

ngrok 启动后会显示类似信息：

```
Forwarding   https://xxxx-xxxx-xxxx.ngrok-free.app -> http://localhost:8081
```

**复制这个 HTTPS URL**（例如：`https://xxxx-xxxx-xxxx.ngrok-free.app`）

#### 4. 访问应用

在浏览器中打开 ngrok 提供的 URL：

- 登录页面：`https://your-ngrok-url.ngrok-free.app/login.html`
- 员工仪表板：`https://your-ngrok-url.ngrok-free.app/employee-dashboard-summary.html`
- 管理员仪表板：`https://your-ngrok-url.ngrok-free.app/admin-dashboard-summary.html`

## ⚙️ 配置说明

### 已自动配置的项目

✅ **Cookie 设置**：
- `COOKIE_SECURE=true` - 支持 HTTPS
- `COOKIE_SAME_SITE=none` - 允许跨域 cookie

✅ **CORS 配置**：
- 已允许所有来源（包括 ngrok 域名）
- 允许携带凭证（cookies）

✅ **端口配置**：
- 默认端口：8081
- 可通过环境变量 `PORT` 修改

### 环境变量（可选）

如果需要自定义配置：

```bash
# Windows PowerShell
$env:PORT="8081"
$env:COOKIE_SECURE="true"
$env:COOKIE_SAME_SITE="none"

# Windows CMD
set PORT=8081
set COOKIE_SECURE=true
set COOKIE_SAME_SITE=none

# Mac/Linux
export PORT=8081
export COOKIE_SECURE=true
export COOKIE_SAME_SITE=none
```

## 🔍 验证步骤

1. **检查应用是否运行**：
   - 访问：http://localhost:8081
   - 应该能看到登录页面

2. **检查 ngrok 状态**：
   - 访问：http://localhost:4040（ngrok 本地管理界面）
   - 查看请求日志和统计信息

3. **测试登录功能**：
   - 使用 ngrok URL 访问登录页面
   - 尝试登录，检查 session 是否正常

## ⚠️ 注意事项

### ngrok Free 计划限制

- ✅ URL 每次重启都会变化（免费计划）
- ✅ 有连接数限制
- ✅ 可能需要等待页面加载（ngrok 警告页面）
- ✅ 点击 "Visit Site" 按钮继续访问

### 升级到付费计划（可选）

如果需要固定域名和更高性能：
1. 访问 https://dashboard.ngrok.com/pricing
2. 选择付费计划
3. 配置固定域名：`ngrok config add-domain your-domain.ngrok.app`

## 🐛 故障排除

### 问题 1：无法访问应用

**检查清单**：
- [ ] Spring Boot 应用是否在运行？
- [ ] 应用是否运行在端口 8081？
- [ ] ngrok 是否正在运行？
- [ ] 浏览器控制台是否有错误？

**解决方案**：
```bash
# 检查端口占用
netstat -ano | findstr :8081  # Windows
lsof -i :8081                 # Mac/Linux

# 检查 ngrok 状态
# 访问 http://localhost:4040
```

### 问题 2：无法登录或 session 丢失

**解决方案**：
1. 清除浏览器 cookies
2. 确保使用 HTTPS URL（不是 HTTP）
3. 检查浏览器控制台的 CORS 错误
4. 确认 `application.yml` 中 `COOKIE_SECURE=true`

### 问题 3：API 调用失败

**解决方案**：
1. 检查浏览器网络标签页的错误信息
2. 确认 API 路径使用相对路径（如 `/api/auth/login`）
3. 检查 ngrok 日志：http://localhost:4040

### 问题 4：ngrok 显示警告页面

这是 **正常行为**（免费计划）：
- 点击 "Visit Site" 按钮继续
- 或升级到付费计划以移除警告

## 📱 移动设备测试

ngrok URL 可以在移动设备上访问：

1. 确保手机和电脑在同一网络（或使用移动数据）
2. 在手机浏览器中输入 ngrok URL
3. 测试响应式设计和移动端功能

## 🔐 安全建议

⚠️ **重要**：ngrok 会将你的本地应用暴露到公网

1. **不要在生产环境使用 ngrok**
2. **测试完成后及时关闭 ngrok**
3. **不要在 ngrok URL 上使用真实的生产数据**
4. **考虑使用 ngrok 的 IP 限制功能**（付费计划）

## 📞 需要帮助？

如果遇到问题：
1. 查看详细文档：`NGROK_SETUP.md`
2. 检查 ngrok 官方文档：https://ngrok.com/docs
3. 查看应用日志和浏览器控制台

---

**祝使用愉快！** 🎉

