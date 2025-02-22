# Fork Management Initialization Workflow

The Initialization Workflow (`.github/workflows/init.yml`) automates the setup of a fork management repository, ensuring proper branch structures, protection rules, and configurations are applied. This workflow streamlines the process of bringing online a fork with its upstream repository while maintaining controlled updates and synchronization.

## Purpose

The goal of this workflow is to:

- Establish the required branch structure for proper upstream synchronization.

- Configure branch protection rules to maintain repository integrity.

- Manage repository setup through automated issue tracking.

- Provide a clean initialization process by handling setup, validation, and cleanup efficiently.

## How the Workflow is Triggered

The workflow can be triggered through multiple mechanisms:

```mermaid
graph LR
    A[Repository Created] -->|create: main| D[Workflow]
    B[Issue Comment] -->|issue_comment: created| D
    C[Manual Trigger] -->|workflow_dispatch| D
```

### Trigger Details

1. Automatic – Runs when a repository is created with the main branch.

2. Issue Comment – Activated when a user provides an upstream repository reference in an issue.

3. Manual – A repository owner can trigger it from GitHub Actions.

## Initialization Process

### 1. Workflow Startup & Validation

The workflow first checks if initialization is necessary by determining:

- If .github/workflow.env exists (indicating prior initialization).

- If required branches (main, fork_upstream, fork_integration) are already present and protected.

```mermaid
flowchart TD
    A[Start] --> B{workflow.env exists?}
    B -->|Yes| C[Skip Init]
    B -->|No| D{Check Branches}
    D -->|All Exist & Protected| E[Skip Init]
    D -->|Missing/Unprotected| F[Continue Init]
```

### 2. Issue-Based Setup

If initialization is required, the workflow creates an issue and awaits the user to provide the upstream repository reference.

```mermaid
sequenceDiagram
    participant U as User
    participant W as Workflow
    participant I as Issue
    participant R as Repository

    W->>I: Create initialization issue
    Note over I: Await user input for upstream repo
    U->>I: Provide upstream repository reference
    W->>I: Validate repository format
    alt Valid Format
        W->>R: Begin setup process
        W->>I: Post progress updates
        W->>R: Create cleanup PR
        Note over R: PR merges & closes issue
    else Invalid Format
        W->>I: Post error message
    end
```

### 3. Repository Configuration

Once a valid upstream repository is provided, the following setup process begins:

```mermaid
graph TD
    A[Configure Git] -->|Set user & email| B[Configure Upstream]
    B -->|Add remote & fetch| C[Setup Branches]
    C -->|Create & configure| D[Store URL]
    D -->|Set repository secret| E[Configure Protection]
    E -->|Apply branch protection rules| F[Create Cleanup PR]

    subgraph "Branch Setup"
    C1[fork_upstream] -->|Track upstream changes| C2[fork_integration]
    C2 -->|Initialize workflows| C3[main]
    end
```

### 4. Branch Protection Rules

The workflow enforces branch protection policies on main, fork_upstream, and fork_integration:

- Requires status checks (validation workflow must pass before merging).

- Enforces linear commit history (no forced push or branch deletions).

- Enables conversation resolution for PR discussions.

### 5. Cleanup Process

Once the repository is initialized, the workflow automatically generates a cleanup PR to remove unnecessary initialization files:

- Deletes ISSUE_TEMPLATE/ directory.

- Removes PULL_REQUEST_TEMPLATE/init-pr.md.

- Deletes .github/workflows/init.yml.

- Cleans up editor-specific files.

The cleanup PR closes the initialization issue upon merging.

## Error Handling & Troubleshooting

The workflow includes robust error-handling mechanisms to ensure a smooth setup process.

```mermaid
flowchart LR
    A[Error Occurs] --> B{Error Type}
    B -->|Invalid Repo Format| C[Comment on Issue]
    B -->|Setup Error| D[Continue Execution]
    B -->|Branch Protection Error| E[Retry with Elevated Permissions]
    
    C --> F[Allow User to Retry]
    D --> G[Request Manual Intervention]
    E --> H[Reapply Protection Rules]
```

## Final Repository Structure

```mermaid
gitGraph
    commit id: "Initial Setup"
    branch fork_upstream
    checkout fork_upstream
    commit id: "Upstream Sync"
    branch fork_integration
    checkout fork_integration
    commit id: "Setup Workflows"
    checkout main
    merge fork_integration id: "Complete Initialization"
```