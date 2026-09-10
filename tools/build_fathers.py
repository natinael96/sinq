#!/usr/bin/env python3
"""Build the bundled patristic commentary on the New Testament and the Psalter.

Only the Fathers the Oriental Orthodox churches receive are kept, which is the
undivided Church down to Chalcedon: the Alexandrian, Greek, Syriac and Latin
fathers who died before 451. Excluded are everyone after the council on either
side of it (Gregory the Great, Bede, Rabanus, Remigius, Anselm, the Glossa,
Theophylact, John Damascene), Leo, whose Tome is the very thing the Oriental
Orthodox rejected there, Origen, and Eusebius for his Arian sympathies. That
rule is what ORIENTAL below encodes, and it is applied to the Catena Aurea,
which quotes freely across the whole later Latin tradition.

Sources are public-domain volumes from the Christian Classics Ethereal Library,
in their ThML markup, which carries the scripture anchors this build depends on:

  Catena Aurea (Aquinas, tr. Newman 1841)   Matthew, Mark  (filtered)
  NPNF 1.7  Augustine                        John, 1 John
  NPNF 1.8  Augustine                        Psalms
  NPNF 1.10 Chrysostom                       Matthew
  NPNF 1.11 Chrysostom                       Acts, Romans
  NPNF 1.12 Chrysostom                       1-2 Corinthians
  NPNF 1.13 Chrysostom                       Galatians - Philemon
  NPNF 1.14 Chrysostom                       John, Hebrews

The XML is downloaded to sources/fathers/ on first run and is not committed.
Output: app/src/main/assets/content/fathers/{index,<book slug>}.json
"""
import json, os, re, sys, urllib.request, collections
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from _thml import parse

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_DIR = os.path.join(ROOT, 'sources', 'fathers')
OUT_DIR = os.path.join(ROOT, 'app', 'src', 'main', 'assets', 'content', 'fathers')

# (file, url, author, work title)
VOLUMES = [
    ('catena1.xml', 'https://ccel.org/ccel/a/aquinas/catena1.xml', None,
     'Catena Aurea on Matthew'),
    ('catena2.xml', 'https://ccel.org/ccel/a/aquinas/catena2.xml', None,
     'Catena Aurea on Mark'),
    ('npnf107.xml', 'https://ccel.org/ccel/s/schaff/npnf107.xml', 'Augustine',
     'Homilies on the Gospel of John'),
    ('npnf108.xml', 'https://ccel.org/ccel/s/schaff/npnf108.xml', 'Augustine',
     'Expositions on the Psalms'),
    ('npnf110.xml', 'https://ccel.org/ccel/s/schaff/npnf110.xml', 'Chrysostom',
     'Homilies on Matthew'),
    ('npnf111.xml', 'https://ccel.org/ccel/s/schaff/npnf111.xml', 'Chrysostom',
     'Homilies on Acts and Romans'),
    ('npnf112.xml', 'https://ccel.org/ccel/s/schaff/npnf112.xml', 'Chrysostom',
     'Homilies on the Corinthians'),
    ('npnf113.xml', 'https://ccel.org/ccel/s/schaff/npnf113.xml', 'Chrysostom',
     'Homilies on Galatians to Philemon'),
    ('npnf114.xml', 'https://ccel.org/ccel/s/schaff/npnf114.xml', 'Chrysostom',
     'Homilies on John and Hebrews'),
]

# OSIS id -> the slug ScriptureRepository derives from the Bible asset filename
SLUG = {
    'Matt': 'matthew', 'Mark': 'mark', 'Luke': 'luke', 'John': 'john',
    'Acts': 'acts', 'Rom': 'romans', '1Cor': '1-corinthians', '2Cor': '2-corinthians',
    'Gal': 'galatians', 'Eph': 'ephesians', 'Phil': 'philippians', 'Col': 'colossians',
    '1Thess': '1-thessalonians', '2Thess': '2-thessalonians',
    '1Tim': '1-timothy', '2Tim': '2-timothy', 'Titus': 'titus', 'Phlm': 'philemon',
    'Heb': 'hebrews', '1John': '1-john', 'Ps': 'psalms',
}

# Sinq's Psalter is numbered by the Ge'ez/LXX reckoning; NPNF prints Augustine
# under the Hebrew numbers. Index is the Ge'ez psalm, value the Hebrew one.
# Kept identical to CatenaLink.PSALM_TO_MASORETIC.
PSALM_TO_HEBREW = [
    1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
    22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
    42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
    62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81,
    82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101,
    102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 116, 116, 117,
    118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133,
    134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 147, 148,
    149, 150,
]

# The Catena names a Father at the head of each extract, in the 1841 abbreviations.
FATHERS = {
    'chrys.': 'Chrysostom', 'chrys': 'Chrysostom', 'chyrs.': 'Chrysostom',
    'pseudo-chrys.': 'Pseudo-Chrysostom', 'pseudo-chyrs.': 'Pseudo-Chrysostom',
    'psuedo-chrys.': 'Pseudo-Chrysostom',
    'aug.': 'Augustine', 'aug': 'Augustine', 'augustine': 'Augustine',
    'pseudo-aug.': 'Pseudo-Augustine', 'psuedo-aug.': 'Pseudo-Augustine',
    'pseudo-augustine': 'Pseudo-Augustine',
    'jerome': 'Jerome', 'pseudo-jerome': 'Pseudo-Jerome',
    'bede': 'Bede', 'remig.': 'Remigius', 'remig': 'Remigius', 'remigius': 'Remigius',
    'theophylact': 'Theophylact', 'theophlyact': 'Theophylact', 'theophyact': 'Theophylact',
    'hilary': 'Hilary', 'hil.': 'Hilary',
    'origen': 'Origen', 'origin': 'Origen', 'pseudo-origen': 'Pseudo-Origen',
    'raban.': 'Rabanus', 'raban': 'Rabanus', 'rabanus': 'Rabanus',
    'greg.': 'Gregory the Great', 'greg': 'Gregory the Great',
    'gregory': 'Gregory the Great',
    'greg. nyss.': 'Gregory of Nyssa', 'gregory nyss.': 'Gregory of Nyssa',
    'ambrose': 'Ambrose', 'leo': 'Leo', 'cyprian': 'Cyprian', 'cyril': 'Cyril',
    'cyril of alexandria': 'Cyril', 'cassian': 'Cassian', 'haymo': 'Haymo',
    'severianus': 'Severianus', 'anselm': 'Anselm', 'isid.': 'Isidore',
    'isidore': 'Isidore', 'euseb.': 'Eusebius', 'josephus': 'Josephus',
    'chrysol.': 'Peter Chrysologus', 'chrysologus': 'Peter Chrysologus',
    'damas.': 'John Damascene', 'damasc.': 'John Damascene',
    'damascenus': 'John Damascene', 'dionys.': 'Dionysius', 'dionysius': 'Dionysius',
    'maximus': 'Maximus', 'gennadius': 'Gennadius', 'lanfranc': 'Lanfranc',
    'paschasius': 'Paschasius', 'nemesius': 'Nemesius', 'faustus': 'Faustus',
    'ambrosiaster': 'Ambrosiaster', 'theodotus': 'Theodotus', 'titus': 'Titus of Bostra',
}
# The Fathers the Oriental Orthodox churches receive: those of the undivided
# Church, who died before Chalcedon in 451. See the module docstring for who
# this deliberately leaves out and why.
ORIENTAL = {
    'Athanasius', 'Basil', 'Gregory of Nyssa', 'Gregory Nazianzen',
    'Chrysostom', 'Cyril', 'Theodotus', 'Titus of Bostra', 'Severianus',
    'Ephrem', 'Severus', 'Dioscorus',
    'Augustine', 'Jerome', 'Ambrose', 'Hilary', 'Cyprian',
}


def father(raw):
    """Normalise a Catena attribution, or return '' when it is not a name."""
    k = raw.strip().lower().rstrip(',')
    if k in FATHERS: return FATHERS[k]
    base = re.split(r'\s+(?:e|ap\.|in|non|ord|interlin|serm|quaest)\b', k)[0].strip(' .')
    if base.startswith('gloss'): return 'Glossa Ordinaria'
    return FATHERS.get(base, FATHERS.get(base + '.', ''))


def fetch():
    os.makedirs(SRC_DIR, exist_ok=True)
    for name, url, _, _ in VOLUMES:
        path = os.path.join(SRC_DIR, name)
        if os.path.exists(path) and os.path.getsize(path) > 100_000: continue
        print(f"  downloading {name} ...", flush=True)
        req = urllib.request.Request(url, headers={'User-Agent': 'sinq-build'})
        with urllib.request.urlopen(req, timeout=180) as r, open(path, 'wb') as f:
            f.write(r.read())


def main():
    fetch()
    works, authors = [], []
    def author_id(name):
        if name not in authors: authors.append(name)
        return authors.index(name)

    books = collections.defaultdict(lambda: collections.defaultdict(list))
    for name, _, auth, title in VOLUMES:
        wid = len(works)
        works.append({'title': title, 'author': auth or 'various'})
        rows = parse(os.path.join(SRC_DIR, name), auth)
        kept = dropped = 0
        running = ''      # the Catena runs an extract over several paragraphs
        for osis, ch, verse, attrib, text in rows:
            slug = SLUG.get(osis)
            if not slug: continue
            if auth:
                who = auth
            else:
                named = father(attrib)
                if named:
                    running = named
                elif attrib:
                    running = ''   # a named voice this build does not receive
                who = running
            if who not in ORIENTAL:
                dropped += 1
                continue
            if osis == 'Ps':
                # NPNF prints the Hebrew number; place it on every Ge'ez psalm
                # that answers to it, and keep the anchor at psalm level because
                # the two reckonings also disagree about verse one.
                for geez in range(1, 151):
                    if PSALM_TO_HEBREW[geez - 1] == ch:
                        books[slug][f'{geez}:0'].append([wid, author_id(who), text])
                        kept += 1
                continue
            books[slug][f'{ch}:{verse}'].append([wid, author_id(who), text])
            kept += 1
        print(f"  {title:38} {kept:6,} kept, {dropped:5,} not received")

    os.makedirs(OUT_DIR, exist_ok=True)
    for f in os.listdir(OUT_DIR):
        if f.endswith('.json'): os.remove(os.path.join(OUT_DIR, f))
    index = []
    for slug in sorted(books):
        entries = {k: v for k, v in sorted(
            books[slug].items(), key=lambda kv: tuple(int(n) for n in kv[0].split(':')))}
        blob = json.dumps({'book': slug, 'entries': entries},
                          ensure_ascii=False, separators=(',', ':'))
        with open(os.path.join(OUT_DIR, f'{slug}.json'), 'w', encoding='utf-8') as fh:
            fh.write(blob)
        index.append({'book': slug, 'file': f'{slug}.json',
                      'anchors': len(entries),
                      'paragraphs': sum(len(v) for v in entries.values()),
                      'bytes': len(blob.encode())})
    with open(os.path.join(OUT_DIR, 'index.json'), 'w', encoding='utf-8') as fh:
        json.dump({'schemaVersion': 1, 'works': works, 'authors': authors,
                   'books': index}, fh, ensure_ascii=False, separators=(',', ':'))
    tot = sum(b['bytes'] for b in index)
    print(f"\nfathers: {len(index)} books, {sum(b['anchors'] for b in index):,} anchors, "
          f"{sum(b['paragraphs'] for b in index):,} paragraphs, {tot/1048576:.2f} MB")

if __name__ == '__main__':
    main()
