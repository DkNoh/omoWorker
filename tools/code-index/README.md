# code-index

자바 코드베이스를 JSON 구조 인덱스로 변환하고, 로컬 LLM으로 의미 태그를 붙여
에이전트 작업자에게 최소 컨텍스트(슬라이스)를 전달하는 파이프라인.

## 구성

| 파일 | 역할 |
|---|---|
| `build_index.py` | tree-sitter로 자바 구조 추출 → `out/index.json` (결정적, 멱등) |
| `tag_nodes.py` | LM Studio API로 노드별 summary/features 태깅 → `out/tags.json`, `out/index-tagged.json` (구조 해시 캐시로 증분) |
| `slice.py` | 기능 질의 → 노드+deps 1단계+비자바 자산 슬라이스 |
| `refresh.sh` | venv 자동 구성(오프라인 wheel 지원) + 전체 재생성 |

## 사용

```bash
./refresh.sh                                   # 전체 재생성 (변경 노드만 재태깅)
.venv/bin/python slice.py CustomerSearch --out /tmp/slice.json
```

## 환경변수 (tag_nodes.py)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `CODE_INDEX_LMSTUDIO_URL` | `http://100.120.61.117:1234/v1` | OpenAI 호환 API base URL |
| `CODE_INDEX_MODEL` | `qwen/qwen3.6-27b` | 태깅 모델 ID |
| `CODE_INDEX_REASONING_EFFORT` | `none` | 추론 모델용. 빈 문자열이면 요청에서 생략(미지원 서버 대비) |

## 폐쇄망 이식

- 외부 통신 없음. 유일한 호출은 `CODE_INDEX_LMSTUDIO_URL` 한 곳.
- `vendor/`에 wheel 번들 포함 — 인터넷 없이 `refresh.sh`가 venv를 자동 구성.
  (wheel은 linux x86_64 기준. `tree_sitter`는 cp314 전용이라 Python 3.14 필요,
  버전이 다르면 해당 버전 wheel로 교체)
- 로컬 LLM 서버가 없으면 `tag_nodes.py`만 건너뛰면 됨 — 구조 인덱스와 슬라이스는
  태그 없이도 완전 동작.
- 공용 LLM 서버(예: Continue 공용 서버)를 쓸 때는 순차 호출 유지, 과부하 주의.
