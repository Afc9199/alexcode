# Ngrok 配置指南

## 快速开始

### 1. 启动应用程序

确保 Spring Boot 应用程序正在运行在端口 8081：

```bash
mvn spring-boot:run
```

或者如果已经打包：

```bash
java -jar target/employee-management-0.0.1-SNAPSHOT.jar
```

### 2. 启动 ngrok

在另一个终端窗口中运行：

```bash
ngrok http 8081
```

### 3. 访问应用程序

ngrok 会提供一个公共 URL，例如：
```
https://suety-natosha-partly.ngrok-free.dev
```

直接在浏览器中访问这个 URL 即可。

## 配置说明

### Cookie 设置

应用程序已配置为支持 ngrok：
- `COOKIE_SECURE=true` - 因为 ngrok 使用 HTTPS
- `COOKIE_SAME_SITE=none` - 允许跨域 cookie（ngrok 域名与 localhost 不同）

### CORS 配置

已添加 CORS 配置以允许所有来源（包括 ngrok 域名）：
- 允许所有 HTTP 方法
- 允许所有请求头
- 允许携带凭证（cookies）

### 环境变量（可选）

如果需要自定义配置，可以设置以下环境变量：

```bash
# Windows PowerShell
$env:PORT="8081"
$env:COOKIE_SECURE="true"
$env:COOKIE_SAME_SITE="none"

# Linux/Mac
export PORT=8081
export COOKIE_SECURE=true
export COOKIE_SAME_SITE=none
```

## 注意事项

1. **ngrok Free 计划限制**：
   - URL 每次重启都会变化
   - 有连接数限制
   - 可能需要等待页面加载（ngrok 警告页面）

2. **Session Cookie**：
   - 由于 ngrok 使用 HTTPS，cookie secure 标志已设置为 true
   - 这确保了 session 可以正常工作

3. **API 调用**：
   - 前端使用相对路径（如 `/api/auth/login`）
   - 会自动使用当前域名（ngrok URL）
   - 无需修改代码

4. **文件上传**：
   - 文件上传功能应该可以正常工作
   - 上传的文件会保存在本地 `uploads/` 目录

## 故障排除

### 问题：无法登录或 session 丢失

**解决方案**：
- 确保 `COOKIE_SECURE=true` 在 application.yml 中
- 清除浏览器 cookies 后重试
- 检查浏览器控制台是否有 CORS 错误

### 问题：API 调用失败

**解决方案**：
- 检查 ngrok 是否正在运行
- 确认应用程序运行在 8081 端口
- 查看浏览器网络标签页中的错误信息

### 问题：ngrok 显示警告页面

**解决方案**：
- 这是 ngrok Free 计划的正常行为
- 点击 "Visit Site" 按钮继续
- 或升级到付费计划以移除警告

## 测试

访问以下 URL 测试功能：
- 登录页面：`https://your-ngrok-url.ngrok-free.dev/login.html`
- 员工仪表板：`https://your-ngrok-url.ngrok-free.dev/employee-dashboard-summary.html`
- 管理员仪表板：`https://your-ngrok-url.ngrok-free.dev/admin-dashboard-summary.html`

