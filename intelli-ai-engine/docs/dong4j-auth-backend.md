# Dong4j 默认服务商后端鉴权实现说明（Spring Boot 3）

> 目标：插件不保存长期密钥，启动/调用时用设备唯一标识换取短期 token，再访问 OpenAI 兼容 API。

## 1. 接口约定

### 1.1 获取短期 Token

- URL：`POST https://api.dong4j.site/auth`
- Content-Type：`application/json`

**请求示例**

```json
{
  "deviceId": "b94c8b6a-8d1f-4d33-8f6d-5f5f1cfb4b3a",
  "timestamp": 1738636800000,
  "nonce": "b7d1cfa4a4d14b5f8ad7f01e8f5b5d0d",
  "pluginId": "dev.dong4j.zeka.stack.idea.plugin.common.ai",
  "pluginVersion": "1.4.0"
}
```

**响应示例**

```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9....",
  "token_type": "Bearer",
  "expires_in": 3600
}
```

### 1.2 访问 OpenAI 兼容 API

- URL：`POST https://api.dong4j.site/v1/chat/completions`
- Header：`Authorization: Bearer <access_token>`

---

## 2. 安全策略建议

1. **时间戳窗口**：只允许 `timestamp` 在 5 分钟内的请求。
2. **Nonce 去重**：对 `deviceId + nonce` 做短期缓存（如 5~10 分钟）防重放。
3. **黑白名单**：可按 `deviceId` 做允许/拒绝策略。
4. **频控**：按 `deviceId`/IP 做 QPS 和日额度控制。
5. **短期 Token**：1 小时有效期，过期后重新换取。

---

## 3. Spring Boot 3 示例实现

### 3.1 依赖（任选其一）

**方案 A：jjwt**

```kotlin
implementation("io.jsonwebtoken:jjwt-api:0.12.5")
runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")
```

**方案 B：java-jwt**

```kotlin
implementation("com.auth0:java-jwt:4.4.0")
```

### 3.2 请求/响应 DTO

```java
public record AuthRequest(
    String deviceId,
    Long timestamp,
    String nonce,
    String pluginId,
    String pluginVersion
) {}

public record AuthResponse(
    String access_token,
    String token_type,
    long expires_in
) {}
```

### 3.3 控制器示例

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    private final TokenService tokenService;
    private final ReplayGuard replayGuard;

    public AuthController(TokenService tokenService, ReplayGuard replayGuard) {
        this.tokenService = tokenService;
        this.replayGuard = replayGuard;
    }

    @PostMapping
    public AuthResponse issue(@RequestBody AuthRequest req) {
        tokenService.validateRequest(req);       // 时间戳、必填字段
        replayGuard.checkAndMark(req.deviceId(), req.nonce()); // 防重放
        String token = tokenService.createToken(req);
        return new AuthResponse(token, "Bearer", 3600);
    }
}
```

### 3.4 TokenService 示例（JWT）

```java
@Service
public class TokenService {
    private final byte[] secret = "CHANGE_ME_TO_LONG_RANDOM_SECRET".getBytes(StandardCharsets.UTF_8);

    public void validateRequest(AuthRequest req) {
        if (req.deviceId() == null || req.deviceId().isBlank()) {
            throw new IllegalArgumentException("deviceId missing");
        }
        long now = System.currentTimeMillis();
        if (req.timestamp() == null || Math.abs(now - req.timestamp()) > 5 * 60 * 1000) {
            throw new IllegalArgumentException("timestamp out of window");
        }
    }

    public String createToken(AuthRequest req) {
        long now = System.currentTimeMillis();
        long exp = now + 3600_000L;
        return Jwts.builder()
            .subject(req.deviceId())
            .claim("pluginId", req.pluginId())
            .claim("pluginVersion", req.pluginVersion())
            .issuedAt(new Date(now))
            .expiration(new Date(exp))
            .signWith(Keys.hmacShaKeyFor(secret))
            .compact();
    }
}
```

### 3.5 OpenAI 兼容接口鉴权过滤

```java
@Component
public class AuthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String token = auth.substring("Bearer ".length());
        // 校验 token（JWT 解析、过期判断等）
        chain.doFilter(request, response);
    }
}
```

---

## 4. 兼容插件侧字段

插件侧会发送：

- `deviceId`：来自 `DeviceIdGenerator.generateDeviceId()`
- `timestamp`：`System.currentTimeMillis()`
- `nonce`：随机 UUID 去掉 `-`
- `pluginId`：`EngineContents.PLUGIN_ID`
- `pluginVersion`：从 IDE 插件元信息中读取

后端只要按上述字段验证即可。

---

## 5. 返回字段兼容规则

插件兼容以下字段（任意一种即可）：

- `access_token` 或 `token`
- `expires_in` 或 `expiresIn`

建议统一返回 `access_token` 和 `expires_in`。
