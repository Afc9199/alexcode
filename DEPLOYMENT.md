# Google Cloud Run 部署指南

本指南将帮助你将员工管理系统部署到 Google Cloud Run。

## 📋 前置要求

1. **Google Cloud 账号**：如果没有，请访问 [Google Cloud Console](https://console.cloud.google.com/) 注册
2. **Google Cloud CLI (gcloud)**：安装并配置
   ```bash
   # Windows (使用 Chocolatey)
   choco install gcloudsdk
   
   # 或下载安装包
   # https://cloud.google.com/sdk/docs/install
   ```
3. **Docker**：用于本地测试（可选）
4. **Maven**：用于构建项目

## 🚀 快速部署步骤

### 1. 初始化 Google Cloud 项目

```bash
# 登录 Google Cloud
gcloud auth login

# 设置项目 ID（替换为你的项目 ID）
gcloud config set project YOUR_PROJECT_ID

# 启用必要的 API
gcloud services enable cloudbuild.googleapis.com
gcloud services enable run.googleapis.com
gcloud services enable artifactregistry.googleapis.com
```

### 2. 创建 Artifact Registry 仓库（用于存储 Docker 镜像）

```bash
# 设置变量
export REGION=asia-southeast1  # 或选择其他区域
export REPO_NAME=employee-management
export SERVICE_NAME=employee-management

# 创建仓库
gcloud artifacts repositories create $REPO_NAME \
    --repository-format=docker \
    --location=$REGION \
    --description="Employee Management Docker images"
```

### 3. 配置环境变量

在 Google Cloud Console 中设置环境变量，或使用 gcloud 命令：

```bash
# 设置 MongoDB 连接（使用你的实际 MongoDB URI）
gcloud run services update $SERVICE_NAME \
    --region=$REGION \
    --set-env-vars="MONGODB_URI=mongodb+srv://user:password@cluster.mongodb.net/database"

# 设置 Gemini API Key
gcloud run services update $SERVICE_NAME \
    --region=$REGION \
    --set-env-vars="GEMINI_API_KEY=your-api-key-here"

# 设置考勤位置（可选，如果与默认值不同）
gcloud run services update $SERVICE_NAME \
    --region=$REGION \
    --set-env-vars="ATTENDANCE_LATITUDE=3.201388,ATTENDANCE_LONGITUDE=101.71495"

# 设置 Cookie Secure（Cloud Run 使用 HTTPS，应设为 true）
gcloud run services update $SERVICE_NAME \
    --region=$REGION \
    --set-env-vars="COOKIE_SECURE=true"
```

### 4. 构建并部署

#### 方法 A：使用 Cloud Build（推荐）

```bash
# 提交代码到 Git 仓库（GitHub/GitLab/Bitbucket）
# 然后在 Cloud Console 中设置 Cloud Build 触发器

# 或手动触发构建
gcloud builds submit --config=cloudbuild.yaml \
    --substitutions=_REGION=$REGION,_REPO_NAME=$REPO_NAME,_SERVICE_NAME=$SERVICE_NAME
```

#### 方法 B：本地构建并推送

```bash
# 1. 构建 JAR 文件
mvn clean package -DskipTests

# 2. 构建 Docker 镜像
docker build -t $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$SERVICE_NAME:latest .

# 3. 配置 Docker 认证
gcloud auth configure-docker $REGION-docker.pkg.dev

# 4. 推送镜像
docker push $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$SERVICE_NAME:latest

# 5. 部署到 Cloud Run
gcloud run deploy $SERVICE_NAME \
    --image $REGION-docker.pkg.dev/$PROJECT_ID/$REPO_NAME/$SERVICE_NAME:latest \
    --region $REGION \
    --platform managed \
    --allow-unauthenticated \
    --memory 512Mi \
    --cpu 1 \
    --min-instances 0 \
    --max-instances 10 \
    --timeout 300 \
    --port 8080
```

### 5. 配置网络白名单（重要！）

由于 Cloud Run 部署在云端，**所有请求都会经过 Google 的负载均衡器**。后端代码已经实现了从 `X-Forwarded-For` header 获取真实客户端 IP 的逻辑，所以应该能正确识别公司 Wi‑Fi 的公网出口 IP。

**配置方法：**

1. **确认公司网络的公网出口 IP**：
   - ✅ 已确认：`161.142.145.141`
   - 在公司 Wi‑Fi 下访问：https://api.ipify.org 验证

2. **IP 已在配置中**：
   - `application.yml` 中已包含：`161.142.145.141/32`
   - 部署到 Cloud Run 后，这个配置会自动生效

3. **验证 IP 获取**：
   - 部署后，查看 Cloud Run 日志，确认后端能正确获取到 `161.142.145.141`
   - 如果日志显示的是其他 IP，可能需要调整 `allowed-networks` 配置

4. **高级选项 - VPC 连接**（可选）：
   - 如果公司有 Google Cloud VPC，可以配置 VPC 连接
   - 这样 Cloud Run 可以看到内网 IP（`192.168.0.x`）
   - 但通常使用公网出口 IP 就足够了

### 6. 访问应用

部署成功后，Cloud Run 会提供一个 HTTPS URL，例如：
```
https://employee-management-xxxxx-xx.a.run.app
```

## 🔧 配置说明

### 环境变量列表

| 变量名 | 说明 | 默认值 | 必需 |
|--------|------|--------|------|
| `PORT` | 服务端口（Cloud Run 自动设置） | 8080 | 否 |
| `MONGODB_URI` | MongoDB 连接字符串 | 见 application.yml | 是 |
| `MONGODB_DATABASE` | 数据库名称 | employee_management | 否 |
| `MONGODB_USERNAME` | 数据库用户名 | admin | 否 |
| `MONGODB_PASSWORD` | 数据库密码 | admin123 | 否 |
| `GEMINI_API_KEY` | Gemini API 密钥 | 见 application.yml | 是 |
| `COOKIE_SECURE` | Cookie Secure 标志 | false | 否（Cloud Run 应设为 true） |
| `ATTENDANCE_LATITUDE` | 考勤位置纬度 | 3.201388 | 否 |
| `ATTENDANCE_LONGITUDE` | 考勤位置经度 | 101.71495 | 否 |
| `ATTENDANCE_RADIUS` | 考勤半径（米） | 500 | 否 |
| `ATTENDANCE_MAX_ACCURACY` | 最大定位精度（米） | 1200 | 否 |

### 资源配置建议

- **内存**：512Mi（最小）- 1Gi（推荐）
- **CPU**：1 核心（最小）- 2 核心（推荐，如果并发高）
- **最小实例数**：0（按需启动，节省成本）
- **最大实例数**：10（根据实际需求调整）
- **超时时间**：300 秒（5 分钟）

## 📝 注意事项

### 1. 文件上传存储

Cloud Run 是**无状态**的，文件系统是临时的。上传的文件（如 KPI 证据、请假文档）会丢失。

**解决方案：**
- 使用 **Google Cloud Storage (GCS)** 存储文件
- 修改代码中的文件上传逻辑，保存到 GCS bucket

### 2. Session 存储

默认使用内存 Session，多实例时会有问题。

**解决方案：**
- 使用 **Redis** 或 **Memcached** 存储 Session
- 或使用 **Spring Session** 配合 GCS/数据库

### 3. 网络白名单

在 Cloud Run 中，客户端 IP 可能不是真实的公司 Wi‑Fi IP。

**解决方案：**
- 配置公司网络的公网出口 IP 到白名单
- 或使用 Cloud Run 的 VPC 连接（如果公司有 GCP VPC）

### 4. 成本优化

- 设置 `min-instances: 0` 以节省成本（冷启动需要几秒）
- 如果对响应时间要求高，设置 `min-instances: 1`
- 监控 Cloud Run 的使用情况，调整资源配置

## 🔍 故障排查

### 应用无法启动

```bash
# 查看日志
gcloud run services logs read $SERVICE_NAME --region $REGION --limit 50
```

### 连接 MongoDB 失败

- 检查 MongoDB Atlas 的 IP 白名单：添加 `0.0.0.0/0`（允许所有 IP）或 Cloud Run 的 IP 范围
- 检查 MongoDB URI 是否正确

### 考勤验证失败

- 检查 `allowed-networks` 配置
- 查看后端日志中的实际客户端 IP
- 考虑使用 VPC 连接或配置公司网络的公网 IP

## 📚 相关资源

- [Cloud Run 文档](https://cloud.google.com/run/docs)
- [Artifact Registry 文档](https://cloud.google.com/artifact-registry/docs)
- [Cloud Build 文档](https://cloud.google.com/build/docs)

## 🆘 需要帮助？

如果遇到问题，可以：
1. 查看 Cloud Run 日志
2. 检查环境变量配置
3. 验证 Docker 镜像是否正确构建
4. 联系 Google Cloud 支持

