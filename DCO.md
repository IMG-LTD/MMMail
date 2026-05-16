# Developer Certificate of Origin

MMMail uses the Developer Certificate of Origin (DCO) instead of a Contributor License Agreement.

Every commit in a pull request must include a `Signed-off-by` trailer:

```text
Signed-off-by: Name <email@example.com>
```

By signing off, you certify that:

- you created the contribution, or received it from a source that allows you to submit it;
- you have the right to submit the contribution under this repository's Apache 2.0 license;
- the contribution can be redistributed as part of MMMail;
- the sign-off name and email are intentionally recorded in the public project history.

Use Git's built-in sign-off support:

```bash
git commit -s -m "fix: describe the change"
```

For an existing local commit:

```bash
git commit --amend -s --no-edit
```

For a local branch with multiple unsigned commits, use an interactive rebase or:

```bash
git rebase --signoff <base-ref>
```

The DCO check is enforced by `.github/workflows/dco.yml`. See `CONTRIBUTING.md` for the wider PR and validation rules.
