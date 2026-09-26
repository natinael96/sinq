#!/usr/bin/env python3
"""Correct ግጻዌ ምስባክ citations that point at the wrong psalm.

A ምስባክ is three lines of chant. The ግጻዌ prints the lines and, beside them, the
psalm and verses they come from. The lines are what the app shows; the citation
is what its "open the psalm" door and its Catena link are built from. Twenty-odd
citations name a psalm the lines are not in.

This finds them the way a reader would — by looking the chant up in the Ge'ez
Psalter — and rewrites the citation to where the words actually are. It never
touches the chant itself: the ምስባክ follows its own recension, with its own
spellings and sometimes its own word order, and "correcting" it against the
Psalter would be destroying the thing the book is for.

Matching folds the Ethiopic homophone series exactly as com.agpeya.app.search
.AmharicSearch does, because the two traditions spell the same sounds
differently: ሠ/ሰ, ሐ/ኀ/ኸ/ሀ, ዐ/አ, ፀ/ጸ.

Sources, and what has to be re-run after:

    sources/gitsawe/months/*.json               -> tools/import_gitsawe_months.py
    sources/gitsawe/parts/03-sunday-cycle-*.json -> tools/import_gitsawe_part3.py
    assets/content/gitsawe/seasonal-gitsawe.json  (no source; edited in place)

Run with --apply to write; without it, nothing changes. Running it twice is
safe: the second pass finds nothing to do.
"""
import argparse
import glob
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MONTHS = os.path.join(ROOT, 'sources/gitsawe/months')
SUNDAY = os.path.join(ROOT, 'sources/gitsawe/parts/03-sunday-cycle-and-mezmur.json')
SEASONAL = os.path.join(ROOT, 'app/src/main/assets/content/gitsawe/seasonal-gitsawe.json')
GEEZ_PSALTER = os.path.join(ROOT, 'app/src/main/assets/content/bible/gez-1980/books')

ONES = '፩፪፫፬፭፮፯፰፱'
TENS = '፲፳፴፵፶፷፸፹፺'


def geez(n):
    """1–999 in Ethiopic. The Psalter never needs more."""
    if n <= 0:
        return ''
    out = ''
    if n >= 100:
        h, n = divmod(n, 100)
        out += (ONES[h - 1] if h > 1 else '') + '፻'
    t, o = divmod(n, 10)
    return out + (TENS[t - 1] if t else '') + (ONES[o - 1] if o else '')


def foldchar(c):
    cp = ord(c)
    if 0x1220 <= cp <= 0x1227: return chr(0x1230 + cp - 0x1220)   # ሠ -> ሰ
    if 0x1210 <= cp <= 0x1217: return chr(0x1200 + cp - 0x1210)   # ሐ -> ሀ
    if 0x1280 <= cp <= 0x1287: return chr(0x1200 + cp - 0x1280)   # ኀ -> ሀ
    if 0x12B8 <= cp <= 0x12BF: return chr(0x1200 + cp - 0x12B8)   # ኸ -> ሀ
    if 0x12D0 <= cp <= 0x12D7: return chr(0x12A0 + cp - 0x12D0)   # ዐ -> አ
    if 0x1340 <= cp <= 0x1347: return chr(0x1338 + cp - 0x1340)   # ፀ -> ጸ
    return c


PUNCT = re.compile(r'[።፡፣፤፥፦፧፨፠\s\.\:\-–—]+')


def fold(s):
    return PUNCT.sub('', ''.join(foldchar(c) for c in s or ''))


def psalter():
    for f in glob.glob(os.path.join(GEEZ_PSALTER, '*.json')):
        d = json.load(open(f, encoding='utf-8'))
        if d.get('usfm_id') == 'PSA':
            return {c['n']: [v.get('t', '') for v in c['verses']] for c in d['chapters']}
    raise SystemExit('the Ge\'ez Psalter is not where this tool expects it')


CH = psalter()
FLAT = {c: fold(' '.join(v)) for c, v in CH.items()}


def where_is(lines, cited):
    """Where this chant really is, or None if the citation already stands.

    The chant's opening sixteen characters, folded, locate the candidate psalms.
    If the cited psalm is among them the citation is right and nothing is
    reported — the Psalter repeats phrases, so "some other psalm also contains
    these words" is not evidence of an error. Among the rest, a psalm every line
    of the chant lands in beats one only the opening reaches.
    """
    folded = [f for f in (fold(l) for l in lines) if f]
    if not folded:
        return None
    probe = ''.join(folded)[:16]
    candidates = [p for p, flat in FLAT.items() if probe in flat]
    if not candidates or cited in candidates:
        return None
    whole = [p for p in candidates if all(f[:12] in FLAT[p] for f in folded)]
    psalm = (whole or candidates)[0]
    hits = [i for i, v in enumerate(CH[psalm], 1)
            if any(f[:12] in fold(v) for f in folded)]
    if not hits:
        return None
    return psalm, min(hits), max(hits)


def cited_chapter(chapter_verse):
    """The psalm number out of a Ge'ez citation like '፷፬ ቍ ፲፩ – ፲፪'.

    The verse marker is spelled ቍ on most pages, ቄ or ቁ on a few, and the number
    is sometimes followed by a middle dot. Everything from the first of those
    onwards is the verse, and only what precedes it is the psalm — reading the
    whole string as one number is how '፻፲፰ ቄ· ፻፶፭' becomes 11855.
    """
    head = re.split(r'[ቍቄቁ]', chapter_verse or '', 1)[0]
    digits = re.sub(r'[^፩-፼]', '', head)
    if not digits:
        return None
    vals = {**{c: i + 1 for i, c in enumerate(ONES)},
            **{c: (i + 1) * 10 for i, c in enumerate(TENS)}}
    group = 0
    for c in digits:
        if c in vals: group += vals[c]
        elif c == '፻': group = (group or 1) * 100
    return group or None


def citation(psalm, start, end):
    v = geez(start) + (f' – {geez(end)}' if end and end != start else '')
    return f'{geez(psalm)} ቍ {v}'


def walk_msbak(node, visit):
    """Every ምስባክ in a source tree, source shape or asset shape."""
    if isinstance(node, dict):
        for k, v in node.items():
            if k == 'ምስባክ' and isinstance(v, dict) and v.get('verses'):
                visit(v, 'source')
            elif k == 'msbak' and isinstance(v, list):
                for m in v:
                    visit(m, 'asset')
            else:
                walk_msbak(v, visit)
    elif isinstance(node, list):
        for x in node:
            walk_msbak(x, visit)


def indent_of(raw, data):
    """The indent that reproduces this file byte for byte, or None.

    These files are hand-kept transcriptions of page scans. Rewriting one at the
    wrong indent turns a two-character fix into a four-thousand-line diff, so a
    file that will not round-trip exactly is left alone and reported.
    """
    for n in (1, 2, 4):
        if json.dumps(data, ensure_ascii=False, indent=n) + '\n' == raw:
            return n
    return None


def repair(path, label, apply, cited_for=None):
    raw = open(path, encoding='utf-8').read()
    data = json.loads(raw)
    fixed = []

    def visit(m, shape):
        if shape == 'source':
            lines = m.get('verses') or []
            cited = cited_chapter(m.get('chapter_verse'))
        else:
            geez_text = ((m.get('text') or {}).get('geez') or '')
            lines = [l for l in re.split(r'።', geez_text) if l.strip()]
            cited = (m.get('verse') or {}).get('chapter')
        if not lines or not cited:
            return
        found = where_is(lines, cited)
        if not found:
            return
        psalm, lo, hi = found
        if lo is None:
            return
        fixed.append((cited, psalm, lo, hi, ' '.join(lines)[:44]))
        if not apply:
            return
        if shape == 'source':
            m['chapter_verse'] = citation(psalm, lo, hi)
        else:
            m['citation'] = citation(psalm, lo, hi)
            ver = m.setdefault('verse', {})
            ver['chapter'] = psalm
            ver['start'] = lo
            ver['end'] = hi if hi != lo else None
            ver['citation'] = m['citation']
            m['verse'] = {k: v for k, v in ver.items() if v is not None}

    walk_msbak(data, visit)
    if fixed and apply:
        indent = indent_of(raw, json.loads(raw))
        if indent is None:
            print(f'  !! {label}: will not round-trip cleanly, left untouched')
            return 0
        with open(path, 'w', encoding='utf-8') as f:
            f.write(json.dumps(data, ensure_ascii=False, indent=indent) + '\n')
    for cited, psalm, lo, hi, text in fixed:
        print(f'  {label:26} መዝ {cited} -> መዝ {psalm}:{lo}' +
              (f'–{hi}' if hi != lo else '') + f'   {text}')
    return len(fixed)


def shipped_citations():
    """Chant text -> the psalm the shipped asset says it comes from."""
    out = {}
    for f in glob.glob(os.path.join(ROOT, 'app/src/main/assets/content/gitsawe/*gitsawe*.json')):
        def visit(m, shape):
            if shape != 'asset':
                return
            text = ((m.get('text') or {}).get('geez') or '')
            chapter = (m.get('verse') or {}).get('chapter')
            if text and chapter:
                out[fold(text)] = chapter
        walk_msbak(json.load(open(f, encoding='utf-8')), visit)
    return out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true', help='write the corrections')
    args = ap.parse_args()

    cited_for = shipped_citations()
    total = 0
    for path in sorted(glob.glob(os.path.join(MONTHS, '*.json'))):
        total += repair(path, os.path.basename(path), args.apply, cited_for)
    total += repair(SUNDAY, 'sunday-cycle (source)', args.apply, cited_for)
    total += repair(SEASONAL, 'seasonal (asset)', args.apply)

    print(f'\n{total} ምስባክ citation(s) ' + ('corrected' if args.apply else 'would be corrected'))
    if total and args.apply:
        print('Now re-run:  python3 tools/import_gitsawe_months.py'
              '  &&  python3 tools/import_gitsawe_part3.py')
    return 0


if __name__ == '__main__':
    sys.exit(main())
