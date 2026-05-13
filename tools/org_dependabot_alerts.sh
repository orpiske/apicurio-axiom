#!/bin/bash
set -euo pipefail

# Queries open Dependabot security alerts at critical and high severity
# across specified repos. Returns JSON array.
#
# Parameters (via environment or positional):
#   REPOS  - comma-separated owner/repo list

REPOS="${REPOS:-$1}"

if [ -z "$REPOS" ]; then
    echo "Error: REPOS parameter is required (comma-separated owner/repo list)" >&2
    exit 1
fi

ALL_RESULTS="[]"
for REPO in $(echo "$REPOS" | tr ',' ' '); do
    [ -z "$REPO" ] && continue

    # Dependabot alerts API may return 403 if not enabled for repo — handle gracefully
    ALERTS=$(gh api "repos/$REPO/dependabot/alerts?state=open&severity=critical,high&per_page=25" \
        2>/dev/null || echo "[]")

    FORMATTED=$(echo "$ALERTS" | jq --arg repo "$REPO" '
        if type == "array" then
            [.[] | {
                number: .number,
                severity: .security_advisory.severity,
                summary: .security_advisory.summary,
                package: .dependency.package.name,
                ecosystem: .dependency.package.ecosystem,
                vulnerableRange: .dependency.package.ecosystem,
                url: .html_url,
                repository: $repo,
                createdAt: .created_at
            }]
        else [] end')

    ALL_RESULTS=$(echo "$ALL_RESULTS" "$FORMATTED" | jq -s '.[0] + .[1]')
done

echo "$ALL_RESULTS" | jq 'sort_by(.severity) | reverse'
