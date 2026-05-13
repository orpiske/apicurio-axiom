#!/bin/bash
set -euo pipefail

# Finds open PRs with failing CI checks across specified repos.
# Excludes bot/Renovate PRs. Uses GraphQL statusCheckRollup. Returns JSON array.
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

    OWNER=$(echo "$REPO" | cut -d'/' -f1)
    NAME=$(echo "$REPO" | cut -d'/' -f2)

    RESULT=$(gh api graphql -f query='
    {
      repository(owner: "'"$OWNER"'", name: "'"$NAME"'") {
        pullRequests(states: OPEN, first: 50, orderBy: {field: UPDATED_AT, direction: DESC}) {
          nodes {
            number
            title
            url
            author { login }
            createdAt
            updatedAt
            isDraft
            commits(last: 1) {
              nodes {
                commit {
                  statusCheckRollup {
                    state
                    contexts(first: 30) {
                      nodes {
                        ... on CheckRun {
                          name
                          conclusion
                          status
                        }
                        ... on StatusContext {
                          context
                          state
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }' 2>/dev/null || echo '{"data":{"repository":{"pullRequests":{"nodes":[]}}}}')

    FILTERED=$(echo "$RESULT" | jq --arg repo "$REPO" '
        [.data.repository.pullRequests.nodes[] |
            select(
                .author.login != "renovate[bot]" and
                .author.login != "dependabot[bot]" and
                .author.login != "github-actions[bot]" and
                .commits.nodes[0].commit.statusCheckRollup.state == "FAILURE"
            ) |
            {
                number,
                title,
                url,
                author: .author.login,
                createdAt,
                updatedAt,
                isDraft,
                repository: $repo,
                checkState: .commits.nodes[0].commit.statusCheckRollup.state,
                failedChecks: [.commits.nodes[0].commit.statusCheckRollup.contexts.nodes[] |
                    select(.conclusion == "FAILURE" or .conclusion == "failure" or
                           .state == "FAILURE" or .state == "failure") |
                    (.name // .context)]
            }
        ]')

    ALL_RESULTS=$(echo "$ALL_RESULTS" "$FILTERED" | jq -s '.[0] + .[1]')
done

echo "$ALL_RESULTS" | jq '.'
