#!/usr/bin/env python3
"""
kvolegi arcade - Automated Test Runner
Compiles domain core and executes Headless Test Suite verifying real game engines.
"""
import subprocess
import sys
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TARGET_DIR = os.path.join(ROOT, "target", "test-classes")
CORE_DIR = os.path.join(ROOT, "source", "core")
TESTS_DIR = os.path.join(ROOT, "tests")
TEST_JAVA = os.path.join(TESTS_DIR, "ArcadeTestSuite.java")

def main():
    import shutil
    mvn_path = shutil.which("mvn")
    if mvn_path:
        print("Executing kvolegi arcade Complete Automated Test Suite (75 Tests across 6 suites)...")
        res = subprocess.run([mvn_path, "test"], cwd=ROOT)
        sys.exit(res.returncode)

    os.makedirs(TARGET_DIR, exist_ok=True)
    print("Compiling arcade core and test suite...")
    compile_cmd = [
        "javac",
        "-d", TARGET_DIR,
        "-sourcepath", f"{CORE_DIR}:{TESTS_DIR}",
        TEST_JAVA
    ]
    res_compile = subprocess.run(compile_cmd, cwd=ROOT)
    if res_compile.returncode != 0:
        print("Compilation failed.")
        sys.exit(res_compile.returncode)

    print("Executing kvolegi arcade Automated Test Suite...")
    run_cmd = ["java", "-cp", TARGET_DIR, "tests.ArcadeTestSuite"]
    res_run = subprocess.run(run_cmd, cwd=ROOT)
    sys.exit(res_run.returncode)

if __name__ == "__main__":
    main()
