#!/usr/bin/env python3
"""
kvolegi arcade - Automated Test Runner
Copyright (c) 2023-2024 kvolegi (@kvolegi). All Rights Reserved.
"""
import subprocess
import sys
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEST_JAVA = os.path.join(ROOT, "tests", "ArcadeTestSuite.java")

def main():
    print("Executing kvolegi arcade Automated Test Suite...")
    res = subprocess.run(["java", TEST_JAVA], cwd=ROOT)
    sys.exit(res.returncode)

if __name__ == "__main__":
    main()
