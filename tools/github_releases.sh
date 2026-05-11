#!/bin/bash
set -euo pipefail

# Fetches all GitHub releases published since one week ago in repositories the authenticated
# user has been active in. Discovers repos via recent merged PRs and commits.

SINCE_DATE=$(date -d '7 days ago' '+%Y-%m-%d')
SINCE_ISO="${SINCE_DATE}T00:00:00Z"
USER=$(gh api user --jq '.login')

if [ -z "$USER" ]; then
    echo "Error: could not determine authenticated GitHub user." >&2
    exit 1
fi

echo "# Releases in $USER's active repositories (since $SINCE_DATE)"
echo ""

REPOS=""

PR_REPOS=$(gh api graphql -f query="
{
  search(query: \"author:$USER is:pr is:merged merged:>=$SINCE_DATE\", type: ISSUE, first: 100) {
    nodes { ... on PullRequest { repository { nameWithOwner } } }
  }
}" --jq '.data.search.nodes[].repository.nameWithOwner' 2>/dev/null || true)

COMMIT_REPOS=$(gh search commits \
    --author "$USER" \
    --author-date ">=$SINCE_DATE" \
    --json repository \
    --limit 200 \
    --jq '.[].repository.fullName' 2>/dev/null || true)

REPOS=$(printf "%s\n%s" "$PR_REPOS" "$COMMIT_REPOS" | sort -u | grep -v '^$')

if [ -z "$REPOS" ]; then
    echo "*No repositories with recent activity found.*"
    exit 0
fi

FOUND=false

for REPO in $REPOS; do
    RELEASES=$(gh api "repos/$REPO/releases" --jq \
        "[.[] | select(.published_at >= \"$SINCE_ISO\") | {
            tag: .tag_name,
            name: .name,
            publishedAt: .published_at,
            url: .html_url,
            body: .body
        }]" 2>/dev/null || echo "[]")

    if [ "$RELEASES" != "[]" ] && [ -n "$RELEASES" ]; then
        FOUND=true

        echo "## $REPO"
        echo ""

        echo "$RELEASES" | jq -r '
          .[] | "### \(.name // .tag)\n\n- **Tag:** \(.tag)\n- **Published:** \(.publishedAt)\n- **URL:** \(.url)\n\n\(if .body and .body != "" then (.body | split("\n") | map(select(. != "")) | join(" ") | if length > 512 then .[:512] + "..." else . end) else "(no release notes)" end)\n"'
    fi
done

if [ "$FOUND" = false ]; then
    echo "*No releases found in the past week.*"
fi
