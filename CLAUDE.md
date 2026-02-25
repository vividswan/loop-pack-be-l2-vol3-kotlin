# CLAUDE.md

이 파일은 Claude Code가 이 프로젝트를 이해하는 데 필요한 핵심 정보를 제공합니다.

## 프로젝트 개요

- **프로젝트명**: loopers-kotlin-spring-template
- **그룹**: com.loopers
- **빌드 도구**: Gradle 8.13 (Kotlin DSL)
- **언어**: Kotlin 2.0.20 / Java 21

## 기술 스택 및 버전

| 기술 | 버전 |
|------|------|
| Kotlin | 2.0.20 |
| Java | 21 |
| Spring Boot | 3.4.4 |
| Spring Cloud | 2024.0.1 |
| SpringDoc OpenAPI | 2.7.0 |
| QueryDSL | Jakarta |
| KtLint | 1.0.1 |

### 테스트 라이브러리

| 라이브러리 | 버전 |
|-----------|------|
| SpringMockk | 4.0.2 |
| Mockito | 5.14.0 |
| Mockito Kotlin | 5.4.0 |
| Instancio | 5.0.2 |
| TestContainers | (Spring Boot 관리) |

## 모듈 구조

```
Root
├── apps/                    # 실행 가능한 Spring Boot 애플리케이션
│   ├── commerce-api         # REST API 서비스
│   ├── commerce-batch       # 배치 처리 서비스
│   └── commerce-streamer    # Kafka 이벤트 스트리밍 서비스
├── modules/                 # 재사용 가능한 인프라 모듈
│   ├── jpa                  # JPA + QueryDSL + MySQL
│   ├── redis                # Spring Data Redis
│   └── kafka                # Spring Kafka
└── supports/                # 유틸리티 모듈
    ├── jackson              # JSON 직렬화 설정
    ├── logging              # 로깅 + Slack 알림
    └── monitoring           # Actuator + Prometheus 메트릭
```

### 모듈 의존성

| App | Modules | Supports |
|-----|---------|----------|
| commerce-api | jpa, redis | jackson, logging, monitoring |
| commerce-batch | jpa, redis | jackson, logging, monitoring |
| commerce-streamer | jpa, redis, kafka | jackson, logging, monitoring |

## 주요 명령어

```bash
# 빌드
./gradlew build

# 테스트
./gradlew test

# 특정 앱 실행
./gradlew :apps:commerce-api:bootRun
./gradlew :apps:commerce-batch:bootRun
./gradlew :apps:commerce-streamer:bootRun

# 코드 스타일 검사
./gradlew ktlintCheck

# 코드 스타일 자동 수정
./gradlew ktlintFormat
```

## 코드 컨벤션

- **코드 스타일**: KtLint (pre-commit hook 활성화)
- **Null Safety**: JSR305 strict 모드 적용
- **테스트 타임존**: Asia/Seoul
- **테스트 프로파일**: test

## 디렉토리 규칙

- `apps/*/src/main/kotlin`: 애플리케이션 코드
- `apps/*/src/main/resources`: 애플리케이션 설정 (application.yml)
- `modules/*/src/main/kotlin`: 모듈 코드
- `modules/*/src/testFixtures`: 테스트 픽스처 (다른 모듈에서 재사용)
- `docker/`: Docker Compose 및 인프라 설정
- `http/**/*.http`: API 테스트용 HTTP 파일

---

## AI 협업 규칙

### 증강 코딩 원칙

- **의사결정 주도권**: 방향성 및 주요 결정은 개발자가 최종 승인. AI는 제안만 가능.
- **중간 보고**: 반복 작업, 요청하지 않은 기능 구현, 테스트 삭제 시 개발자 확인 필요.
- **임의판단 금지**: AI는 독단적 판단 없이, 불확실한 사항은 반드시 확인 후 진행.

### TDD 워크플로우 (Red → Green → Refactor)

모든 테스트는 **3A 원칙** 준수: `Arrange` → `Act` → `Assert`

1. **Red Phase**: 실패하는 테스트 먼저 작성
   - 요구사항을 검증하는 테스트 케이스 정의

2. **Green Phase**: 테스트를 통과하는 최소한의 코드 작성
   - 오버엔지니어링 금지

3. **Refactor Phase**: 코드 품질 개선
   - 불필요한 private 함수 제거, 객체지향적 설계
   - unused import 제거
   - 성능 최적화
   - 모든 테스트 통과 확인

---

## 개발 주의사항

### 금지 사항

- 실제 동작하지 않는 코드, 불필요한 Mock 데이터 기반 구현
- null-safety 위반 코드 작성
- `println` 코드 커밋

### 권장 사항

- E2E 테스트 코드 작성 (실제 API 호출 검증)
- 재사용 가능한 객체 설계
- 성능 최적화 대안 제안
- 완성된 API는 `http/**/*.http`에 정리

### 우선순위

1. 실제 동작하는 해결책만 고려
2. null-safety, thread-safety 보장
3. 테스트 가능한 구조 설계
4. 기존 코드 패턴 분석 후 일관성 유지

---

## 도메인 & 객체 설계 전략

### Entity / Value Object / Domain Service 구분 기준

| 구분 | 판단 기준 | 프로젝트 내 예시 |
|------|-----------|-----------------|
| **Entity** | 고유 식별자(ID)를 가지며 상태 변화와 연속성이 중요한 객체 | `ProductModel`, `OrderModel`, `LikeModel`, `BrandModel` |
| **Value Object** | 식별자 없이 값 자체로 동등성을 판단하는 불변 객체 | `OrderStatus`, `ProductSortType` 등 Enum 기반 VO |
| **Domain Service** | 특정 Entity에 속하기 어려운 도메인 로직을 상태 없이 처리 | `OrderService`(재고 차감 + 주문 생성 조합), `LikeService`(좋아요 + 상품 카운트 연동) |

### 도메인 모델링 원칙

- 도메인 객체는 **비즈니스 규칙을 캡슐화**해야 한다. 외부에서 상태를 직접 변경하지 않고, 객체가 제공하는 행위를 통해서만 변경한다.
  - 예: `ProductModel.decreaseStock(quantity)` — 재고 음수 방지 검증을 객체 내부에서 수행
- 도메인 규칙이 **여러 서비스에 반복적으로 나타나면** 해당 규칙은 도메인 객체에 속할 가능성이 높다.
- Domain Service는 **상태를 갖지 않으며**, 동일 도메인 경계 내 객체 간 협력을 조율한다.
- Entity의 생성은 **팩토리 메서드(`create()`)를 통해** 검증과 초기화를 보장한다.
- 검증 로직은 **생성 시점(`init` / `companion object`)에서 수행**하여, 유효하지 않은 객체가 존재할 수 없도록 한다.

### 도메인 간 협력 규칙

- 서로 다른 도메인 간의 조합(예: Product + Brand + Like)은 **Application Layer(Facade)에서 처리**한다.
- 동일 도메인 내 객체 간 협력(예: Product 재고 차감 → Order 생성)은 **Domain Service에서 처리**한다.
- 각 기능의 **책임과 결합도**에 대해 개발자의 의도를 확인하고 개발을 진행한다.

---

## 아키텍처 가이드

### 아키텍처 전략 및 의사결정 근거

- 본 프로젝트는 **레이어드 아키텍처 + DIP(의존성 역전 원칙)** 를 채택한다.
- DIP를 적용하는 이유: 도메인 계층이 인프라 기술(JPA, Redis 등)에 의존하지 않도록 하여, **도메인 로직의 테스트 가능성과 교체 용이성**을 확보한다.
- Repository Interface를 Domain Layer에 두는 이유: 도메인이 필요로 하는 데이터 접근 계약을 도메인 스스로 정의하고, 구현 기술은 Infrastructure에서 자유롭게 선택할 수 있도록 한다.
- **트랜잭션 경계는 Application Layer(Facade)에 설정**한다: 유즈케이스 단위로 트랜잭션을 관리하여, 도메인 서비스는 트랜잭션에 무관하게 순수한 로직에 집중한다.
- Facade vs Service 사용 기준:
  - **Facade**: 여러 도메인 Service를 조합하여 유즈케이스를 완성. `@Transactional` 경계 담당.
  - **Domain Service**: 단일 도메인 경계 내의 비즈니스 규칙 처리. 상태 없음.
- API Request/Response Dto와 Application Layer의 Info 객체는 **분리하여 작성**한다: 계층 간 결합을 방지하고, 각 계층의 변경이 다른 계층에 전파되지 않도록 한다.

### 레이어 구조 (Clean Architecture 기반)

```
┌─────────────────────────────────────────────────────────────────┐
│                        interfaces (외부)                         │
│  Controller, Dto, ApiSpec                                       │
├─────────────────────────────────────────────────────────────────┤
│                        application (응용)                        │
│  Facade, Info                                                   │
├─────────────────────────────────────────────────────────────────┤
│                          domain (핵심)                           │
│  Service, Model, Repository(Interface)                          │
├─────────────────────────────────────────────────────────────────┤
│                      infrastructure (구현)                       │
│  RepositoryImpl, JpaRepository                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 패키지 구조 예시

```
com.loopers
├── interfaces/api/{도메인}/
│   ├── {Domain}V1Controller.kt    # HTTP 요청/응답 처리
│   ├── {Domain}V1ApiSpec.kt       # Swagger 문서화 인터페이스
│   └── {Domain}V1Dto.kt           # Request/Response DTO
├── application/{도메인}/
│   ├── {Domain}Facade.kt          # 유즈케이스 조합, 여러 Service 호출
│   └── {Domain}Info.kt            # 레이어 간 데이터 전달 객체
├── domain/{도메인}/
│   ├── {Domain}Service.kt         # 핵심 비즈니스 로직
│   ├── {Domain}Model.kt           # JPA Entity, 도메인 검증
│   └── {Domain}Repository.kt      # Repository 인터페이스 (Port)
└── infrastructure/{도메인}/
    ├── {Domain}RepositoryImpl.kt  # Repository 구현체 (Adapter)
    └── {Domain}JpaRepository.kt   # Spring Data JPA 인터페이스
```

### 레이어별 책임

| 레이어 | 클래스 | 책임 | 금지 |
|--------|--------|------|------|
| **interfaces** | Controller, Dto | HTTP 요청/응답, 파라미터 검증 | 비즈니스 로직 |
| **application** | Facade, Info | 유즈케이스 조합, 트랜잭션 경계 | 도메인 로직 직접 구현 |
| **domain** | Service, Model | 핵심 비즈니스 로직, 도메인 검증 | HTTP/DB 의존성 |
| **infrastructure** | RepositoryImpl | 외부 시스템 연동 (DB, 외부 API) | 비즈니스 로직 |

### 데이터 흐름

```
Request → Controller(Dto) → Facade(Info) → Service(Model) → Repository → DB
Response ← Controller(Dto) ← Facade(Info) ← Service(Model) ← Repository ← DB
```

- **Dto**: API 계층 전용 (Request/Response)
- **Info**: Application ↔ Interfaces 계층 간 전달용
- **Model**: 도메인 엔티티, 비즈니스 로직 포함

### 예외 처리 전략

| HTTP 상태 | 상황 |
|-----------|------|
| 400 Bad Request | 검증 실패, 필수 필드 누락, 잘못된 형식 |
| 403 Forbidden | 권한 없음 |
| 404 Not Found | 리소스 없음 |
| 500 Internal Server Error | 서버 오류 |

- 명확한 에러 메시지 제공
- 부분 실패 시나리오 고려 (배치 처리 등)

### 코드 품질 체크리스트

- Companion object는 클래스 최하단에 위치
- 공통 사용 Enum은 core/common 모듈로 이동
- 검증 후에는 not-null 타입 사용 (불필요한 null 체크 제거)
- 불필요한 타입 변환 로직 제거

