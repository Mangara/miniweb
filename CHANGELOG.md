# MiniWeb - Change Log
All notable changes to this project will be documented in this file.

## [1.1] - ????-??-??

### Added
- Classnames in inline JS are now replaced in the same way as classnames in external JS files.

### Changed
- The class lists in miniweb.properties are now space-separated, instead of comma-separated to match HTML usage.
- MiniWeb now parses all miniweb.properties files it finds in directories containing HTML files, instead of only the first one. The most restrictive options from each settings file are combined, so if any settings file turns an optimization off, it will not be used, and the final exclusion list is the union of all exclusion lists in the files.

### Fixed
- A crash when processing inline script elements that contain only white-space.

## [1.0] - 2016-10-25
Initial release.
