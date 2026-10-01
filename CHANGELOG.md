<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

QOL improvements.

### Added

- A general progress bar to show the progress of achievements.
- Fixed the UI on light-based themes.
- Added a "SECRET" to see which achievements are and were hidden.
- A "File Voyager" achievement for opening different file types.
- Live updates of the achievements dialog while it is open.
- A collapsible log of unlocked achievement steps with their dates.

### Changed

- Improved the UI of the achievements' progress bars.
- Progress bars use the IDE's native look.
- Easter Egg, Super Easter Egg and Millennium File are unlocked once instead of counting every trigger.
- The achievements dialog remembers its size and position, and opening it again brings the open one to the front.
- Renamed "Millenium File" to "Millennium File".

### Fixed

- Resetting all progress did not reset File Voyager, which unlocked itself again on the next file open.
- Resetting all progress removed the dialog buttons.
- Code Necromancer read the file's VCS history on every file open, even after being unlocked.
- Rare errors when an action closed a project or when progress was saved while being updated.

## Release 0.1.0

Initial release.

### Added

- Achievements popup available to open from the Tools menu.
- Achievements:
    - **Code Necromancer** – Open a file that hasn't been changed in over 2 years.
    - **Millenium File** – Open a file with 1,000 or more lines.
    - **Tab Avalanche** – Have 10, 25, 50 or 100 tabs opened simultaneously.
    - **Clean Sweep** – Close 10, 25, 50 or 100 tabs with a single click.
    - **Easter Egg** – Find the Easter Egg.
    - **Super Easter Egg** – Find the Super Easter Egg.
