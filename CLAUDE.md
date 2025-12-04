# CLAUDE.md

이 파일은 Claude Code (claude.ai/code)가 이 레포지토리의 코드를 작업할 때 따라야 할 가이드를 제공합니다.

## 프로젝트 개요

Java 21과 Gradle로 빌드된 Spring Boot 3.5.7 기반 이커머스 애플리케이션입니다. Lombok을 사용하여 보일러플레이트 코드를 줄이고, Spring Boot DevTools로 개발 생산성을 향상시킵니다.

**패키지 구조**: 기본 패키지는 `com.example.hhplus_ecommerce`입니다 (하이픈이 아닌 언더스코어 사용 - 'com.example.hhplus-ecommerce'는 Java에서 유효하지 않은 패키지명).

## 개발 명령어

### 빌드 및 실행
```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행
./gradlew bootRun

# 클린 빌드
./gradlew clean build
```

### 테스트
```bash
# 모든 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests com.example.hhplus_ecommerce.SpecificTestClass

# 패턴으로 테스트 실행
./gradlew test --tests '*SpecificTest'

# 테스트 지속 실행 (watch mode)
./gradlew test --continuous
```

### 개발
```bash
# Spring Boot DevTools로 실행 (자동 리로드)
./gradlew bootRun

# 의존성 업데이트 확인
./gradlew dependencyUpdates
```

## 아키텍처

### 기술 스택
- **프레임워크**: Spring Boot 3.5.7
- **언어**: Java 21
- **빌드 도구**: Gradle with Kotlin DSL
- **웹**: Spring Web (RESTful services)
- **개발 도구**: Spring Boot DevTools (hot reload)
- **코드 생성**: Lombok
- **테스팅**: JUnit 5 (Jupiter) with Spring Boot Test

### 프로젝트 구조
```
src/
├── main/
│   ├── java/com/example/hhplus_ecommerce/
│   │   └── HhplusEcommerceApplication.java (메인 진입점)
│   └── resources/
│       ├── application.properties (설정 파일)
│       ├── static/ (정적 웹 리소스)
│       └── templates/ (템플릿 파일)
└── test/
    └── java/com/example/hhplus_ecommerce/
        └── HhplusEcommerceApplicationTests.java
```

### 설정
- 애플리케이션 설정은 `src/main/resources/application.properties`에 위치
- Spring 애플리케이션명: `hhplus-ecommerce`
- 기본 Spring Boot 포트: 8080 (application.properties에서 변경 가능)

## 개발 가이드라인

### Java 버전
이 프로젝트는 Java 21을 요구합니다. build.gradle에서 툴체인이 설정되어 일관성을 보장합니다.

### Lombok 사용
Lombok은 어노테이션 프로세싱을 위해 설정되어 있습니다. 주로 사용되는 어노테이션:
- `@Data`, `@Getter`, `@Setter` - POJO용
- `@Builder` - 빌더 패턴용
- `@AllArgsConstructor`, `@NoArgsConstructor` - 생성자용
- `@Slf4j` - 로깅용

### 테스팅
- 단위 테스트는 JUnit 5 (Jupiter) 사용
- 통합 테스트는 `@SpringBootTest` 사용
- 테스트 파일은 메인 소스 파일의 구조를 반영

## 프로젝트 아키텍처 규칙

### 레이어드 아키텍처 (Layered Architecture)
이 프로젝트는 명확한 레이어 분리를 따릅니다:

```
presentation → application → domain ← infrastructure
```

#### 1. Presentation Layer (`presentation/`)
- **역할**: HTTP 요청/응답 처리, API 엔드포인트 정의
- **구성요소**:
  - `controller/`: REST API 컨트롤러
  - `dto/`: 요청/응답 DTO (Record 타입 사용)
  - `common/`: 공통 예외, 에러코드, 핸들러
- **규칙**:
  - Controller는 비즈니스 로직을 포함하지 않음
  - Service 계층을 호출하여 처리
  - DTO는 Record 타입으로 정의
  - 검증은 `@Valid`, `@Validated` 사용
  - **Swagger 문서화**:
    - API 문서화를 위해 `{Domain}ControllerAPI` 인터페이스를 정의
    - 인터페이스에 Swagger 어노테이션 작성 (`@Operation`, `@ApiResponse` 등)
    - 실제 Controller 클래스는 API 인터페이스를 구현 (`implements`)
    - 이를 통해 문서화와 구현을 분리하여 가독성 향상

#### 2. Application Layer (`application/service/`)
- **역할**: 비즈니스 로직 조율, 트랜잭션 관리
- **규칙**:
  - `@Service` 어노테이션 사용
  - `@Transactional` 어노테이션으로 트랜잭션 관리
  - 기본적으로 `@Transactional(readOnly = true)` 적용
  - 쓰기 작업은 메서드 레벨에서 `@Transactional` 오버라이드
  - Domain 객체의 비즈니스 로직 호출
  - Repository를 통한 데이터 접근

#### 3. Domain Layer (`domain/model/`)
- **역할**: 핵심 비즈니스 로직, 엔티티 정의
- **규칙**:
  - `@Entity` JPA 엔티티
  - 비즈니스 로직은 도메인 모델 내부에 구현
  - Setter 사용 금지 (불변성 유지)
  - 상태 변경은 명확한 의미를 가진 메서드로 구현 (예: `usePoint()`, `chargePoint()`)
  - 검증 로직은 도메인 모델 내부에서 처리
  - `BaseEntity` 상속으로 공통 필드 관리 (ID, 생성일시, 수정일시)

#### 4. Infrastructure Layer (`infrastructure/`)
- **역할**: 외부 시스템과의 연동, 기술적 구현
- **구성요소**:
  - `repository/`: JPA Repository 인터페이스
  - `config/`: 설정 클래스 (Redis, Redisson 등)
  - `lock/`: 분산 락 구현
- **규칙**:
  - Repository는 `JpaRepository` 상속
  - 커스텀 쿼리는 메서드명 규칙 또는 `@Query` 사용
  - 분산 락은 `@DistributedLock` 어노테이션 사용

### 동시성 제어

#### 1. 비관적 락 (Pessimistic Lock)
- 데이터베이스 레벨에서 락 획득
- Repository에 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 적용
- 예시: `findByIdWithLockOrThrow()`
- 사용 케이스: 포인트 충전, 재고 차감

#### 2. 분산 락 (Distributed Lock)
- Redisson을 사용한 Redis 기반 분산 락
- `@DistributedLock` 어노테이션 사용
- SpEL 표현식으로 동적 키 생성 가능
- 예시: `@DistributedLock(key = "payment:#{#orderId}")`
- 설정 가능 파라미터:
  - `waitTime`: 락 획득 대기 시간 (기본 5초)
  - `leaseTime`: 락 유지 시간 (기본 3초)
  - `timeUnit`: 시간 단위 (기본 SECONDS)

### 예외 처리

#### 계층별 예외 처리
- **Domain Layer**: 비즈니스 규칙 위반 시 예외 발생
- **Application Layer**: Repository 조회 실패 시 예외 발생
- **Presentation Layer**: `GlobalExceptionHandler`에서 통합 처리

#### 예외 타입
- `BaseException`: 모든 커스텀 예외의 기본 클래스
  - `BadRequestException`: 잘못된 요청 (400)
  - `NotFoundException`: 리소스 없음 (404)
  - `ConflictException`: 비즈니스 규칙 충돌 (409)
  - `InternalServerException`: 서버 내부 오류 (500)

#### ErrorCode
- 도메인별로 ErrorCode enum 정의
  - `UserErrorCode`, `ProductErrorCode`, `OrderErrorCode` 등
- ErrorCode는 `code`와 `message` 포함
- 예외 발생 시 ErrorCode를 통해 일관된 에러 응답 생성

### 테스트 작성 규칙

#### 1. 단위 테스트
- 각 도메인 모델은 별도의 테스트 클래스 작성
- 파일명: `{ClassName}Test.java`
- 비즈니스 로직의 정상/예외 케이스 모두 검증

#### 2. 서비스 테스트
- Mock을 사용한 단위 테스트
- 파일명: `{ServiceName}Test.java`
- `@Mock`, `@InjectMocks` 사용

#### 3. 컨트롤러 테스트
- `@WebMvcTest` 사용
- MockMvc를 통한 HTTP 요청/응답 검증

#### 4. 통합 테스트
- `AbstractIntegrationTest` 상속
- Testcontainers를 통한 실제 DB 환경 테스트
- 트랜잭션 롤백으로 테스트 격리

#### 5. 동시성 테스트
- `ExecutorService`를 사용한 멀티스레드 테스트
- `CountDownLatch`로 동시 실행 보장
- 파일명: `{Feature}ConcurrencyTest.java`

### 코드 작성 규칙

#### 1. Naming Convention
- **Entity**: 명사형, 단수 (예: `User`, `Product`, `Order`)
- **Service**: `{Domain}Service` (예: `UserService`, `ProductService`)
- **Repository**: `{Domain}Repository` (예: `UserRepository`)
- **Controller**: `{Domain}Controller` (예: `UserController`)
- **Controller API Interface**: `{Domain}ControllerAPI` (예: `UserControllerAPI`)
  - Swagger 문서화 어노테이션만 포함
  - 실제 Controller가 이 인터페이스를 구현
- **DTO**: 내부 클래스로 정의, Record 타입 사용
  - Request: `{Action}Request`
  - Response: `{Data}Response`

#### 2. 패키지 구조
```
com.example.hhplus_ecommerce/
├── domain/model/           # 도메인 엔티티
├── application/service/    # 비즈니스 로직
├── infrastructure/
│   ├── repository/        # 데이터 접근
│   ├── config/           # 설정
│   └── lock/             # 분산 락
└── presentation/
    ├── controller/       # API 엔드포인트
    ├── dto/             # DTO
    └── common/          # 공통 (예외, 핸들러)
```

#### 3. 의존성 주입
- 생성자 주입 사용 (필드 주입 금지)
- `@RequiredArgsConstructor` 활용

#### 4. 불변성
- Entity의 Setter 사용 금지
- DTO는 Record 타입으로 불변성 보장
- 상태 변경은 명확한 의미의 메서드로 제공

#### 5. 검증
- Domain 레벨: 비즈니스 규칙 검증 (도메인 메서드 내부)
- Presentation 레벨: 입력 형식 검증 (`@Valid`)

### 데이터베이스

#### JPA 설정
- MySQL 8.0 사용
- Testcontainers로 테스트 환경 구성
- `BaseEntity`로 공통 필드 관리 (id, createdAt, updatedAt)

#### Entity 작성 규칙
- `@Entity` 필수
- `@SuperBuilder` 사용 (상속 구조)
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` - JPA용
- `@AllArgsConstructor(access = AccessLevel.PRIVATE)` - Builder용
- ID는 자동 생성 (`@GeneratedValue`)

### Redis & 분산 락

#### Redis 설정
- `application.properties`에서 호스트, 포트 설정
- Redisson 사용

#### 분산 락 사용
```java
@DistributedLock(
    key = "resource:#{#resourceId}",
    waitTime = 5L,
    leaseTime = 3L,
    timeUnit = TimeUnit.SECONDS
)
public void processWithLock(Long resourceId) {
    // 비즈니스 로직
}
```

### 코드 커버리지

#### Jacoco 설정
- 빌드 시 자동으로 커버리지 리포트 생성
- 제외 대상:
  - Application 메인 클래스
  - DTO
  - Common (예외, 핸들러 등)
  - Repository 인터페이스
  - Controller

#### 리포트 확인
```bash
./gradlew test
# 리포트 위치: build/jacocoHtml/index.html
```

### API 문서화

#### SpringDoc OpenAPI 설정
- SpringDoc OpenAPI 사용
- Swagger UI 자동 생성
- 접속: `http://localhost:8080/swagger-ui.html` (서버 실행 후)

#### Controller API 인터페이스 패턴
문서화와 구현을 분리하기 위해 인터페이스 기반 패턴을 사용합니다:

**구조**:
- `{Domain}ControllerAPI` 인터페이스: Swagger 어노테이션만 포함
- `{Domain}Controller` 클래스: 인터페이스를 구현하고 실제 비즈니스 로직 호출

**예시**:
```java
// UserControllerAPI.java (인터페이스)
public interface UserControllerAPI {
    @Operation(summary = "사용자 조회", description = "사용자 ID로 사용자 정보를 조회합니다")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    ResponseEntity<UserResponse> getUser(@PathVariable Long userId);
}

// UserController.java (구현체)
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserControllerAPI {
    private final UserService userService;

    @Override
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        // 실제 구현
        return ResponseEntity.ok(userService.getUser(userId));
    }
}
```

**장점**:
- Controller 클래스의 가독성 향상 (문서화 어노테이션 분리)
- Swagger 문서 관리 용이
- 인터페이스만 보고도 API 명세 파악 가능
