# Common 模块

各微服务共享的公共库，**不可独立部署**。

## 基本信息

| 项 | 值 |
|----|-----|
| 模块 | `onlyfriends-common` |
| 类型 | Maven 依赖库 |
| 被引用 | 全部 `onlyfriends-*-service` 模块 |

## 主要内容

### 统一响应

- `ApiResponse<T>` — 标准 API 响应封装（`code`、`message`、`data`）
- 全局异常处理与错误码

### 安全

- JWT 工具类（生成、解析、校验 Token）
- 用户上下文（`CurrentUser` 等）

### 文件存储

- `StorageService` 抽象
- 本地存储（`local`）与 MinIO（`minio`）实现
- 通过 `STORAGE_TYPE` 环境变量切换

### 其他

- 公共 DTO 与枚举
- MyBatis-Plus 通用配置辅助
- 跨服务复用的工具类

## 使用方式

在服务的 `pom.xml` 中声明依赖：

```xml
<dependency>
    <groupId>com.onlyfriends</groupId>
    <artifactId>onlyfriends-common</artifactId>
</dependency>
```

## 相关配置

各服务通过 `application.yml` 中的 `app.storage` 段配置存储：

| 变量 | 说明 |
|------|------|
| `STORAGE_TYPE` | `local` 或 `minio` |
| `STORAGE_LOCAL_DIR` | 本地存储目录 |
| `MINIO_ENDPOINT` | MinIO 地址 |
| `MINIO_ACCESS_KEY` / `MINIO_SECRET_KEY` | MinIO 凭据 |

## 相关文档

- [本地开发指南](../getting-started/local-setup.md)
- [系统设计 v3](../architecture/system-design-v3.md)
