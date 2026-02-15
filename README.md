# stock-trading

주식매매 프로그램 백엔드 초기 세팅 프로젝트입니다.

## Stack
- Java 21
- Spring Boot 3.5.9
- Gradle (Groovy DSL)
- MySQL 8.4 (Docker)

## Run MySQL
```bash
docker compose up -d
```

`3306` 포트가 이미 사용 중이면 아래처럼 호스트 포트를 바꿔 실행할 수 있습니다.

```bash
MYSQL_HOST_PORT=3307 docker compose up -d
```

## Run App
```bash
./gradlew bootRun
```

## Test
```bash
./gradlew test
```

## API Response Rule
`ResponseEntity`를 사용하지 않고, 모든 API 응답 바디는 아래 표준 형식을 사용합니다.

```json
{
  "code": "COMMON-200",
  "message": "요청이 성공했습니다.",
  "data": {}
}
```

- 성공: HTTP `200/201/204` + `COMMON-2xx` 코드
- 실패: HTTP 상태코드 유지 + 문자열 에러코드(`COMMON-400`, `COMMON-500` 등)

## Environment Variables
기본값은 `application.yml`과 동일하며, 필요 시 환경변수로 덮어쓸 수 있습니다.

- `DB_HOST` (default: `localhost`)
- `DB_PORT` (default: `3306`)
- `DB_NAME` (default: `trading`)
- `DB_USERNAME` (default: `trading_user`)
- `DB_PASSWORD` (default: `trading_password`)
- `MYSQL_HOST_PORT` (default: `3306`, docker host port)
