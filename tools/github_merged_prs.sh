#!/bin/bash
set -euo pipefail

# Fetches all Pull Requests opened by the authenticated user and merged since 2026-05-01.
# Uses the GitHub GraphQL API to search across all repositories.

SINCE=$(date -d '7 days ago' '+%Y-%m-%d')
USER=$(gh api user --jq '.login')

if [ -z "$USER" ]; then
    echo "Error: could not determine authenticated GitHub user." >&2
    exit 1
fi

echo "# Merged PRs for $USER (since $SINCE)"
echo ""

QUERY="author:$USER is:pr is:merged merged:>=$SINCE"
HAS_NEXT=true
CURSOR=""
ALL_RESULTS="[]"

while [ "$HAS_NEXT" = "true" ]; do
    if [ -z "$CURSOR" ]; then
        AFTER_CLAUSE=""
    else
        AFTER_CLAUSE=", after: \"$CURSOR\""
    fi

    RESULT=$(gh api graphql -f query="
    {
      search(query: \"$QUERY\", type: ISSUE, first: 100$AFTER_CLAUSE) {
        pageInfo {
          hasNextPage
          endCursor
        }
        nodes {
          ... on PullRequest {
            number
            title
            mergedAt
            url
            body
            repository {
              nameWithOwner
            }
          }
        }
      }
    }")

    ALL_RESULTS=$(echo "$ALL_RESULTS" "$RESULT" | jq -s '.[0] + (.[1].data.search.nodes)')

    HAS_NEXT=$(echo "$RESULT" | jq -r '.data.search.pageInfo.hasNextPage')
    CURSOR=$(echo "$RESULT" | jq -r '.data.search.pageInfo.endCursor')
done

echo "$ALL_RESULTS" | jq -r '
  group_by(.repository.nameWithOwner)
  | sort_by(.[0].repository.nameWithOwner)
  | .[] | "## \(.[0].repository.nameWithOwner)\n\n" + (
    [.[] | "### [#\(.number)] \(.title)\n\n- **Merged:** \(.mergedAt)\n- **URL:** \(.url)\n\n\(if .body and .body != "" then (.body | split("\n") | map(select(. != "")) | join(" ") | if length > 512 then .[:512] + "..." else . end) else "(no description)" end)\n"]
    | join("\n")
  ) + "\n"'
