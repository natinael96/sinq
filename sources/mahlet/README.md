# Merged Mahlet — Clean Structured Editorial Edition

> Source record reconciled and modernized into domain-driven schema.
> Unused comparison scratch dumps and review audits have been cleaned up.
> See [project status](../../docs/PROJECT_STATUS.md#content-inventory).

Start with `months/`: 13 readable monthly JSON files representing the Ethiopian liturgical calendar (`01-meskerem.json` through `13-pagumen.json`).

## Domain-Driven Schema Structure

Each month file contains structured liturgical feasts and service orders:

```json
{
  "month_number": 1,
  "month_name": "መስከረም",
  "season": "መፀው",
  "summary": "ሥርዓተ ማኅሌት ዘወርኃ መስከረም — የ፲፫ቱ በዓላትና ሰንበታት ማኅሌታት።",
  "feasts": [
    {
      "id": "meskerem_01_awde_amet",
      "day": 1,
      "name": "ዓውደ ዓመት",
      "calendar": "fixed",
      "origin": "book",
      "services": {
        "wazema": {
          "title": "ሥርዓተ ዋዜማ ዘዓውደ ዓመት መስከረም ፩",
          "hymns": [
            {
              "id": "meskerem_01_awde_amet_wazema_01",
              "form": "ዋዜማ",
              "lyrics": {
                "gez": "ብጹዕ አንተ ዮሐንስ፤ ዘሀለወከ ታእምር፤ ..."
              },
              "text": "ብጹዕ አንተ ዮሐንስ፤ ዘሀለወከ ታእምር፤ ...",
              "is_alternative": false,
              "is_rubric": false
            }
          ],
          "versions": [...]
        },
        "mahlet": {
          "title": "ሥርዓተ ማኅሌት ዘዓውደ ዓመት መስከረም ፩",
          "hymns": [...],
          "versions": [...]
        }
      }
    }
  ]
}
```

### Key Schema Fields

1. **Top-Level Month:**
   - `month_number`: 1 to 13 (Ethiopian calendar).
   - `month_name`: Month in Ge'ez/Amharic script ("መስከረም", "ጥቅምት", etc.).
   - `season`: Canonical liturgical season ("መፀው", "በጋ", "ጸደይ", "ክረምት").

2. **Feast (`feasts[]`):**
   - `id`: Human-readable unique slug (e.g. `meskerem_01_awde_amet`).
   - `day`: Day of month, or `null` for movable / seasonal feasts computed by `MahletComputus`.
   - `name`: Feast title.
   - `calendar`: `"fixed"` or `"movable"`.
   - `services`: Canonical services (`wazema` for evening vigil, `mahlet` for dawn celebration, `angergari_order`, etc.).

3. **Hymns & Chants (`services[service].hymns[]`):**
   - `id`: Unique hymn identifier.
   - `form`: Liturgical form (`ዋዜማ`, `ነግሥ`, `ሰላም`, `ዚቅ`, `ምልጣን`, `እስመ ለዓለም`, `ወረብ`, `መመሪያ`).
   - `melody_mode`: Musical mode (`ግዕዝ`, `ዕዝል`, `አራራይ`) where indicated.
   - `lyrics`: Multilingual lyrics object (`{"gez": "..."}`).
   - `is_alternative`: Boolean flag (`true` indicates an alternative choice / "ወይም", not a subsequent compulsory chant).
   - `is_rubric`: Boolean flag indicating a liturgical instruction / performance direction (ቀይ ጽሕፈት).

4. **Alternative Editions (`versions[]`):**
   - Alternative editions to choose between, preserving source links and unique manuscript variants without concatenating them into compulsory sequences.

## Clean Directory Layout

- `months/`: The 13 canonical monthly files with the clean structured schema.
- `sources/book.json`: The baseline scanned snapshot reference used by `tools/build_mahlet.py`.
- `README.md`: This architecture and schema specification.

## Building Assets

To rebuild the app runtime assets:

```bash
python3 tools/build_mahlet.py
```

The output is written into `app/src/main/assets/content/mahlet/`.
