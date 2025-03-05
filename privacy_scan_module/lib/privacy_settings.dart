import 'dart:typed_data';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class SocialMediaApp {
  final String name;
  final String packageName;
  final String description;
  final Uint8List icon;
  final List<String> recommendations;

  SocialMediaApp({
    required this.name,
    required this.packageName,
    required this.description,
    required this.icon,
    required this.recommendations,
  });

  factory SocialMediaApp.fromMap(Map<String, dynamic> map) {
    try {
      return SocialMediaApp(
        name: map['name']?.toString() ?? 'Unknown App',
        packageName: map['packageName']?.toString() ?? '',
        description:
            map['description']?.toString() ?? 'No description available',
        icon: map['icon'] as Uint8List,
        recommendations:
            _getRecommendations(map['packageName']?.toString() ?? ''),
      );
    } catch (e) {
      print('Error creating SocialMediaApp from map: $e');
      print('Map contents: $map');
      rethrow;
    }
  }

  static List<String> _getRecommendations(String packageName) {
    // Base recommendations for all apps
    final List<String> baseRecommendations = [
      'Review app permissions regularly',
      'Enable two-factor authentication if available',
      'Check privacy settings after each app update',
      'Be cautious when sharing location data',
    ];

    final Map<String, List<String>> specificRecommendations = {
      'com.instagram.android': [
        'Set account to private',
        'Disable activity status',
        'Review tagged photos before they appear',
        'Limit story visibility',
        'Control who can message you'
      ],
      'com.snapchat.android': [
        'Enable Ghost Mode for location',
        'Set story visibility to "Friends Only"',
        'Enable two-factor authentication',
        'Review who can contact you',
        'Disable Quick Add'
      ],
      'com.whatsapp': [
        'Review who can see your profile photo',
        'Control "Last Seen" visibility',
        'Review group privacy settings',
        'Enable two-step verification',
        'Disable live location sharing when not needed'
      ],
      'com.dropbox.android': [
        'Enable two-step verification',
        'Review shared folder permissions',
        'Check connected devices regularly',
        'Set up password protection for shared links',
        'Monitor account activity'
      ],
      'com.facebook.katana': [
        'Review tagged posts before they appear',
        'Set default post audience to "Friends"',
        'Limit past post visibility',
        'Control location history',
        'Review connected apps and websites'
      ],
      'com.zhiliaoapp.musically': [
        'Set account to private',
        'Disable "Allow others to find me"',
        'Restrict direct messages',
        'Review blocked accounts regularly',
        'Disable location features'
      ],
    };

    return [
      ...baseRecommendations,
      ...(specificRecommendations[packageName] ?? [])
    ];
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
          recommendations: List<String>.from(appData['recommendations']),
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
