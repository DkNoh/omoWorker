#!/usr/bin/env bash
# 코드 인덱스 재생성: venv 자동 구성 → 구조 추출 → 의미 태깅(증분) → 병합
# 산출물: out/index.json, out/tags.json, out/index-tagged.json
# 환경변수: CODE_INDEX_LMSTUDIO_URL, CODE_INDEX_MODEL, CODE_INDEX_REASONING_EFFORT
set -euo pipefail
cd "$(dirname "$0")"

PY=.venv/bin/python
if [ ! -x "$PY" ]; then
  python3 -m venv .venv
  if [ -d vendor ] && ls vendor/*.whl >/dev/null 2>&1; then
    # 폐쇄망 등 오프라인 설치
    "$PY" -m pip install --no-index --find-links vendor -r requirements.txt
  else
    "$PY" -m pip install -r requirements.txt
  fi
fi

"$PY" build_index.py
"$PY" tag_nodes.py --merge
echo "완료: out/index.json, out/tags.json, out/index-tagged.json"
