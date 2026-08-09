#!/usr/bin/env python3
"""LM Studio API로 index.json 노드에 의미 태그(summary/features)를 부착한다.

- 노드 구조의 sha256 해시로 캐시 → 코드 변경 노드만 재태깅(증분/재개 가능)
- 출력: out/tags.json(태그 사전), --merge 시 out/index-tagged.json(병합본)
"""
import argparse
import hashlib
import json
import os
import sys
import time
import urllib.error
import urllib.request
from pathlib import Path

BASE = Path(__file__).resolve().parent
INDEX = BASE / "out" / "index.json"
CACHE = BASE / "out" / "tags-cache.json"
TAGS = BASE / "out" / "tags.json"
MERGED = BASE / "out" / "index-tagged.json"

API_URL = os.environ.get("CODE_INDEX_LMSTUDIO_URL", "http://100.120.61.117:1234/v1") + "/chat/completions"
MODEL = os.environ.get("CODE_INDEX_MODEL", "qwen/qwen3.6-27b")
# 빈 문자열이면 요청에서 생략(파라미터 미지원 서버 대비)
REASONING_EFFORT = os.environ.get("CODE_INDEX_REASONING_EFFORT", "none")

SYSTEM = (
    "너는 Java 클래스의 책임을 분석하는 분석기다. "
    "반드시 summary와 features 두 키만 가진 JSON 객체로만 답하라. "
    "summary는 클래스의 책임을 나타내는 한국어 한 문장(50자 이내), "
    "features는 주요 기능을 나타내는 짧은 한국어 태그 배열(2~5개, 각 12자 이내)이다."
)


def node_key(node):
    payload = json.dumps(
        {k: node.get(k) for k in ("id", "kind", "annotations", "extends", "implements", "fields", "methods")},
        sort_keys=True,
        ensure_ascii=False,
    )
    return hashlib.sha256(payload.encode()).hexdigest()[:16]


def render(node):
    lines = [f"type: {node['kind']} {node['id']}"]
    if node.get("annotations"):
        lines.append("annotations: " + ", ".join(node["annotations"]))
    if node.get("extends"):
        lines.append("extends: " + str(node["extends"]))
    if node.get("implements"):
        lines.append("implements: " + ", ".join(node["implements"]))
    for f in (node.get("fields") or [])[:20]:
        lines.append(f"field: {f.get('type')} {f.get('name')}")
    for m in (node.get("methods") or [])[:40]:
        lines.append("method: " + str(m.get("signature") or m.get("name") or ""))
    return "\n".join(lines)


def call(prompt, retries=2):
    payload = {
        "model": MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM},
            {"role": "user", "content": prompt},
        ],
        "temperature": 0.2,
        "max_tokens": 512,
    }
    if REASONING_EFFORT:
        payload["reasoning_effort"] = REASONING_EFFORT
    body = json.dumps(payload).encode()
    last = None
    for attempt in range(retries + 1):
        try:
            req = urllib.request.Request(API_URL, data=body, headers={"Content-Type": "application/json"})
            with urllib.request.urlopen(req, timeout=180) as r:
                data = json.loads(r.read().decode())
            return data["choices"][0]["message"]["content"]
        except (urllib.error.URLError, TimeoutError, KeyError, json.JSONDecodeError) as e:
            last = e
            if attempt < retries:
                time.sleep(2 * (attempt + 1))
    raise RuntimeError(f"API 호출 실패: {last}")


def parse_tags(text):
    text = text.strip()
    if text.startswith("```"):
        text = text.split("\n", 1)[1].rsplit("```", 1)[0]
    start, end = text.find("{"), text.rfind("}")
    if start == -1 or end == -1:
        raise ValueError(f"JSON 객체 없음: {text[:80]!r}")
    obj = json.loads(text[start : end + 1])
    summary = str(obj.get("summary", "")).strip()
    features = [str(x).strip() for x in obj.get("features", []) if str(x).strip()]
    if not summary:
        raise ValueError("summary 비어 있음")
    return {"summary": summary, "features": features[:5]}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0, help="파일럿용 선두 N개 노드만 처리")
    ap.add_argument("--merge", action="store_true", help="index-tagged.json 병합본 생성")
    args = ap.parse_args()

    index = json.loads(INDEX.read_text(encoding="utf-8"))
    nodes = index["nodes"]
    if args.limit:
        nodes = nodes[: args.limit]

    cache = json.loads(CACHE.read_text(encoding="utf-8")) if CACHE.exists() else {}
    tags = json.loads(TAGS.read_text(encoding="utf-8")) if TAGS.exists() else {}
    failures = []
    hit = 0

    for i, node in enumerate(nodes, 1):
        nid = node["id"]
        key = node_key(node)
        if key in cache:
            tags[nid] = cache[key]
            hit += 1
        else:
            try:
                tags[nid] = parse_tags(call(render(node)))
                cache[key] = tags[nid]
            except Exception as e:
                failures.append({"id": nid, "error": str(e)})
                print(f"[{i}/{len(nodes)}] FAIL {nid}: {e}", file=sys.stderr)
        if i % 10 == 0 or i == len(nodes):
            CACHE.write_text(json.dumps(cache, ensure_ascii=False, indent=1), encoding="utf-8")
            TAGS.write_text(json.dumps(tags, ensure_ascii=False, indent=1), encoding="utf-8")
            print(f"[{i}/{len(nodes)}] 진행 (캐시 적중 {hit}, 실패 {len(failures)})")

    if args.merge:
        for node in index["nodes"]:
            t = tags.get(node["id"])
            if t:
                node["summary"] = t["summary"]
                node["features"] = t["features"]
        MERGED.write_text(json.dumps(index, ensure_ascii=False, indent=1), encoding="utf-8")
        print(f"병합본 작성: {MERGED}")

    print(f"완료: 태깅 {len(tags)}개 노드, 캐시 적중 {hit}, 실패 {len(failures)}")
    if failures:
        sys.exit(1)


if __name__ == "__main__":
    main()
