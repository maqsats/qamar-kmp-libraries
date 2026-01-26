#!/usr/bin/env python3
"""
Generate a unified Quran SQLite database and JSON payloads from the legacy
Android assets. Sources:
- Arabic text + transliterations: /Users/maqsat/StudioProjects/QamarOld/app/src/main/res/values
- Page map: /Users/maqsat/StudioProjects/QamarOld/app/src/main/assets/databases/qurankz.db

Outputs (under build/generated):
- quran.db (tables: verses, quran_paged)
- arabic.json
- translit_en.json, translit_ru.json, translit_kk.json
- quran_paged.json
"""

from __future__ import annotations

import json
import sqlite3
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, List, Tuple
from xml.etree import ElementTree

PROJECT_ROOT = Path("/Users/maqsat/StudioProjects/qamar-kmp-libraries")
LEGACY_APP = Path("/Users/maqsat/StudioProjects/QamarOld/app/src/main")

OUTPUT_DIR = PROJECT_ROOT / "build/generated"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

ARABIC_XML = LEGACY_APP / "res/values/arabic.xml"
TRANSLIT_KK_XML = LEGACY_APP / "res/values/translit.xml"
TRANSLIT_EN_XML = LEGACY_APP / "res/values/translit_en.xml"
TRANSLIT_RU_XML = LEGACY_APP / "res/values/translit_ru.xml"

LEGACY_DB = LEGACY_APP / "assets/databases/qurankz.db"


@dataclass
class Verse:
    sura: int
    ayah: int
    text: str


def load_string_arrays(xml_path: Path, prefix: str) -> Dict[int, List[str]]:
    tree = ElementTree.parse(xml_path)
    root = tree.getroot()
    arrays: Dict[int, List[str]] = {}
    for arr in root.findall("string-array"):
        name = arr.attrib.get("name", "")
        if not name.startswith(prefix + "_"):
            continue
        try:
            sura_num = int(name.split("_")[1])
        except (ValueError, IndexError):
            continue
        arrays[sura_num] = [item.text or "" for item in arr.findall("item")]
    return arrays


def normalize_verses(raw_arrays: Dict[int, List[str]]) -> List[Verse]:
    verses: List[Verse] = []
    for sura in range(1, 115):
        items = raw_arrays.get(sura, [])
        # Legacy arrays include the basmala as element 0 for all suras.
        # Keep basmala for sura 1, drop for others to match canonical numbering.
        effective_items = items if sura == 1 else items[1:]
        for idx, text in enumerate(effective_items, start=1):
            verses.append(Verse(sura=sura, ayah=idx, text=text.strip()))
    return verses


def dump_json(path: Path, payload) -> None:
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Wrote {path.relative_to(PROJECT_ROOT)}")


def build_sqlite_db(verses: List[Verse], page_rows: List[Tuple[int, int, int]]) -> None:
    db_path = OUTPUT_DIR / "quran.db"
    if db_path.exists():
        db_path.unlink()
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()
    cur.execute(
        """
        CREATE TABLE verses (
            sura INTEGER NOT NULL,
            ayah INTEGER NOT NULL,
            arabic_text TEXT NOT NULL,
            PRIMARY KEY (sura, ayah)
        );
        """
    )
    cur.execute(
        """
        CREATE TABLE quran_paged (
            page INTEGER NOT NULL,
            sura INTEGER NOT NULL,
            ayah INTEGER NOT NULL,
            PRIMARY KEY (page, sura, ayah)
        );
        """
    )
    cur.executemany(
        "INSERT INTO verses (sura, ayah, arabic_text) VALUES (?, ?, ?);",
        [(v.sura, v.ayah, v.text) for v in verses],
    )
    cur.executemany(
        "INSERT INTO quran_paged (page, sura, ayah) VALUES (?, ?, ?);",
        page_rows,
    )
    conn.commit()
    conn.close()
    print(f"Wrote {db_path.relative_to(PROJECT_ROOT)} "
          f"(verses={len(verses)}, pages={len(page_rows)})")


def read_page_map() -> List[Tuple[int, int, int]]:
    conn = sqlite3.connect(LEGACY_DB)
    cur = conn.cursor()
    cur.execute("SELECT page, sura, ayat FROM quran_paged;")
    rows = [(int(p), int(s), int(a)) for p, s, a in cur.fetchall()]
    conn.close()
    return rows


def main() -> None:
    arabic_arrays = load_string_arrays(ARABIC_XML, "arabic")
    translit_en_arrays = load_string_arrays(TRANSLIT_EN_XML, "translit_en")
    translit_ru_arrays = load_string_arrays(TRANSLIT_RU_XML, "translit_ru")
    translit_kk_arrays = load_string_arrays(TRANSLIT_KK_XML, "translit")

    verses = normalize_verses(arabic_arrays)
    page_rows = read_page_map()

    build_sqlite_db(verses, page_rows)

    def trim_arrays(raw: Dict[int, List[str]]) -> List[List[str]]:
        data: List[List[str]] = []
        for sura in range(1, 115):
            items = raw.get(sura, [])
            data.append(items if sura == 1 else items[1:])
        return data

    dump_json(OUTPUT_DIR / "arabic.json", trim_arrays(arabic_arrays))
    dump_json(OUTPUT_DIR / "translit_en.json", trim_arrays(translit_en_arrays))
    dump_json(OUTPUT_DIR / "translit_ru.json", trim_arrays(translit_ru_arrays))
    dump_json(OUTPUT_DIR / "translit_kk.json", trim_arrays(translit_kk_arrays))
    dump_json(OUTPUT_DIR / "quran_paged.json", page_rows)


if __name__ == "__main__":
    main()
