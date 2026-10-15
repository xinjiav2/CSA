# Make build repair for Debian WSL

Branch: `fix/microblog-rate-limit` in `pages` and `CSA`.

## Cause

The workspace path contains a space: `/mnt/c/Users/nishi/CSA Folder/pages`.
The shared project Makefile used `$(notdir $(CURDIR))`, which treats a path as a
whitespace-separated list. It produced `CSA astronaut-platformer-game` instead
of `astronaut-platformer-game`. Unquoted destination paths then created stray
directories and copied assets into the source project. The notebook converter's
absolute Python executable path was also unquoted, causing another build failure.

After repairing those paths, Debian's virtual environment reported missing
`yaml`. Installed the repository's `requirements.txt` into `venv`. Debian also
needed its own native Ruby gems instead of the Windows bundle from the earlier
verification session.

## Changes

- `_projects/_template/Makefile` derives the project name with quoted shell
  `basename`, quotes executable and file paths, and resolves individual notebook
  paths before changing directories. The repository generator refreshes existing
  untracked project Makefiles from this template.
- The root Makefile applies `_config.local.yml` to Jekyll serving, building, and
  watched rebuilds when present. It checks whether the gem bundle is usable even
  when an installation marker already exists.
- Startup allows 900 seconds by default (`STARTUP_TIMEOUT` can override it),
  because a first full build on WSL's mounted Windows filesystem exceeded the
  previous 300-second limit. Jekyll starts with `nohup` so closing its launching
  shell does not interrupt rendering with SIGHUP.
- Local `.bundle/config` now selects `vendor/bundle-debian`. Windows gems remain
  in `vendor/bundle`; to use them from PowerShell, explicitly set
  `$env:BUNDLE_PATH='vendor/bundle'` for that session.
- Seventeen misplaced files were verified against their original project assets
  by SHA-256 and moved to `vendor/make-output-backup/astronaut-platformer-game`.
  The original source assets are intact.

## Running in Debian

```bash
cd "/mnt/c/Users/nishi/CSA Folder/pages"
make
```

Use `make serve-current PORT=4501` to run the build and server without launching
the default background watchers. The cached theme in `_config.local.yml` is used
automatically. Normal generation creates assets, layouts, converted posts, and
project Makefiles; those outputs are intentionally ignored by Git. Fix source
files and the shared template rather than editing generated copies.

## Regression test

```bash
python3 -m unittest discover -s tests -p test_project_makefile.py -v
```

The test runs actual Make commands for flat and nested projects in temporary
workspaces whose names contain spaces. It verifies asset destinations, navigation,
documentation, converter arguments, single-notebook filenames containing spaces,
absence of stray output, and that an unchanged index is not rewritten.
It also verifies startup timeout failure and success with an existing ready log.

## Verification results

- Both regression tests passed in Debian WSL (flat/nested project fixtures and
  server readiness/timeout behavior).
- `make serve-current PORT=4501` completed successfully, including registered
  project builds, notebook conversion, course splitting, documentation publication,
  SASS import generation, and Jekyll startup.
- Jekyll reported `done in 585.814 seconds`; Make observed readiness after
  625 seconds, within the new configurable 900-second limit.
- `http://localhost:4501/microblog` and the limiter module both returned HTTP 200;
  the served page contains the microblog form and the served limiter has the
  expected 30-second interval.
- Existing notebook missing-cell-ID and Jekyll `post_url` deprecation warnings
  remain nonfatal. `git diff --check` passes.
- The default `make` target adds background watchers after the same tested
  `serve-current` pipeline; those extra watchers were not started for verification.
