"""Restructure the ግጻዌ metadata so feasts/seasonal/monthly are date-matchable.

Rewrites (in place) under app/src/main/assets/content/gitsawe/:
  feasts.json   — recover DD-MM + monthNum + day from the Amharic name; flag movable
  seasonal-gitsawe.json — split the overloaded 'date' into season / week / part
  monthly-gitsawe.json  — parse month + day-range / nth-Sunday; drop the duplicate

Daily readings and all negh/kidassie/mahlet content are untouched.

One-time migration: it consumes the original npm-derived fields ('date',
'amharicName', 'month'), so it is NOT idempotent — re-run it only against the
pre-restructure files, not its own output. Run:  python tools/restructure_gitsawe.py
"""
import json, re
from pathlib import Path

DIR = Path(__file__).resolve().parent.parent / "app/src/main/assets/content/gitsawe"

# Ethiopian month key -> number (1..13). tsige/seasonal are movable (no fixed month).
MONTH_NUM = {
    "meskerem":1,"tikimt":2,"hidar":3,"tahsas":4,"tir":5,"yekatit":6,"megabit":7,
    "miyazya":8,"ginbot":9,"sene":10,"hamle":11,"nehase":12,"pagumen":13,
    "tsige":None,"seasonal":None,
}
# Amharic month name (as written in feast parentheticals) -> key
AM_MONTH = {
    "መስከረም":"meskerem","ጥቅምት":"tikimt","ህዳር":"hidar","ኅዳር":"hidar","ታኅሣሥ":"tahsas",
    "ጥር":"tir","የካቲት":"yekatit","መጋቢት":"megabit","ሚያዝያ":"miyazya","ግንቦት":"ginbot",
    "ሰኔ":"sene","ሐምሌ":"hamle","ነሐሴ":"nehase","ጳጉሜን":"pagumen","ጳጉሜ":"pagumen",
}
GEEZ = {"፩":1,"፪":2,"፫":3,"፬":4,"፭":5,"፮":6,"፯":7,"፰":8,"፱":9,
        "፲":10,"፳":20,"፴":30,"፵":40,"፶":50,"፷":60,"፸":70,"፹":80,"፺":90,"፻":100}

def parse_num(token: str):
    """A run of Ge'ez numerals (additive, <200) or Arabic digits -> int, else None."""
    token = token.strip()
    if re.fullmatch(r"\d+", token): return int(token)
    if token and all(c in GEEZ for c in token): return sum(GEEZ[c] for c in token)
    return None

def load(name): return json.loads((DIR/name).read_text(encoding="utf-8"))
def save(name, data): (DIR/name).write_text(json.dumps(data, ensure_ascii=False, indent=1), encoding="utf-8")

# ---------- FEASTS ----------
def fix_feasts():
    out=[]
    for f in load("feasts.json"):
        month = f.get("month")
        monthNum = MONTH_NUM.get(month)
        day=None
        # Pull "(<month?> <day>)" out of the Amharic name.
        m = re.search(r"\(([^)]*)\)", f.get("amharicName",""))
        if m:
            inside = m.group(1)
            # last numeric token in the parenthetical is the day
            for tok in reversed(re.findall(r"[፩-፼]+|\d+", inside)):
                d = parse_num(tok)
                if d: day=d; break
            # a month name inside the parens overrides/confirms the key
            for am,key in AM_MONTH.items():
                if am in inside:
                    month=key; monthNum=MONTH_NUM.get(key, monthNum); break
        movable = monthNum is None or day is None
        rec = {
            "key": f["key"], "name": f["name"], "amharicName": f["amharicName"],
            "month": month, "monthNum": monthNum, "day": day,
            "dateKey": (f"{day:02d}-{monthNum:02d}" if not movable else None),
            "movable": movable,
        }
        out.append(rec)
    save("feasts.json", out)
    return out

# ---------- SEASONAL ----------
AM_SEASON = [   # keyword -> canonical season slug (for Amharic-named entries)
    ("ክረምት","kremt"),("ፍልሰታ","filseta"),("ጵጉሜን","pagumen"),("ጳጉሜን","pagumen"),
    ("ዕርገት","erget"),("ጸሎተ ሐሙስ","holy_thursday"),("አስተምህሮ","astemhro"),("ዘርዕ ደመና","zere_demena"),
]
def fix_seasonal():
    out=[]
    for e in load("seasonal-gitsawe.json"):
        raw = e.get("date","")
        season=None; week=None; part=None
        m = re.match(r"^(\d+)-([A-Za-z]+)(?:-(\d+))?$", raw)
        if m:
            week=int(m.group(1)); season=m.group(2); part=int(m.group(3)) if m.group(3) else None
        else:
            for kw,slug in AM_SEASON:
                if kw in raw: season=slug; break
            gz = re.search(r"[፩-፼]+", raw)   # a Ge'ez ordinal in the name
            if gz: week=parse_num(gz.group(0))
        rec={"season":season, "week":week, "part":part, "raw":raw, "movable":True,
             "title":e.get("title")}
        for k in ("negh","kidassie"):
            if e.get(k): rec[k]=e[k]
        out.append(rec)
    save("seasonal-gitsawe.json", out)
    return out

# ---------- MONTHLY ----------
AM_MONTH_PREFIX = {"መስከረም":"meskerem","ጥቅምት":"tikimt","ኅዳር":"hidar","ኅድር":"hidar",
                   "ታኅሣሥ":"tahsas","ጥር":"tir","ጽጌ":"tsige"}
def fix_monthly():
    seen=set(); out=[]
    for e in load("monthly-gitsawe.json"):
        raw=e.get("date",""); title=e.get("title","")
        # month from the date prefix "ዘ<month>"
        month=None
        for am,key in AM_MONTH_PREFIX.items():
            if am in raw: month=key; break
        monthNum=MONTH_NUM.get(month)
        fromDay=toDay=nthSunday=None; crossMonth=False
        rng=re.search(r"([፩-፼]+)\s*እስከ\s*([፩-፼]+)", title)
        if rng:
            fromDay=parse_num(rng.group(1)); toDay=parse_num(rng.group(2))
            if fromDay and toDay and toDay<fromDay: crossMonth=True
        nth=re.search(r"([፩-፼]+)ኛ\s*ሰንበት", title) or re.search(r"([፩-፼]+)ኛ\s*ሰንበት", raw)
        if nth: nthSunday=parse_num(nth.group(1))
        if nthSunday is None:
            sfx=re.search(r"-([፩-፼\d]+)$", raw)   # "ዘጽጌ-፫" -> 3rd Sunday
            if sfx: nthSunday=parse_num(sfx.group(1))
        if fromDay is None and nthSunday is None:
            # A lone day with a Sunday qualifier, e.g. "ዘመስከረም፡ ፳፭ ሰንበት" (25th, if Sunday).
            single=re.search(r"([፩-፼]+)\s*(?:ሰንበት|እሑድ)", raw) or re.search(r"([፩-፼]+)\s*እሑድ", title)
            if single: fromDay=toDay=parse_num(single.group(1))
        rec={"month":month,"monthNum":monthNum,"fromDay":fromDay,"toDay":toDay,
             "nthSunday":nthSunday,"crossMonth":crossMonth,"appliesTo":"sunday",
             "mezmur":e.get("mezmur"),"raw":raw,"title":title}
        for k in ("negh","kidassie"):
            if e.get(k): rec[k]=e[k]
        sig=(month,fromDay,toDay,nthSunday,title)
        if sig in seen: continue         # drop the exact duplicate
        seen.add(sig); out.append(rec)
    save("monthly-gitsawe.json", out)
    return out

if __name__ == "__main__":
    fe=fix_feasts(); se=fix_seasonal(); mo=fix_monthly()
    print(f"feasts:   {len(fe)}  (fixed-date {sum(not x['movable'] for x in fe)}, movable {sum(x['movable'] for x in fe)})")
    print(f"seasonal: {len(se)}")
    print(f"monthly:  {len(mo)}  (deduped)")
