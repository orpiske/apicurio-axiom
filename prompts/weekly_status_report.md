# Weekly Status Report

Generate a weekly status report summarizing my work over the past 7 days. This report is intended
for my manager and should be concise, professional, and focused on impact and outcomes rather than
raw activity.

## Data Collection

Run the following tools to gather the raw data. Run all four in parallel
since they are independent:

1. `github_merged_prs.sh` — Pull Requests I authored that were merged this week
2. `github_merged_commits.sh` — Commits I made to default branches this week
3. `github_open_prs.sh` — Pull Requests I have currently open or in review
4. `github_releases.sh` — Releases published this week in my active repositories

## Report Structure

Using the collected data, produce a Markdown report with the following sections:

### Header
- Title: "Weekly Status Report"
- Date range (past 7 days)
- My name (from GitHub)

### Highlights
- 2-4 bullet points summarizing the most important accomplishments this week
- Focus on features, fixes, and milestones — not chores like dependency bumps
- Mention releases as shipped milestones

### Merged Work
- Group by repository
- For each repository, summarize the merged PRs into logical categories (features, bug fixes,
  dependency updates, CI/infrastructure, cleanup/refactoring)
- Consolidate related PRs into a single line where appropriate (e.g. batch dependency updates
  into one bullet instead of listing each individually)
- Include PR numbers as references

### Releases
- List each release with its repository, tag, and a one-line summary
- Skip this section entirely if there were no releases

### In Progress
- List open/in-review PRs grouped by repository
- Include the current review status (Draft, Open, Approved, Changes Requested)
- Skip PRs that are clearly stale (older than 30 days with no recent updates)

### Next Week
- Based on the open PRs and the trajectory of this week's work, suggest 2-3 brief bullets
  about what is likely coming next week

## Formatting Guidelines
- Use clean, readable Markdown
- Keep the report to roughly one page of content — be concise
- Use bullet points, not paragraphs
- Don't include raw URLs in the body text — use Markdown links where referencing specific PRs
  or releases
- Don't include commit SHAs or individual commits — the merged PRs capture the meaningful units
  of work
- Omit the commits data from the report body unless a significant commit landed directly on main
  without a PR
