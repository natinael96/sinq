#!/usr/bin/env bash
# Deploy the site by hand.
#
# CI does this on every release (see .github/workflows/site.yml). This is for
# the times you want it out now, or want to see what a change looks like before
# tagging anything.
#
#   tools/deploy_site.sh            # regenerate + deploy to production
#   tools/deploy_site.sh --preview  # regenerate + deploy to a preview URL
#   tools/deploy_site.sh --dry      # regenerate only, deploy nothing
#
# Needs the Vercel CLI and a login: `npm i -g vercel && vercel login`.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SITE="${SINQ_SITE:-$ROOT/../sinq-site}"
MODE="${1:-}"

[ -d "$SITE" ] || { echo "site checkout not found at $SITE (set SINQ_SITE)"; exit 1; }
[ -f "$SITE/index.html" ] || { echo "$SITE has no index.html — is it the gh-pages worktree?"; exit 1; }

echo "site:   $SITE"
echo "source: $ROOT/CHANGELOG.md"
echo

python3 "$ROOT/tools/build_site.py" "$SITE"

if [ "$MODE" = "--dry" ]; then
  echo
  echo "dry run — nothing deployed. Changed files:"
  git -C "$SITE" --no-pager diff --stat
  exit 0
fi

command -v vercel >/dev/null || { echo "vercel CLI not found: npm i -g vercel"; exit 1; }

cd "$SITE"
if [ "$MODE" = "--preview" ]; then
  echo; echo "deploying a preview…"
  vercel deploy --yes
else
  echo; echo "deploying to production…"
  vercel deploy --prod --yes
fi

echo
echo "Note: this deploys the working tree, committed or not."
git diff --quiet || echo "You have uncommitted site changes — commit and push them so gh-pages matches what is live."
