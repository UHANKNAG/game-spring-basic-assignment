# Crimson Citadel — Spring Basic Assignment

로그라이크 덱빌딩 게임 **Crimson Citadel**의 백엔드 서버입니다.
프로젝트에 포함된 프론트엔드(`src/main/resources/static`)와 연동되어,
`http://localhost:8080` 에서 실제로 게임을 플레이하고 여정을 저장·이어하기 할 수 있습니다.

- 과제 원본: [f-api/game-spring-basic-assignment](https://github.com/f-api/game-spring-basic-assignment)
- API 명세: [game-spring-api-docs](https://f-api.github.io/game-spring-api-docs/basic/api-docs.html)

---

## 기술 스택

| 구분 | 내용 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 (Web MVC, Data JPA, Validation, Actuator) |
| Database | MySQL 8 (Docker), H2 (테스트 전용) |
| Build | Gradle |
| Etc | Lombok, Docker |

---

## 실행 방법

### 1. MySQL 컨테이너 실행

```bash
docker run -d --name mysql-basic \
  -p 3307:3306 \
  -e MYSQL_ROOT_PASSWORD=1234 \
  -e MYSQL_DATABASE=basic \
  mysql:8
```

### 2. 서버 실행

로컬 개발용 설정이라 `src/main/resources/application.properties`는 저장소에 포함되어 있습니다.
위 컨테이너를 그대로 띄웠다면 별도 설정 없이 바로 실행하면 됩니다.

```bash
./gradlew bootRun
```

브라우저에서 `http://localhost:8080` 접속.

<details>
<summary>application.properties</summary>

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/basic
spring.datasource.username=root
spring.datasource.password=1234
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
```

`ddl-auto=update` — 서버를 재시작해도 저장된 여정이 유지되도록 선택했습니다.

</details>

---

## API 명세

| Method | URI | 설명 | 성공 응답 |
| --- | --- | --- | --- |
| `POST` | `/games` | 게임 생성 (시작 덱 함께 저장) | `201` `GameDetailResponse` |
| `GET` | `/games` | 게임 목록 조회 (id 내림차순) | `200` `List<GameSummaryResponse>` |
| `GET` | `/games/{gameId}` | 게임 상세 조회 (덱은 id 오름차순) | `200` `GameDetailResponse` |
| `PUT` | `/games/{gameId}/progress` | 진행 상황 + 덱 전체 저장 | `200` `GameDetailResponse` |
| `PATCH` | `/games/{gameId}` | 플레이어 이름 변경 | `204 No Content` |
| `DELETE` | `/games/{gameId}` | 게임 삭제 (RunCard 먼저 삭제) | `204 No Content` |

### 에러 응답

모든 에러는 `GlobalExceptionHandler`를 통해 동일한 형식으로 내려갑니다.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "게임을 찾을 수 없습니다. id=999",
  "path": "/games/999"
}
```

| 상태 | 상황 | 예외 |
| --- | --- | --- |
| `400` | 요청 값 검증 실패 / 본문·파라미터 형식 오류 | `MethodArgumentNotValidException` 등 |
| `404` | 존재하지 않는 게임 | `GameNotFoundException` |
| `409` | 이미 끝난 게임(`CLEARED`/`FAILED`)에 진행 저장 시도 | `GameFinishedException` |

---

## 패키지 구조

```
com.gamebasic
├── GameBasicApplication.java      # @EnableJpaAuditing
├── common
│   ├── dto/ErrorResponse.java
│   └── exception
│       ├── GameNotFoundException.java   # 404
│       ├── GameFinishedException.java   # 409
│       └── GlobalExceptionHandler.java  # @RestControllerAdvice
├── game
│   ├── controller/GameController.java
│   ├── service/GameService.java
│   ├── repository/GameRepository.java
│   ├── entity/    Game, BaseEntity, GamePhase, GameStatus
│   └── dto/       CreateRequest, ProgressRequest, RenameRequest,
│                  GameDetailResponse, GameSummaryResponse
└── runcard
    ├── repository/ RunCardRepository, DeckCount(집계 DTO)
    ├── entity/     RunCard
    └── dto/        RunCardRequest, CardResponse
```

- Controller → Service → Repository 3 Layer Architecture
- 연관관계는 `RunCard → Game` **단방향 `@ManyToOne(LAZY)`** 만 사용
- `cascade` / `orphanRemoval` / 양방향 컬렉션 미사용 — 자식(`RunCard`)의 조회·저장·삭제는 `RunCardRepository`로 명시 처리

---

## 구현 내용 (커밋 기준)

### 필수 기능

| Lv | 내용 | 커밋 |
| --- | --- | --- |
| — | 배포용 Dockerfile 작성 (`eclipse-temurin:21-jdk-alpine`) | `feat: Create Dockerfile` |
| — | `.gitignore` 정리 | `docs: Set .gitignore` |
| 1 | Docker MySQL 연결 및 `application.properties` 작성 | `docs: 환경변수세팅` |
| 2 | 빈 등록 누락 수정 — 생성자 주입으로 DI 해결 | `fix: 의존성 주입(DI)` |
| 3 | RESTful 경로 수정 — `GET /games` 매핑 정정 | `fix: API URL RESTful` |
| 4 | `createGame`의 `@Transactional(readOnly = true)` 버그 수정 | `fix: createGame readOnly 문제` |
| 5 | `RunCardRequest` 검증 · `CardResponse` 응답 DTO 완성 | `feat: Create RunCardRequest, CardResponse` |
| 6 | 진행/덱 전체 저장 API 구현 | `feat: GameController updateProgress` |
| 7 | 목록·상세 조회 구현 (`findAllByOrderByIdDesc`, `findAllByGameOrderByIdAsc`) | `feat: getGames(), getGame()` |
| 8 | 더티 체킹 이름 변경 · 자식부터 삭제하는 게임 삭제 | `feat: 이름 변경과 게임 삭제` |
| — | `@Range` → `@Min` / `@Max` 로 교체 (Jakarta 표준 어노테이션 사용) | `refactor: @Range 어노테이션을 @Min @Max로` |

### 도전 기능

| Lv | 내용 | 커밋 |
| --- | --- | --- |
| 9 | 끝난 게임 덮어쓰기 차단 — `isFinished()` 판정 후 409 | `fix: 끝난 게임 덮어쓰기 시 409 응답` |
| 10 | 전역 예외 처리 — 404·409 응답에 `message` 포함 | `fix: 404, 409 에러에 message 붙이기` |
| 11 | JPA Auditing으로 `createdAt` / `updatedAt` 기록 | `feat: createAt, updatedAt` |
| 11 | 목록 조회의 N+1 제거 — `group by` 집계 1회로 `deckSize` 계산 | `perf: 게임 목록 조회의 N+1 제거` |
| 11 | 집계 결과를 인터페이스 프로젝션 → **DTO 프로젝션**으로 변경 | `refactor: 카드 수 집계 조회를 DTO 프로젝션으로 변경` |

> Lv 12(외부 랭킹 API 연동)는 미구현입니다.

---

## 트러블슈팅 & 학습 포인트

### `Connection is read-only` (Lv 4)

`createGame`에 `@Transactional(readOnly = true)`가 걸려 있어 저장 시점에 500이 발생했습니다.
스택트레이스에서 `com.gamebasic` 프레임을 따라가 메서드를 특정하고, 쓰기 트랜잭션으로 수정했습니다.

### 숫자 범위 검증: `@Size` → `@Range` → `@Min`/`@Max` (Lv 5)

`acquiredFloor`(0~10)에 범위 제약을 걸려고 처음에는 `@Size`를 붙였는데 적용되지 않았습니다.
`@Size`는 **문자열 길이나 컬렉션 크기**를 검증하는 어노테이션이라 `Integer`에는 동작하지 않습니다.
이후 `@Range`로 바꿔 동작은 시켰지만, 이것은 Jakarta Bean Validation 표준이 아니라 **Hibernate Validator 전용 확장**이라는 걸 알게 되어
표준 어노테이션인 `@Min`, `@Max` 조합으로 최종 변경했습니다.

```java
@NotBlank
private String cardType;

@Min(0) @Max(10)
private int acquiredFloor;
```

> 정리: [Bean Validation으로 숫자 범위 검증하기](https://hanknag.tistory.com/131)

### 응답 DTO에 `@Getter`가 없어 값이 비어 나간 문제 (Lv 5)

응답 JSON의 필드가 비어 있는데 **예외도 에러 로그도 뜨지 않아** 원인을 찾기 어려웠습니다.
Jackson은 직렬화할 때 getter를 기준으로 필드를 읽는데, DTO에 `@Getter`가 없으면 읽을 프로퍼티가 없다고 판단할 뿐 실패로 보지 않습니다.
그래서 "조용히" 빈 값이 나갑니다. 응답 DTO에 `@Getter`를 붙여 해결했습니다.

> 정리: [응답 DTO에 @Getter가 없을 때 생기는 일](https://hanknag.tistory.com/132)

### JSON 키와 DTO 필드명 불일치 (Lv 5)

요청은 정상적으로 들어오는데 DTO 필드가 계속 `null`이었습니다.
Jackson은 **요청 JSON의 키 이름과 DTO 필드명을 그대로 대조**해 바인딩하기 때문에, 이름이 한 글자라도 다르면 매칭에 실패하고 조용히 기본값이 들어갑니다.
API 명세의 키 이름과 DTO 필드명을 하나씩 대조해 맞췄습니다.

> 정리: [JSON 키와 DTO 필드명이 다르면 생기는 일](https://hanknag.tistory.com/133)

### 생성 시각 / 수정 시각 (Lv 11)

`BaseEntity`에 `@CreatedDate`, `@LastModifiedDate`를 두고 `@EntityListeners(AuditingEntityListener.class)`를 적용,
`GameBasicApplication`에 `@EnableJpaAuditing`을 선언했습니다.

### 게임 목록 조회의 N+1 제거 (Lv 11)

게임마다 `count` 쿼리를 날리면 게임 N개에 쿼리 N+1회가 발생합니다.
`group by`로 **게임 ID별 카드 수를 한 번에 집계**하고, `Map<Long, Long>`으로 변환해 조회하도록 바꿨습니다.

### 인터페이스 프로젝션 → DTO 프로젝션 (Lv 11 리팩터링)

처음에는 getter만 선언한 인터페이스 프로젝션으로 집계 결과를 받았습니다.
동작은 하지만 **런타임에 프록시 객체가 만들어져** 타입이 불투명하고, 별칭(`as gameId`)과 getter 이름이 어긋나도 컴파일 단계에서 잡히지 않습니다.
그래서 집계 전용 DTO 클래스를 만들고 JPQL의 `select new` 구문으로 **생성자에 직접 매핑**하도록 바꿨습니다.

```java
// DeckCount.java — 집계 전용 DTO
@Getter
@RequiredArgsConstructor
public class DeckCount {
    private final Long gameId;
    private final Long cardCount;
}
```

```java
// RunCardRepository.java
@Query("""
    SELECT new com.gamebasic.runcard.repository.DeckCount(rc.game.id, COUNT(rc.id))
    FROM RunCard rc
    WHERE rc.game in :games
    GROUP BY rc.game.id
""")
List<DeckCount> countByGames(List<Game> games);
```

```java
// GameService.java — 게임 ID로 카드 수를 O(1) 조회하도록 Map 변환
Map<Long, Long> deckCountOfGames = counts.stream()
        .collect(Collectors.toMap(DeckCount::getGameId, DeckCount::getCardCount));
```

- `select new` 는 **FQCN(패키지 포함 전체 경로)** 으로 적어야 하고, 생성자 파라미터의 타입·순서가 정확히 맞아야 합니다 (`COUNT()`의 반환 타입은 `Long`)
- 카드가 0장인 게임은 `group by` 결과에 없으므로 `getOrDefault(id, 0L)`로 보정
- 최종 쿼리 수: **게임 조회 1회 + 카드 수 집계 1회 = 총 2회** (게임 수와 무관하게 일정)
