#!/usr/bin/env python3
"""
JIRA REST API Client - Called by AI agents via Bash.

Usage:
    python tools/jira_client.py fetch PROJ-123
    python tools/jira_client.py attachments PROJ-123
    python tools/jira_client.py links PROJ-123
    python tools/jira_client.py comments PROJ-123
"""

import base64
import json
import os
import re
import sys
from pathlib import Path

import requests


class JiraClient:
    def __init__(self):
        self.base_url = os.environ["JIRA_BASE_URL"].rstrip("/")
        self.email = os.environ["JIRA_EMAIL"]
        self.api_token = os.environ["JIRA_API_TOKEN"]
        self.auth = (self.email, self.api_token)
        self.headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
        }

    def _get(self, endpoint: str, params: dict = None) -> dict:
        url = f"{self.base_url}/rest/api/3/{endpoint}"
        resp = requests.get(url, headers=self.headers, auth=self.auth, params=params)
        resp.raise_for_status()
        return resp.json()

    def _adf_to_text(self, node: dict, depth: int = 0) -> str:
        """Recursively convert Atlassian Document Format to plain text."""
        if not node or not isinstance(node, dict):
            return ""

        text_parts = []
        node_type = node.get("type", "")

        if node_type == "text":
            return node.get("text", "")

        if node_type == "heading":
            level = node.get("attrs", {}).get("level", 1)
            prefix = "#" * level + " "
            content = "".join(self._adf_to_text(c, depth) for c in node.get("content", []))
            return f"\n{prefix}{content}\n"

        if node_type == "paragraph":
            content = "".join(self._adf_to_text(c, depth) for c in node.get("content", []))
            return f"{content}\n"

        if node_type == "bulletList":
            items = []
            for item in node.get("content", []):
                item_text = "".join(self._adf_to_text(c, depth + 1) for c in item.get("content", []))
                items.append(f"{'  ' * depth}- {item_text.strip()}")
            return "\n".join(items) + "\n"

        if node_type == "orderedList":
            items = []
            for i, item in enumerate(node.get("content", []), 1):
                item_text = "".join(self._adf_to_text(c, depth + 1) for c in item.get("content", []))
                items.append(f"{'  ' * depth}{i}. {item_text.strip()}")
            return "\n".join(items) + "\n"

        if node_type == "codeBlock":
            content = "".join(self._adf_to_text(c, depth) for c in node.get("content", []))
            return f"\n```\n{content}\n```\n"

        if node_type == "table":
            rows = []
            for row in node.get("content", []):
                cells = []
                for cell in row.get("content", []):
                    cell_text = "".join(self._adf_to_text(c, depth) for c in cell.get("content", []))
                    cells.append(cell_text.strip())
                rows.append(" | ".join(cells))
            return "\n".join(rows) + "\n"

        if node_type == "mention":
            return f"@{node.get('attrs', {}).get('text', 'unknown')}"

        if node_type in ("inlineCard", "blockCard"):
            return node.get("attrs", {}).get("url", "")

        # Recurse into child content
        for child in node.get("content", []):
            text_parts.append(self._adf_to_text(child, depth))

        return "".join(text_parts)

    def fetch(self, ticket_id: str) -> dict:
        """Fetch complete ticket details."""
        issue = self._get(f"issue/{ticket_id}", params={
            "expand": "renderedFields,names,changelog",
            "fields": "*all",
        })

        description_adf = issue.get("fields", {}).get("description", {})
        description_text = self._adf_to_text(description_adf) if description_adf else ""

        result = {
            "ticket_id": ticket_id,
            "key": issue.get("key"),
            "summary": issue["fields"].get("summary", ""),
            "description_raw": description_text,
            "status": issue["fields"].get("status", {}).get("name", ""),
            "priority": issue["fields"].get("priority", {}).get("name", ""),
            "issue_type": issue["fields"].get("issuetype", {}).get("name", ""),
            "labels": issue["fields"].get("labels", []),
            "story_points": issue["fields"].get("customfield_10028"),
            "assignee": (issue["fields"].get("assignee") or {}).get("displayName", "Unassigned"),
            "reporter": (issue["fields"].get("reporter") or {}).get("displayName", "Unknown"),
            "created": issue["fields"].get("created", ""),
            "updated": issue["fields"].get("updated", ""),
            "components": [c.get("name") for c in issue["fields"].get("components", [])],
            "fix_versions": [v.get("name") for v in issue["fields"].get("fixVersions", [])],
            "attachment_count": len(issue["fields"].get("attachment", [])),
            "subtasks": [
                {"key": st["key"], "summary": st["fields"]["summary"], "status": st["fields"]["status"]["name"]}
                for st in issue["fields"].get("subtasks", [])
            ],
            "linked_issues": [],
        }

        # Extract issue links
        for link in issue["fields"].get("issuelinks", []):
            link_info = {}
            if "outwardIssue" in link:
                link_info = {
                    "type": link["type"]["outward"],
                    "key": link["outwardIssue"]["key"],
                    "summary": link["outwardIssue"]["fields"]["summary"],
                    "status": link["outwardIssue"]["fields"]["status"]["name"],
                }
            elif "inwardIssue" in link:
                link_info = {
                    "type": link["type"]["inward"],
                    "key": link["inwardIssue"]["key"],
                    "summary": link["inwardIssue"]["fields"]["summary"],
                    "status": link["inwardIssue"]["fields"]["status"]["name"],
                }
            if link_info:
                result["linked_issues"].append(link_info)

        # Extract FDC/external document URLs from description
        fdc_documents = []
        desc_json = json.dumps(description_adf) if description_adf else ""
        for match in re.finditer(r'https?://[^\s"\\]+sharepoint\.com[^\s"\\]*', desc_json):
            url = match.group(0).rstrip('.,;)')
            fdc_documents.append({"url": url, "type": "sharepoint", "auto_downloadable": False})
        for match in re.finditer(r'https?://docs\.google\.com[^\s"\\]*|https?://drive\.google\.com[^\s"\\]*', desc_json):
            url = match.group(0).rstrip('.,;)')
            fdc_documents.append({"url": url, "type": "google_docs", "auto_downloadable": False})
        for match in re.finditer(r'https?://[^\s"\\]+\.atlassian\.net/wiki[^\s"\\]*', desc_json):
            url = match.group(0).rstrip('.,;)')
            fdc_documents.append({"url": url, "type": "confluence", "auto_downloadable": True})
        if fdc_documents:
            result["fdc_documents"] = fdc_documents

        return result

    def attachments(self, ticket_id: str) -> dict:
        """Download all attachments for a ticket."""
        issue = self._get(f"issue/{ticket_id}", params={"fields": "attachment"})
        attachments = issue.get("fields", {}).get("attachment", [])

        output_dir = Path(os.getenv("OUTPUT_DIR", "./output")) / ticket_id / "attachments"
        output_dir.mkdir(parents=True, exist_ok=True)

        downloaded = []
        for att in attachments:
            filename = att["filename"]
            url = att["content"]
            filepath = output_dir / filename

            resp = requests.get(url, auth=self.auth, stream=True)
            resp.raise_for_status()
            with open(filepath, "wb") as f:
                for chunk in resp.iter_content(chunk_size=8192):
                    f.write(chunk)

            downloaded.append({
                "filename": filename,
                "path": str(filepath),
                "size": att.get("size", 0),
                "mime_type": att.get("mimeType", "unknown"),
                "created": att.get("created", ""),
            })

        return {"ticket_id": ticket_id, "attachments": downloaded, "count": len(downloaded)}

    def links(self, ticket_id: str) -> dict:
        """Extract all external links: PRs, Confluence pages, web links."""
        result = {"ticket_id": ticket_id, "pull_requests": [], "confluence_pages": [], "web_links": [], "branches": []}

        # SOURCE 1: JIRA Development Section (dev-status API)
        try:
            issue = self._get(f"issue/{ticket_id}", params={"fields": "summary"})
            issue_id = issue.get("id")
            if issue_id:
                dev_url = f"{self.base_url}/rest/dev-status/1.0/issue/detail"
                dev_resp = requests.get(
                    dev_url,
                    headers=self.headers,
                    auth=self.auth,
                    params={"issueId": issue_id, "applicationType": "bitbucket", "dataType": "pullrequest"},
                )
                if dev_resp.status_code == 200:
                    dev_data = dev_resp.json()
                    for detail in dev_data.get("detail", []):
                        for branch in detail.get("branches", []):
                            repo_info = branch.get("repository", {})
                            repo_name_full = repo_info.get("name", "")
                            repo_slug = repo_name_full.split("/")[-1] if "/" in repo_name_full else repo_name_full
                            result["branches"].append({
                                "name": branch.get("name", ""),
                                "repo_slug": repo_slug,
                                "last_commit": branch.get("lastCommit", {}).get("id", "")[:12],
                                "author": branch.get("lastCommit", {}).get("author", {}).get("name", ""),
                            })

                        for pr in detail.get("pullRequests", []):
                            pr_id = pr.get("id")
                            pr_name = pr.get("name", "")
                            source_branch = pr.get("source", {}).get("branch", "")
                            dest_branch = pr.get("destination", {}).get("branch", "")

                            is_feature_pr = source_branch.startswith("feature/") or source_branch.startswith("hotfix/")
                            if not is_feature_pr:
                                continue

                            repo_name = ""
                            repo_name_full = pr.get("repositoryName", "")
                            if repo_name_full:
                                repo_name = repo_name_full.split("/")[-1] if "/" in repo_name_full else repo_name_full

                            pr_url = f"https://bitbucket.org/syncappinc/{repo_name}/pull-requests/{pr_id}" if repo_name and pr_id else ""
                            result["pull_requests"].append({
                                "title": pr_name,
                                "url": pr_url,
                                "repo_slug": repo_name,
                                "pr_id": int(pr_id) if pr_id else None,
                                "source_branch": source_branch,
                                "target_branch": dest_branch,
                                "author": pr.get("author", {}).get("name", ""),
                                "status": pr.get("status", ""),
                                "source": "jira_development_section",
                            })
        except Exception:
            pass

        # SOURCE 2: JIRA Remote Links
        try:
            remote_links = self._get(f"issue/{ticket_id}/remotelink")
            for link in remote_links:
                obj = link.get("object", {})
                url = obj.get("url", "")
                title = obj.get("title", "")

                if "bitbucket" in url and "pull-request" in url:
                    parts = url.rstrip("/").split("/")
                    pr_id = parts[-1] if parts[-1].isdigit() else None
                    repo_slug = parts[-3] if len(parts) >= 4 else None
                    if not any(pr.get("pr_id") == int(pr_id) and pr.get("repo_slug") == repo_slug for pr in result["pull_requests"] if pr_id):
                        result["pull_requests"].append({
                            "title": title,
                            "url": url,
                            "repo_slug": repo_slug,
                            "pr_id": int(pr_id) if pr_id else None,
                            "source": "jira_remote_link",
                        })
                elif "confluence" in url or "wiki" in url:
                    result["confluence_pages"].append({"title": title, "url": url})
                else:
                    result["web_links"].append({"title": title, "url": url})
        except requests.HTTPError:
            pass

        # SOURCE 3: Description text parsing
        try:
            issue = self._get(f"issue/{ticket_id}", params={"fields": "description"})
            desc = json.dumps(issue.get("fields", {}).get("description", {}))

            bb_pattern = r'https?://bitbucket\.org/[\w-]+/[\w-]+/pull-requests?/(\d+)'
            for match in re.finditer(bb_pattern, desc):
                url = match.group(0)
                parts = url.rstrip("/").split("/")
                pr_id = parts[-1] if parts[-1].isdigit() else None
                repo_slug = parts[-3] if len(parts) >= 4 else None
                if not any(pr.get("pr_id") == int(pr_id) and pr.get("repo_slug") == repo_slug for pr in result["pull_requests"] if pr_id):
                    result["pull_requests"].append({
                        "title": "PR from description",
                        "url": url,
                        "repo_slug": repo_slug,
                        "pr_id": int(pr_id) if pr_id else None,
                        "source": "description_text",
                    })
        except Exception:
            pass

        return result

    def comments(self, ticket_id: str) -> dict:
        """Get all comments on a ticket."""
        data = self._get(f"issue/{ticket_id}/comment", params={"orderBy": "-created"})
        comments = []
        for comment in data.get("comments", []):
            body = self._adf_to_text(comment.get("body", {}))
            comments.append({
                "author": comment.get("author", {}).get("displayName", "Unknown"),
                "created": comment.get("created", ""),
                "body": body.strip(),
            })
        return {"ticket_id": ticket_id, "comments": comments, "count": len(comments)}


def main():
    from dotenv import load_dotenv
    load_dotenv(Path(__file__).parent.parent / ".env")

    if len(sys.argv) < 3:
        print("Usage: python tools/jira_client.py <command> <ticket_id>")
        print("Commands: fetch, attachments, links, comments")
        sys.exit(1)

    command = sys.argv[1]
    ticket_id = sys.argv[2]

    client = JiraClient()
    commands = {
        "fetch": client.fetch,
        "attachments": client.attachments,
        "links": client.links,
        "comments": client.comments,
    }

    if command not in commands:
        print(f"Unknown command: {command}")
        sys.exit(1)

    result = commands[command](ticket_id)
    print(json.dumps(result, indent=2, default=str))


if __name__ == "__main__":
    main()
