#!/bin/bash
set -euo pipefail

# Lists issues created since a given date across specified repos.
# Excludes epic/tracking/wishlist labels and bot-created issues. Returns JSON array.
#
# Parameters (via environment or positional):
#   REPOS  - comma-separated owner/repo list
#   SINCE  - ISO date (e.g. "2026-05-12")

REPOS="${REPOS:-$1}"
SINCE="${SINCE:-$2}"

if [ -z "$REPOS" ]; then
    echo "Error: REPOS parameter is required (comma-separated owner/repo list)" >&2
    exit 1
fi

if [ -z "$SINCE" ]; then
    echo "Error: SINCE parameter is required (ISO date, e.g. 2026-05-12)" >&2
    exit 1
fi

ALL_RESULTS="[]"
for REPO in $(echo "$REPOS" | tr ',' ' '); do
    [ -z "$REPO" ] && continue

    ISSUES=$(gh issue list --repo "$REPO" --state all --limit 100 \
        --json number,title,author,labels,createdAt,updatedAt,url,comments,state \
        2>/dev/null || echo "[]")

    FILTERED=$(echo "$ISSUES" | jq --arg repo "$REPO" --arg since "$SINCE" '
        [.[] | select(
            .createdAt >= $since and
            (.author.login | test("\\[bot\\]$") | not) and
            ([.labels[].name] | map(ascii_downcase) |
                any(. == "epic" or . == "tracking" or . == "wishlist" or
                    . == "someday" or . == "wontfix" or . == "wont-fix") | not)
        ) | {
            number,
            title,
            state,
            author: .author.login,
            labels: [.labels[].name],
            createdAt,
            url,
            commentCount: (.comments | length),
            repository: $repo
        }]')

    ALL_RESULTS=$(echo "$ALL_RESULTS" "$FILTERED" | jq -s '.[0] + .[1]')
done

echo "$ALL_RESULTS" | jq 'sort_by(.createdAt) | reverse'
