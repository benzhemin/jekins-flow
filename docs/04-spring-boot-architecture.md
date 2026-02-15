# Spring Boot Multi-Module Architecture - From Concepts to Code

## Table of Contents
1. [Architectural Principles](#architectural-principles)
2. [Layered Architecture Pattern](#layered-architecture-pattern)
3. [Module Dependencies](#module-dependencies)
4. [Domain-Driven Design](#domain-driven-design)
5. [Code Examples & Patterns](#code-examples--patterns)
6. [Testing Strategy](#testing-strategy)
7. [Best Practices](#best-practices)

---

## Architectural Principles

### 1. **Separation of Concerns**

**Concept**: Each module handles one responsibility

```
┌──────────────────────────────────────────────────────────────┐
│                    Single Responsibility                     │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  ✅ GOOD:                    ❌ BAD:                        │
│  UserService                 UserService                     │
│  ├─ Business Logic           ├─ Database queries             │
│  └─ ONLY business logic       ├─ HTTP requests               │
│                              ├─ File operations              │
│                              ├─ Email sending                │
│  Easier to:                  ├─ Everything!                  │
│  - Test (mock dependencies)  └─ Hard to test, maintain       │
│  - Change database                                          │
│  - Reuse logic                                              │
└──────────────────────────────────────────────────────────────┘
```

### 2. **Dependency Inversion Principle (DIP)**

**Core Idea**: Depend on abstractions, not concretions

#### Without DIP (Tightly Coupled):
```java
// ❌ BAD: Domain layer depends on persistence layer
public class UserService {
    private UserJpaRepository repository;  // Direct dependency on JPA

    public User getUser(Long id) {
        return repository.findById(id);  // Tied to JPA
    }
}

// Problems:
// - UserService knows about database
// - Can't easily change to MongoDB or REST API
// - Hard to unit test (need real database)
// - Can't reuse UserService without JPA
```

#### With DIP (Loosely Coupled):
```java
// ✅ GOOD: Domain layer defines interface, doesn't care about implementation

// domain/ module - defines interface (abstraction)
public interface UserRepository {
    User findById(Long id);
    void save(User user);
}

public class UserService {
    private UserRepository repository;  // Depends on abstraction

    public User getUser(Long id) {
        return repository.findById(id);  // Agnostic to implementation
    }
}

// infrastructure/ module - implements interface
@Component
public class UserRepositoryImpl implements UserRepository {
    @Autowired
    private UserJpaRepository jpaRepository;  // Use JPA internally

    @Override
    public User findById(Long id) {
        return jpaRepository.findById(id).orElse(null);
    }
}

// Benefits:
// + UserService doesn't know about JPA
// + Can easily swap implementation
// + Simple unit testing (mock UserRepository)
// + Can use UserService with different backends
```

**Visual Representation**:
```
WITHOUT DIP (Traditional):
┌─────────────┐
│   API       │
│ (Controllers)
└──────┬──────┘
       │ uses
       ↓
┌─────────────┐
│   Service   │
│   Layer     │
└──────┬──────┘
       │ directly uses JPA
       ↓
┌─────────────┐
│  JPA        │
│ Repository  │
└──────┬──────┘
       │ uses
       ↓
┌─────────────┐
│ Database    │
└─────────────┘

Problem: Service tightly coupled to JPA


WITH DIP (Recommended):
┌─────────────┐
│   API       │
│ (Controllers)
└──────┬──────┘
       │ uses
       ↓
┌─────────────────────┐
│   Service           │
│   Layer             │
│ (depends on         │
│  abstraction)       │
└──────┬──────────────┘
       │ uses interface
       ↓
┌─────────────────────┐
│ UserRepository      │
│ (Interface - the    │
│  abstraction)       │
└──────┬──────────────┘
       │ implemented by
       ├──────────────┬──────────────┐
       ↓              ↓              ↓
  ┌────────┐   ┌────────┐    ┌────────┐
  │  JPA   │   │MongoDB │    │REST API│
  │Impl    │   │ Impl   │    │ Impl   │
  └────────┘   └────────┘    └────────┘

Benefit: Service is flexible, can use any implementation
```

### 3. **Dependency Hierarchy**

**Rule**: A module can only depend on modules "below" it (toward core)

```
Allowed Dependencies
═══════════════════════════════════════════════════════════

api/  (REST API)
 ├─ depends on ↓
 ↓
domain/  (Business Logic)
 ├─ depends on ↓
 ↓
infrastructure/  (Database, HTTP clients)
 ├─ depends on ↓
 ↓
common/  (Utilities, DTOs)
 └─ no dependencies (foundation)


NOT Allowed
═══════════════════════════════════════════════════════════

common/ cannot depend on domain/   ❌
domain/ cannot depend on api/      ❌
infrastructure/ cannot depend on api/  ❌

Why?
- Circular dependencies = code becomes unmaintainable
- Core modules should not know about higher layers
- Easier to reason about dependencies
```

---

## Layered Architecture Pattern

### The Four-Layer Model

```
┌─────────────────────────────────────────────────────────────┐
│  API Layer (Presentation)                                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - REST Controllers                                   │   │
│  │ - Request/Response DTOs                              │   │
│  │ - Input validation                                   │   │
│  │ - Exception handling                                 │   │
│  │ - HTTP protocol concerns                             │   │
│  └──────────────────────────────────────────────────────┘   │
│  Transforms HTTP requests into domain objects               │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────┴───────────────────────────────────┐
│  Domain Layer (Business Logic)                              │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - Domain Entities (User, Product, etc)               │   │
│  │ - Business Services (UserService, etc)               │   │
│  │ - Repository Interfaces                              │   │
│  │ - Business Rules & Validation                        │   │
│  │ - Completely independent of frameworks               │   │
│  └──────────────────────────────────────────────────────┘   │
│  Pure business logic - the core of the application          │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────┴───────────────────────────────────┐
│  Infrastructure Layer (Technical Details)                   │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - JPA Entities                                       │   │
│  │ - Repository Implementations                         │   │
│  │ - Database Configuration                             │   │
│  │ - External API clients                               │   │
│  │ - Cache implementations                              │   │
│  └──────────────────────────────────────────────────────┘   │
│  Handles "how to store/retrieve" data                       │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────┴───────────────────────────────────┐
│  Common Layer (Shared Utilities)                            │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ - DTOs (Data Transfer Objects)                       │   │
│  │ - Exceptions                                         │   │
│  │ - Utilities & Constants                              │   │
│  │ - No framework dependencies                          │   │
│  └──────────────────────────────────────────────────────┘   │
│  Foundation - everything else depends on this               │
└─────────────────────────────────────────────────────────────┘
```

### Example: User Feature Through Layers

#### User Creation Flow

```
1. USER MAKES REQUEST
   POST /api/users
   {"name": "John Doe", "email": "john@example.com"}

   ↓ (HTTP request arrives)

2. API LAYER (api module)
   UserController.createUser()
   ├─ Receives HTTP request body (JSON)
   ├─ Deserializes to UserDTO
   ├─ Validates UserDTO (annotations: @NotNull, @Email)
   ├─ Calls UserService from domain layer
   ├─ Receives User domain object
   ├─ Serializes to JSON
   └─ Returns HTTP response (201 Created)

   ↓ (Business logic needed)

3. DOMAIN LAYER (domain module)
   UserService.createUser()
   ├─ Receives UserDTO (from API layer)
   ├─ Converts DTO to User domain entity
   ├─ Applies business rules:
   │  ├─ Check email format valid
   │  ├─ Check email not already used (calls repository interface)
   │  ├─ Set default values (active=true, createdAt=now)
   │  └─ Validate against business rules
   ├─ Calls repository interface (doesn't know how it works)
   ├─ Returns User entity
   └─ Note: No knowledge of HTTP, databases, JPA

   ↓ (Need to store in database)

4. INFRASTRUCTURE LAYER (infrastructure module)
   UserRepositoryImpl.save()
   ├─ Receives User domain entity
   ├─ Converts to UserEntity (JPA entity)
   ├─ Uses UserJpaRepository to save
   ├─ Executes SQL: INSERT INTO users (...)
   ├─ Database persists data
   └─ Converts back to User domain entity

   ↓ (Back up the layers)

5. RESPONSE RETURNED
   ← User object returned to service
   ← User converted to DTO in controller
   ← DTO serialized to JSON
   ← HTTP 201 response with user data

   ↓

USER RECEIVES RESPONSE
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "active": true
}
```

---

## Module Dependencies

### Dependency Graph

```
application/
├── api/
│   │
│   ├─ depends on → domain/
│   │
│   ├─ depends on → infrastructure/
│   │
│   └─ depends on → common/
│
├── domain/
│   │
│   └─ depends on → common/
│
├── infrastructure/
│   │
│   ├─ depends on → domain/
│   │
│   └─ depends on → common/
│
└── common/
    └─ no dependencies (foundation)
```

### How to Enforce Dependencies in Gradle

**File: `application/build.gradle.kts`**

```kotlin
// API module can depend on all others
subprojects {
    // Common should have no dependencies
    if (name == "common") {
        // Only Spring framework basics
        dependencies {
            implementation("org.springframework.boot:spring-boot-starter-validation")
        }
    }

    // Domain depends on common
    if (name == "domain") {
        dependencies {
            api(project(":application:common"))
            // Can also have Spring dependencies
        }
    }

    // Infrastructure depends on domain and common
    if (name == "infrastructure") {
        dependencies {
            implementation(project(":application:domain"))
            // Never implement common directly (inherited from domain)
        }
    }

    // API depends on everything
    if (name == "api") {
        dependencies {
            implementation(project(":application:common"))
            implementation(project(":application:domain"))
            implementation(project(":application:infrastructure"))
        }
    }
}
```

---

## Domain-Driven Design

### Entity vs DTO vs JPA Entity

**Critical Distinction**: Three different representations of "User"

```
┌──────────────────────────────────────────────────────────────┐
│  API / HTTP                                                  │
└──────────────────────────────────────────────────────────────┘
         ↓ (JSON received)
┌──────────────────────────────────────────────────────────────┐
│  UserDTO (api module)                                        │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ public class UserDTO {                                  ││
│  │     String name;                                        ││
│  │     String email;                                       ││
│  │     Boolean active;                                     ││
│  │                                                         ││
│  │     Purpose: Transfer data from HTTP to service         ││
│  │     - Only includes fields needed for this request      ││
│  │     - Can exclude sensitive fields (password)           ││
│  │     - Can differ from domain model                      ││
│  │ }                                                       ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
         ↓ (converted)
┌──────────────────────────────────────────────────────────────┐
│  User Domain Entity (domain module)                          │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ public class User {                                     ││
│  │     Long id;                                            ││
│  │     String name;                                        ││
│  │     String email;                                       ││
│  │     Boolean active;                                     ││
│  │     Long createdAt;                                     ││
│  │                                                         ││
│  │     // Business methods (pure domain logic)             ││
│  │     public boolean isAccountActive() {                  ││
│  │         return active != null && active;                ││
│  │     }                                                   ││
│  │     public void deactivateAccount() {                   ││
│  │         this.active = false;                            ││
│  │     }                                                   ││
│  │                                                         ││
│  │     Purpose: Pure domain logic                          ││
│  │     - No framework knowledge                            ││
│  │     - Contains business rules                           ││
│  │     - Database-agnostic                                 ││
│  │ }                                                       ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
         ↓ (converted for persistence)
┌──────────────────────────────────────────────────────────────┐
│  UserEntity (infrastructure module)                          │
│  ┌─────────────────────────────────────────────────────────┐│
│  │ @Entity                                                 ││
│  │ @Table(name = "users")                                  ││
│  │ public class UserEntity {                               ││
│  │     @Id                                                 ││
│  │     @GeneratedValue(strategy = GenerationType.IDENTITY) ││
│  │     Long id;                                            ││
│  │     @Column(nullable = false)                           ││
│  │     String name;                                        ││
│  │     @Column(unique = true)                              ││
│  │     String email;                                       ││
│  │     @Column(nullable = false)                           ││
│  │     Boolean active;                                     ││
│  │     Long createdAt;                                     ││
│  │                                                         ││
│  │     Purpose: JPA mapping to database                    ││
│  │     - Knows about database structure                    ││
│  │     - Contains JPA annotations                          ││
│  │     - Handles ORM concerns                              ││
│  │ }                                                       ││
│  └─────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────┘
         ↓ (stored)
┌──────────────────────────────────────────────────────────────┐
│  Database Table: users                                       │
│  id | name | email | active | created_at                    │
│  1  | John | ...   | true   | ...                            │
└──────────────────────────────────────────────────────────────┘
```

### Conversions Between Layers

**DTO → Domain Entity**:
```java
// In UserService (domain layer)
public User createUser(UserDTO dto) {
    // Convert DTO to domain entity
    User user = User.builder()
        .name(dto.getName())
        .email(dto.getEmail())
        .active(true)  // Business rule: new users start active
        .createdAt(System.currentTimeMillis())
        .build();

    // Apply business rules
    // (in real app: check email uniqueness, etc)

    return user;
}
```

**Domain Entity → JPA Entity**:
```java
// In UserRepositoryImpl (infrastructure layer)
public User save(User user) {
    // Convert domain entity to JPA entity
    UserEntity entity = UserEntity.fromDomainEntity(user);

    // Save using JPA
    UserEntity saved = userJpaRepository.save(entity);

    // Convert back to domain entity
    return saved.toDomainEntity();
}

// In UserEntity
public static UserEntity fromDomainEntity(User user) {
    return UserEntity.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .active(user.getActive())
        .createdAt(user.getCreatedAt())
        .build();
}

public User toDomainEntity() {
    return User.builder()
        .id(this.id)
        .name(this.name)
        .email(this.email)
        .active(this.active)
        .createdAt(this.createdAt)
        .build();
}
```

**Domain Entity → DTO for Response**:
```java
// In UserController (api layer)
@GetMapping("/{id}")
public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
    User user = userService.getUserById(id);

    // Convert domain entity to DTO
    UserDTO dto = UserDTO.builder()
        .id(user.getId())
        .name(user.getName())
        .email(user.getEmail())
        .active(user.getActive())
        .build();

    return ResponseEntity.ok(dto);
}
```

---

## Code Examples & Patterns

### Pattern 1: Repository Pattern with DIP

**Why**: Isolate data access, make it testable

```java
// domain/repository/UserRepository.java
// Interface - domain layer
public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    void delete(User user);
}

// infrastructure/persistence/UserJpaRepository.java
// Spring Data Interface - infrastructure layer
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
}

// infrastructure/persistence/UserRepositoryImpl.java
// Implementation - infrastructure layer
@Component
public class UserRepositoryImpl implements UserRepository {
    @Autowired
    private UserJpaRepository jpaRepository;

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomainEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return saved.toDomainEntity();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
            .map(UserEntity::toDomainEntity);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
            .map(UserEntity::toDomainEntity);
    }

    // ... other methods
}
```

**Key Benefits**:
1. **Testability**: Mock UserRepository in tests, no database needed
2. **Flexibility**: Swap JPA for MongoDB, REST API, etc without changing service
3. **Single Responsibility**: Repository only handles data access
4. **Reusability**: UserService can be used in different contexts

### Pattern 2: Domain Services with Business Logic

```java
// domain/service/UserService.java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    public User createUser(UserDTO dto) {
        // Business rule 1: Email must be unique
        if (repository.findByEmail(dto.getEmail()).isPresent()) {
            throw new ApplicationException(
                "EMAIL_ALREADY_EXISTS",
                "Email " + dto.getEmail() + " is already in use"
            );
        }

        // Business rule 2: Create with default values
        User user = User.builder()
            .name(dto.getName())
            .email(dto.getEmail())
            .active(true)  // New users start active
            .createdAt(System.currentTimeMillis())
            .build();

        // Persist
        return repository.save(user);
    }

    public User getUserById(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public List<User> getAllUsers() {
        return repository.findAll();
    }

    public User deactivateUser(Long id) {
        User user = getUserById(id);
        user.deactivateAccount();  // Domain method
        return repository.save(user);
    }
}
```

### Pattern 3: Controller with Proper Separation

```java
// api/controller/UserController.java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<UserDTO> dtos = users.stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<UserDTO> createUser(@Valid @RequestBody UserDTO dto) {
        User user = userService.createUser(dto);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toDTO(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(toDTO(user));
    }

    // Conversion helper
    private UserDTO toDTO(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .name(user.getName())
            .email(user.getEmail())
            .active(user.getActive())
            .createdAt(user.getCreatedAt())
            .build();
    }
}
```

---

## Testing Strategy

### Unit Testing Services (No Database)

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository repository;

    @InjectMocks
    private UserService service;

    @Test
    void createUser_Success() {
        // Arrange
        UserDTO dto = UserDTO.builder()
            .name("John Doe")
            .email("john@example.com")
            .active(true)
            .build();

        User expectedUser = User.builder()
            .id(1L)
            .name("John Doe")
            .email("john@example.com")
            .active(true)
            .createdAt(System.currentTimeMillis())
            .build();

        // Mock repository to return user when save() called
        when(repository.save(any(User.class)))
            .thenReturn(expectedUser);

        // Act
        User result = service.createUser(dto);

        // Assert
        assertNotNull(result);
        assertEquals("John Doe", result.getName());
        assertEquals("john@example.com", result.getEmail());
        assertTrue(result.isAccountActive());

        // Verify repository was called
        verify(repository).save(any(User.class));
    }

    @Test
    void createUser_Failure_EmailAlreadyExists() {
        // Arrange
        UserDTO dto = UserDTO.builder()
            .name("John Doe")
            .email("john@example.com")
            .active(true)
            .build();

        // Mock: email already exists
        when(repository.findByEmail("john@example.com"))
            .thenReturn(Optional.of(new User()));

        // Act & Assert
        assertThrows(ApplicationException.class, () -> {
            service.createUser(dto);
        });

        // Verify save was never called (short-circuited)
        verify(repository, never()).save(any(User.class));
    }
}
```

**Key Testing Principles**:
1. **No database**: Mock the repository
2. **Test behavior**: What does method do under different conditions
3. **Verify interactions**: Was repository called correctly
4. **Focus on business logic**: Not framework features

---

## Best Practices

### 1. **Use Dependency Injection**

✅ **GOOD**:
```java
@Service
public class UserService {
    private final UserRepository repository;

    // Constructor injection (immutable, testable)
    public UserService(UserRepository repository) {
        this.repository = repository;
    }
}

// OR with Lombok:
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;  // Injected automatically
}
```

❌ **BAD**:
```java
@Service
public class UserService {
    @Autowired
    private UserRepository repository;  // Field injection (mutable, hard to test)
}
```

### 2. **Don't Use Spring Annotations in Domain Layer**

✅ **GOOD**:
```java
// domain/entity/User.java
public class User {
    private Long id;
    private String name;
    // No Spring annotations!
}
```

❌ **BAD**:
```java
@Entity  // WRONG! Domain shouldn't know about JPA
@Data
public class User {
    @Id
    private Long id;
}
```

Domain entity should be a plain Java class.

### 3. **Keep Domain Layer Independent**

✅ **GOOD**:
```java
// domain/service/UserService.java
// Only depends on domain interfaces
public class UserService {
    private final UserRepository repository;  // Interface from domain

    public User createUser(UserDTO dto) {
        // Pure business logic
        User user = new User(dto.getName(), dto.getEmail());
        return repository.save(user);
    }
}
```

❌ **BAD**:
```java
// Tightly coupled to infrastructure
public class UserService {
    @Autowired
    private UserJpaRepository jpaRepository;  // From infrastructure!

    public User createUser(UserDTO dto) {
        UserEntity entity = new UserEntity(dto.getName());
        return jpaRepository.save(entity);  // Mixing concerns
    }
}
```

### 4. **Use Value Objects for Validation**

```java
// domain/value/Email.java
public class Email {
    private final String value;

    public Email(String value) {
        if (!isValidEmail(value)) {
            throw new InvalidEmailException();
        }
        this.value = value;
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    public String getValue() {
        return value;
    }
}

// Usage:
Email email = new Email(dto.getEmail());  // Throws if invalid
```

Benefits:
- Validation at object creation
- Type-safe (Email vs String)
- Business logic encapsulated

### 5. **Use Exceptions for Business Rules**

```java
// common/exception/ApplicationException.java
public class ApplicationException extends RuntimeException {
    private final String code;

    public ApplicationException(String code, String message) {
        super(message);
        this.code = code;
    }
}

// common/exception/ResourceNotFoundException.java
public class ResourceNotFoundException extends ApplicationException {
    public ResourceNotFoundException(String resource, Long id) {
        super("NOT_FOUND", resource + " with ID " + id + " not found");
    }
}

// Usage in service:
public User getUserById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", id));
}
```

### 6. **Document Module Responsibilities**

```
application/
├── common/
│   └── README.md
│       Purpose: Shared DTOs, exceptions, utilities
│       Dependencies: None
│       Examples: UserDTO, ApplicationException
│
├── domain/
│   └── README.md
│       Purpose: Business logic and domain logic
│       Dependencies: common
│       Examples: User entity, UserService, UserRepository interface
│
├── infrastructure/
│   └── README.md
│       Purpose: Persistence and external integrations
│       Dependencies: domain, common
│       Examples: UserEntity, UserRepositoryImpl
│
└── api/
    └── README.md
        Purpose: REST API and HTTP handling
        Dependencies: all
        Examples: UserController, GlobalExceptionHandler
```

---

## Summary

**Key Architectural Principles**:
1. ✅ **Layered architecture** - Each layer has clear responsibility
2. ✅ **Dependency inversion** - Depend on abstractions, not concretions
3. ✅ **Separation of concerns** - Single responsibility per class
4. ✅ **Testability** - Easy to unit test with mocks
5. ✅ **Framework independence** - Domain layer is pure Java
6. ✅ **Flexibility** - Easy to swap implementations

**The Pattern**:
```
HTTP Request
    ↓
API Layer (converts HTTP → domain objects)
    ↓
Domain Layer (pure business logic)
    ↓
Infrastructure Layer (implements interfaces, talks to database)
    ↓
Database
    ↓
(Reverse back up)
    ↓
HTTP Response
```

This architecture allows you to:
- Write business logic without framework knowledge
- Test easily with mocks
- Change databases without changing business logic
- Reuse domain logic in different contexts (REST API, GraphQL, gRPC)
