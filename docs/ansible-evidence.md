# Ansible Evidence Workflow

This workflow creates auditable evidence for Ansible operations by producing logs, idempotence re-run results, and CI-archivable artifacts.

## What Gets Produced

Each execution writes a timestamped folder under `artifacts/ansible/<timestamp>/` containing:

- `run-1.log`: first playbook execution output
- `run-2.log`: second execution output for idempotence verification
- `summary.json`: machine-readable status (success/failure, changed/failed/unreachable counters)
- `report.md`: human-readable report for audit evidence

## Windows

```powershell
.\scripts\run-ansible-evidence.ps1 `
  -Inventory ansible/inventory/hosts.yml `
  -Playbook ansible/playbooks/deploy-all.yml `
  -Environment staging
```

## Unix

```bash
./scripts/run-ansible-evidence.sh
```

## Acceptance Rules

- Run 1 must complete with no unreachable and no failed hosts.
- Run 2 (re-run) must complete with no failed/unreachable and `changed=0` to pass idempotence.
- Script returns non-zero exit code when overall evidence fails.

## CI Archival

Jenkins archives all generated evidence under `artifacts/ansible/**/*`.

This converts deployment operations from non-verifiable claims to traceable artifacts.