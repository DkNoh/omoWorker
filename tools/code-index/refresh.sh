#!/usr/bin/env bash
# 코드 인덱스 재생성: 구조 추출 → 의미 태깅(구조 해시 캐시로 변경 노드만 재태깅) → 병합
# 산출물: out/index.json, out/tags.json, out/index-tagged.json
set -euo pipefail
cd "$(dirname "$0")"

PY=.venv/bin/python
if [ ! -x "$PY" ]; then
  echo "venv 없음: python3 -m venv .venv && .venv/bin/pip install tree-sitter tree-sitter-java" >&2
  exit 1
fi

"$PY" build_index.py
"$PY" tag_nodes.py --merge
echo "완료: out/index.json, out/tags.json, out/index-tagged.json"
