#!/usr/bin/env python3
"""
kvolegi arcade - Automated Universal Multiplatform Build Script
Copyright (c) 2023-2024 kvolegi (@kvolegi). All Rights Reserved.

Builds:
1. macOS App Bundle:    builds/mac/kvolegi arcade.app & kvolegi-arcade-mac.jar
2. Windows Executable:  builds/windows/kvolegi-arcade.exe & kvolegi-arcade-windows.jar
3. Android Release APK: builds/android/kvolegi-arcade.apk
"""

import os
import sys
import shutil
import subprocess
import urllib.request
from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
CORE_SRC = os.path.join(ROOT, "source/core")
DESKTOP_SRC = os.path.join(ROOT, "source/desktop")
ANDROID_SRC = os.path.join(ROOT, "source/android/src/main")
ASSETS_SRC = os.path.join(ROOT, "source/assets")
ICONS_SRC = os.path.join(ROOT, "icons")

BUILDS_DIR = os.path.join(ROOT, "builds")
MAC_BUILD = os.path.join(BUILDS_DIR, "mac")
WIN_BUILD = os.path.join(BUILDS_DIR, "windows")
APK_BUILD = os.path.join(BUILDS_DIR, "android")

def build_desktop():
    print("=" * 60)
    print("BUILDING DESKTOP ARTIFACTS (macOS & Windows)...")
    print("=" * 60)

    jfx_mac_jars = [
        os.path.expanduser("~/.m2/repository/org/openjfx/javafx-base/21.0.4/javafx-base-21.0.4-mac.jar"),
        os.path.expanduser("~/.m2/repository/org/openjfx/javafx-graphics/21.0.4/javafx-graphics-21.0.4-mac.jar"),
        os.path.expanduser("~/.m2/repository/org/openjfx/javafx-controls/21.0.4/javafx-controls-21.0.4-mac.jar")
    ]
    jfx_cp = ":".join(jfx_mac_jars)

    build_tmp = "/tmp/arcade_desktop_build_auto"
    if os.path.exists(build_tmp):
        shutil.rmtree(build_tmp)
    os.makedirs(build_tmp)

    sources = []
    for d in [CORE_SRC, DESKTOP_SRC]:
        for r, _, files in os.walk(d):
            for f in files:
                if f.endswith(".java"):
                    sources.append(os.path.join(r, f))

    sources_file = "/tmp/arcade_sources_auto.txt"
    with open(sources_file, "w") as f:
        for s in sources:
            f.write(f'"{s}"\n')

    print(f"Compiling {len(sources)} Desktop Java sources...")
    subprocess.run(["javac", "-cp", jfx_cp, "@" + sources_file, "-d", build_tmp], check=True)
    print("Desktop compilation successful.")

    # 1. macOS Build
    os.makedirs(MAC_BUILD, exist_ok=True)
    mac_staging = "/tmp/arcade_mac_staging_auto"
    if os.path.exists(mac_staging):
        shutil.rmtree(mac_staging)
    os.makedirs(mac_staging)
    classes_mac = os.path.join(mac_staging, "classes")
    shutil.copytree(build_tmp, classes_mac)

    for item in os.listdir(ASSETS_SRC):
        s = os.path.join(ASSETS_SRC, item)
        d = os.path.join(classes_mac, item)
        if os.path.isdir(s):
            shutil.copytree(s, d, dirs_exist_ok=True)
        else:
            shutil.copy2(s, d)

    for jar in jfx_mac_jars:
        subprocess.run(["jar", "xf", jar], cwd=classes_mac, check=True)

    mi = os.path.join(classes_mac, "module-info.class")
    if os.path.exists(mi):
        os.remove(mi)

    manifest_mac = os.path.join(mac_staging, "MANIFEST.MF")
    with open(manifest_mac, "w") as f:
        f.write("Manifest-Version: 1.0\nMain-Class: arcade.desktop.Launcher\n")

    jar_mac = os.path.join(MAC_BUILD, "kvolegi-arcade-mac.jar")
    subprocess.run(["jar", "cfm", jar_mac, manifest_mac, "-C", classes_mac, "."], check=True)
    print(f"Created macOS JAR: {jar_mac} ({os.path.getsize(jar_mac):,} bytes)")

    # Rebuild .app bundle
    app_res = os.path.join(MAC_BUILD, "kvolegi arcade.app/Contents/Resources")
    os.makedirs(app_res, exist_ok=True)
    shutil.copy2(jar_mac, os.path.join(app_res, "kvolegi-arcade-mac.jar"))
    print("Updated kvolegi arcade.app bundle.")

    # 2. Windows Build
    os.makedirs(WIN_BUILD, exist_ok=True)
    win_staging = "/tmp/arcade_win_staging_auto"
    if os.path.exists(win_staging):
        shutil.rmtree(win_staging)
    os.makedirs(win_staging)
    classes_win = os.path.join(win_staging, "classes")
    shutil.copytree(build_tmp, classes_win)

    for item in os.listdir(ASSETS_SRC):
        s = os.path.join(ASSETS_SRC, item)
        d = os.path.join(classes_win, item)
        if os.path.isdir(s):
            shutil.copytree(s, d, dirs_exist_ok=True)
        else:
            shutil.copy2(s, d)

    win_jars = [
        ("javafx-base-21.0.2-win.jar", "https://repo1.maven.org/maven2/org/openjfx/javafx-base/21.0.2/javafx-base-21.0.2-win.jar"),
        ("javafx-graphics-21.0.2-win.jar", "https://repo1.maven.org/maven2/org/openjfx/javafx-graphics/21.0.2/javafx-graphics-21.0.2-win.jar"),
        ("javafx-controls-21.0.2-win.jar", "https://repo1.maven.org/maven2/org/openjfx/javafx-controls/21.0.2/javafx-controls-21.0.2-win.jar")
    ]
    cache_dir = "/tmp/jfx_win_cache"
    os.makedirs(cache_dir, exist_ok=True)
    for name, url in win_jars:
        cached_path = os.path.join(cache_dir, name)
        if not os.path.exists(cached_path):
            urllib.request.urlretrieve(url, cached_path)
        subprocess.run(["jar", "xf", cached_path], cwd=classes_win, check=True)

    mi = os.path.join(classes_win, "module-info.class")
    if os.path.exists(mi):
        os.remove(mi)

    manifest_win = os.path.join(win_staging, "MANIFEST.MF")
    with open(manifest_win, "w") as f:
        f.write("Manifest-Version: 1.0\nMain-Class: arcade.desktop.Launcher\n")

    jar_win = os.path.join(WIN_BUILD, "kvolegi-arcade-windows.jar")
    subprocess.run(["jar", "cfm", jar_win, manifest_win, "-C", classes_win, "."], check=True)
    print(f"Created Windows JAR: {jar_win} ({os.path.getsize(jar_win):,} bytes)")

    # 3. Windows Native PE32 .exe via Launch4j
    exe_out = os.path.join(WIN_BUILD, "kvolegi-arcade.exe")
    icon_path = "/tmp/app_icon.ico"
    if not os.path.exists(icon_path):
        img = Image.open(os.path.join(ICONS_SRC, "app_icon_256.png"))
        img.save(icon_path, format="ICO", sizes=[(16,16),(32,32),(48,48),(64,64),(128,128),(256,256)])

    launch4jc = "/tmp/launch4j_dir/launch4j/launch4jc"
    if os.path.exists(launch4jc):
        config_xml = f"""<launch4jConfig>
  <dontWrapJar>false</dontWrapJar>
  <headerType>gui</headerType>
  <jar>{jar_win}</jar>
  <outfile>{exe_out}</outfile>
  <errTitle>kvolegi arcade</errTitle>
  <cmdLine></cmdLine>
  <chdir>.</chdir>
  <priority>normal</priority>
  <downloadUrl>https://adoptium.net/</downloadUrl>
  <supportUrl></supportUrl>
  <stayAlive>false</stayAlive>
  <restartOnCrash>false</restartOnCrash>
  <icon>{icon_path}</icon>
  <classPath>
    <mainClass>arcade.desktop.Launcher</mainClass>
  </classPath>
  <jre>
    <path>%JAVA_HOME%;%PATH%</path>
    <bundledJre64Bit>false</bundledJre64Bit>
    <bundledJreAsFallback>false</bundledJreAsFallback>
    <minVersion>17</minVersion>
    <maxVersion></maxVersion>
    <jdkPreference>preferJre</jdkPreference>
    <runtimeBits>64/32</runtimeBits>
  </jre>
  <versionInfo>
    <fileVersion>1.0.0.0</fileVersion>
    <txtFileVersion>1.0.0.0</txtFileVersion>
    <fileDescription>kvolegi arcade</fileDescription>
    <copyright>kvolegi arcade</copyright>
    <productVersion>1.0.0.0</productVersion>
    <txtProductVersion>1.0.0.0</txtProductVersion>
    <productName>kvolegi arcade</productName>
    <companyName>kvolegi</companyName>
    <internalName>kvolegi-arcade</internalName>
    <originalFilename>kvolegi-arcade.exe</originalFilename>
  </versionInfo>
</launch4jConfig>
"""
        cfg_path = "/tmp/l4j_auto.xml"
        with open(cfg_path, "w") as f:
            f.write(config_xml)
        subprocess.run([launch4jc, cfg_path], check=True)
        print(f"Created Windows Native EXE: {exe_out} ({os.path.getsize(exe_out):,} bytes)")

def build_android():
    print("=" * 60)
    print("BUILDING ANDROID APK...")
    print("=" * 60)
    sdk_dir = os.path.expanduser("~/Library/Android/sdk")
    build_tools = os.path.join(sdk_dir, "build-tools/35.0.0")
    aapt2 = os.path.join(build_tools, "aapt2")
    d8 = os.path.join(build_tools, "d8")
    zipalign = os.path.join(build_tools, "zipalign")
    apksigner = os.path.join(build_tools, "apksigner")
    android_jar = os.path.join(sdk_dir, "platforms/android-35/android.jar")

    tmp_build = "/tmp/android_build_auto"
    if os.path.exists(tmp_build):
        shutil.rmtree(tmp_build)
    os.makedirs(tmp_build)

    compiled_res = os.path.join(tmp_build, "compiled_res.zip")
    subprocess.run([aapt2, "compile", "--dir", os.path.join(ANDROID_SRC, "res"), "-o", compiled_res], check=True)

    unaligned_apk = os.path.join(tmp_build, "unaligned.apk")
    gen_r = os.path.join(tmp_build, "gen")
    os.makedirs(gen_r, exist_ok=True)
    subprocess.run([
        aapt2, "link",
        "-I", android_jar,
        "--manifest", os.path.join(ANDROID_SRC, "AndroidManifest.xml"),
        "-o", unaligned_apk,
        "-A", ASSETS_SRC,
        "--java", gen_r,
        "--auto-add-overlay",
        compiled_res
    ], check=True)

    classes_dir = os.path.join(tmp_build, "classes")
    os.makedirs(classes_dir, exist_ok=True)
    java_files = []
    for d in [CORE_SRC, os.path.join(ANDROID_SRC, "java"), gen_r]:
        for r, _, files in os.walk(d):
            for f in files:
                if f.endswith(".java"):
                    java_files.append(os.path.join(r, f))

    with open(os.path.join(tmp_build, "sources.txt"), "w") as f:
        for jf in java_files:
            f.write(f'"{jf}"\n')

    subprocess.run([
        "javac", "--release", "17", "-cp", android_jar,
        "@" + os.path.join(tmp_build, "sources.txt"),
        "-d", classes_dir
    ], check=True)

    dex_dir = os.path.join(tmp_build, "dex")
    os.makedirs(dex_dir, exist_ok=True)
    class_files = []
    for r, _, files in os.walk(classes_dir):
        for f in files:
            if f.endswith(".class"):
                class_files.append(os.path.join(r, f))

    subprocess.run([d8, "--output", dex_dir] + class_files, check=True)
    subprocess.run(["zip", "-j", "-u", unaligned_apk, os.path.join(dex_dir, "classes.dex")], check=True)

    aligned_apk = os.path.join(tmp_build, "aligned.apk")
    subprocess.run([zipalign, "-v", "-p", "4", unaligned_apk, aligned_apk], check=True)

    keystore = os.path.join(tmp_build, "debug.keystore")
    subprocess.run([
        "keytool", "-genkeypair", "-v",
        "-keystore", keystore, "-storepass", "android",
        "-alias", "androiddebugkey", "-keypass", "android",
        "-keyalg", "RSA", "-keysize", "2048", "-validity", "10000",
        "-dname", "CN=Android Debug,O=Android,C=US"
    ], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, check=True)

    os.makedirs(APK_BUILD, exist_ok=True)
    final_apk = os.path.join(APK_BUILD, "kvolegi-arcade.apk")
    subprocess.run([
        apksigner, "sign",
        "--ks", keystore, "--ks-pass", "pass:android",
        "--key-pass", "pass:android", "--ks-key-alias", "androiddebugkey",
        "--out", final_apk, aligned_apk
    ], check=True)
    print(f"Created Android Release APK: {final_apk} ({os.path.getsize(final_apk):,} bytes)")

if __name__ == "__main__":
    build_desktop()
    if os.path.exists(os.path.expanduser("~/Library/Android/sdk")):
        build_android()
    print("\nALL BUILDS GENERATED SUCCESSFULLY!")
