# Git workflow

1. `git init` in the repository root; first commit is the Phase 0 baseline.
2. One branch per phase or feature: `feature/phase-1-auth`.
3. Commit using Conventional Commits, e.g. `feat(auth): add JWT login endpoint`.
4. Merge into `main` when the phase is complete and checks pass; tag it: `git tag phase-1-done`.
5. Never commit `.env`, secrets, `node_modules/` or `target/`.
