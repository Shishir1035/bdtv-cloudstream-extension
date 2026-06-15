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
build.gradle.kts        root: gradle plugin, deps, common metadata, manifest reader
settings.gradle.kts     defines one module per manifest key
config.json             the manifest (gitignored; = CONFIG_JSON secret in CI)
shared/src/main/kotlin/recloudstream/
  M3uLivePlugin.kt        @CloudstreamPlugin entry (shared by every module)
  M3uLiveProvider.kt      MainAPI: parse M3U -> Live items (shared by every module)
<key>/                    created on demand; holds only build output
```

Every module is the *same* generic M3U reader compiled from `shared/`. A module's identity
is `BuildConfig.MODULE_KEY` (= its key). There are no per-module source or gradle files.

## The manifest (`config.json` / `CONFIG_JSON`)

Single source of truth — its keys are the extensions that get built:

```json
{
  "t0ff33": { "link": "https://…/playlist.m3u", "desc": "Toffee Live", "logo": "" }
}
```

- `link` — playlist URL, read at runtime by the provider.
- `desc` — extension description (shown in CloudStream's extension list).
- `logo` — extension icon URL; empty falls back to the default.

Locally it's the gitignored `config.json`; in CI it's the `CONFIG_JSON` GitHub secret (same shape).

## How it works

- `getMainPage` / `search` — fetch + parse the playlist, group by `group-title`, expose entries as `LiveSearchResponse`.
- `load` — return a `LiveStreamLoadResponse`.
- `loadLinks` — resolve HLS variants via `M3u8Helper`, fall back to the raw URL.

## Adding / changing an extension

Add or edit a block in the manifest (`config.json` locally, `CONFIG_JSON` secret in CI).
A new key becomes a new extension automatically — no new files. Any `#EXTM3U` playlist works.

## Legal

This repository contains only an M3U-parsing client. It hosts and distributes no media. Source playlists are third-party; availability and legality of any referenced stream vary by region and rights — users are responsible for the lists they load.
