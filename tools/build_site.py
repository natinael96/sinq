#!/usr/bin/env python3
"""Regenerate the marketing site's changed-log page and version strings.

The site says of itself: "No network, no tracking: the same principle as the
app." So the version it shows is baked in at build time rather than fetched
from GitHub in the browser — the page stays a static file that works offline
and tells no one it was read.

Source of truth is CHANGELOG.md. Run with the site checkout as the argument:

    python3 tools/build_site.py ../sinq-site
"""
import html
import json
import os
import re
import sys
import urllib.error
import urllib.request
from datetime import date

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHANGELOG = os.path.join(ROOT, "CHANGELOG.md")
# How many releases the page carries. Older ones stay in CHANGELOG.md, which
# the page links to — a marketing page is not an archive.
KEEP = 6

MONTHS = ["January", "February", "March", "April", "May", "June", "July",
          "August", "September", "October", "November", "December"]


def inline(md: str) -> str:
    """Markdown inline → HTML, escaping everything else."""
    out, i, parts = "", 0, []
    # Protect code spans first so their contents are never treated as markup.
    for seg in re.split(r"(`[^`]+`)", md):
        if seg.startswith("`") and seg.endswith("`") and len(seg) > 1:
            parts.append("<code>" + html.escape(seg[1:-1]) + "</code>")
            continue
        t = html.escape(seg)
        t = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r'<a href="\2">\1</a>', t)
        t = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", t)
        t = re.sub(r"(?<!\w)_([^_]+)_(?!\w)", r"<i>\1</i>", t)
        parts.append(t)
    return "".join(parts)


def parse(text: str):
    """CHANGELOG.md → [{version, date, motto, sections:[(name,[items])]}]."""
    releases = []
    blocks = re.split(r"^## \[", text, flags=re.M)[1:]
    for b in blocks:
        m = re.match(r"([0-9.]+)\]\s*—\s*(\d{4})-(\d{2})-(\d{2})", b)
        if not m:
            continue
        ver, y, mo, d = m.group(1), int(m.group(2)), int(m.group(3)), int(m.group(4))
        motto = ""
        mm = re.search(r"^_versionCode \d+ · (.+?)_$", b, flags=re.M)
        if mm:
            motto = mm.group(1)
        sections = []
        for sm in re.finditer(r"^### (.+?)$\n(.*?)(?=^### |\Z)", b, flags=re.M | re.S):
            items, cur = [], ""
            for line in sm.group(2).split("\n"):
                if line.startswith("- "):
                    if cur:
                        items.append(cur.strip())
                    cur = line[2:]
                elif line.startswith("  ") and cur:
                    cur += " " + line.strip()
                elif not line.strip() and cur:
                    items.append(cur.strip())
                    cur = ""
            if cur:
                items.append(cur.strip())
            if items:
                sections.append((sm.group(1).strip(), items))
        releases.append({"version": ver, "date": f"{d} {MONTHS[mo - 1]} {y}",
                         "motto": motto, "sections": sections})
    return releases


def article(r) -> str:
    out = ['<article class="release">', "  <header>",
           f'    <h2>{r["version"]} <span class="date">{r["date"]}</span></h2>']
    if r["motto"]:
        out.append(f'    <p class="motto"><span>{inline(r["motto"])}</span></p>')
    out += ["  </header>", '  <div class="body">']
    for name, items in r["sections"]:
        out.append(f"  <h3><span>{html.escape(name)}</span></h3>")
        out.append("  <ul>")
        for it in items:
            out.append(f"    <li><span>{inline(it)}</span></li>")
        out.append("  </ul>")
    out += ["  </div>", "</article>"]
    return "\n".join(out)


def download_total():
    """Total APK downloads across every release, or None if GitHub is unreachable.

    Counted here, at build time, and baked into the page as a number. The site
    promises no tracking and means it: the reader's browser never contacts
    GitHub, and nothing observes who is reading.
    """
    total, page = 0, 1
    try:
        while page <= 10:
            req = urllib.request.Request(
                f"https://api.github.com/repos/natinael96/sinq/releases?per_page=100&page={page}",
                headers={"Accept": "application/vnd.github+json", "User-Agent": "sinq-site-build"},
            )
            batch = json.load(urllib.request.urlopen(req, timeout=30))
            if not batch:
                break
            for release in batch:
                for asset in release.get("assets", []):
                    if asset.get("name", "").endswith(".apk"):
                        total += asset.get("download_count", 0)
            if len(batch) < 100:
                break
            page += 1
    except (urllib.error.URLError, TimeoutError, ValueError) as e:
        print(f"  downloads: unavailable ({e.__class__.__name__}) — leaving the page as it is")
        return None
    return total


def replace_between(text, start_mark, end_mark, body):
    a = text.index(start_mark) + len(start_mark)
    b = text.index(end_mark)
    return text[:a] + "\n" + body + "\n" + text[b:]


def main():
    site = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "..", "sinq-site")
    site = os.path.abspath(site)
    releases = parse(open(CHANGELOG, encoding="utf-8").read())
    if not releases:
        sys.exit("no releases parsed from CHANGELOG.md")
    latest = releases[0]
    print(f"latest {latest['version']} ({latest['date']}) · {len(releases)} releases parsed")

    # ── changelog.html ───────────────────────────────────────────────────
    p = os.path.join(site, "changelog.html")
    s = open(p, encoding="utf-8").read()
    body = "\n\n".join(article(r) for r in releases[:KEEP])
    s = replace_between(s, "<!-- releases:start -->", "<!-- releases:end -->", body)
    open(p, "w", encoding="utf-8").write(s)
    print(f"  changelog.html: {min(KEEP, len(releases))} releases")

    # ── version strings elsewhere ────────────────────────────────────────
    ver = latest["version"]
    p = os.path.join(site, "index.html")
    s = open(p, encoding="utf-8").read()
    s = re.sub(r"v\d+\.\d+\.\d+ · Android [\d.]+\+", f"v{ver} · Android {MIN_ANDROID}+", s)
    open(p, "w", encoding="utf-8").write(s)

    # ── the download count ───────────────────────────────────────────────
    total = download_total()
    if total is not None:
        p = os.path.join(site, "index.html")
        s = open(p, encoding="utf-8").read()
        if "<!-- downloads:start -->" in s:
            s = replace_between(
                s, "<!-- downloads:start -->", "<!-- downloads:end -->",
                f'        <span>{total:,} downloads</span>',
            )
            open(p, "w", encoding="utf-8").write(s)
            print(f"  index.html: {total:,} downloads")

    p = os.path.join(site, "install.html")
    s = open(p, encoding="utf-8").read()
    s = re.sub(r"Sinq-v\d+\.\d+\.\d+\.apk", f"Sinq-v{ver}.apk", s)
    open(p, "w", encoding="utf-8").write(s)

    # ── canonical URLs ───────────────────────────────────────────────────
    for name in os.listdir(site):
        if not name.endswith(".html"):
            continue
        p = os.path.join(site, name)
        s = open(p, encoding="utf-8").read()
        fixed = re.sub(
            r'(<link rel="canonical" href=")[^"]*?/([^/"]+\.html")',
            lambda m: m.group(1) + SITE_BASE + "/" + m.group(2),
            s,
        )
        fixed = re.sub(
            r'(<link rel="canonical" href=")[^"]*?"(\s*>)',
            lambda m: m.group(1) + SITE_BASE + "/\"" + m.group(2),
            fixed,
        ) if "canonical" in fixed and "index" in name else fixed
        if fixed != s:
            open(p, "w", encoding="utf-8").write(fixed)
    print(f"  canonicals -> {SITE_BASE}")
    print(f"  index.html / install.html: v{ver}, Android {MIN_ANDROID}+")


# Kept beside the app's minSdk; update both together.
MIN_ANDROID = "6.0"

# What the pages call themselves. Canonical URLs must name a page that actually
# resolves — pointing them at a host that 404s tells search engines the real
# copy is missing and buries the one that works.
#
# sinq.natinael96.tech is the intended home, on Vercel. Flip this back the
# moment that deployment is serving again; nothing else has to change.
SITE_BASE = "https://natinael96.github.io/sinq"

if __name__ == "__main__":
    main()
