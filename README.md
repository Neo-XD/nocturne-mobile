# nocturne-mobile
Designed from Scratch as a Companion app for nocturne-music and also as a standalone app for vibing on the go

## Building

Last.fm credentials are read from the environment, never committed. Scrobbling stays disabled
without them and everything else builds and runs as normal.

Add them to `local.properties` (gitignored) for a local build:

```properties
LASTFM_API_KEY=your_key
LASTFM_SECRET=your_secret
```

`LASTFM_API_KEY` and `LASTFM_SECRET` environment variables work too, which is how CI supplies them.
