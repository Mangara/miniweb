# MiniWeb - Change Log
All notable changes to this project will be documented in this file.

## [1.2] - 2017-10-07

### Added
- The MiniWeb settings can now be configured from source code.

## [1.1] - 2017-06-14

### Added
- Classnames in inline JS are now replaced in the same way as classnames in external JS files.

### Changed
- The class lists in miniweb.properties are now space-separated, instead of comma-separated to match HTML usage.
- MiniWeb now parses all miniweb.properties files it finds in directories containing HTML files, instead of only the first one. The most restrictive options from each settings file are combined, so if any settings file turns an optimization off, it will not be used, and the final exclusion list is the union of all exclusion lists in the files.
- Improved exception handling when linked JS or CSS files don't exist.

### Fixed
- A crash when processing inline script elements that contain only white-space.

## [1.0] - 2016-10-25
Initial release.
