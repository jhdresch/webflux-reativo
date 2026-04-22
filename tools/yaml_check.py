import os
import sys
import subprocess
import traceback

EXCLUDE_DIRS = {'.git', 'target', 'node_modules', '.idea', '.vscode', 'data'}
REPORT_DIR = 'reports'


def ensure_pyyaml():
    try:
        import yaml  # noqa: F401
        return True
    except Exception:
        print('PyYAML not found, attempting to install via pip...')
        try:
            subprocess.check_call([sys.executable, '-m', 'pip', 'install', 'pyyaml'])
            import yaml  # try again
            return True
        except Exception as e:
            print('Failed to install PyYAML:', e)
            return False


def check_file(path):
    errors = []
    warnings = []
    try:
        with open(path, 'r', encoding='utf-8', errors='replace') as f:
            text = f.read()
    except Exception as e:
        errors.append(f'Could not read file: {e}')
        return errors, warnings

    # Warning for tabs
    for i, line in enumerate(text.splitlines(), start=1):
        if '\t' in line:
            warnings.append(f'Tab character found at line {i}')
            break

    # Try parsing with PyYAML
    try:
        import yaml
        # Use safe_load_all to support multi-document files.
        docs = list(yaml.safe_load_all(text))
        # If docs is empty (empty file), that's allowed; no error.
    except Exception as e:
        # Provide traceback snippet
        tb = traceback.format_exc()
        errors.append(f'YAML parse error: {e}\n{tb}')

    return errors, warnings


def main():
    root = os.getcwd()
    print(f'Checking YAML files under: {root}')

    if not ensure_pyyaml():
        print('PyYAML is required but could not be installed. Exiting.')
        sys.exit(3)

    yaml_files = []
    for dirpath, dirnames, filenames in os.walk(root):
        # Modify dirnames in-place to skip excluded directories
        dirnames[:] = [d for d in dirnames if d not in EXCLUDE_DIRS]
        for fn in filenames:
            if fn.lower().endswith(('.yml', '.yaml')):
                full = os.path.join(dirpath, fn)
                yaml_files.append(full)

    if not yaml_files:
        print('No YAML files found.')
        return

    total = 0
    ok = 0
    has_warn = 0
    has_err = 0
    details = []

    for path in sorted(yaml_files):
        total += 1
        rel = os.path.relpath(path, root)
        errors, warnings = check_file(path)
        if errors:
            has_err += 1
            details.append({'file': rel, 'status': 'ERROR', 'errors': errors, 'warnings': warnings})
            print(f'[ERROR] {rel} -> {len(errors)} error(s), {len(warnings)} warning(s)')
        elif warnings:
            has_warn += 1
            details.append({'file': rel, 'status': 'WARN', 'errors': [], 'warnings': warnings})
            ok += 1
            print(f'[WARN ] {rel} -> {len(warnings)} warning(s)')
        else:
            ok += 1
            details.append({'file': rel, 'status': 'OK', 'errors': [], 'warnings': []})
            print(f'[OK   ] {rel}')

    # Ensure report directory
    os.makedirs(REPORT_DIR, exist_ok=True)
    report_path = os.path.join(REPORT_DIR, 'yaml_report.txt')
    with open(report_path, 'w', encoding='utf-8') as rf:
        rf.write(f'YAML Check Report\nRoot: {root}\n\n')
        rf.write(f'Total files: {total}\nOK: {ok}\nWith warnings: {has_warn}\nWith errors: {has_err}\n\n')
        for d in details:
            rf.write(f"- {d['status']}: {d['file']}\n")
            for w in d['warnings']:
                rf.write(f"    WARN: {w}\n")
            for e in d['errors']:
                # truncate long tracebacks to keep report readable
                rf.write(f"    ERROR: {e[:1000]}\n")
            rf.write('\n')

    print('\nSummary:')
    print(f'  Total files: {total}')
    print(f'  OK: {ok}')
    print(f'  With warnings: {has_warn}')
    print(f'  With errors: {has_err}')
    print(f'Full report written to: {report_path}')

    if has_err:
        sys.exit(2)


if __name__ == '__main__':
    main()

