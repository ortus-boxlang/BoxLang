#!/usr/bin/env python3
"""Strip optional trailing semicolons from BoxLang code blocks in workbench/samples.
Keeps semicolons that are required: property declarations, body-less script components (bx:xxx ...;),
for(;;) headers, and anything inside non-BoxLang fences (xml, html, sql, json, bash, etc).
Usage: strip_sample_semicolons.py <dir> [--dry-run]
"""
import re, sys, pathlib

BOX_LANGS = {"java", "js", "javascript", "boxlang", "bx", ""}
KEEP = [
    re.compile(r"^\s*property\b"),          # property declarations require ;
    re.compile(r"\bbx:\w+"),                # script components (bodyless need ;)
    re.compile(r"^\s*for\s*\(.*;.*\)"),     # for(;;)
    re.compile(r"^\s*\w+\s*\(.*\)\s*;?\s*$") if False else re.compile(r"(?!x)x"),
]

def process(text):
    out, in_code, lang, changed = [], False, "", 0
    for line in text.split("\n"):
        m = re.match(r"^```(\w*)", line)
        if m:
            if in_code:
                in_code = False
            else:
                in_code, lang = True, m.group(1).lower()
            out.append(line)
            continue
        if in_code and lang in BOX_LANGS and line.rstrip().endswith(";") and not any(k.search(line) for k in KEEP):
            line = line.rstrip()[:-1].rstrip()
            changed += 1
        out.append(line)
    return "\n".join(out), changed

root = pathlib.Path(sys.argv[1])
dry = "--dry-run" in sys.argv
total = files = 0
for f in sorted(root.rglob("*.md")):
    new, n = process(f.read_text())
    if n:
        files += 1; total += n
        if not dry: f.write_text(new)
print(f"{'Would change' if dry else 'Changed'} {total} lines in {files} files")
