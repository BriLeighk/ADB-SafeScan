import 'dart:typed_data';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class SocialMediaApp {
  final String name;
  final String packageName;
  final String description;
  final Uint8List icon;
  final List<String> baseRecommendations;
  final List<dynamic> permissions;

  SocialMediaApp({
    required this.name,
    required this.packageName,
    required this.description,
    required this.icon,
    required this.baseRecommendations,
    required this.permissions,
  });

  factory SocialMediaApp.fromMap(Map<String, dynamic> map) {
    try {
      List<String> recommendations = [];

      // Add app-specific base recommendations
      switch (map['packageName']?.toString()) {
        case 'com.google.android.gm':
          recommendations.addAll([
            'Review email sync settings',
            'Check contact sharing permissions',
            'Enable 2-step verification',
            'Review connected third-party apps'
          ]);
          break;
        case 'com.google.android.apps.docs':
          recommendations.addAll([
            'Review file sharing defaults',
            'Check offline access settings',
            'Review third-party app access',
            'Enable link sharing restrictions'
          ]);
          break;
        case 'com.google.android.apps.maps':
          recommendations.addAll([
            'Review location sharing settings',
            'Check Timeline history settings',
            'Review saved places privacy',
            'Manage location data collection'
          ]);
          break;
        case 'com.instagram.android':
          recommendations.addAll([
            'Set account to private',
            'Control who can message you',
            'Review tagged posts settings'
          ]);
          break;
        case 'com.snapchat.android':
          recommendations.addAll([
            'Enable Ghost Mode',
            'Set story visibility to Friends Only',
            'Review app permissions'
          ]);
          break;
        case 'com.whatsapp':
          recommendations.addAll([
            'Review who can see your profile',
            'Control Last Seen visibility',
            'Check group privacy settings'
          ]);
          break;
        case 'com.facebook.katana':
          recommendations.addAll([
            'Review tagged posts before they appear',
            'Set default post audience to Friends',
            'Check third-party app permissions'
          ]);
          break;
        case 'com.zhiliaoapp.musically':
          recommendations.addAll([
            'Set account to private',
            'Disable Allow others to find me',
            'Review direct message settings'
          ]);
          break;
        default:
          recommendations.add('Review app privacy settings regularly');
      }

      return SocialMediaApp(
        name: map['name']?.toString() ?? 'Unknown App',
        packageName: map['packageName']?.toString() ?? '',
        description:
            map['description']?.toString() ?? 'No description available',
        icon: map['icon'] as Uint8List,
        baseRecommendations: recommendations,
        permissions: List<dynamic>.from(map['permissions'] ?? []),
      );
    } catch (e) {
      print('Error creating SocialMediaApp from map: $e');
      print('Map contents: $map');
      rethrow;
    }
  }

  List<String> getAllRecommendations() {
    List<String> allRecommendations = [];

    // First add permission-specific recommendations with warning emoji
    List<String> permissionRecs = _getPermissionRecommendations();
    if (permissionRecs.isNotEmpty) {
      allRecommendations.add('⚠️ Permission-Specific Alerts:');
      allRecommendations.addAll(permissionRecs);
      allRecommendations.add(''); // Add spacing
    }

    // Then add app-specific recommendations
    if (baseRecommendations.isNotEmpty) {
      allRecommendations.add('📱 App-specific recommendations:');
      allRecommendations.addAll(baseRecommendations.map((rec) => '• $rec'));
      allRecommendations.add(''); // Add spacing
    }

    // Finally add base recommendations
    allRecommendations.add('🔒 General security recommendations:');
    allRecommendations.addAll([
      '• Review app permissions regularly',
      '• Enable two-factor authentication if available',
      '• Check privacy settings after each app update'
    ]);

    return allRecommendations;
  }

  List<String> _getPermissionRecommendations() {
    List<String> recommendations = [];
    Set<String> permissionTypes = _extractPermissionTypes();

    if (permissionTypes.isEmpty) {
      return recommendations;
    }

    if (permissionTypes.contains('location')) {
      recommendations.addAll([
        '📍 Location access is enabled:',
        '  • Consider disabling location access when not actively using navigation',
        '  • Check location history settings',
        '  • Review which apps can access location in background'
      ]);
    }

    if (permissionTypes.contains('camera')) {
      recommendations.addAll([
        '📸 Camera access is enabled:',
        '  • Ensure camera access is necessary for app functionality',
        '  • Review app\'s photo access permissions',
        '  • Consider revoking if not regularly used'
      ]);
    }

    if (permissionTypes.contains('microphone')) {
      recommendations.addAll([
        '🎤 Microphone access is enabled:',
        '  • Monitor which apps can record audio',
        '  • Disable microphone access when not in calls/recordings',
        '  • Check if background audio recording is enabled'
      ]);
    }

    if (permissionTypes.contains('storage')) {
      recommendations.addAll([
        '📁 Storage access is enabled:',
        '  • Review which files the app can access',
        '  • Consider using secure folders for sensitive data',
        '  • Monitor file access patterns'
      ]);

      // Add app-specific storage recommendations
      switch (packageName) {
        case 'com.google.android.apps.docs':
          recommendations.addAll([
            '  • Review offline access settings',
            '  • Check shared file permissions'
          ]);
          break;
      }
    }

    // Add app-specific permission recommendations
    switch (packageName) {
      case 'com.google.android.gm':
        if (permissionTypes.contains('storage')) {
          recommendations.addAll([
            '  • Review attachment download location',
            '  • Check auto-download settings'
          ]);
        }
        break;
      case 'com.google.android.apps.maps':
        if (permissionTypes.contains('location')) {
          recommendations.addAll([
            '  • Review Timeline settings',
            '  • Check location sharing duration',
            '  • Disable location history if not needed'
          ]);
        }
        break;
    }

    return recommendations;
  }

  Set<String> _extractPermissionTypes() {
    Set<String> types = {};
    for (var perm in permissions) {
      String permission = '';
      if (perm is String) {
        permission = perm.toLowerCase();
      } else if (perm is Map) {
        permission = (perm['permission']?.toString() ?? '').toLowerCase();
      }

      if (permission.contains('location')) {
        types.add('location');
      } else if (permission.contains('camera')) {
        types.add('camera');
      } else if (permission.contains('audio') ||
          permission.contains('microphone')) {
        types.add('microphone');
      } else if (permission.contains('storage') ||
          permission.contains('media')) {
        types.add('storage');
      }
    }
    return types;
  }

  static Future<List<SocialMediaApp>> getInstalledApps(
      BuildContext context, bool fromTarget) async {
    const platform = MethodChannel('com.htetznaing.adbotg/privacy_settings');

    try {
      final List<dynamic> result = await platform
          .invokeMethod('getSocialMediaApps', {'fromTarget': fromTarget});
      List<SocialMediaApp> socialMediaApps = [];

      for (var appData in result) {
        socialMediaApps.add(SocialMediaApp(
          name: appData['name'],
          packageName: appData['packageName'],
          description: appData['description'],
          icon: base64.decode(appData['icon']),
          baseRecommendations: List<String>.from(appData['recommendations']),
          permissions: List<dynamic>.from(appData['permissions'] ?? []),
        ));
      }

      return socialMediaApps;
    } catch (e) {
      print('Error fetching social media apps: $e');
      return [];
    }
  }
}

class PrivacySettings {
  static const platform =
      MethodChannel('com.htetznaing.adbotg/privacy_settings');

  static Future<void> openGooglePrivacySettings(BuildContext context) async {
    try {
      await platform.invokeMethod('openGooglePrivacySettings');
    } catch (e) {
      _showError(context, 'Could not open Google Privacy settings');
    }
  }

  static Future<void> openAppSettings(
      BuildContext context, String packageName) async {
    try {
      await platform.invokeMethod('openAppSettings', {'package': packageName});
    } catch (e) {
      _showError(context, 'Could not open app settings');
    }
  }

  static void _showError(BuildContext context, String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message)),
    );
  }

  static Future<List<SocialMediaApp>> getInstalledSocialMediaApps(
      BuildContext context) async {
    try {
      print('Fetching installed social media apps...');
      final List<dynamic>? rawApps = await platform
          .invokeMethod<List<dynamic>>('getInstalledSocialMediaApps');

      if (rawApps == null) {
        print('No apps received from native side');
        return [];
      }

      print('Received ${rawApps.length} apps from native side');

      final List<SocialMediaApp> socialMediaApps = rawApps.map((dynamic app) {
        final Map<String, dynamic> appMap =
            Map<String, dynamic>.from(app as Map);
        print('Processing app: ${appMap['name']}');
        return SocialMediaApp.fromMap(appMap);
      }).toList();

      print('Processed ${socialMediaApps.length} social media apps');
      return socialMediaApps;
    } catch (e, stackTrace) {
      print('Error fetching social media apps: $e');
      print('Stack trace: $stackTrace');
      _showError(context, 'Could not fetch installed social media apps: $e');
      return [];
    }
  }
}
