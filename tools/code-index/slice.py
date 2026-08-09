#!/usr/bin/env python3
"""index-tagged.json에서 기능 범위를 질의해 작업자용 슬라이스를 만든다.

자바 노드(질의 매칭 + 프로젝트 내부 deps 1단계)와 함께
기능 관련 비자바 자산(화면 템플릿/JS/MyBatis 매퍼 XML)을 연결해 포함한다.

사용 예:
  slice.py CustomerSearch --out /tmp/slice.json
  slice.py CustomerSearch --asset-pattern customer-search
"""
import argparse
import json
import re
from pathlib import Path

BASE = Path(__file__).resolve().parent
REPO = BASE.parent.parent
INDEX = BASE / "out" / "index-tagged.json"

ASSET_ROOTS = [
    ("template", REPO / "src" / "main" / "resources" / "templates"),
    ("js", REPO / "src" / "main" / "resources" / "static"),
    ("mapper_xml", REPO / "src" / "main" / "resources" / "mapper"),
]


def kebab(name):
    return re.sub(r"(?<=[a-z0-9])(?=[A-Z])", "-", name).lower()


def asset_patterns(query, explicit):
    if explicit:
        base = [explicit]
    else:
        base = [query]
    out = set()
    for b in base:
        out.add(b.lower())
        out.add(kebab(b))
    return out


NAMESPACE_RE = re.compile(r'namespace\s*=\s*"([^"]+)"')


def find_assets(patterns, index_nodes):
    node_ids = {n["id"] for n in index_nodes}
    assets = []
    for kind, root in ASSET_ROOTS:
        if not root.exists():
            continue
        for path in sorted(root.rglob("*")):
            if not path.is_file():
                continue
            name_low = path.name.lower()
            if not any(p in name_low for p in patterns):
                continue
            linked_to = None
            if kind == "mapper_xml":
                text = path.read_text(encoding="utf-8", errors="replace")
                m = NAMESPACE_RE.search(text)
                if not m or m.group(1) not in node_ids:
                    continue
                linked_to = m.group(1)
            rel = str(path.relative_to(REPO))
            assets.append({"path": rel, "kind": kind, "linked_to": linked_to})
    return assets


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("query", help="노드 이름/id/패키지 부분 일치 질의")
    ap.add_argument("--asset-pattern", default="", help="자산 파일명 매칭 패턴(기본: 질의어의 카밤파생 변형)")
    ap.add_argument("--out", default="", help="출력 파일 경로(기본: stdout)")
    args = ap.parse_args()

    index = json.loads(INDEX.read_text(encoding="utf-8"))
    all_nodes = {n["id"]: n for n in index["nodes"]}

    matched = [n for n in index["nodes"] if args.query.lower() in (n["id"] + " " + n["name"] + " " + n.get("package", "")).lower()]
    if not matched:
        raise SystemExit(f"매칭 노드 없음: {args.query}")

    selected = {n["id"]: n for n in matched}
    for n in matched:
        for dep in n.get("deps", []):
            if dep in all_nodes:
                selected.setdefault(dep, all_nodes[dep])

    patterns = asset_patterns(args.query, args.asset_pattern)
    assets = find_assets(patterns, list(selected.values()))

    slim = [
        {
            "id": n["id"],
            "kind": n["kind"],
            "name": n["name"],
            "file": n["file"],
            "summary": n.get("summary"),
            "features": n.get("features"),
            "annotations": n.get("annotations"),
            "extends": n.get("extends"),
            "implements": n.get("implements"),
            "deps": n.get("deps"),
            "methods": [{"name": m.get("name"), "signature": m.get("signature")} for m in (n.get("methods") or [])],
        }
        for n in selected.values()
    ]

    out = {"scope": args.query, "nodes": slim, "assets": assets}
    text = json.dumps(out, ensure_ascii=False, indent=1)
    if args.out:
        Path(args.out).write_text(text, encoding="utf-8")
        print(f"슬라이스 작성: 노드 {len(slim)}개, 자산 {len(assets)}개 -> {args.out}")
    else:
        print(text)


if __name__ == "__main__":
    main()
