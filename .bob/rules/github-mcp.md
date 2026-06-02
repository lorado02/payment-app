# GitHub MCP Server Rules

Always use the GitHub MCP server tools for GitHub operations instead of `gh` CLI commands or raw `git` commands when an MCP tool is available for the task.

## General Guidelines

- Prefer GitHub MCP tools over `gh` CLI or shell commands for any GitHub API interaction such as managing repositories, branches, pull requests, issues, code search, and reviews
- For local git operations (staging, committing, pushing to already-configured remotes), `git` CLI is acceptable
- For all GitHub API tasks, always check if a GitHub MCP tool is available before falling back to CLI commands
- When a GitHub task requires multiple steps, chain MCP tool calls rather than shelling out
- When a GitHub task requires multiple steps, use the same MCP tool for all steps