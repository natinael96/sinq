"""Parse CCEL ThML volumes into verse-keyed patristic commentary."""
import re, html

SKIP_CLASS = {'scripture','endnote','MsoEndnoteText','index1','index2','pages','bref','bbook','pb'}
TAG = re.compile(r'<div[12]\b[^>]*>|<scripCom\b[^>]*?/?>|<scripRef\b[^>]*>|<p\b[^>]*>|</p>', re.S)
LEMMA = re.compile(r'^(?:Ver\.?\s*)?(\d+)\s*(?:[-\u2013]\s*(\d+))?\.')
ATTRIB = re.compile(r'^([A-Z][A-Za-z\-\.’\' ]{1,40}?)(?:,|:)\s')

NAMES = {
 'matt':'Matt','matthew':'Matt','mark':'Mark','luke':'Luke','john':'John',
 'acts':'Acts','rom':'Rom','romans':'Rom','1cor':'1Cor','2cor':'2Cor',
 'gal':'Gal','galatians':'Gal','eph':'Eph','ephesians':'Eph',
 'phil':'Phil','philip':'Phil','philippians':'Phil','col':'Col','colossians':'Col',
 '1thess':'1Thess','1thessalonians':'1Thess','2thess':'2Thess','2thessalonians':'2Thess',
 '1tim':'1Tim','1timothy':'1Tim','2tim':'2Tim','2timothy':'2Tim',
 'titus':'Titus','philemon':'Phlm','phlm':'Phlm','heb':'Heb','hebrews':'Heb',
 'jas':'Jas','james':'Jas','1pet':'1Pet','2pet':'2Pet',
 '1john':'1John','2john':'2John','3john':'3John','jude':'Jude','rev':'Rev','ps':'Ps',
}
ROMAN = {'i':1,'ii':2,'iii':3,'iv':4,'v':5,'vi':6,'vii':7,'viii':8,'ix':9,'x':10,
 'xi':11,'xii':12,'xiii':13,'xiv':14,'xv':15,'xvi':16,'xvii':17,'xviii':18,'xix':19,'xx':20,
 'xxi':21,'xxii':22,'xxiii':23,'xxiv':24,'xxv':25,'xxvi':26,'xxvii':27,'xxviii':28}

def clean(t):
    t = re.sub(r'<note.*?</note>', ' ', t, flags=re.S)
    t = re.sub(r'\[ed\. note:.*?\]', ' ', t, flags=re.S)
    t = re.sub(r'<[^>]+>', ' ', t)
    return re.sub(r'\s+', ' ', html.unescape(t)).strip()

def osis_start(ref):
    ref = ref.replace('Bible:', '').split('-')[0].split('.')
    if len(ref) >= 3 and ref[1].isdigit() and ref[2].isdigit():
        return ref[0], (int(ref[1]), int(ref[2]))
    if len(ref) == 2 and ref[1].isdigit():
        return ref[0], (int(ref[1]), 0)
    return (ref[0] if ref else None), None

def passage_start(p):
    """'1 Thessalonians iv.' / 'Eph. 2.' / 'Gal. 1:5' -> ('1Thess',(4,0))"""
    m = re.match(r'\s*((?:[123]\s*)?[A-Za-z]+)\.?\s*([ivxlc]+|\d+)?\.?\s*:?\s*(\d+)?', p)
    if not m: return None, None
    book = NAMES.get(re.sub(r'\s+','',m.group(1)).lower())
    if not book: return None, None
    ch = 0
    if m.group(2):
        ch = int(m.group(2)) if m.group(2).isdigit() else ROMAN.get(m.group(2).lower(), 0)
    return book, (ch, int(m.group(3)) if m.group(3) else 0)

def parse(path, author=None, want=None):
    """author=None -> attribution taken from each paragraph's opening name."""
    x = open(path, encoding='utf-8', errors='replace').read()
    i = x.find('<div1')
    if i > 0: x = x[i:]
    rows, sec, cur = [], None, None
    stack = []
    pcount = 0
    for m in TAG.finditer(x):
        s = m.group(0)
        if s.startswith('<div'):
            sec = cur = None            # a new section must re-arm its own anchor
            stack = []
            pcount = 0
        elif s.startswith('<scripCom'):
            r = re.search(r'osisRef="([^"]+)"', s)
            b, v = osis_start(r.group(1)) if r else (None, None)
            if not b:
                pa = re.search(r'passage="([^"]+)"', s)
                if pa: b, v = passage_start(pa.group(1))
            if b and (want is None or b in want):
                sec, cur = b, v
        elif s.startswith('<scripRef'):
            r = re.search(r'osisRef="([^"]+)"', s)
            if r:
                b, v = osis_start(r.group(1))
                if sec is None and pcount <= 1 and b and (want is None or b in want):
                    sec, cur = b, v
                elif b == sec and v and v[1]:
                    cur = v
        elif s.startswith('<p'):
            c = re.search(r'class="([^"]+)"', s)
            stack.append((m.end(), c.group(1) if c else ''))
        elif s == '</p>':
            if not stack: continue
            pstart, pcls = stack.pop()
            pcount += 1
            if pcls == 'scripture' and sec and cur:
                mm = LEMMA.match(clean(x[pstart:m.start()]))
                if mm:
                    a = int(mm.group(1)); b = int(mm.group(2) or a)
                    if a >= 1 and b >= a and b - a < 60: cur = (cur[0], a)
            if sec and cur and cur[0] and pcls not in SKIP_CLASS:
                t = clean(x[pstart:m.start()])
                if len(t) > 60:
                    if author:
                        rows.append((sec, cur[0], cur[1], author, t))
                    else:
                        a = ATTRIB.match(t)
                        rows.append((sec, cur[0], cur[1],
                                     a.group(1).strip() if a else '',
                                     t[a.end():] if a else t))
    return rows
