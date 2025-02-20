# Privacy Scan Module

A Flutter module that provides privacy scanning capabilities for the SafeScan application. This module handles both app scanning and privacy settings analysis for source and target devices.

## Features

### App Scanning
- Scans installed applications against known spyware/dual-use app database
- Risk categorization system:
  - Red: Off-store downloads (highest risk)
  - Yellow: Known spyware applications 
  - Light Blue: Dual-use applications (potential for misuse)
  - Grey: Unknown/uncategorized apps
- Integration with remote CSV database for up-to-date threat detection
- Secure installer verification (Play Store, Samsung Store, etc.)

### Privacy Settings
- Social media app privacy analysis
- Permission monitoring and recommendations
- Privacy setting optimization suggestions
- Direct links to app settings when available

## Components

### Main Pages
- `app_scan_page.dart`: Handles app scanning and result display
- `privacy_scan_page.dart`: Manages privacy settings analysis
- `adb_info_page.dart`: Provides ADB connection guidance
- `main_page.dart`: Main navigation interface

### Utilities
- `csv_utils.dart`: CSV data fetching and parsing
- `app_state.dart`: Global state management
- `privacy_settings.dart`: Privacy settings analysis logic

## Integration

This module is designed to be integrated into the main SafeScan application. It requires:

1. ADB connection capabilities from the host app
2. Access to device package manager
3. Permission to read app installation sources
4. USB connection state management

## Dependencies
- provider: ^6.0.0 (State management)
- http: ^0.13.4 (Network requests)
- csv: ^5.0.0 (CSV parsing)
- path_provider: ^2.0.11 (File management)
- url_launcher: ^6.1.14 (External link handling)

## Usage

The module can be integrated into an Android application following Flutter's add-to-app documentation. See the [main SafeScan repository](https://github.com/BriLeighk/ADB-SafeScan) for full implementation details.
