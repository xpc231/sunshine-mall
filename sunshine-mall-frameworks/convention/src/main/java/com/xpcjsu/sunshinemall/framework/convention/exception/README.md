# GlobalExceptionHandler 全局异常处理器

## 功能概述

GlobalExceptionHandler是sunshine-mall电商平台的全局异常处理器，专注于统一处理业务异常，提供标准化的错误响应。

## 核心特性

### 1. 统一异常处理
- 集成BaseException异常体系（ValidationException、BusinessException、SystemException）
- 统一返回Result格式的响应
- 自动记录异常日志，包含请求路径、错误码、上下文信息

### 2. 全面的异常类型支持
- **ValidationException**：参数校验异常（HTTP 400）
- **BusinessException**：业务逻辑异常（HTTP 200，业务错误码）
- **SystemException**：系统级异常（HTTP 500）
- **MethodArgumentNotValidException**：Spring Validation校验失败（HTTP 400）
- **BindException**：表单绑定失败（HTTP 400）
- **ConstraintViolationException**：Bean Validation约束违反（HTTP 400）
- **MethodArgumentTypeMismatchException**：参数类型不匹配（HTTP 400）
- **Exception**：未预期的其他异常（HTTP 500）

### 3. 详细的日志记录
- 自动记录请求路径、错误码、异常消息
- 支持上下文信息记录，便于问题排查
- 区分日志级别：参数/业务异常用WARN，系统异常用ERROR

## 使用示例

### 业务代码中抛出异常

```java
@Service
public class ProductService {
    
    public ProductDTO getProduct(Long productId) {
        // 参数校验
        if (productId == null) {
            throw new ValidationException(
                BusinessErrorCode.SYSTEM_PARAM_ERROR, 
                "商品ID不能为空"
            );
        }
        
        // 业务逻辑校验
        Product product = productRepository.findById(productId);
        if (product == null) {
            throw new BusinessException(
                BusinessErrorCode.PRODUCT_NOT_FOUND, 
                "商品不存在"
            ).addContext("productId", productId);
        }
        
        // 库存检查
        if (product.getStock() <= 0) {
            throw new BusinessException(
                BusinessErrorCode.PRODUCT_INSUFFICIENT_STOCK, 
                "商品库存不足"
            ).addContext("productId", productId)
             .addContext("currentStock", product.getStock());
        }
        
        return convertToDTO(product);
    }
}
```

### Controller中的自动处理

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;
    
    // 成功响应
    @GetMapping("/{id}")
    public Result<ProductDTO> getProduct(@PathVariable Long id) {
        ProductDTO product = productService.getProduct(id);
        return Result.success(product);
    }
    
    // 异常会被GlobalExceptionHandler自动捕获并处理
    // 无需在Controller中try-catch
}
```

### 响应示例

**成功响应：**
```json
{
  "code": "0",
  "message": "操作成功",
  "data": {
    "productId": 123,
    "productName": "iPhone 15"
  },
  "timestamp": 1697203200000
}
```

**业务异常响应：**
```json
{
  "code": "PRODUCT_NOT_FOUND",
  "message": "商品不存在",
  "data": null,
  "timestamp": 1697203200000
}
```

**参数校验失败响应：**
```json
{
  "code": "SYSTEM_PARAM_ERROR",
  "message": "productName: 商品名称不能为空; price: 价格必须大于0",
  "data": null,
  "timestamp": 1697203200000
}
```

## 最佳实践

### 1. 异常选择原则

- **ValidationException**：用于参数校验失败
  ```java
  if (StringUtils.isEmpty(productName)) {
      throw new ValidationException("INVALID_PARAM", "商品名称不能为空");
  }
  ```

- **BusinessException**：用于业务规则校验失败
  ```java
  if (!product.canBeSold()) {
      throw new BusinessException("PRODUCT_OFFLINE", "商品已下架");
  }
  ```

- **SystemException**：用于系统级错误
  ```java
  try {
      database.connect();
  } catch (SQLException e) {
      throw new SystemException("DB_ERROR", "数据库连接失败", e);
  }
  ```

### 2. 添加上下文信息

```java
throw new BusinessException("ORDER_NOT_FOUND", "订单不存在")
    .addContext("orderId", orderId)
    .addContext("userId", userId)
    .addContext("timestamp", System.currentTimeMillis());
```

### 3. 使用Spring Validation

```java
@PostMapping
public Result<Void> createProduct(@RequestBody @Valid ProductCreateRequest request) {
    // Spring自动校验，失败会被GlobalExceptionHandler捕获
    productService.create(request);
    return Result.success();
}

public class ProductCreateRequest {
    @NotBlank(message = "商品名称不能为空")
    private String productName;
    
    @Min(value = 0, message = "价格必须大于等于0")
    private BigDecimal price;
}
```

## 日志示例

### 参数校验异常日志
```
WARN  - 参数校验异常 - 请求路径: /api/products/123, 错误码: SYSTEM_PARAM_ERROR, 
        消息: 商品ID格式错误, 上下文: {productId=abc}
```

### 业务异常日志
```
WARN  - 业务异常 - 请求路径: /api/orders/create, 错误码: PRODUCT_INSUFFICIENT_STOCK, 
        消息: 商品库存不足, 上下文: {productId=123, requestQuantity=10, availableStock=5}
```

### 系统异常日志
```
ERROR - 系统异常 - 请求路径: /api/payments/callback, 错误码: SYSTEM_ERROR, 
        消息: 数据库连接超时, 详情: Connection timeout after 30s, 
        上下文: {paymentId=PAY20231014001}
java.sql.SQLException: Connection timeout
    at com.mysql.jdbc.ConnectionImpl.connect(...)
    ...
```

## 配置说明

GlobalExceptionHandler使用`@RestControllerAdvice`注解，会自动被Spring扫描并注册。

确保在Spring Boot启动类或配置类中开启组件扫描：

```java
@SpringBootApplication
@ComponentScan(basePackages = {
    "com.xpcjsu.sunshinemall.framework.convention"
})
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 依赖要求

- Spring Boot 3.x
- Jakarta Validation API
- SLF4J日志框架
- BaseException异常体系
- Result统一响应类
- BusinessErrorCode错误码定义

## 注意事项

1. **避免在Controller中捕获异常**：让GlobalExceptionHandler统一处理
2. **合理使用错误码**：使用BusinessErrorCode中定义的标准错误码
3. **添加上下文信息**：便于问题排查和日志分析
4. **区分异常类型**：根据实际情况选择合适的异常类型
5. **不要过度设计**：只处理项目中实际会出现的异常

## 扩展指南

如需处理新的异常类型，在GlobalExceptionHandler中添加新的`@ExceptionHandler`方法：

```java
@ExceptionHandler(CustomException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public Result<Void> handleCustomException(CustomException ex, HttpServletRequest request) {
    log.warn("自定义异常 - 请求路径: {}, 消息: {}", request.getRequestURI(), ex.getMessage());
    return Result.failure("CUSTOM_ERROR", ex.getMessage());
}
```
