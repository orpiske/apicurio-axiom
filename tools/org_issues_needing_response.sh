#!/bin/bash
set -euo pipefail

# Finds open issues with recent comments that may need a response from maintainers.
# Uses GraphQL to fetch issues updated since a date along with their last 3 comments.
# Returns JSON array.
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

    OWNER=$(echo "$REPO" | cut -d'/' -f1)
    NAME=$(echo "$REPO" | cut -d'/' -f2)

    RESULT=$(gh api graphql -f query='
    {
      search(query: "repo:'"$REPO"' is:issue is:open updated:>='"$SINCE"'", type: ISSUE, first: 50) {
        nodes {
          ... on Issue {
            number
            title
            url
            createdAt
            updatedAt
            author { login }
            labels(first: 10) { nodes { name } }
            comments(last: 3) {
              totalCount
              nodes {
                author { login }
                createdAt
                body
              }
            }
          }
        }
      }
    }' 2>/dev/null || echo '{"data":{"search":{"nodes":[]}}}')

    FILTERED=$(echo "$RESULT" | jq --arg repo "$REPO" '
        [.data.search.nodes[] |
            select(.comments.totalCount > 0) |
            {
                number,
                title,
                url,
                createdAt,
                updatedAt,
                author: .author.login,
                labels: [.labels.nodes[].name],
                totalComments: .comments.totalCount,
                recentComments: [.comments.nodes[] | {
                    author: .author.login,
                    createdAt,
                    bodyPreview: (.body | split("\n") | map(select(. != "")) | join(" ") |
                        if length > 200 then .[:200] + "..." else . end)
                }],
                lastCommentAuthor: .comments.nodes[-1].author.login,
                lastCommentDate: .comments.nodes[-1].createdAt,
                repository: $repo
            }
        ]')

    ALL_RESULTS=$(echo "$ALL_RESULTS" "$FILTERED" | jq -s '.[0] + .[1]')
done

echo "$ALL_RESULTS" | jq 'sort_by(.updatedAt) | reverse'
