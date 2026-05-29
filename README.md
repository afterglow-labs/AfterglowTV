# Afterglow TV

<p align="center">
  <a href="https://github.com/xuninc/AfterglowTV/releases/latest/download/AfterglowTV.apk"><img src="https://img.shields.io/badge/Download-AfterglowTV.apk-2ea44f?style=for-the-badge&logo=android" alt="Download Afterglow TV APK" /></a>
  <a href="https://github.com/xuninc/AfterglowTV/releases/latest"><img src="https://img.shields.io/github/v/release/xuninc/AfterglowTV?display_name=tag&style=for-the-badge&color=0f766e" alt="Latest Release" /></a>
  <a href="https://github.com/xuninc/AfterglowTV/releases"><img src="https://img.shields.io/github/downloads/xuninc/AfterglowTV/total?style=for-the-badge&color=8b5cf6" alt="Downloads" /></a>
  <a href="docs/CHANGELOG.md"><img src="https://img.shields.io/badge/Changelog-View-2563eb?style=for-the-badge" alt="Changelog" /></a>
  <a href="https://github.com/xuninc/AfterglowTV/actions/workflows/release.yml"><img src="https://img.shields.io/github/actions/workflow/status/xuninc/AfterglowTV/release.yml?branch=main&style=for-the-badge&label=CI" alt="CI Status" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Proprietary-0284c7?style=for-the-badge" alt="License" /></a>
</p>

Afterglow TV is a TV-first IPTV player for Android TV built with Kotlin, Jetpack Compose, Room, Hilt, and Media3.

Designed specifically for televisions and remote controls, Afterglow TV focuses on fast navigation, large playlist performance, polished playback, and features that many IPTV players either lack or implement poorly.

Afterglow TV supports M3U playlists, Xtream Codes providers, Portal/MAG providers, XMLTV guide data, DVR recording, multi-view playback, parental controls, and Android TV platform integrations.

---

## Preview

Updated screenshots are coming soon.

Current application views include:

- Home
- Live TV
- Channel Preview
- Guide
- Movies
- Movie Details
- Series Episodes
- Settings
- Multi-View Playback
- DVR Management
- Search
- Provider Management

---

## Highlights

- Android TV-first interface with proper D-pad navigation
- M3U, Xtream Codes, and Portal/MAG support
- Live TV, Movies, Series, and EPG support
- Fast browsing for very large playlists
- Favorites, recently watched channels, custom groups, and pinned categories
- XMLTV guide support
- Provider archive and catch-up support when available
- Live rewind and timeshift playback
- Built-in DVR recording system
- Multi-view split-screen playback
- Strong parental controls with PIN protection
- Android TV launcher integrations
- Google Cast support
- In-app update delivery through GitHub Releases

---

## Features

### Provider Support

- Xtream Codes
- Portal/MAG
- M3U playlist URLs
- Local M3U playlist files
- XMLTV guide sources
- Combined M3U provider profiles
- Fast provider switching
- Provider-scoped settings

### Live TV

- Favorites
- Recently watched channels
- Custom channel groups
- Pinned categories
- Hidden categories
- Locked categories
- Channel reordering
- Category filtering
- Numeric channel entry
- Preview browsing mode
- Direct remote navigation

### Guide & Playback

- Full EPG grid
- XMLTV source management
- Guide search
- Manual EPG matching
- Provider archive support
- Catch-up playback
- Live rewind buffer
- Timeshift playback
- Subtitle selection
- Audio track selection
- Playback speed controls
- Aspect ratio controls
- Quality selection
- Google Cast support

### DVR

- Scheduled recording
- Background recording
- Recording conflict detection
- Recording persistence
- Recording playback
- App-managed recording storage
- Optional custom recording folders
- Program reminders
- Recording repair support

### Movies & Series

- Modern shelf-based browsing
- Classic category-based browsing
- Detailed movie pages
- Detailed series pages
- Continue Watching
- Playback history
- Resume playback
- Episode switching
- Automatic next-episode playback

### Search

- Global search across:
  - Live TV
  - Movies
  - Series
  - Guide Data

### Multi-View

- Watch multiple live streams simultaneously
- Queue channels directly from the guide
- Optimized for large-screen viewing

### Parental Controls

- PIN-protected categories
- Hidden categories
- Locked categories
- Adult-category detection
- Optional hidden locked content

### Platform Integrations

- Android TV Launcher
- Leanback Support
- Watch Next Integration
- Launcher Recommendations
- Android TV Input Framework Integration
- Google Cast Sender Support
- GitHub Release Updates

### Languages

- English
- 25 additional translated locale packs

---

## Quick Tips

- Long-press channels to add them to Favorites or Custom Groups.
- Long-press categories to pin, hide, lock, unlock, or reorder them.
- Long-press channels to queue them for Multi-View playback.
- Use remote number keys to jump directly to channels.
- Switch episodes directly from the player without returning to the details page.

---

## Download

### Latest Release

https://github.com/xuninc/AfterglowTV/releases/latest

### Direct APK

https://github.com/xuninc/AfterglowTV/releases/latest/download/AfterglowTV.apk

Versioned releases are available through GitHub Releases.

The application can also detect and install newer releases directly from within the app.

---

## Project Structure

```text
app/       Android application UI, navigation, dependency injection,
           Android TV integrations, settings, and onboarding

data/      Database, playlist parsing, sync engine,
           provider implementations, repositories

domain/    Models, repository contracts, managers,
           business logic, and use cases

player/    Playback abstraction layer and Media3 implementation

docs/      Documentation, changelog, and project notes
