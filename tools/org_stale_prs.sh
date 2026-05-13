#!/bin/bash
set -euo pipefail

# Finds open PRs that have gone stale (no updates for N+ days).
# Excludes bot/Renovate PRs and draft PRs. Returns JSON array sorted by staleness.
#
# Parameters (via environment or positional):
#   REPOS      - comma-separated owner/repo list
#   STALE_DAYS - number of days without update to consider stale (default: 7)

REPOS="${REPOS:-$1}"
STALE_DAYS="${STALE_DAYS:-${2:-7}}"

if [ -z "$REPOS" ]; then
    echo "Error: REPOS parameter is required (comma-separated owner/repo list)" >&2
    exit 1
fi

CUTOFF=$(date -d "$STALE_DAYS days ago" -u '+%Y-%m-%dT%H:%M:%SZ')

ALL_RESULTS="[]"
for REPO in $(echo "$REPOS" | tr ',' ' '); do
    [ -z "$REPO" ] && continue

    PRS=$(gh pr list --repo "$REPO" --state open --limit 100 \
        --json number,title,author,createdAt,updatedAt,isDraft,reviewDecision,url \
        2>/dev/null || echo "[]")

    FILTERED=$(echo "$PRS" | jq --arg repo "$REPO" --arg cutoff "$CUTOFF" '
        [.[] | select(
            .updatedAt < $cutoff and
            .isDraft == false and
            .author.login != "renovate[bot]" and
            .author.login != "dependabot[bot]" and
            .author.login != "github-actions[bot]"
        ) | . + {
            repository: $repo,
            staleDays: (((now - (.updatedAt | fromdateiso8601)) / 86400) | floor)
        }]')

    ALL_RESULTS=$(echo "$ALL_RESULTS" "$FILTERED" | jq -s '.[0] + .[1]')
done

echo "$ALL_RESULTS" | jq 'sort_by(-.staleDays)'
