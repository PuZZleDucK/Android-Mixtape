# Android Mixtape

Android Mixtape turns the music stored on your device into a case full of virtual mixtapes. It groups local tracks into tape-sized collections, gives each one a memorable name, and presents playback through customizable cassette decks, cases, sleeves, labels, stickers, and handwritten track lists.

Everything runs locally. The app reads audio through Android's media library and does not upload your music or require an online account.

The modern build targets Android 16. Returning from system settings refreshes audio permission; revoking access clears the inaccessible library and playback queue.

## Highlights

- Automatically groups local music into mixtapes.
- Chooses names from a bundled collection of 1,714 mixtape names, with manual editing and rerolling available.
- Provides multiple deck, cassette, screw, sticker, case, sleeve, label, and handwriting styles.
- Keeps each mixtape's name and visual design stable between launches.
- Offers configurable handwriting messiness: **High**, **Low**, or **Off**.
- Uses **Blackout Portable** as the initial deck on devices in dark mode and **Silverface Hi-Fi** in light mode.
- Includes playback controls, seeking, track selection, and portrait and landscape layouts.
- Supports Android Auto through the modern build.
- Keeps emoji and punctuation intact in handwritten labels.

## Compatibility

Two app variants are available:

- **Modern:** Android 7.0 or newer, with the full Compose interface, Media3 playback, and Android Auto support.
- **Legacy:** Android 4.4 or newer, with a simpler classic Android interface and platform media playback.

## Getting started

1. Install the APK appropriate for your Android version.
2. Open **Mixtape** and grant access to audio files when prompted.
3. The app scans music already indexed by Android and creates your mixtapes.
4. Open a tape to view its tracks and use the deck controls.
5. Open **Settings** to change grouping, handwriting, names, visual themes, and filename exclusions.

If no music appears, place supported audio files in the device's Music folder and ask Android or your media app to rescan them.

## Privacy

Android Mixtape operates on local media. It does not contain an AI model, use an online naming service, or send track information to a remote service.

## License

Android Mixtape is available under the [MIT License](LICENSE).

## Screenshots

### Mixtape case

![Mixtape case and tape list](docs/screenshots/tape-list.png)

### Portrait player

![Cassette player in portrait orientation](docs/screenshots/player-portrait.png)

### Landscape player

![Cassette player in landscape orientation](docs/screenshots/player-landscape.png)
