# 智能教学平台 Web 端

`teach-frontend` 是管理员、教师和学生 PC 端的 Vue 3 应用。生产镜像由 Nginx 托管，并将 `/api/**` 代理到 Java 后端、将 `/face/detect` 代理到 Python AI 服务。

## 环境

- Node.js 22
- npm
- 本地后端默认地址：`http://localhost:8820/api`

## 本地开发

```powershell
npm ci
npm run dev
```

默认访问 `http://localhost:5173`。

## 检查与构建

```powershell
npm run build
```

`build` 同时执行 `vue-tsc` 类型检查和 Vite 生产构建。

格式化或修复 ESLint 问题前先检查改动范围：

```powershell
npm run lint
npm run format
```

这两个命令会写入源码，不应在存在未确认改动时批量执行。

## 环境变量

| 文件 | 用途 |
|---|---|
| `.env.development` | 本地开发，API 指向 `http://localhost:8820/api` |
| `.env.production` | Docker/Nginx 部署，使用同源 `/api` |

不要在 `VITE_*` 变量中保存服务端密钥。Vite 变量会进入浏览器构建产物。

完整运行、部署和故障排查见根目录 `README.md`。
