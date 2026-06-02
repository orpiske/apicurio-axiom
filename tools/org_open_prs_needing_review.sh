#!/bin/bash
set -euo pipefail

# Lists all open PRs across specified repos that need human review.
# Excludes bot/Renovate PRs. Returns JSON array.
#
# Parameters (via environment or positional):
#   REPOS  - comma-separated owner/repo list (e.g. "Apitomy/apitomy-data-models,Apitomy/apitomy-codegen")

REPOS="${REPOS:-$1}"

if [ -z "$REPOS" ]; then
    echo "Error: REPOS parameter is required (comma-separated owner/repo list)" >&2
    exit 1
fi

ALL_RESULTS="[]"
for REPO in $(echo "$REPOS" | tr ',' ' '); do
    # Skip empty entries
    [ -z "$REPO" ] && continue

    PRS=$(gh pr list --repo "$REPO" --state open --limit 100 \
        --json number,title,author,createdAt,updatedAt,isDraft,reviewDecision,url \
        2>/dev/null || echo "[]")

    FILTERED=$(echo "$PRS" | jq --arg repo "$REPO" '
        [.[] | select(
            .author.login != "renovate[bot]" and
            .author.login != "dependabot[bot]" and
            .author.login != "github-actions[bot]" and
            (.title | test("^(chore\\(deps\\)|fix\\(deps\\)|Update dependency)"; "i") | not)
        ) | . + {
            repository: $repo,
            ageInDays: (((now - (.createdAt | fromdateiso8601)) / 86400) | floor),
            daysSinceUpdate: (((now - (.updatedAt | fromdateiso8601)) / 86400) | floor)
        }]')

    ALL_RESULTS=$(echo "$ALL_RESULTS" "$FILTERED" | jq -s '.[0] + .[1]')
done

echo "$ALL_RESULTS" | jq '.'
