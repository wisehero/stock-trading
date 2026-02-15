# 요구사항 정리 (초기 세팅 단계)

## 목표
주식매매 프로그램 개발을 위한 초기 실행 가능한 백엔드 기반 환경을 구축한다.

## 기술 스택
- Java 21
- Spring Boot 3.5.9
- Docker
- MySQL 8.4
- Gradle (Groovy DSL)

## 사용자 확정 사항
- 프로젝트 경로/이름: `/Users/wisehero/Documents/GitHub/stock-trading`
- 빌드 도구: Gradle Groovy DSL
- 기본 패키지: `com.wisehero.stocktrading`
- 초기 구조: 단일 모듈 모놀리식
- DB 컨테이너: MySQL 8.4, DB명 `trading`

## 완료 기준 (세팅 단계)
- 프로젝트가 Gradle 기반으로 빌드 가능해야 한다.
- 테스트(`./gradlew test`)가 통과해야 한다.
- Docker Compose로 MySQL 컨테이너 기동이 가능해야 한다.
- 애플리케이션이 MySQL 연결 설정을 가져야 한다.
