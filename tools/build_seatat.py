#!/usr/bin/env python3
"""Build assets/content/seatat/seatat.json from tools/seatat-source.json.

The source is a digitization of መጽሐፈ ሰዓታት ዘሌሊት ወዘነግህ (the Seatat of Night
and Dawn), Tinsae Ze-Gubae printing house, Addis Ababa, ፲፱፻፶፯ ዓ.ም. — kept
verbatim in the repo (one numeral typo in the edition year corrected). This
script only reshapes it into the app's SeatatContent schema:

  {id, title{gez,am}, meqdim{...}, sections[{id, title{gez,am}, verses[{gez,am}]}]}
    → {contentVersion, sections[{id, label, titleGe, titleAm, lines[{ge,am}]}]}

The መቅድም (the preface on አባ ጊዮርጊስ ዘጋሥጫ, Amharic only) becomes the first
section, with Ge'ez-less lines; the reader renders those in the primary style.
Never edit the generated file by hand — change the source and rerun.
"""

import json
import pathlib

ROOT = pathlib.Path(__file__).resolve().parent.parent
SRC = ROOT / "tools" / "seatat-source.json"
OUT = ROOT / "app" / "src" / "main" / "assets" / "content" / "seatat" / "seatat.json"


def main() -> None:
    src = json.loads(SRC.read_text(encoding="utf-8"))

    sections = []

    meqdim = src.get("meqdim") or {}
    if meqdim.get("paragraphs"):
        sections.append({
            "id": "meqdim",
            "label": "መቅድም",
            "titleGe": "መቅድም",
            "titleAm": meqdim.get("title", "መቅድም"),
            "lines": [{"ge": "", "am": p} for p in meqdim["paragraphs"]],
        })

    for s in src["sections"]:
        title = s.get("title") or {}
        sections.append({
            "id": s["id"],
            "label": "",  # navigation is the contents sheet, not chips
            "titleGe": title.get("gez", ""),
            "titleAm": title.get("am", ""),
            "lines": [
                {"ge": v.get("gez", ""), "am": v.get("am", "")}
                for v in s.get("verses", [])
            ],
        })

    out = {"contentVersion": 2, "sections": sections}
    OUT.write_text(
        json.dumps(out, ensure_ascii=False, indent=1) + "\n",
        encoding="utf-8",
    )
    total = sum(len(s["lines"]) for s in sections)
    print(f"wrote {OUT.relative_to(ROOT)}: {len(sections)} sections, {total} lines")


if __name__ == "__main__":
    main()
