# Google Play publishing checklist

Research checked against current Google Play and Android documentation on 2026-08-15.

## Publishing decisions and account setup

- [ ] Confirm that the modern app is the Play Store product; decide whether the legacy flavor remains sideload-only rather than becoming a separate listing.
- [ ] Choose the permanent production application ID before the first upload; replace `com.example.androidmixtape` with an owned package name because Play package names cannot be deleted or reused.
- [ ] Confirm the public app name (**30 characters maximum**), default language, app/game classification, free/paid status, and distribution countries.
- [ ] Choose the **Music & Audio** category and appropriate Play tags.
- [ ] Prepare the required public support email and optional support website and phone number.
- [ ] Complete Play Console developer identity, contact, and device verification requirements.
- [ ] Accept the Developer Program Policies, US export-law declaration, Developer Distribution Agreement, and Play App Signing terms.
- [ ] If using a personal developer account created after 2023-11-13, plan a closed test with at least **12 opted-in testers continuously for 14 days**, then apply for production access.

## Production identity, signing, and bundle

- [ ] Create and securely back up a production upload keystore, alias, passwords, and recovery information; do not commit secrets.
- [ ] Configure a non-debuggable release build signed with the upload key.
- [ ] Enroll in Play App Signing and securely retain the upload key separately from Google's app-signing key.
- [ ] Build and upload a signed **Android App Bundle (`.aab`)**, not a debug APK; new Play apps have required App Bundles since August 2021.
- [ ] Test APKs generated from the bundle with `bundletool` or Play Console internal app sharing before release.
- [ ] Set a unique, increasing `versionCode` and a user-facing `versionName`.
- [ ] Generate and archive checksums and release notes for the exact submitted bundle.

## Android and Play technical requirements

- [ ] Upgrade `compileSdk` and `targetSdk` from 35 to **API 36 / Android 16** before submission. API 36 becomes mandatory for new phone apps and updates on **2026-08-31**; API 35 is only sufficient before that date.
- [ ] Run the Android 16 behavior-change and compatibility checks after raising the target SDK.
- [ ] Verify all bundled native libraries support **16 KB memory pages**, required for Play submissions targeting Android 15 or newer.
- [ ] Verify required 64-bit ABI support and remove unused native ABIs/libraries from the release bundle.
- [ ] Audit release dependencies and ensure debug/test tooling is not packaged in production.
- [ ] Review R8/resource shrinking, keep rules, startup behavior, and release crash reporting strategy.
- [ ] Confirm the modern minimum Android version and supported device catalog are intentional.
- [ ] Verify permissions on supported Android versions, especially `READ_MEDIA_AUDIO` and legacy storage access, and prepare any Play permission declarations that the Console requests.
- [ ] Run unit, lint, release-build, bundle, accessibility, rotation, playback/background, and Android Auto tests.
- [ ] Upload to internal testing and review Play pre-launch reports for crashes, ANRs, compatibility, security, and accessibility issues.
- [ ] Resolve Play SDK Index warnings and confirm every third-party SDK complies with current Play policy.

## Store listing text

- [ ] Finalize the app title (**30 characters maximum**).
- [ ] Write a plain-language short description (**80 characters maximum**; no emojis, repeated punctuation, rankings, pricing claims, or calls to action).
- [ ] Write the full description (**4,000 characters maximum**) covering local-library scanning, mixtape grouping, playback, customization, privacy, Android Auto, and supported Android versions.
- [ ] Proofread metadata against the actual release; remove claims that are not visible or functional in the submitted build.
- [ ] Prepare localized title, short description, full description, graphics, and support/privacy pages for every supported listing language, or intentionally publish only the default language initially.
- [ ] Prepare concise release notes for the production rollout and each testing track.

## Icons, graphics, and video

- [ ] Produce the Play Store app icon as a **512 × 512**, 32-bit PNG with alpha, no larger than **1,024 KB**; verify it follows Play icon specifications and matches the launcher/adaptive icon.
- [ ] Produce the required feature graphic as **1024 × 500**, JPEG or 24-bit PNG with no alpha.
- [ ] Keep feature-graphic focal content near the center and out of crop/overlay zones; avoid store badges, device frames, rankings, prices, and time-sensitive claims.
- [ ] Write alt text of **140 characters or fewer** for the icon/feature artwork where Play Console offers it.
- [ ] Decide whether to create an optional YouTube preview video; if used, make it public or unlisted, ad-free, not age-restricted, and representative of the app.
- [ ] Do not prepare a TV banner unless Android TV distribution is intentionally added; Android TV requires a separate **1280 × 720** banner and TV screenshot.

## Play Store screenshots

- [ ] Prepare at least **2 phone screenshots** and preferably **4–8** strong screenshots showing real app use.
- [ ] Export screenshots as JPEG or 24-bit PNG with no alpha, each between **320 px and 3840 px**; the longest dimension must not exceed twice the shortest.
- [ ] For recommendation eligibility, provide at least **4 screenshots at 1080 px or higher**, using 9:16 portrait (minimum 1080 × 1920) or 16:9 landscape (minimum 1920 × 1080).
- [ ] Show the tape case/list, portrait player, landscape player, theme customization, handwriting options, and Android Auto where appropriate.
- [ ] Use clean status bars with no personal notifications, carrier names, or unrelated branding; do not use device frames or misleading overlays.
- [ ] Ensure screenshots use only owned or licensed artwork and audio metadata.
- [ ] Write useful screenshot alt text of **140 characters or fewer**.
- [ ] If distributing directly to Android Automotive OS, prepare generic-system screenshots: at least 2 portrait **800 × 1280** and 2 landscape **1024 × 768**, with no vehicle/OEM-specific UI.
- [ ] **Update Git screenshots. Different tapes and decks for screenshots. Mock tracks.**

## Privacy, policy, and app-content declarations

- [ ] Publish a stable HTTPS privacy-policy page and link it in Play Console and from within the app.
- [ ] Complete the Data safety form accurately, including all libraries/SDKs; even an app that collects or shares no user data must submit the form and provide a privacy-policy link.
- [ ] Document that local audio titles, artists, filenames, and playback state remain on-device, then verify this against network and dependency behavior.
- [ ] Complete the ads declaration.
- [ ] Complete the target-audience and content questionnaire; decide whether children are in the target audience and comply with Families policy if applicable.
- [ ] Complete the IARC content-rating questionnaire.
- [ ] Complete app-access instructions; declare that no login is required, or provide durable review credentials if that changes.
- [ ] Complete declarations for account creation/deletion, news, health, financial features, government affiliation, or other special categories as applicable; mark them not applicable only after review.
- [ ] Review metadata, intellectual-property, impersonation, repetitive-content, and user-data policies.
- [ ] Confirm licenses and redistribution rights for fonts, icons, artwork, the bundled name list, mock audio, and any other shipped assets; retain attribution/source records.
- [ ] Review Android Auto/Car App Library quality rules and Play Console car declarations before enabling car distribution.

## Final release preparation

- [ ] Profile and optimize Now Playing track-list scrolling on a representative physical device with a 100+ track tape; resolve sustained >10% janky frames or >32 ms 95th-percentile frame time. Emulator evidence: `docs/evidence/card3843-kunlun-gfxinfo-after.txt`.
- [ ] Test a clean install from the Play-generated package with no previous preferences or media permissions.
- [ ] Test upgrade behavior from the last distributed build without clearing user data.
- [ ] Verify the store listing, privacy policy, Data safety answers, content rating, screenshots, and submitted bundle all describe the same release.
- [ ] Review the device catalog and exclude only devices with a documented incompatibility.
- [ ] Complete internal and closed testing, collect tester feedback, and resolve release-blocking issues.
- [ ] Choose standard or managed publishing and decide whether to use a staged production rollout.
- [ ] Submit for review with enough lead time for policy questions or rejection fixes.
- [ ] After approval, monitor Android vitals, crashes, ANRs, reviews, policy notices, and pre-launch reports.

## Official references

- [Create and set up your app](https://support.google.com/googleplay/android-developer/answer/9859152)
- [Target API level requirements](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Add preview assets to showcase your app](https://support.google.com/googleplay/android-developer/answer/9866151)
- [Android App Bundles](https://developer.android.com/guide/app-bundle)
- [Data safety requirements](https://support.google.com/googleplay/android-developer/answer/10787469)
- [Testing requirements for new personal developer accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
- [Support 16 KB page sizes](https://developer.android.com/guide/practices/page-sizes)
