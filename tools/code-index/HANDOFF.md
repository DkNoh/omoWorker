# 핸드오프 브리핑 — 코드 인덱스 파이프라인 (2026-08-09)

## 배경
자바 코드베이스를 "JSON 구조 인덱스 → 의미 태깅 → 작업자 슬라이스 전달" 파이프라인으로
구축하고 A/B 실측으로 검증함. 목표: 에이전트가 코드베이스 탐색 대신 인덱스로 판단하게 하기.

## 산출물 (커밋 완료, 푸시 안 함)
- `80718a6` feat(tools): 코드 구조 인덱스와 의미 태깅 파이프라인 추가
- `d8eb9b1` feat(tools): 코드 인덱스 폐쇄망 이식 준비

`tools/code-index/`
- `build_index.py`: tree-sitter 구조 추출 → `out/index.json` (138파일 → 159타입, 파싱 오류 0)
- `tag_nodes.py`: LM Studio API로 노드별 summary/features 태깅 (구조 해시 캐시로 증분) → `out/tags.json`, `out/index-tagged.json`
- `slice.py`: 기능 질의 → 슬라이스 (노드+deps 1단계+비자바 자산 html/js/mapper XML)
- `refresh.sh`: 전체 재생성 (venv 자동 부트스트랩, vendor/ wheel로 오프라인 설치 지원)
- `README.md`: 사용법·환경변수·폐쇄망 이식 안내

사용 예:
```bash
./tools/code-index/refresh.sh
tools/code-index/.venv/bin/python tools/code-index/slice.py CustomerSearch --out /tmp/slice.json
```

## 검증된 핵심 사실
- A/B 실측: 인덱스 없으면 17.5분 타임아웃·응답 미제출. 슬라이스+규율 프리앰블이면 4분 38초에 만점급 영향도 분석 제출.
- qwen3.6-27b는 추론 모델: API 호출 시 토큰 예산을 '생각'으로 소진해 빈 응답 반환 → payload에 `"reasoning_effort": "none"` 필수 (환경변수 CODE_INDEX_REASONING_EFFORT, 빈 값이면 생략).
- 환경변수 오버라이드: `CODE_INDEX_LMSTUDIO_URL` (기본값 http://100.120.61.117:1234/v1 — 현 환경 전용 Tailscale), `CODE_INDEX_MODEL` (기본 qwen/qwen3.6-27b).
- 폐쇄망 이식 준비 완료: vendor/ wheel 번들 포함. 폐쇄망의 Continue 공용 LLM 서버를 재활용 예정, 모델 ID는 미확인.
- 27B 작업자 디스패치 규율 프리앰블 필수 요소: 읽기 전용 명시, 슬라이스 우선, 추가 소스 읽기 최대 3파일, 범위 밖 탐색 금지, 이중 보고(파일 + worker_done).
- worker_done 미수신 시 회수법: 터미널 닫고 ~/.qwen/projects/<project>/chats/<session>.jsonl 의 assistant 메시지에서 결과 추출.

## 다음 단계 후보 (우선순위순)
1. 인덱스를 이용한 실전 개발 작업 디스패치 (아직 미사용 — 본게임)
2. MyBatis XML 파싱을 인덱스에 추가 (statement id/테이블/동적 조건 — 제안됐고 대기 중)
3. 스캐폴드 화면 생성 후 refresh.sh 자동 연결
4. 메서드 수준 호출 그래프 정밀화 (선택, 급하지 않음)

## 기록 위치
- git log, tools/code-index/README.md
- Orca Run: run_bb0a7fbbf770 (Task/Dispatch/메시지 전체 이력, 코디네이터 터미널 term_4273efe7)
- ~/.qwen 메모리: 모델 라우팅 기준, 인덱스 슬라이스 효과, LM Studio API 함정
