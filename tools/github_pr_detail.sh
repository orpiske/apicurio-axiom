#!/bin/bash
set -euo pipefail

# Fetches full details for a specific GitHub Pull Request.
# Usage: github_pr_detail.sh <repo> <pr_number>
#   e.g. github_pr_detail.sh Apicurio/apicurio-registry 7984

if [ $# -ne 2 ]; then
    echo "Usage: $0 <owner/repo> <pr_number>" >&2
    echo "  e.g. $0 Apicurio/apicurio-registry 7984" >&2
    exit 1
fi

REPO="$1"
PR_NUMBER="$2"

RESULT=$(gh api graphql -f query="
{
  repository(owner: \"${REPO%%/*}\", name: \"${REPO##*/}\") {
    pullRequest(number: $PR_NUMBER) {
      number
      title
      state
      isDraft
      createdAt
      updatedAt
      mergedAt
      closedAt
      url
      body
      additions
      deletions
      changedFiles
      reviewDecision
      author {
        login
      }
      mergedBy {
        login
      }
      labels(first: 20) {
        nodes { name }
      }
      reviews(first: 20) {
        nodes {
          author { login }
          state
          submittedAt
        }
      }
      comments(first: 50) {
        totalCount
        nodes {
          author { login }
          body
          createdAt
        }
      }
      files(first: 100) {
        nodes {
          path
          additions
          deletions
        }
      }
    }
  }
}")

echo "$RESULT" | jq -r '
  .data.repository.pullRequest as $pr |

  "# [\($pr.author.login)/'"$REPO"'#\($pr.number)] \($pr.title)\n" +
  "\n" +
  "- **State:** \(if $pr.isDraft then "Draft" else $pr.state end)\n" +
  "- **Review:** \($pr.reviewDecision // "None")\n" +
  "- **Created:** \($pr.createdAt)\n" +
  "- **Updated:** \($pr.updatedAt)\n" +
  (if $pr.mergedAt then "- **Merged:** \($pr.mergedAt) (by \($pr.mergedBy.login))\n" else "" end) +
  "- **URL:** \($pr.url)\n" +
  "- **Changes:** +\($pr.additions) -\($pr.deletions) across \($pr.changedFiles) files\n" +
  (if ($pr.labels.nodes | length) > 0 then "- **Labels:** \([$pr.labels.nodes[].name] | join(", "))\n" else "" end) +
  "\n## Description\n\n\($pr.body // "(no description)")\n" +
  "\n## Reviews\n\n" +
  (if ($pr.reviews.nodes | length) > 0 then
    ([$pr.reviews.nodes[] | "- **\(.author.login):** \(.state) (\(.submittedAt))"] | join("\n")) + "\n"
  else
    "*No reviews.*\n"
  end) +
  "\n## Comments (\($pr.comments.totalCount))\n\n" +
  (if ($pr.comments.nodes | length) > 0 then
    ([$pr.comments.nodes[] | "### \(.author.login) (\(.createdAt))\n\n\(.body)\n"] | join("\n"))
  else
    "*No comments.*\n"
  end) +
  "\n## Changed Files\n\n" +
  ([$pr.files.nodes[] | "- `\(.path)` (+\(.additions) -\(.deletions))"] | join("\n")) + "\n"'
