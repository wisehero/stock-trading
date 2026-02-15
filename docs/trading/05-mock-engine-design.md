# 브로커 코어 + 내부 모의 거래소 설계 (1차)

## 목표
- 외부 브로커/거래소 없이 주문-체결-후처리의 핵심 도메인 흐름을 검증한다.
- 브로커 코어와 내부 모의 거래소 경계를 명확히 분리한다.

## 아키텍처
- 브로커 코어:
  - 주문 API 수신
  - 사전체크(현금/수량)
  - 선점(hold)
  - 주문 상태 저장
  - 내부 모의 거래소 이벤트 반영
- 내부 모의 매칭 엔진:
  - 주문 체결 가능 여부 판단
  - 부분체결/완전체결 계산
  - 체결 이벤트 생성
  - 시세 갱신 시 재매칭

## 컴포넌트 인터페이스
- `OrderExecutionGateway` (브로커 코어 -> 모의 거래소)
  - `submit(Order order)`
  - `cancel(Order order)`
- `ExecutionEventPublisher` (모의 거래소 -> 브로커 코어)
  - `publishAccepted(...)`
  - `publishRejected(...)`
  - `publishPartiallyFilled(...)`
  - `publishFilled(...)`
  - `publishCanceled(...)`

## 데이터 모델 (초안)
- `orders`
  - `id`, `account_id`, `symbol`, `side`, `order_type`, `limit_price`, `quantity`
  - `filled_quantity`, `remaining_quantity`, `status`, `created_at`, `updated_at`
- `order_holds`
  - `id`, `order_id`, `hold_type(CASH|QUANTITY)`, `hold_amount`, `released_amount`
- `fills`
  - `id`, `order_id`, `fill_price`, `fill_quantity`, `fee_amount`, `tax_amount`, `filled_at`
- `mock_quotes`
  - `symbol`, `price`, `available_quantity`, `updated_at`

## 체결 규칙
- 시장가:
  - 매수/매도 모두 현재 시세 기준으로 즉시 체결 시도
- 지정가:
  - 매수: `quote.price <= limitPrice`
  - 매도: `quote.price >= limitPrice`
- 체결 수량:
  - `min(order.remaining_quantity, quote.available_quantity)`
- 상태 전이:
  - 체결 수량이 0이면 상태 유지(`NEW` 또는 `PARTIALLY_FILLED`)
  - 일부 체결이면 `PARTIALLY_FILLED`
  - 잔량 0이면 `FILLED`

## 재매칭 정책
- 트리거: `mock_quotes` 갱신 API 호출 시
- 대상: `NEW`, `PARTIALLY_FILLED` 상태 주문
- 처리 순서: 생성 시각 오름차순(FIFO)

## 멱등/정합성
- 이벤트는 `event_id` 기반 중복 처리 방지
- 동일 주문의 상태 변경은 낙관적 락(버전 필드)으로 보호
- 체결/선점/잔고 반영은 트랜잭션으로 묶는다.

## 비기능
- 1차는 단일 인스턴스 기준
- 성능 목표보다 정합성을 우선
- 운영 확장 시 모의 거래소를 외부 서비스로 분리 가능한 구조 유지
