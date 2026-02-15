# 주문 상태 전이

## 컴포넌트 경계
- Broker Core: 주문 검증, 선점, 주문 상태 저장, 체결 이벤트 반영
- Mock Exchange Engine: 체결 판단 및 체결 이벤트 생성

## 대표 상태
- `PENDING_NEW`: 주문 생성 요청 수신
- `NEW`: 주문 접수 완료(거래소/브로커에 유효 접수)
- `REJECTED`: 주문 거부
- `PARTIALLY_FILLED`: 부분체결
- `FILLED`: 완전체결
- `PENDING_CANCEL`: 취소 요청 접수
- `CANCELED`: 취소 완료
- `EXPIRED`: 유효기간 만료

## 전이 예시
- `PENDING_NEW` -> `NEW`
- `PENDING_NEW` -> `REJECTED`
- `NEW` -> `PARTIALLY_FILLED`
- `NEW` -> `FILLED`
- `PARTIALLY_FILLED` -> `FILLED`
- `NEW` -> `PENDING_CANCEL` -> `CANCELED`
- `PARTIALLY_FILLED` -> `PENDING_CANCEL` -> `CANCELED` (미체결 잔량만 취소)

## 체결 판단 규칙 (1차)
- 시세 소스: `mock_quotes` 테이블
- 시장가:
  - 매수: 현재 매도호가 수량 범위 내 즉시 체결
  - 매도: 현재 매수호가 수량 범위 내 즉시 체결
- 지정가:
  - 매수: `quote.price <= limitPrice`일 때 체결
  - 매도: `quote.price >= limitPrice`일 때 체결
- 부분체결: `available_quantity < remaining_quantity`이면 가능한 수량만 체결
- 재매칭 트리거: 시세 갱신 API 호출 시

## 주의점
- 상태 전이는 이벤트 기반으로 순서 역전(out-of-order) 가능성이 있으므로 버전/시퀀스 체크가 필요
- 동일 이벤트 재수신(중복) 대비 멱등 처리 필요
