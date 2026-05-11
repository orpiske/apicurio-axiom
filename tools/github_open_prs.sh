#!/bin/bash
set -euo pipefail

# Fetches all Pull Requests opened by the authenticated user that are currently open or in review.
# Uses the GitHub GraphQL API to search across all repositories.

USER=$(gh api user --jq '.login')

if [ -z "$USER" ]; then
    echo "Error: could not determine authenticated GitHub user." >&2
    exit 1
fi

echo "# Open PRs by $USER"
echo ""

QUERY="author:$USER is:pr is:open"
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
            createdAt
            updatedAt
            url
            body
            isDraft
            reviewDecision
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
    [.[] |
      (if .isDraft then "Draft" elif .reviewDecision == "APPROVED" then "Approved" elif .reviewDecision == "CHANGES_REQUESTED" then "Changes Requested" elif .reviewDecision == "REVIEW_REQUIRED" then "Review Required" else "Open" end) as $status |
      "### [#\(.number)] \(.title)\n\n- **Status:** \($status)\n- **Created:** \(.createdAt)\n- **Updated:** \(.updatedAt)\n- **URL:** \(.url)\n\n\(if .body and .body != "" then (.body | split("\n") | map(select(. != "")) | join(" ") | if length > 512 then .[:512] + "..." else . end) else "(no description)" end)\n"
    ] | join("\n")
  ) + "\n"'
