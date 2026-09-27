# Handwriting font size

Added Small (70%), Medium (85%) and Large (100%, unchanged default) to Settings > Appearance and the Handwriting fonts page. The preference is saved with the existing mixtape settings; missing or unknown saved values use Large. It scales handwritten labels, previews and track text, not standard settings/control labels. Track ink bounds, width measurement and scrolling estimates use the same scaled size.

Verification on Kunlun API 24:
- 285 unit tests passed.
- Modern debug app and instrumentation APKs built successfully.
- Two connected tests passed for actual preference persistence/defaults and both settings controls. Production Now Playing row heights scale to 70% and 85% of Large.
- All eight handwriting fonts passed the highlight alignment check at each of the three sizes with High messiness: 24 font/size combinations.
- Screenshots are in `font-size/`; test logs and measurements accompany this file.
- Installed and launched the updated app through `kunlun-sync.sh` using the current APK under `app/build/intermediates/apk/modern/debug/`.
- Stopped the temporary emulator and ADB tunnel after testing. No phone access, commit or push.
