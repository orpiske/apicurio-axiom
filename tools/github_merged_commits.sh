#!/bin/bash
set -euo pipefail

# Fetches all commits made by the authenticated user to default branches since one week ago.
# GitHub's commit search only indexes the default branch, so results are limited to main/master.

SINCE=$(date -d '7 days ago' '+%Y-%m-%d')
USER=$(gh api user --jq '.login')

if [ -z "$USER" ]; then
    echo "Error: could not determine authenticated GitHub user." >&2
    exit 1
fi

echo "# Commits by $USER to default branches (since $SINCE)"
echo ""

ALL_RESULTS=$(gh search commits \
    --author "$USER" \
    --author-date ">=$SINCE" \
    --json repository,sha,commit,url \
    --limit 500)

echo "$ALL_RESULTS" | jq -r '
  group_by(.repository.fullName)
  | sort_by(.[0].repository.fullName)
  | .[] | "## \(.[0].repository.fullName)\n\n" + (
    [.[] |
      (.commit.message | split("\n")[0]) as $subject |
      (.commit.message | split("\n")[1:] | map(select(. != "")) | join(" ")) as $body |
      "### [`\(.sha[0:7])`](\(.url)) \($subject)\n\n- **Date:** \(.commit.author.date)\n\n\(if $body != "" then (if ($body | length) > 512 then $body[:512] + "..." else $body end) else "(no description)" end)\n"
    ] | join("\n")
  ) + "\n"'
