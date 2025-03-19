# SafeScan Development TODOs

## Frontend Tasks (Flutter)

### App Scan Page (`privacy_scan_module/lib/app_scan_page.dart`)
- [x] Fix color assignment for target apps based on CSV classification
  - Currently shows all as unsecure/unknown despite secure installation status
  - Need to match source scan color logic
  - Affects `_getColorForType()` method

- [x] Remove duplicate permission icons in app listings
  [x] Consolidate permission icon display logic
  [x]  Ensure each permission is shown only once per app

- [x] Redesign dropdown information display
  - Replace dropdowns with horizontal icon row
  - Implement info popup on icon click
  - Maintain existing text content in new format

- [x] Enhance app interaction functionality
  [x] Make non-offstore SOURCE apps clickable to open settings
  - Make non-offstore TARGET apps clickable to launch where app was downloaded from
  - Update `_launchAppStore()` and related methods

### Privacy Scan Page (`privacy_scan_module/lib/privacy_scan_page.dart`)
- [x] Enhance privacy recommendations
  [x] Add permission status checking
  [x] Implement permission-specific recommendations
  [x] Create recommendation templates for common permissions
  [x] Handle above for SOURCE device permissions
  - Handle above for TARGET device permissions

## Backend Tasks (Java)

### SpywareDetector (`app/src/main/java/com/htetznaing/adbotg/SpywareDetector.java`)
- [x] Fix app classification logic for TARGET devices
  - Review `compareWithCSV()` method
  - See if its possible to get the installer type from the target device through adb instead of just referencing csv flag with disclaimer to the user.

### App Icons
- [ ] Implement icon retrieval for TARGET apps
  - Research and implement app icon API integration
  - Consider creating/using icon repository
  - Add to both app scan and privacy scan pages

### Permission Analysis (`app/src/main/java/com/htetznaing/adbotg/PrivacySettingsHandler.java`)
- [x] Implement TARGET device permission checking
  - Add ADB shell command execution for permission queries
  - Parse and return permission data to Flutter layer
  - Handle permission status interpretation

## Integration Points
- Frontend needs to handle new permission data format from backend
- Backend needs to provide consistent app classification across source/target devices
- Icon retrieval system needs to work with both scan types
- Permission recommendations need to sync between Flutter UI and Java backend

## Notes
- Target device operations require ADB shell commands
- Consider caching mechanisms for app icons
- Maintain consistent behavior between source and target devices
- Ensure error handling for all new features 