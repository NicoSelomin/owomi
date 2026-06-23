# CLAUDE.md — OWOMI Backend

> Ce fichier complète le `CLAUDE.md` global avec les règles spécifiques
> au projet Java 21 + Spring Boot 3.
> Lire `CLAUDE.md` global avant ce fichier.

---

## 1. Stack & Versions

```xml
<properties>
  <java.version>21</java.version>
  <spring-boot.version>3.3.x</spring-boot.version>
  <jjwt.version>0.12.x</jjwt.version>
  <mapstruct.version>1.6.x</mapstruct.version>
  <itext.version>7.2.x</itext.version>
</properties>
```

---

## 2. Structure du projet

```
backend/
├── src/
│   ├── main/
│   │   ├── java/dev/selonick/owomi/
│   │   │   ├── OWOMIApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── JwtConfig.java
│   │   │   │   ├── CorsConfig.java
│   │   │   │   └── OpenApiConfig.java
│   │   │   ├── auth/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── JwtService.java
│   │   │   │   ├── RefreshTokenService.java
│   │   │   │   └── dto/
│   │   │   │       ├── RegisterRequest.java
│   │   │   │       ├── LoginRequest.java
│   │   │   │       └── AuthResponse.java
│   │   │   ├── user/
│   │   │   │   ├── User.java              # Entité JPA
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── dto/
│   │   │   │       ├── UserDTO.java
│   │   │   │       ├── UpdateProfileRequest.java
│   │   │   │       └── ChangePasswordRequest.java
│   │   │   ├── transaction/
│   │   │   │   ├── Transaction.java
│   │   │   │   ├── TransactionType.java   # Enum INCOME | EXPENSE
│   │   │   │   ├── TransactionRepository.java
│   │   │   │   ├── TransactionService.java
│   │   │   │   ├── TransactionController.java
│   │   │   │   └── dto/
│   │   │   │       ├── TransactionRequest.java
│   │   │   │       └── TransactionResponse.java
│   │   │   ├── category/
│   │   │   │   ├── Category.java
│   │   │   │   ├── CategoryRepository.java
│   │   │   │   ├── CategoryService.java
│   │   │   │   ├── CategoryController.java
│   │   │   │   └── dto/
│   │   │   │       ├── CategoryRequest.java
│   │   │   │       └── CategoryResponse.java
│   │   │   ├── currency/
│   │   │   │   ├── Currency.java
│   │   │   │   ├── CurrencyRepository.java
│   │   │   │   ├── CurrencyController.java
│   │   │   │   └── dto/
│   │   │   │       └── CurrencyDTO.java
│   │   │   ├── report/
│   │   │   │   ├── ReportController.java
│   │   │   │   ├── ReportService.java
│   │   │   │   ├── PdfExportService.java
│   │   │   │   └── dto/
│   │   │   │       └── ReportSummaryDTO.java
│   │   │   └── common/
│   │   │       ├── exception/
│   │   │       │   ├── GlobalExceptionHandler.java
│   │   │       │   ├── BusinessException.java
│   │   │       │   └── ErrorCode.java
│   │   │       ├── response/
│   │   │       │   └── ApiResponse.java
│   │   │       └── entity/
│   │   │           └── BaseEntity.java    # id, createdAt, updatedAt
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/              # Flyway migrations
│   │           ├── V1__create_users.sql
│   │           ├── V2__create_currencies.sql
│   │           ├── V3__seed_currencies.sql
│   │           ├── V4__create_categories.sql
│   │           ├── V5__seed_default_categories.sql
│   │           └── V6__create_transactions.sql
│   └── test/
│       └── java/dev/selonick/owomi/
│           ├── auth/AuthServiceTest.java
│           ├── transaction/TransactionServiceTest.java
│           ├── category/CategoryServiceTest.java
│           └── report/ReportServiceTest.java
├── pom.xml
├── Dockerfile
└── .env.example
```

---

## 3. Entités JPA

### 3.1 BaseEntity — Héritage commun
```java
// common/entity/BaseEntity.java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
```

### 3.2 User
```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    @JsonIgnore                    // Ne jamais exposer le hash en JSON
    private String password;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code", referencedColumnName = "code")
    private Currency currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;
}
```

### 3.3 Transaction
```java
@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;      // Toujours BigDecimal, jamais double

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
```

---

## 4. Sécurité Spring Security

### 4.1 SecurityConfig
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(AbstractHttpConfigurer::disable)      // API REST stateless
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/currencies",
                    "/swagger-ui/**",
                    "/v3/api-docs/**",
                    "/actuator/health"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(customAuthEntryPoint())
                .accessDeniedHandler(customAccessDeniedHandler())
            )
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);   // Strength 12 — OWASP recommandé
    }
}
```

### 4.2 JwtService
```java
@Service
public class JwtService {

    // Chargé depuis application.yml → variable d'environnement
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-expiration}")   // 3600000 ms = 1h
    private long accessExpiration;

    @Value("${jwt.refresh-expiration}")  // 604800000 ms = 7j
    private long refreshExpiration;

    public String generateAccessToken(User user) {
        return buildToken(user, accessExpiration);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshExpiration);
    }

    private String buildToken(User user, long expiration) {
        return Jwts.builder()
            .subject(user.getEmail())
            .claim("userId", user.getId().toString())
            .claim("role", user.getRole().name())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    // Ne jamais logger le token — uniquement l'email
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }
}
```

### 4.3 Vérification propriété ressource (Anti-IDOR)
```java
// TOUJOURS vérifier que la ressource appartient à l'utilisateur authentifié
@Service
public class TransactionService {

    public Transaction getById(Long id, UUID userId) {
        Transaction transaction = transactionRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                "Transaction introuvable."));

        // Vérification IDOR — obligatoire sur chaque accès à une ressource
        if (!transaction.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                "Accès refusé.");
        }
        return transaction;
    }
}
```

---

## 5. Validation des données

### 5.1 DTOs avec validation
```java
// transaction/dto/TransactionRequest.java
public record TransactionRequest(

    @NotNull(message = "Le montant est obligatoire.")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0.")
    @Digits(integer = 13, fraction = 2, message = "Format de montant invalide.")
    BigDecimal amount,

    @NotNull(message = "Le type est obligatoire.")
    TransactionType type,

    @NotNull(message = "La catégorie est obligatoire.")
    Long categoryId,

    @NotNull(message = "La date est obligatoire.")
    @PastOrPresent(message = "La date ne peut pas être dans le futur.")
    LocalDate date,

    @Size(max = 500, message = "La note ne peut pas dépasser 500 caractères.")
    String note
) {}
```

### 5.2 Controller avec @Valid
```java
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<ApiResponse<TransactionResponse>> create(
            @Valid @RequestBody TransactionRequest request,  // @Valid obligatoire
            @AuthenticationPrincipal UserDetails userDetails) {

        UUID userId = extractUserId(userDetails);
        TransactionResponse response = transactionService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Transaction enregistrée avec succès."));
    }
}
```

---

## 6. Gestion des erreurs globale

```java
// common/exception/GlobalExceptionHandler.java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // Erreurs métier personnalisées
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        log.warn("Business error: code={}, message={}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
            .status(ex.getHttpStatus())
            .body(ApiResponse.error(ex.getErrorCode().name(), ex.getMessage()));
    }

    // Erreurs de validation @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.toList());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error("VALIDATION_ERROR", "Données invalides.", details));
    }

    // Erreur générique — NE PAS exposer le détail technique à l'utilisateur
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);    // Log complet côté serveur
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("INTERNAL_ERROR",
                "Une erreur inattendue est survenue. Réessayez."));  // Message générique pour l'user
    }
}
```

---

## 7. ApiResponse — Format uniforme

```java
// common/response/ApiResponse.java
@Builder
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    ApiError error,
    String timestamp
) {
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .message(message)
            .timestamp(Instant.now().toString())
            .build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(new ApiError(code, message, List.of()))
            .timestamp(Instant.now().toString())
            .build();
    }

    public static <T> ApiResponse<T> error(String code, String message, List<String> details) {
        return ApiResponse.<T>builder()
            .success(false)
            .error(new ApiError(code, message, details))
            .timestamp(Instant.now().toString())
            .build();
    }
}
```

---

## 8. Repository — Requêtes custom

```java
// transaction/TransactionRepository.java
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Toutes les transactions de l'utilisateur pour un mois donné
    @Query("""
        SELECT t FROM Transaction t
        WHERE t.user.id = :userId
        AND YEAR(t.date) = :year
        AND MONTH(t.date) = :month
        AND (:type IS NULL OR t.type = :type)
        AND (:categoryId IS NULL OR t.category.id = :categoryId)
        ORDER BY t.date DESC, t.createdAt DESC
        """)
    Page<Transaction> findByUserAndPeriod(
        @Param("userId") UUID userId,
        @Param("year") int year,
        @Param("month") int month,
        @Param("type") TransactionType type,
        @Param("categoryId") Long categoryId,
        Pageable pageable
    );

    // Calcul solde mensuel
    @Query("""
        SELECT COALESCE(SUM(CASE WHEN t.type = 'INCOME' THEN t.amount ELSE -t.amount END), 0)
        FROM Transaction t
        WHERE t.user.id = :userId
        AND YEAR(t.date) = :year
        AND MONTH(t.date) = :month
        """)
    BigDecimal calculateMonthlyBalance(
        @Param("userId") UUID userId,
        @Param("year") int year,
        @Param("month") int month
    );

    // Répartition par catégorie
    @Query("""
        SELECT t.category.name, SUM(t.amount)
        FROM Transaction t
        WHERE t.user.id = :userId
        AND t.type = 'EXPENSE'
        AND YEAR(t.date) = :year
        AND MONTH(t.date) = :month
        GROUP BY t.category.name
        ORDER BY SUM(t.amount) DESC
        """)
    List<Object[]> findExpensesByCategory(
        @Param("userId") UUID userId,
        @Param("year") int year,
        @Param("month") int month
    );
}
```

---

## 9. Rate Limiting (OWASP A04)

```java
// config/RateLimitFilter.java
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    // Bucket par IP pour les endpoints d'auth
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();
    // Bucket par userId pour les autres endpoints
    private final Map<String, Bucket> apiBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String clientId = getClientId(request, path);
        Bucket bucket = getBucket(clientId, path);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {"success":false,"error":{"code":"RATE_LIMIT_EXCEEDED",
                "message":"Trop de requêtes. Réessayez dans quelques instants."}}
                """);
        }
    }

    private Bucket getBucket(String key, String path) {
        boolean isAuth = path.startsWith("/api/auth/");
        Map<String, Bucket> buckets = isAuth ? authBuckets : apiBuckets;

        return buckets.computeIfAbsent(key, k -> {
            // Auth : 10 req/min | API : 100 req/min
            long capacity = isAuth ? 10L : 100L;
            return Bucket.builder()
                .addLimit(Bandwidth.classic(capacity,
                    Refill.intervally(capacity, Duration.ofMinutes(1))))
                .build();
        });
    }
}
```

---

## 10. Configuration application.yml

```yaml
# application.yml
spring:
  application:
    name: owomi-backend

  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate          # Flyway gère le schéma — pas Hibernate
    show-sql: false               # true uniquement en dev local
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: true

# JWT
jwt:
  secret: ${JWT_SECRET}
  access-expiration: ${JWT_ACCESS_EXPIRATION:3600000}
  refresh-expiration: ${JWT_REFRESH_EXPIRATION:604800000}

# CORS
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:4200}

# Swagger — désactivé en production
springdoc:
  api-docs:
    enabled: ${SWAGGER_ENABLED:true}
  swagger-ui:
    enabled: ${SWAGGER_ENABLED:true}
    path: /swagger-ui.html

# Actuator — uniquement health en public
management:
  endpoints:
    web:
      exposure:
        include: health
  endpoint:
    health:
      show-details: never

# Logging — pas de données sensibles
logging:
  level:
    dev.selonick.owomi: INFO
    org.springframework.security: WARN
    org.hibernate.SQL: WARN
```

---

## 11. Flyway — Migrations SQL

```sql
-- V1__create_users.sql
CREATE TABLE users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    currency_code VARCHAR(3),
    role        VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_users_email ON users(email);

-- V2__create_currencies.sql
CREATE TABLE currencies (
    code    VARCHAR(3) PRIMARY KEY,
    name    VARCHAR(100) NOT NULL,
    symbol  VARCHAR(10) NOT NULL,
    locale  VARCHAR(10) NOT NULL
);

ALTER TABLE users
    ADD CONSTRAINT fk_users_currency
    FOREIGN KEY (currency_code) REFERENCES currencies(code);

-- V3__seed_currencies.sql
INSERT INTO currencies (code, name, symbol, locale) VALUES
    ('XOF', 'Franc CFA UEMOA', 'FCFA', 'fr-BJ'),
    ('XAF', 'Franc CFA CEMAC', 'FCFA', 'fr-CM'),
    ('EUR', 'Euro', '€', 'fr-FR'),
    ('USD', 'Dollar américain', '$', 'en-US'),
    ('GBP', 'Livre sterling', '£', 'en-GB'),
    ('MAD', 'Dirham marocain', 'DH', 'fr-MA'),
    ('DZD', 'Dinar algérien', 'DA', 'fr-DZ'),
    ('TND', 'Dinar tunisien', 'DT', 'fr-TN'),
    ('NGN', 'Naira', '₦', 'en-NG'),
    ('GHS', 'Cedi', '₵', 'en-GH'),
    ('KES', 'Shilling kenyan', 'KSh', 'en-KE'),
    ('CAD', 'Dollar canadien', 'CA$', 'fr-CA'),
    ('CHF', 'Franc suisse', 'CHF', 'fr-CH');

-- V4__create_categories.sql
CREATE TABLE categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    icon        VARCHAR(50) NOT NULL,
    color       VARCHAR(7) NOT NULL,
    type        VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    is_default  BOOLEAN NOT NULL DEFAULT FALSE,
    user_id     UUID REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_category_name_user UNIQUE (name, user_id)
);

CREATE INDEX idx_categories_user ON categories(user_id);

-- V5__seed_default_categories.sql
INSERT INTO categories (name, icon, color, type, is_default) VALUES
    ('Alimentation', 'restaurant-outline', '#D85A30', 'EXPENSE', TRUE),
    ('Transport',    'car-outline',        '#185FA5', 'EXPENSE', TRUE),
    ('Logement',     'home-outline',       '#854F0B', 'EXPENSE', TRUE),
    ('Santé',        'medical-outline',    '#1D9E75', 'EXPENSE', TRUE),
    ('Loisirs',      'game-controller-outline', '#888780', 'EXPENSE', TRUE),
    ('Revenus',      'cash-outline',       '#1D9E75', 'INCOME',  TRUE);

-- V6__create_transactions.sql
CREATE TABLE transactions (
    id          BIGSERIAL PRIMARY KEY,
    amount      DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    type        VARCHAR(10) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
    note        TEXT,
    date        DATE NOT NULL,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_date_not_future CHECK (date <= CURRENT_DATE)
);

CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_user_date ON transactions(user_id, date DESC);
CREATE INDEX idx_transactions_category ON transactions(category_id);
```

---

## 12. Tests unitaires

```java
// auth/AuthServiceTest.java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private AuthService authService;

    @Test
    @DisplayName("Inscription : email déjà existant → EmailAlreadyExistsException")
    void register_EmailAlreadyExists_ShouldThrow() {
        // Arrange
        RegisterRequest request = new RegisterRequest("Test", "existing@email.com",
            "Password1!", "BJ", "XOF");
        when(userRepository.existsByEmail("existing@email.com")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("Connexion : identifiants incorrects → InvalidCredentialsException")
    void login_InvalidCredentials_ShouldThrow() {
        // Arrange
        LoginRequest request = new LoginRequest("test@email.com", "WrongPassword");
        User user = buildTestUser();
        when(userRepository.findByEmail("test@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("WrongPassword", user.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
    }
}
```

---

## 13. Dockerfile

```dockerfile
# backend/Dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

# Créer un utilisateur non-root pour la sécurité
RUN addgroup -g 1001 owomi && adduser -u 1001 -G owomi -s /bin/sh -D owomi

COPY target/owomi-backend-*.jar app.jar

# Changer le propriétaire
RUN chown owomi:owomi app.jar

USER owomi

EXPOSE 8080

# Options JVM pour production
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
```

---

## 14. Commandes utiles

```bash
# Build
./mvnw clean package -DskipTests

# Lancer en dev
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Tests
./mvnw test
./mvnw test -Dtest=AuthServiceTest

# Coverage
./mvnw verify -Pjacoco

# Docker
docker build -t owomi-backend .
docker run -p 8080:8080 --env-file .env owomi-backend

# Vérifier les migrations Flyway
./mvnw flyway:info -Dflyway.url=${DB_URL}
```

---

## 15. Checklist sécurité Backend

- [ ] JWT secret chargé depuis variable d'environnement (jamais hardcodé)
- [ ] BCrypt strength 12 pour les mots de passe
- [ ] Vérification de propriété (IDOR) sur chaque accès à une ressource
- [ ] `@Valid` sur tous les `@RequestBody`
- [ ] Pas de stack trace dans les réponses d'erreur production
- [ ] Pas de données sensibles dans les logs (email OK, password/token INTERDIT)
- [ ] Rate limiting activé sur `/api/auth/**`
- [ ] CORS configuré pour le domaine frontend uniquement
- [ ] Headers de sécurité HTTP configurés
- [ ] `ddl-auto: validate` en production (Flyway gère le schéma)
- [ ] Swagger désactivé en production (`SWAGGER_ENABLED=false`)
- [ ] Utilisateur Docker non-root
- [ ] Refresh tokens invalidés à la déconnexion
