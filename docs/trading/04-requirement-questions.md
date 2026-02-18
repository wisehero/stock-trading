# 구현 전 필수 확정 질문

## 현재 확정된 항목
- 1차 릴리즈는 100% 현금주문만 지원 (미수/신용 제외)
- 일반주문 범위: 매수/매도 + 시장가/지정가 (예약/시간외/자동전략 제외)
- 체결 주체: 내부 Mock Exchange Engine
- 부분체결: 지원
- 체결 판단 데이터: `mock_quotes` 고정 시세 테이블 사용
- 부분체결 기준: `available_quantity` 기반
- 지정가 체결 조건:
  - 매수: `quote.price <= limitPrice`
  - 매도: `quote.price >= limitPrice`
- 미체결 재매칭 시점: 시세 갱신 API 호출 시

## 주문/체결 정책
- [확정] 부분체결 상태 주문은 즉시 취소 가능
- [확정] 지정가 주문은 `DAY/IOC/FOK`, 시장가 주문은 `IOC` 고정

## 금액/수량 규칙
- 수량 단위(1주 단위만?)
- 호가 단위/가격 반올림 규칙
- [확정] 수수료는 체결 단위 계산, 세금은 2차 미적용(0), 반올림은 소수점 4자리 `HALF_UP`

## 상태/이벤트 모델
- [확정] DB를 상태 전이 소스 오브 트루스로 사용하고 이벤트는 파생 로그로 관리
- [확정] `executionId` 멱등 + 주문 버전 검사로 중복/역순 이벤트 방어
- [확정] 사용자 노출 상태: `NEW`, `PARTIALLY_FILLED`, `FILLED`, `CANCELED`, `REJECTED`, `EXPIRED`

## 운영/장애 대응
- [확정] 이벤트 처리 실패 시 최대 3회 지수 백오프 재시도 후 오류 테이블 적재
- [확정] 체결 처리 타임아웃 3초, 실패 시 `REJECTED`/에러코드 기록
- [확정] 정산 대사 실패건은 `RECONCILIATION_FAILED` 표기 + 수동 재처리 API 제공
- [확정] `DAY` 주문 만료는 장 종료 배치에서 `EXPIRED` 처리

## 2차/3차 확정 참고 문서
- `06-phase2-requirement-lock.md`: Q1~Q4 확정안
- `08-phase3-requirement-lock.md`: Q1~Q6 확정안
