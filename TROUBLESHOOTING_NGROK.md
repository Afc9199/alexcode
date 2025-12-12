# 🔧 Ngrok 故障排除指南

## ❌ ERR_NGROK_3200: 端点离线

这个错误表示 ngrok 隧道已断开或本地应用未运行。

## 🔍 快速诊断

### 方法 1：使用诊断脚本

运行诊断脚本检查状态：

```bash
.\check-ngrok-status.bat
```

### 方法 2：手动检查

#### 1. 检查 Spring Boot 应用是否运行

**Windows:**
```bash
netstat -ano | findstr :8081
```

**Mac/Linux:**
```bash
lsof -i :8081
```

**如果端口未被占用：**
- 应用没有运行
- 需要启动应用：`mvn spring-boot:run`

#### 2. 检查 ngrok 是否运行

**Windows:**
```bash
tasklist | findstr ngrok
```

**Mac/Linux:**
```bash
ps aux | grep ngrok
```

**如果 ngrok 未运行：**
- 需要重新启动：`ngrok http 8081`

#### 3. 检查 ngrok 状态页面

访问：http://localhost:4040

如果无法访问，说明 ngrok 没有运行。

## 🚀 解决步骤

### 步骤 1：确保 Spring Boot 应用正在运行

在第一个终端窗口：

```bash
# 进入项目目录
cd "C:\Users\alexf\Videos\employee-management V4\employee-management(latest)\employee-management"

# 启动应用
mvn spring-boot:run
```

**等待看到类似输出：**
```
Started EmployeeManagementApplication in X.XXX seconds
```

### 步骤 2：启动 ngrok

在第二个终端窗口：

```bash
# 方式 A: 使用启动脚本
.\start-ngrok.bat

# 方式 B: 手动启动
ngrok http 8081
```

**等待看到类似输出：**
```
Forwarding   https://xxxx-xxxx-xxxx.ngrok-free.app -> http://localhost:8081
```

### 步骤 3：验证连接

1. **检查本地应用：**
   - 访问：http://localhost:8081
   - 应该能看到登录页面

2. **检查 ngrok 状态：**
   - 访问：http://localhost:4040
   - 查看隧道状态和请求日志

3. **使用新的 ngrok URL：**
   - 复制 ngrok 提供的新 URL
   - 在浏览器中访问

## ⚠️ 常见问题

### 问题 1：应用启动失败

**可能原因：**
- 端口 8081 被占用
- MongoDB 连接失败
- 依赖问题

**解决方案：**
```bash
# 检查端口占用
netstat -ano | findstr :8081

# 如果被占用，杀死进程或更改端口
# 在 application.yml 中修改 server.port
```

### 问题 2：ngrok 无法启动

**可能原因：**
- ngrok 未安装
- authtoken 未配置
- 网络问题

**解决方案：**
```bash
# 检查 ngrok 是否安装
ngrok version

# 如果未安装，下载：https://ngrok.com/download

# 配置 authtoken（推荐）
ngrok config add-authtoken YOUR_TOKEN
```

### 问题 3：ngrok URL 变化

**这是正常行为（免费计划）：**
- 每次重启 ngrok，URL 都会变化
- 这是免费计划的限制

**解决方案：**
- 升级到付费计划可获得固定域名
- 或每次重启后使用新 URL

### 问题 4：连接超时

**可能原因：**
- 本地防火墙阻止连接
- 网络问题
- ngrok 服务问题

**解决方案：**
1. 检查防火墙设置
2. 尝试重启 ngrok
3. 检查网络连接

## 🔄 完整重启流程

如果遇到问题，按以下顺序重启：

### 1. 停止所有进程

**Windows:**
```bash
# 停止 Spring Boot（在运行窗口按 Ctrl+C）
# 停止 ngrok（在运行窗口按 Ctrl+C）

# 或强制停止
taskkill /F /IM java.exe
taskkill /F /IM ngrok.exe
```

**Mac/Linux:**
```bash
# 停止 Spring Boot（在运行窗口按 Ctrl+C）
# 停止 ngrok（在运行窗口按 Ctrl+C）

# 或强制停止
pkill -f "spring-boot"
pkill -f ngrok
```

### 2. 重新启动

**终端 1 - Spring Boot:**
```bash
mvn spring-boot:run
```

**等待应用完全启动后，在终端 2 - ngrok:**
```bash
ngrok http 8081
```

### 3. 验证

1. 检查本地：http://localhost:8081
2. 检查 ngrok 状态：http://localhost:4040
3. 使用新的 ngrok URL

## 📝 检查清单

在报告问题前，请确认：

- [ ] Spring Boot 应用正在运行（端口 8081）
- [ ] ngrok 进程正在运行
- [ ] 可以访问 http://localhost:8081
- [ ] 可以访问 http://localhost:4040（ngrok 状态页）
- [ ] 使用最新的 ngrok URL（不是旧的）
- [ ] 浏览器控制台没有其他错误

## 🆘 仍然无法解决？

如果以上步骤都无法解决问题：

1. **查看详细日志：**
   - Spring Boot 控制台输出
   - ngrok 控制台输出
   - 浏览器控制台（F12）

2. **检查网络：**
   - 确保网络连接正常
   - 检查防火墙/代理设置

3. **尝试其他端口：**
   ```bash
   # 修改 application.yml 中的端口
   server.port: 8082
   
   # 然后使用新端口启动 ngrok
   ngrok http 8082
   ```

4. **重新安装 ngrok：**
   - 下载最新版本
   - 重新配置 authtoken

---

**提示：** 保持两个终端窗口打开，一个运行 Spring Boot，一个运行 ngrok。

