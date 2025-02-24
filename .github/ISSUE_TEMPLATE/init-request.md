### 👋 Welcome to Repository Setup!

To initialize your fork management repository, I need to know which repository you want to sync with.

#### 🔑 Required Setup - Personal Access Token (PAT)

1. Create a Personal Access Token (PAT) with the following permissions:
   - `repo` (Full control of private repositories)
   - `workflow` (Update GitHub Action workflows)
   - `admin:read:org` (Read organization information)
   - `admin:write:repo_hook` (Full control of repository hooks)
   - `write:discussion` (Add comments to issues and pull requests)
2. Add the token as a repository secret named `PAT_TOKEN`
3. Comment on this issue with the upstream repository to sync with, using either:
   - GitHub format: `owner/repo`
   - GitLab URL: `https://gitlab.com/owner/repo`

Example responses:
```kubernetes/kubernetes
microsoft/vscode
apache/spark
```
or
```
https://gitlab.com/gitlab-org/gitlab
```

The initialization process will begin automatically after you comment. 

⚠️ **The initialization process will fail without this token properly configured.**

#### Invalid Formats to Avoid
```
❌ https://github.com/owner/repo         (GitHub URL - use owner/repo instead)
❌ github.com/owner/repo                 (don't include domain for GitHub)
❌ just-repo-name                        (must include owner/group)
❌ Owner/Repo                            (case sensitive)
❌ gitlab.com/group/project              (GitLab needs complete https:// URL)
```
