# Linux activation scripts: security remediation

Review and remediation date: 2026-10-29.

## Finding

The Ubuntu and Kali activation scripts previously ran `chmod -R 777 /opt/gems`
and prepended `/opt/gems/bin` to `PATH`. Any local user could replace an installed
executable, such as `bundle` or `jekyll`, and run code with another account's
permissions when that account invoked it. The review rated this HIGH severity
with confidence 9/10.

## Changes

Both `scripts/activate_ubuntu.sh` and `scripts/activate_kali.sh` now:

- Refuse execution as root and reserve `sudo` for APT commands.
- Export `GEM_HOME="$HOME/.local/gems"` and prepend its `bin` directory to `PATH`
  before installation and version checks.
- Create the gem directory as the current user and set its mode to `700`,
  preventing other accounts from accessing its contents even with a permissive
  installation umask. Existing per-user gem directories get the same mode.
- Install gems as the current user, without `sudo`.
- Remove every exact `/opt/gems/bin` component from the installer's inherited
  `PATH` and replace the two exact `.bashrc` exports written by older versions.
  New exports are added once, so rerunning the script preserves unrelated shell
  configuration and does not duplicate them.
- Use the explicitly exported environment for tool checks instead of sourcing
  `.bashrc`, which can return early in a noninteractive shell or override it.

## Existing installations

1. Run the updated script as each account that needs the tools. Gems are installed
   afresh in that account's private directory; nothing is copied from `/opt/gems`.
2. Check other shell startup files and any customized exports for `/opt/gems/bin`
   or `GEM_HOME=/opt/gems`, and remove those references. Automatic migration only
   removes the exact two lines emitted by the old scripts in `.bashrc`.
3. Start a fresh login session. The installer runs in a child process, so it cannot
   remove the old path from an already-open parent shell; sourcing `.bashrc` in
   that shell alone can leave the old path later in `PATH`.
4. Verify `command -v bundle` and `command -v jekyll` resolve under
   `$HOME/.local/gems/bin`, and `stat -c '%a %U' "$HOME/.local/gems"` shows mode
   `700` and your account as owner.

The scripts leave `/opt/gems` untouched because other accounts may still depend
on it. After all accounts migrate, an administrator should retire that shared
installation. Its existing executables should be treated as untrusted.

## Validation

Run from the repository root:

```bash
bash -n scripts/activate_ubuntu.sh scripts/activate_kali.sh
python3 -m unittest discover -s tests -p test_activation_scripts.py
```

Both checks passed. The regression test executes both scripts using temporary
home directories and mocked package/tool commands. It covers fresh installations,
legacy exports, repeated runs, spaces in home paths, existing permissive gem
directories, a `000` umask, duplicate inherited shared paths, unprivileged gem
installation, and the environment used by version checks. It skips when run as
root. Real APT and RubyGems installation on Ubuntu/Kali was not performed.
