# ConfigManager 使用指南

## 简介

`ConfigManager` 是 sunshine-mall 框架提供的统一配置管理器，支持多种配置源和类型安全的配置访问。

## 主要特性

- **多配置源支持**: 系统属性、环境变量、Spring配置文件
- **类型安全**: 支持 String、Integer、Long、Boolean、Double 类型
- **配置缓存**: 避免重复解析，提升性能
- **Optional支持**: 提供Optional版本的方法，避免null处理
- **优先级管理**: 系统属性 > 环境变量 > Spring配置文件 > 默认值

## 基本用法

### 1. 获取配置实例

```java
// 通过SingletonHolder获取实例（如果可用）
ConfigManager config = SingletonHolder.getInstance(ConfigManager.class);

// 或直接创建实例
ConfigManager config = new ConfigManager();
```

### 2. 基本类型配置读取

```java
// 字符串配置
String appName = config.getString("app.name", "sunshine-mall");
String dbUrl = config.getString("spring.datasource.url", "jdbc:mysql://localhost:3306/mall");

// 整数配置
Integer port = config.getInt("server.port", 8080);
Integer maxPoolSize = config.getInt("spring.datasource.hikari.maximum-pool-size", 20);

// 长整数配置
Long timeout = config.getLong("app.request.timeout", 30000L);

// 布尔配置
Boolean debugMode = config.getBoolean("app.debug", false);

// 双精度配置
Double taxRate = config.getDouble("business.tax.rate", 0.13);
```

### 3. Optional方式获取

```java
// 避免null值处理
Optional<String> redisHost = config.getStringOptional("spring.redis.host");
redisHost.ifPresent(host -> {
    // 使用Redis主机配置
});

Optional<Integer> redisPort = config.getIntOptional("spring.redis.port");
Integer port = redisPort.orElse(6379);
```

### 4. 配置存在性检查

```java
// 检查配置是否存在
if (config.containsKey("spring.redis.host")) {
    // Redis配置存在，使用Redis缓存
} else {
    // 使用本地缓存
}
```

## 配置源优先级

配置读取按以下优先级进行：

### 1. 系统属性（最高优先级）
```bash
java -Dapp.name=sunshine-mall -jar app.jar
```

### 2. 环境变量
```bash
export APP_NAME=sunshine-mall
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/mall
```

注意：环境变量会将配置键中的点号(.)转换为下划线(_)并转为大写

### 3. Spring配置文件（最低优先级）
```yaml
# application.yml
app:
  name: sunshine-mall
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mall
```

## 使用场景

### 数据库配置
```java
String dbUrl = config.getString("spring.datasource.url", "jdbc:mysql://localhost:3306/mall");
String username = config.getString("spring.datasource.username", "root");
String password = config.getString("spring.datasource.password", "");
Integer maxPoolSize = config.getInt("spring.datasource.hikari.maximum-pool-size", 20);
```

### 业务配置
```java
// 订单配置
Long orderTimeout = config.getLong("business.order.timeout-minutes", 30L);
Boolean autoConfirm = config.getBoolean("business.order.auto-confirm", false);

// 文件上传配置
Long maxFileSize = config.getLong("business.upload.max-size-mb", 10L);
String uploadPath = config.getString("business.upload.path", "/tmp/uploads");
```

### 第三方服务配置
```java
// Redis配置
String redisHost = config.getString("spring.redis.host", "localhost");
Integer redisPort = config.getInt("spring.redis.port", 6379);

// 支付配置
String alipayAppId = config.getString("pay.alipay.app-id", "");
Boolean payEnabled = config.getBoolean("pay.enabled", true);
```

## 性能优化

### 配置缓存
```java
// 配置会被自动缓存，避免重复解析
String value1 = config.getString("app.name", "default"); // 从配置源读取
String value2 = config.getString("app.name", "default"); // 从缓存读取

// 手动清理缓存（通常不需要）
config.clearCache();
```

### 缓存监控
```java
// 查看缓存大小
int cacheSize = config.getCacheSize();
System.out.println("当前缓存的配置项数量: " + cacheSize);
```

## 错误处理

### 类型转换错误
```java
// 设置了无效的整数值
System.setProperty("app.port", "invalid");

// 会返回默认值，并在控制台输出警告日志
Integer port = config.getInt("app.port", 8080); // 返回8080
```

### 必需配置检查
```java
// 对于必需的配置，建议进行检查
String dbUrl = config.getString("spring.datasource.url", null);
if (dbUrl == null) {
    throw new ConfigException("数据库URL配置缺失", "spring.datasource.url");
}
```

## 注意事项

1. **配置键命名**: 建议使用点号分隔的层次结构，如 `app.database.url`
2. **环境变量转换**: 环境变量会自动转换，`app.name` 对应 `APP_NAME`
3. **配置缓存**: 配置会被缓存，运行时修改系统属性不会立即生效
4. **线程安全**: ConfigManager是线程安全的，可以在多线程环境中使用
5. **默认值**: 始终提供合理的默认值，提高系统的健壮性

## 最佳实践

1. **集中配置定义**: 在常量类中定义配置键名
```java
public class ConfigKeys {
    public static final String APP_NAME = "app.name";
    public static final String DB_URL = "spring.datasource.url";
}
```

2. **配置验证**: 在应用启动时验证重要配置
```java
@PostConstruct
public void validateConfig() {
    String dbUrl = config.getString(ConfigKeys.DB_URL, null);
    if (dbUrl == null) {
        throw new IllegalStateException("数据库配置缺失");
    }
}
```

3. **环境特定配置**: 使用Spring Profile进行环境区分
```yaml
# application-dev.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mall_dev

# application-prod.yml  
spring:
  datasource:
    url: jdbc:mysql://prod-server:3306/mall_prod
```