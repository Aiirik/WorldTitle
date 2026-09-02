# World Title

World Title adds your current OSRS world to the RuneLite window title.

This is useful when you run multiple RuneLite clients, record gameplay, stream, or alt-tab between clients and want the active world visible from the operating system window title.

## Features

- Shows the current world in the RuneLite title bar.
- Keeps the world suffix applied after alt-tabbing away and back.
- Updates after world hops.
- Works with RuneLite's `Show display name in title` setting enabled or disabled.
- Optional world activity display, such as `Trade` or `Wintertodt`.
- Optional membership display, showing `(Members)` or `(Free)`.
- Configurable world format:
  - `W301`
  - `World 301`
  - `[301]`
  - `301`

## Examples

![World Title example](docs/world-title-example.png)

```text
RuneLite - PlayerName - W301
RuneLite - PlayerName - W301 - Trade - Free
RuneLite - PlayerName - W307 - Wintertodt (Members)
RuneLite - W301
```

## Configuration

Open RuneLite's plugin settings and search for `World Title`.

Available settings:

- `World format`: controls how the world number is displayed.
- `Show world activity`: appends the current world's listed activity when available.
- `Show membership type`: appends whether the world is free-to-play or members.

## Notes

World activity and membership data comes from RuneLite's existing world list service. If that data has not loaded yet, the plugin may briefly show only the world number and update once RuneLite receives the world list.

## Change log

Click to view the <a href="https://github.com/Aiirik/WorldTitle/blob/master/CHANGELOG.md">CHANGELOG</a>
