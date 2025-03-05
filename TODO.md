# SafeScan Development TODOs

## Frontend Tasks (Flutter)

### App Scan Page (`privacy_scan_module/lib/app_scan_page.dart`)
- [ ] Fix color assignment for target apps based on CSV classification
  - Currently shows all as unsecure/unknown despite secure installation status
  - Need to match source scan color logic
  - Affects `_getColorForType()` method

- [ ] Remove duplicate permission icons in app listings
  - Consolidate permission icon display logic
  - Ensure each permission is shown only once per app

- [ ] Redesign dropdown information display
  - Replace dropdowns with horizontal icon row
  - Implement info popup on icon click
  - Maintain existing text content in new format

- [ ] Enhance app interaction functionality
  - Make non-offstore SOURCE apps clickable to open settings
  - Make non-offstore TARGET apps clickable to launch where app was downloaded from
  - Update `_launchAppStore()` and related methods

### Privacy Scan Page (`privacy_scan_module/lib/privacy_scan_page.dart`)
- [ ] Enhance privacy recommendations
  - Add permission status checking
  - Implement permission-specific recommendations
  - Create recommendation templates for common permissions
  - Handle both SOURCE and TARGET device permissions

## Backend Tasks (Java)

### SpywareDetector (`app/src/main/java/com/htetznaing/adbotg/SpywareDetector.java`)
- [ ] Fix app classification logic for target devices
  - Review `compareWithCSV()` method
  - Ensure installer verification works correctly
  - Match source device classification logic

### App Icons
- [ ] Implement icon retrieval for TARGET apps
  - Research and implement app icon API integration
  - Consider creating/using icon repository
  - Add to both app scan and privacy scan pages

### Permission Analysis (`app/src/main/java/com/htetznaing/adbotg/PrivacySettingsHandler.java`)
- [ ] Implement TARGET device permission checking
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