<div align="center">

<img src="./assets/docs/Nocturne-github-image.png" alt="Nocturne Mobile Banner" width="100%">

# Nocturne Mobile

**A native Android YouTube Music client — built from scratch for Nocturne, designed for music on the go.**

<p align="center">
  <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white">
  <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
  <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white">
  <img src="https://img.shields.io/badge/Material_3-757575?style=for-the-badge&logo=materialdesign&logoColor=white">
</p>

**Nocturne Mobile** is a native Android music client built as the mobile counterpart to [Nocturne Desktop](https://github.com/Neo-XD/nocturne-music), while also working completely on its own.

Stream and manage your music, take your library with you, and enjoy a Nocturne experience designed specifically for Android.

</div>

---

## Features

* Built specifically for Android using Kotlin rather than wrapping a website.
* Search, browse, and play all your favourite songs directly from YouTube Music.
* Ever get annoyed by the constant ads? Enjoy Ad-Free Listening on Nocturne Mobile!
* Don't let the beats stop with background playback.
* Access all your playlists, liked songs, artists, and albums directly from YouTube Music 
* Sing along with your music with **Synced Lyrics**
* Scrobble your songs with last.fm integration.
* Part of the Nocturne Ecosystem and is designed to work alongside [Nocturne Desktop](https://github.com/Neo-XD/nocturne-music)

---

<h2 align="center">Download</h2>

<p align="center">
  <a href="https://github.com/Neo-XD/nocturne-mobile/releases/latest">
    <img src="https://img.shields.io/badge/GitHub_Releases-100000?style=for-the-badge&logo=github&logoColor=white" height="40">
  </a>
</p>

Download the latest APK from the [Releases](https://github.com/Neo-XD/nocturne-mobile/releases/latest) page and install it on your Android device.

> Nocturne Mobile is currently under active development. Expect occasional bugs and unfinished features.

---

## Part of the Nocturne Ecosystem

Nocturne Mobile was built alongside **[Nocturne Desktop](https://github.com/Neo-XD/nocturne-music)**, the desktop version of Nocturne available for Windows, Mac and Linux.

The two applications share the same goal:

> A fast, native, customizable YouTube Music experience without the bloat of **Electron**.

| Platform | Project                                                    |
| -------- | ---------------------------------------------------------- |
| Desktop  | [Nocturne Desktop](https://github.com/Neo-XD/nocturne-music) |
| Android  | Nocturne Mobile                                            |

Nocturne Mobile can work independently, but is designed to feel like part of the same ecosystem.

---

## Building from Source

Clone the repository:

```bash
git clone https://github.com/Neo-XD/nocturne-mobile.git
cd nocturne-mobile
```

Open the project in Android Studio or build it using Gradle.

### Last.fm Credentials

> The Local Builds of nocturne do not have the required api keys for scrobbling.

To scrobble with your local build, add your credentials to `local.properties`:

```properties
LASTFM_API_KEY=your_key
LASTFM_SECRET=your_secret
```

Alternatively, provide them through environment variables:

```text
LASTFM_API_KEY
LASTFM_SECRET
```

This is also how CI supplies credentials during builds.

---

## Project Status

Nocturne Mobile is actively developed and evolving quickly.

Features, architecture, and UI may change significantly between releases. Bug reports, feature requests, and contributions are welcome.

---

## Note for Contributors

Like Nocturne Music, this project has been developed heavily through experimentation and AI-assisted development. Some parts of the codebase may not be perfectly optimized or architected.

If you find bugs, performance issues, or areas that can be improved, contributions and constructive feedback are always welcome.

---

## Disclaimer

This project is not affiliated with, funded, authorized, endorsed by, or in any way associated with YouTube, Google LLC, or any of their affiliates and subsidiaries.

All trademarks, service marks, and intellectual property rights referenced in this project belong to their respective owners.

---

## License

[GPL-3.0](LICENSE)
