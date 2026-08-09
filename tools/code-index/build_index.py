#!/usr/bin/env python3
"""Build a JSON structure index for the repository's Java sources."""

from __future__ import annotations

import json
import os
import re
import sys
from collections import Counter, defaultdict
from datetime import datetime, timezone
from pathlib import Path

from tree_sitter import Language, Node, Parser
import tree_sitter_java


REPO_ROOT = Path(__file__).resolve().parents[2]
OUTPUT_FILE = REPO_ROOT / "tools" / "code-index" / "out" / "index.json"
SOURCE_ROOTS = (
    ("main", REPO_ROOT / "src" / "main" / "java"),
    ("test", REPO_ROOT / "src" / "test" / "java"),
)
TYPE_KINDS = {
    "class_declaration": "class",
    "interface_declaration": "interface",
    "enum_declaration": "enum",
    "record_declaration": "record",
    "annotation_type_declaration": "annotation",
}
ANNOTATION_NODES = {"annotation", "marker_annotation"}


def node_text(node: Node | None, source: bytes) -> str:
    if node is None:
        return ""
    return source[node.start_byte : node.end_byte].decode("utf-8")


def compact_text(node: Node | None, source: bytes) -> str:
    return re.sub(r"\s+", " ", node_text(node, source)).strip()


def child_of_type(node: Node, *types: str) -> Node | None:
    return next((child for child in node.named_children if child.type in types), None)


def declaration_modifiers(node: Node, source: bytes) -> tuple[list[str], list[str]]:
    modifiers_node = child_of_type(node, "modifiers")
    if modifiers_node is None:
        return [], []

    modifiers: list[str] = []
    annotations: list[str] = []
    for child in modifiers_node.children:
        if child.type in ANNOTATION_NODES:
            name = child.child_by_field_name("name")
            annotation = compact_text(name, source).split(".")[-1]
            if annotation:
                annotations.append(annotation)
        elif not child.is_named or child.type not in {"line_comment", "block_comment"}:
            value = compact_text(child, source)
            if value and not value.startswith("@"):
                modifiers.append(value)
    return list(dict.fromkeys(modifiers)), list(dict.fromkeys(annotations))


def type_identifiers(node: Node | None, source: bytes) -> set[str]:
    if node is None:
        return set()
    identifiers: set[str] = set()
    stack = [node]
    while stack:
        current = stack.pop()
        if current.type in {"type_identifier", "identifier", "scoped_type_identifier"}:
            value = compact_text(current, source)
            identifiers.add(value)
            identifiers.add(value.split(".")[-1])
        stack.extend(current.named_children)
    return {value for value in identifiers if value}


def parameter_text(parameter: Node, source: bytes) -> str:
    type_node = parameter.child_by_field_name("type")
    name_node = parameter.child_by_field_name("name")
    type_text = compact_text(type_node, source)
    name_text = compact_text(name_node, source)
    raw = compact_text(parameter, source)
    if parameter.type == "spread_parameter" and "..." not in type_text:
        type_text += "..."
    dimensions = child_of_type(parameter, "dimensions")
    if dimensions is not None:
        type_text += compact_text(dimensions, source)
    if type_text:
        return f"{type_text} {name_text}".strip()
    return raw


def extract_method(node: Node, source: bytes) -> tuple[dict, set[str]]:
    modifiers, annotations = declaration_modifiers(node, source)
    return_node = node.child_by_field_name("type")
    name_node = node.child_by_field_name("name")
    parameters_node = node.child_by_field_name("parameters")
    parameters: list[str] = []
    references = type_identifiers(return_node, source)
    if parameters_node is not None:
        for parameter in parameters_node.named_children:
            if parameter.type in {
                "formal_parameter",
                "spread_parameter",
                "receiver_parameter",
            }:
                parameters.append(parameter_text(parameter, source))
                references.update(type_identifiers(parameter.child_by_field_name("type"), source))

    return_type = compact_text(return_node, source)
    name = compact_text(name_node, source)
    signature = f"{return_type} {name}({', '.join(parameters)})".strip()
    return (
        {
            "name": name,
            "signature": signature,
            "modifiers": modifiers,
            "annotations": annotations,
        },
        references,
    )


def extract_fields(body: Node | None, source: bytes) -> tuple[list[dict], set[str]]:
    fields: list[dict] = []
    references: set[str] = set()
    if body is None:
        return fields, references
    for declaration in body.named_children:
        if declaration.type not in {"field_declaration", "constant_declaration"}:
            continue
        field_type_node = declaration.child_by_field_name("type")
        field_type = compact_text(field_type_node, source)
        references.update(type_identifiers(field_type_node, source))
        _, annotations = declaration_modifiers(declaration, source)
        for declarator in declaration.named_children:
            if declarator.type != "variable_declarator":
                continue
            name = compact_text(declarator.child_by_field_name("name"), source)
            dimensions = child_of_type(declarator, "dimensions")
            declared_type = field_type + compact_text(dimensions, source)
            fields.append(
                {"name": name, "type": declared_type, "annotations": annotations}
            )
    return fields, references


def extract_record_components(
    declaration: Node, source: bytes
) -> tuple[list[dict], set[str]]:
    fields: list[dict] = []
    references: set[str] = set()
    parameters = declaration.child_by_field_name("parameters")
    if parameters is None:
        return fields, references
    for parameter in parameters.named_children:
        if parameter.type not in {"formal_parameter", "spread_parameter"}:
            continue
        type_node = parameter.child_by_field_name("type")
        field_type = compact_text(type_node, source)
        if parameter.type == "spread_parameter":
            field_type += "..."
        _, annotations = declaration_modifiers(parameter, source)
        fields.append(
            {
                "name": compact_text(parameter.child_by_field_name("name"), source),
                "type": field_type,
                "annotations": annotations,
            }
        )
        references.update(type_identifiers(type_node, source))
    return fields, references


def extract_annotation_element(node: Node, source: bytes) -> tuple[dict, set[str]]:
    modifiers, annotations = declaration_modifiers(node, source)
    return_node = node.child_by_field_name("type")
    name = compact_text(node.child_by_field_name("name"), source)
    return (
        {
            "name": name,
            "signature": f"{compact_text(return_node, source)} {name}()",
            "modifiers": modifiers,
            "annotations": annotations,
        },
        type_identifiers(return_node, source),
    )


def list_clause_types(node: Node | None, source: bytes) -> list[str]:
    if node is None:
        return []
    return [
        compact_text(child, source)
        for child in node.named_children
        if child.type not in {"annotation"}
    ]


def extract_type_node(
    declaration: Node,
    source: bytes,
    package: str,
    relative_file: str,
    source_set: str,
    outer_names: tuple[str, ...],
) -> dict:
    name = compact_text(declaration.child_by_field_name("name"), source)
    qualified_names = (*outer_names, name)
    local_name = ".".join(qualified_names)
    node_id = f"{package}.{local_name}" if package else local_name
    modifiers, annotations = declaration_modifiers(declaration, source)
    body = declaration.child_by_field_name("body")
    fields, references = extract_fields(body, source)
    if declaration.type == "record_declaration":
        component_fields, component_references = extract_record_components(
            declaration, source
        )
        fields = component_fields + fields
        references.update(component_references)
    methods: list[dict] = []
    if body is not None:
        for member in body.named_children:
            if member.type == "method_declaration":
                method, method_references = extract_method(member, source)
                methods.append(method)
                references.update(method_references)
            elif member.type == "annotation_type_element_declaration":
                method, method_references = extract_annotation_element(member, source)
                methods.append(method)
                references.update(method_references)

    superclass_node = declaration.child_by_field_name("superclass")
    extends_types = list_clause_types(superclass_node, source)
    if declaration.type == "interface_declaration":
        extends_node = declaration.child_by_field_name("extends_interfaces")
        if extends_node is None:
            extends_node = child_of_type(declaration, "extends_interfaces")
        extends_types = list_clause_types(extends_node, source)
    interfaces_node = declaration.child_by_field_name("interfaces")
    if interfaces_node is None:
        interfaces_node = child_of_type(declaration, "super_interfaces")
    implements = list_clause_types(interfaces_node, source)
    references.update(type_identifiers(superclass_node, source))
    references.update(type_identifiers(interfaces_node, source))
    if declaration.type == "interface_declaration":
        references.update(type_identifiers(extends_node, source))

    return {
        "id": node_id,
        "kind": TYPE_KINDS[declaration.type],
        "name": name,
        "package": package,
        "file": relative_file,
        "source_set": source_set,
        "lines": [declaration.start_point.row + 1, declaration.end_point.row + 1],
        "modifiers": modifiers,
        "annotations": annotations,
        "extends": ", ".join(extends_types) if extends_types else None,
        "implements": implements,
        "fields": fields,
        "methods": methods,
        "deps": [],
        "_references": sorted(references),
    }


def find_declarations(
    node: Node,
    source: bytes,
    package: str,
    relative_file: str,
    source_set: str,
    outer_names: tuple[str, ...] = (),
) -> list[dict]:
    results: list[dict] = []
    for child in node.named_children:
        if child.type in TYPE_KINDS:
            extracted = extract_type_node(
                child, source, package, relative_file, source_set, outer_names
            )
            results.append(extracted)
            results.extend(
                find_declarations(
                    child,
                    source,
                    package,
                    relative_file,
                    source_set,
                    (*outer_names, extracted["name"]),
                )
            )
        else:
            results.extend(
                find_declarations(
                    child,
                    source,
                    package,
                    relative_file,
                    source_set,
                    outer_names,
                )
            )
    return results


def parse_package(root: Node, source: bytes) -> str:
    declaration = child_of_type(root, "package_declaration")
    if declaration is None:
        return ""
    value = compact_text(declaration, source)
    return value.removeprefix("package ").removesuffix(";").strip()


def parse_imports(root: Node, source: bytes) -> list[str]:
    imports: list[str] = []
    for child in root.named_children:
        if child.type != "import_declaration":
            continue
        value = compact_text(child, source).removeprefix("import ").removesuffix(";")
        value = value.removeprefix("static ").strip()
        imports.append(value)
    return imports


def resolve_dependencies(nodes: list[dict], file_imports: dict[str, list[str]]) -> None:
    ids = {node["id"] for node in nodes}
    by_package_and_name: dict[tuple[str, str], set[str]] = defaultdict(set)
    for node in nodes:
        by_package_and_name[(node["package"], node["name"])].add(node["id"])

    for node in nodes:
        dependencies: set[str] = set()
        imports = file_imports[node["file"]]
        wildcard_packages: set[str] = set()
        for imported in imports:
            if not imported.startswith("com.scbk.sms."):
                continue
            if imported.endswith(".*"):
                wildcard_packages.add(imported[:-2])
                continue
            candidate = imported
            while candidate.startswith("com.scbk.sms."):
                if candidate in ids:
                    dependencies.add(candidate)
                    break
                candidate = candidate.rpartition(".")[0]

        for reference in node.pop("_references"):
            simple_name = reference.split(".")[-1]
            dependencies.update(
                by_package_and_name.get((node["package"], simple_name), set())
            )
            for imported in imports:
                if imported.endswith(f".{simple_name}") and imported in ids:
                    dependencies.add(imported)
            for wildcard_package in wildcard_packages:
                candidate = f"{wildcard_package}.{simple_name}"
                if candidate in ids:
                    dependencies.add(candidate)
            if reference in ids:
                dependencies.add(reference)
            for candidate in ids:
                if candidate in reference:
                    dependencies.add(candidate)
        dependencies.discard(node["id"])
        node["deps"] = sorted(dependencies)


def main() -> None:
    language = Language(tree_sitter_java.language())
    parser = Parser(language)
    nodes: list[dict] = []
    parse_error_files: list[str] = []
    file_imports: dict[str, list[str]] = {}
    java_files: list[tuple[str, Path]] = []
    for source_set, source_root in SOURCE_ROOTS:
        if source_root.exists():
            java_files.extend((source_set, path) for path in source_root.rglob("*.java"))
    java_files.sort(key=lambda item: item[1].as_posix())

    for source_set, path in java_files:
        relative_file = path.relative_to(REPO_ROOT).as_posix()
        source = path.read_bytes()
        tree = parser.parse(source)
        if tree.root_node.has_error:
            parse_error_files.append(relative_file)
        package = parse_package(tree.root_node, source)
        file_imports[relative_file] = parse_imports(tree.root_node, source)
        nodes.extend(
            find_declarations(
                tree.root_node, source, package, relative_file, source_set
            )
        )
        del tree

    nodes.sort(key=lambda item: item["id"])
    resolve_dependencies(nodes, file_imports)
    kind_counts = Counter(node["kind"] for node in nodes)
    package_counts = Counter(node["package"] for node in nodes)
    index = {
        "generated_at": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "repo": REPO_ROOT.name,
        "stats": {
            "files": len(java_files),
            "types": len(nodes),
            "parse_error_files": parse_error_files,
            "by_kind": {
                kind: kind_counts[kind]
                for kind in ("class", "interface", "enum", "record", "annotation")
            },
            "by_package": dict(sorted(package_counts.items())),
        },
        "nodes": nodes,
    }
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)
    temporary_file = OUTPUT_FILE.with_suffix(".json.tmp")
    temporary_file.write_text(
        json.dumps(index, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
    )
    temporary_file.replace(OUTPUT_FILE)
    print(
        f"Indexed {len(java_files)} Java files and {len(nodes)} types into "
        f"{OUTPUT_FILE.relative_to(REPO_ROOT)}"
    )
    # Release native tree-sitter objects in dependency order.  This avoids
    # relying on Python's interpreter-shutdown order for extension objects.
    del parser
    del language


if __name__ == "__main__":
    main()
    # tree-sitter-java 0.23.5 can crash nondeterministically while Python 3.14
    # tears down extension objects during cyclic GC.  All output is committed
    # atomically above, so skip interpreter finalization after flushing stdout.
    sys.stdout.flush()
    os._exit(0)
