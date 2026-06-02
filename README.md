# Afterglow TV

<p align="center">
	<a href="https://github.com/xuninc/AfterglowTV/releases/latest/download/Afterglow TV.apk"><img src="https://img.shields.io/badge/Download-Afterglow TV.apk-2ea44f?style=for-the-badge&logo=android" alt="Download Afterglow TV APK" /></a>
	<a href="https://github.com/xuninc/AfterglowTV/releases/latest"><img src="https://img.shields.io/github/v/release/xuninc/AfterglowTV?display_name=tag&style=for-the-badge&color=0f766e" alt="Latest Afterglow TV release" /></a>
	<a href="https://github.com/xuninc/AfterglowTV/releases"><img src="https://img.shields.io/github/downloads/xuninc/AfterglowTV/total?style=for-the-badge&color=8b5cf6" alt="Total Downloads" /></a>
	<a href="docs/CHANGELOG.md"><img src="https://img.shields.io/badge/Changelog-View-2563eb?style=for-the-badge" alt="View changelog" /></a>
	<a href="https://github.com/xuninc/AfterglowTV/actions/workflows/release.yml"><img src="https://img.shields.io/github/actions/workflow/status/xuninc/AfterglowTV/release.yml?branch=master&style=for-the-badge&label=CI" alt="GitHub Actions status" /></a>
	<a href="LICENSE"><img src="https://img.shields.io/badge/License-Afterglow_OSL-0284c7?style=for-the-badge" alt="License" /></a>
</p>

Afterglow TV is a TV-first IPTV player for Android TV built with Kotlin, Jetpack Compose, Room, Hilt, and Media3.

It is designed for large playlists, remote-friendly browsing, fast provider switching, and a polished living-room playback experience. Afterglow TV supports `M3U` playlists, `Xtream Codes`, and `Portal/MAG` providers, with dedicated flows for `Live TV`, `Movies`, and `Series`.

Built for Android TV first, Afterglow TV focuses on the things generic IPTV apps usually get wrong: D-pad navigation, quick channel movement, large-library organization, and a player that still feels good to use from the couch. Phone and tablet installs are also supported, but the primary UX target is TV.

## Preview
<p align="center">
	<a href="https://github.com/xuninc/AfterglowTV/raw/master/docs/images/LiveTV.png"><img src="docs/images/LiveTV.png" alt="Live TV" width="88%" /></a>
</p>

<p align="center">
	<a href="https://github.com/xuninc/AfterglowTV/raw/master/docs/images/Movies.png"><img src="docs/images/Movies.png" alt="Movies" width="44%" /></a>
	<a href="https://github.com/xuninc/AfterglowTV/raw/master/docs/images/MovieInfo.png"><img src="docs/images/MovieInfo.png" alt="Movie Details" width="44%" /></a>
</p>

<p align="center">
	<a href="https://github.com/xuninc/AfterglowTV/raw/master/docs/images/Home.png"><img src="docs/images/Home.png" alt="Home" width="19%" /></a>
	<a href="https://github.com/xuninc/AfterglowTV/raw/master/docs/images/LiveTV.png"><img src="docs/images/LiveTV.png" alt="Live TV" width="19%" /></a>
	<a href="https://github.com/xuninc/AfterglowTV/raw/master/docs/images/ChannelView.png"><img src="docs/images/ChannelView.png" alt="Channel Preview" width="19%" /></a>
	<a href="https://github.com/xuninc/AfterglowTV/raw/master/docs/images/Guide.png"><img src="docs/images/Guide.png" alt="Guide" width="19%" /></a>
	<a href="https://github.com/xuninc/AfterglowTV/raw/master/docs/images/Settings.png"><img src="docs/images/Settings.png" alt="Settings" width="19%" /></a>
</p>

<p align="center">
	<a href="https://github.com/xuninc/AfterglowTV/raw/master/docs/images/SeriesEpisodes.png"><img src="docs/images/SeriesEpisodes.png" alt="Series Episodes" width="32%" /></a>
</p>

## Highlights

- Android TV-first interface with D-pad-friendly focus, navigation, and playback flows
- Playlist support for `Xtream Codes`, `Portal/MAG`, and `M3U` sources, including local playlist files
- Combined M3U live-source support with optional in-browser source switching for merged Live TV setups
- Fast live-TV browsing with preview mode, favorites, recent channels, custom groups, and pinned categories
- Movie and series libraries with detailed info pages, resume support, episode switching, and auto-play for next episodes
- Full EPG support with guide search, XMLTV support, and provider archive or catch-up when available
- Built-in DVR with scheduled recording, background capture, recording playback, and app-managed default storage
- Multi-view split-screen playback for watching multiple channels at once
- Strong parental controls with PIN-protected categories and automatic adult-category detection
- TV integrations including Watch Next, launcher recommendations, TV input sync, Cast support, and in-app update delivery

## Features

### Playlist Support

- `Xtream Codes`
- `Portal/MAG`
- `M3U` playlists from URLs plus local files
- Separate onboarding and sync flows for live channels, movies, series, and guide data
- Fast switching between providers with provider-scoped settings
- Combined M3U profiles for merging multiple M3U providers into a single Live TV source

### Navigation And TV UX

- Designed for Android TV and D-pad navigation first
- Fast channel browsing with large-playlist friendly layouts
- Numeric remote input for direct channel entry
- Preview mode while browsing channels
- TV-friendly search and text-entry flows

### Live TV And Channel Management

- Favorites and recently watched channels
- Custom groups for personal channel collections
- Pinned categories surfaced near the top of the live guide rail
- Optional Live TV provider or source browser for M3U-based setups
- Long-press live categories for actions like pin, hide, lock or unlock, and custom-group management
- Channel reordering for favorites and custom groups
- Channel numbering modes by group or across the full provider lineup
- Predefined filter words to make category search cleaner on noisy provider data

### Guide, Search, And Playback

- Full EPG grid view
- Program search inside the guide
- XMLTV guide support with built-in EPG source management
- Manual EPG match overrides and source-priority controls from inside Settings and Guide flows
- Provider archive or catch-up support when the source exposes replay streams
- Live rewind or timeshift playback with up to 30 minutes of buffer, even when provider catch-up is unavailable
- Global search across live TV, movies, and series
- Multi-view for watching multiple live streams at once
- Player controls for subtitles, audio tracks, aspect ratio, playback speed, video quality, and Cast

### Recording And Playback

- Scheduled and background DVR recording for live channels
- Program reminders from guide entries when you want a notification without scheduling a recording
- Conflict detection, persistence, and repair support for recording jobs
- App-managed default recording folder with optional custom storage selection
- In-app playback for completed recordings with a visible on-player recording indicator during active capture
- Playback troubleshooting controls for decoder mode, media session behavior, and timeout tuning

### Movies And Series

- Two VOD layouts:
	- Modern shelf-based browsing
	- Classic left-sidebar category browsing
- Detailed info pages for movies and series
- Continue watching, playback history, and detail-screen resume actions with saved position context
- Long-press VOD categories and custom groups for actions like hide, rename, delete, or reorder when applicable
- In-player episode switching for series
- Automatic next-episode playback when another episode is available

### Parental Controls

- Hide categories completely
- Lock categories behind a PIN
- Option to hide locked content from browsing views
- Adult-category detection using provider flags and category naming heuristics

### Languages And Device Support

- English plus 25 translated locale packs currently ship with the app
- Locale coverage is broader and rendering is more reliable across supported languages
- Built for TV first; phones and tablets are supported, but not the primary design target

### Platform Integrations

- Android TV Watch Next integration
- Launcher recommendations and TV entry points
- Android TV Input Framework channel sync
- Google Cast sender support

## Quick TV Tips

- Long-press a channel, movie, or series to add it to Favorites or a custom group.
- Long-press a live category to open category actions such as pin, hide, lock or unlock, and custom-group actions like reorder.
- In Movies and Series, long-press categories or custom groups for hide or group-management actions where available.
- Long-press a live channel to queue it for Split Screen.
- Use the number keys on a remote while in the player to jump directly to a channel.
- While watching a series, open Episodes in the player to switch episodes without backing out to the details page.

## Download

- [Download latest Afterglow TV.apk](https://github.com/xuninc/AfterglowTV/releases/latest/download/Afterglow TV.apk)
- The app can also detect and download newer releases in-app through GitHub Releases.
- GitHub Actions still runs build and test validation on pushes and pull requests.
- GitHub Releases are now published only when the workflow is started manually with `workflow_dispatch`, so versioned releases do not get created by mistake on every push.

## Support

If Afterglow TV is useful to you, you can support development here:


## Project Structure

- `app/` Android app UI, navigation, dependency injection, and Android TV integrations
- `data/` Room database, sync, parsing, provider implementations, and repositories
- `domain/` models, repository contracts, managers, and use cases
- `player/` playback abstraction and Media3 player implementation
- `docs/` architecture notes and image assets

## Build

Requirements:

- Android Studio
- Android SDK
- JDK 17 or another Gradle-supported JDK 17 runtime

Useful commands:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew testDebugUnitTest
```

## Notes

- Afterglow TV is an IPTV client, not a content provider.
- Use only playlists, streams, and guide sources you are authorized to access.
- Local configuration and signing files are intentionally excluded from git.

## License

This project was originally released without an explicit license.  
As of April 2026, all usage, modification, and distribution are governed by the Afterglow TV Source-Available License (Non-Commercial).

Any use of this project must comply with the terms defined in the LICENSE file.
