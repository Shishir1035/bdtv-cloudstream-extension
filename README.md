# M3U live-stream extensions for CloudStream

A small collection of [CloudStream](https://github.com/recloudstream/cloudstream) providers that read standard `#EXTM3U` playlists and expose their entries as Live items. Each module is an independent extension; users install only the ones they want.

The provider is a generic M3U reader — it parses `tvg-logo`, `group-title`, and the channel name, groups entries by category, and resolves HLS variants. Playlist URLs are configuration, not content; this repo hosts no media.

## Install

1. CloudStream → **Settings → Extensions → Add repository**.
2. Paste the repo's raw `plugins.json` URL.
3. Open the repo and install the set(s) you want.

## Build

```bash
./gradlew make makePluginsJson
```

Each `<module>/build/*.cs3` is one extension; `build/plugins.json` is the repo index. CI (`.github/workflows/build.yml`) builds on push to `master` and publishes to the `builds` branch.

## Structure

```
build.gradle.kts        root: cloudstream gradle plugin + deps
settings.gradle.kts     auto-includes every module dir
<module>/
  build.gradle.kts      metadata (version, tvTypes=Live)
  src/main/kotlin/recloudstream/
    *Plugin.kt           @CloudstreamPlugin entry
    *Provider.kt         MainAPI: parse M3U -> Live items
```

## How it works

- `getMainPage` / `search` — fetch + parse the playlist, group by `group-title`, expose entries as `LiveSearchResponse`.
- `load` — return a `LiveStreamLoadResponse`.
- `loadLinks` — resolve HLS variants via `M3u8Helper`, fall back to the raw URL.

## Changing the source

Edit `sources` / `mainUrl` in a module's `*Provider.kt`. Any standard `#EXTM3U` playlist works.

## Legal

This repository contains only an M3U-parsing client. It hosts and distributes no media. Source playlists are third-party; availability and legality of any referenced stream vary by region and rights — users are responsible for the lists they load.
