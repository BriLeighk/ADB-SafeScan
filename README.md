# SafeScan - Spyware Detection Tool

## Overview
SafeScan is a comprehensive spyware detection tool designed to assist victims of Intimate Partner Violence (IPV) by providing a secure way to detect and manage potential spyware and privacy risks on Android devices. The app uses ADB (Android Debug Bridge) over USB-OTG to scan target devices without requiring root access.

## Key Features

### App Detection
- **Comprehensive App Scanning**
  - Scans installed applications against known spyware/dual-use app database
  - Risk categorization system:
    - Red: Off-store downloads (highest risk)
    - Yellow: Known spyware applications
    - Blue: Dual-use applications (potential for misuse)
  - Integration with remote CSV database for up-to-date threat detection
  - Secure installer verification (Play Store, System, Samsung Store)

### Privacy Analysis
- **Permission Monitoring**
  - Detailed permission analysis for installed apps
  - Privacy recommendations based on app permissions
  - Direct links to app settings for quick modifications

### Device Connection
- **Secure USB Connection**
  - USB OTG connection between devices
  - No root access required
  - Real-time connection state management
  - Clear visual indicators for connection status

### User Interface
- Clean, intuitive interface designed for accessibility
- Clear visual indicators for scan results
- Detailed privacy recommendations
- Step-by-step connection guidance

## Technical Architecture

### Core Components
- **Native Android (Java)**
  - ADB protocol implementation
  - USB host API integration
  - Package manager interface
  - Permission management

- **Flutter UI Layer**
  - Cross-platform UI components
  - State management
  - CSV data processing
  - Privacy analysis logic

### Key Technologies
- Java (Android native development)
- Flutter/Dart (UI and business logic)
- ADB Protocol Implementation
- AndroidX Libraries

## Getting Started

### Prerequisites
- Source device with:
  - Android 5.0 or higher
  - USB Host support
  - USB OTG cable
- Target Android device with:
  - USB debugging enabled
  - Screen unlocked during scan

### Installation
1. Download the latest release from the releases page
2. Install on the source device (device performing the scan)
3. Enable developer options on target device
4. Enable USB debugging on target device

### Basic Usage
1. Launch SafeScan on source device
2. Connect target device via USB OTG cable
3. Accept USB debugging prompt on target device
4. Select desired scan type
5. Review scan results and recommendations

## Development Setup
1. Download the latest release from the releases page
2. Install on the source device (device performing the scan)
3. Enable developer options on target device
4. Enable USB debugging on target device


## Acknowledgments
- Original ADB-OTG project referenced by KhunHtetzNaing
- ADB library developed by wuxudong
- Research support from WISPR Lab
