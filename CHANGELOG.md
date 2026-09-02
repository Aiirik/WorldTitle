# Changelog

## v1.0.0 - 02-Sep-2026

Initial release.

### Added

- Display the current world in the RuneLite window title.
- Reapply the world suffix after the RuneLite window regains focus.
- Update the title after world hops.
- Support RuneLite's `Show display name in title` setting when enabled or disabled.
- Add configurable world number formats:
  - `W301`
  - `World 301`
  - `[301]`
  - `301`
- Add optional world activity text, for example `W307 - Wintertodt`.
- Add optional membership text, for example `W307 - Wintertodt (Members)`.
- Add optional world region text after the world or activity, for example `W307 - Wintertodt (UK)`.
- Add optional player count text, for example `W301 - Trade - 842 players`.
- Add configurable membership styles:
  - `(Members) / (Free)`
  - `(Members) / (F2P)`
  - `Members / Free`
- Add configurable detail separators:
  - `W301 - Trade`
  - `W301 | Trade`
  - `W301: Trade`
- Add title cleanup when the plugin is disabled or the client returns to the login screen.

### Technical

- Uses RuneLite's existing world list service for world activity and membership data.
- Avoids direct HTTP requests, file IO, reflection, and gameplay automation.
- Avoids repeated title writes when the title already matches the expected value.
