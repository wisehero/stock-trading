# 구현 계획 (초기 세팅 단계)

## 1. 프로젝트 부트스트랩
- Gradle Wrapper/설정 파일 생성
- Spring Boot 메인 애플리케이션 클래스 생성

## 2. 의존성 및 런타임 설정
- Web, Validation, JPA, Actuator, MySQL 드라이버 추가
- Java Toolchain 21 설정
- 기본 `application.yml`에 MySQL 접속 정보 환경변수화

## 3. 로컬 인프라 구성
- `docker-compose.yml`로 MySQL 8.4 구성
- 기본 계정/DB 생성 환경변수 설정

## 4. 테스트 기반 정리
- `contextLoads` 테스트 추가
- 테스트 전용 H2 설정으로 CI/로컬 테스트 독립성 확보

## 5. 검증
- `./gradlew test`
- 필요 시 `docker compose up -d` 후 `./gradlew bootRun`으로 앱 실행 검증
