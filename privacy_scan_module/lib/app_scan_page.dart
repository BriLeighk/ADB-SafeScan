import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'app_state.dart';
import 'csv_utils.dart';
import 'package:url_launcher/url_launcher.dart';

class PermissionInfo {
  final String name;
  final IconData icon;
  final String description;

  PermissionInfo(
      {required this.name, required this.icon, required this.description});
}

class AppScanPage extends StatefulWidget {
  final bool scanTarget;

  const AppScanPage({super.key, required this.scanTarget});

  @override
  State<AppScanPage> createState() => _AppScanPageState();
}

class _AppScanPageState extends State<AppScanPage> {
  static const platform = MethodChannel('samples.flutter.dev/spyware');
  static const settingsChannel =
      MethodChannel('com.htetznaing.adbotg/privacy_settings');
  bool _searchPerformed = false;
  bool _isLoading = false;
  List<Map<String, dynamic>> _spywareApps = [];

  final List<PermissionInfo> _permissionsInfo = [
    PermissionInfo(
      name: 'Location Sharing',
      icon: Icons.location_on,
      description:
          'Grants access to your active location. While essential for navigation and weather apps, it can serve details such as your home address, routes you\'ve taken, and other sensitive information to the apps that enable it. It is best to stay cautious and only enable location sharing if absolutely necessary.',
    ),
    PermissionInfo(
      name: 'Camera',
      icon: Icons.camera_alt,
      description:
          'Grants access to your camera for taking photos and videos. Ensure that each app that enables it has a use for it, such as social media, photography, and editing apps. Other apps, such as music and audio apps, should not require camera enabling.',
    ),
    PermissionInfo(
      name: 'Microphone',
      icon: Icons.mic,
      description:
          'Grants access to your microphone for recording audio. Audio is a powerful tool, and if given to a non-trusted application, can be used to record confidential information. Ensure it is only enabled for trustworthy apps that have a use for it.',
    ),
    PermissionInfo(
      name: 'Files and Media',
      icon: Icons.folder,
      description:
          'Grants access to photo galleries and file managers on the device. By giving apps access to storage, any sensitive information contained on the device can be accessed. It is best to be weary of what apps have this permission enabled, and keep sensitive information stored in an encrypted cloud, instead of on the device.',
    ),
  ];

  Future<void> _getSpywareApps() async {
    setState(() {
      _isLoading = true;
    });

    try {
      List<List<dynamic>> remoteCSVData = await fetchCSVData();
      final List<dynamic> result;

      if (!mounted) return;

      if (Provider.of<AppState>(context, listen: false).isConnected) {
        if (Provider.of<AppState>(context, listen: false).selectedDevice ==
            'Target') {
          result = await platform.invokeMethod(
              'getSpywareAppsFromTarget', {"csvData": remoteCSVData});
        } else {
          result = await platform
              .invokeMethod('getSpywareApps', {"csvData": remoteCSVData});
        }
      } else {
        result = await platform
            .invokeMethod('getSpywareApps', {"csvData": remoteCSVData});
      }

      print('Received Spyware Apps: $result');
      List<Map<String, dynamic>> spywareApps = result.map((app) {
        return Map<String, dynamic>.from(app);
      }).toList();

      spywareApps.sort((a, b) => _getSortWeight(a['type'], a['installer'])
          .compareTo(_getSortWeight(b['type'], b['installer'])));

      setState(() {
        _spywareApps = spywareApps;
        _isLoading = false;
        _searchPerformed = true;
      });
    } catch (e) {
      setState(() {
        _isLoading = false;
        _searchPerformed = true;
      });
      print('Error fetching spyware apps: $e');
    }
  }

  List<Widget> _buildPermissionInfoIcons(BuildContext context) {
    return _permissionsInfo.map((info) {
      return Padding(
        padding: const EdgeInsets.symmetric(horizontal: 8.0),
        child: Column(
          children: [
            IconButton(
              icon: Icon(
                info.icon,
                color: Colors.grey[700],
                size: 24.0,
              ),
              onPressed: () => _showPermissionInfo(context, info),
              tooltip: info.name,
            ),
            Text(
              info.name.split(' ')[0], // Just show first word
              style: const TextStyle(fontSize: 12),
            ),
          ],
        ),
      );
    }).toList();
  }

  void _showPermissionInfo(BuildContext context, PermissionInfo info) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          title: Row(
            children: [
              Icon(info.icon, color: Colors.grey[700]),
              const SizedBox(width: 8.0),
              Text(info.name),
            ],
          ),
          content: Text(info.description),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Close'),
            ),
          ],
        );
      },
    );
  }

  Future<void> _openAppSettings(String package) async {
    try {
      await settingsChannel
          .invokeMethod('openAppSettings', {'package': package});
    } catch (e) {
      // Handle error
      print('Failed to open app settings: $e');
    }
  }

  Color lightColor(Map<String, dynamic> app, String installer, String type) {
    // For target device apps, use the type directly
    if (app['isTargetDevice'] == true) {
      switch (app['type'].toString().toLowerCase()) {
        case 'dual-use':
          return const Color.fromARGB(255, 175, 230, 255); // blue
        case 'spyware':
          return const Color.fromARGB(255, 255, 255, 173); // yellow
        default:
          return const Color.fromARGB(255, 255, 177, 177); // red
      }
    }

    // For source device apps, use the existing installer-based logic
    if (installer.contains('com.android.vending') ||
        installer.contains('com.google.android.packageinstaller') ||
        installer.contains('com.samsung.android.app.store')) {
      switch (type.toLowerCase()) {
        case 'dual-use':
          return const Color.fromARGB(255, 175, 230, 255); // blue
        case 'spyware':
          return const Color.fromARGB(255, 255, 255, 173); // yellow
        default:
          return const Color.fromARGB(255, 255, 177, 177); // red
      }
    }
    return const Color.fromARGB(
        255, 255, 177, 177); // red for unknown/unsecure installers
  }

  @override
  Widget build(BuildContext context) {
    final appState = Provider.of<AppState>(context);
    const List<String> secureInstallers = [
      'com.android.vending',
      'com.amazon.venezia',
    ];

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Row(
          children: [
            Text(widget.scanTarget ? 'App Scan' : 'Target App Scan'),
            const SizedBox(width: 8.0),
            if (appState.isConnected)
              DropdownButton<String>(
                value: appState.selectedDevice,
                items: [
                  DropdownMenuItem(
                    value: appState.sourceDeviceName,
                    child: Text(appState.sourceDeviceName),
                  ),
                  DropdownMenuItem(
                    value: appState.targetDeviceName,
                    child: Text(appState.targetDeviceName),
                  ),
                ],
                onChanged: (String? newValue) {
                  if (newValue != null) {
                    appState.setSelectedDevice(newValue);
                  }
                },
              ),
          ],
        ),
        actions: <Widget>[
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () {
              setState(() {
                _spywareApps.clear();
                _searchPerformed = false;
                _isLoading = false;
              });
            },
          ),
        ],
      ),
      body: Column(
        children: <Widget>[
          if (Provider.of<AppState>(context).selectedDevice == 'Target')
            Container(
              padding: const EdgeInsets.all(8.0),
              margin: const EdgeInsets.all(8.0),
              decoration: BoxDecoration(
                color: Colors.yellow[100],
                borderRadius: BorderRadius.circular(8.0),
                border: Border.all(color: Colors.yellow[700]!),
              ),
              child: const Row(
                children: [
                  Icon(Icons.info_outline, color: Colors.orange),
                  SizedBox(width: 8.0),
                  Expanded(
                    child: Text(
                      'Note: For apps on the target device, we cannot verify the installation source. Apps are classified based on known data about their security risks.',
                      style: TextStyle(fontSize: 12),
                    ),
                  ),
                ],
              ),
            ),
          const Padding(
            padding: EdgeInsets.all(4.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(" Color Key: ",
                    style:
                        TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                Text("Dual-use  ",
                    style: TextStyle(
                        backgroundColor: Color.fromARGB(255, 175, 230, 255),
                        fontWeight: FontWeight.bold)),
                Text("Spyware  ",
                    style: TextStyle(
                        backgroundColor: Color.fromARGB(255, 255, 255, 173),
                        fontWeight: FontWeight.bold)),
                Text("Unsecure Download ",
                    style: TextStyle(
                        backgroundColor: Color.fromARGB(255, 255, 177, 177),
                        fontWeight: FontWeight.bold)),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.all(8.0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: _buildPermissionInfoIcons(context),
            ),
          ),
          Expanded(
            child: _isLoading
                ? const Center(child: CircularProgressIndicator())
                : _spywareApps.isEmpty && _searchPerformed
                    ? const Center(
                        child: Text("No spyware apps detected on your device"))
                    : ListView.builder(
                        itemCount: _spywareApps.length,
                        itemBuilder: (context, index) {
                          var app = _spywareApps[index];
                          Color baseColor =
                              lightColor(app, app['installer'], app['type']);
                          return TextButton(
                              onPressed: () async {
                                final appState = Provider.of<AppState>(context,
                                    listen: false);
                                if (appState.selectedDevice ==
                                    appState.sourceDeviceName) {
                                  try {
                                    await settingsChannel.invokeMethod(
                                        'openAppSettings',
                                        {'package': app['id']});
                                  } catch (e) {
                                    print('Error opening app settings: $e');
                                    if (context.mounted) {
                                      ScaffoldMessenger.of(context)
                                          .showSnackBar(
                                        const SnackBar(
                                            content: Text(
                                                'Could not open app settings')),
                                      );
                                    }
                                  }
                                }
                              },
                              child: Container(
                                margin: const EdgeInsets.all(.1),
                                decoration: BoxDecoration(
                                  color: baseColor,
                                  borderRadius: BorderRadius.circular(10.0),
                                ),
                                child: ListTile(
                                    tileColor: Colors.transparent,
                                    leading: app['isTargetDevice'] == true
                                        ? app['iconUrl'] != null
                                            ? Image.network(
                                                app['iconUrl'],
                                                width: 40,
                                                height: 40,
                                                loadingBuilder: (context, child,
                                                    loadingProgress) {
                                                  if (loadingProgress == null)
                                                    return child;
                                                  return const SizedBox(
                                                    width: 40,
                                                    height: 40,
                                                    child: Center(
                                                      child:
                                                          CircularProgressIndicator(
                                                        strokeWidth: 2.0,
                                                      ),
                                                    ),
                                                  );
                                                },
                                                errorBuilder: (context, error,
                                                    stackTrace) {
                                                  print(
                                                      'Primary icon load failed for ${app['id']}: $error');

                                                  // Try backup URL if available
                                                  if (app['backupIconUrl'] !=
                                                      null) {
                                                    return Image.network(
                                                      app['backupIconUrl'],
                                                      width: 40,
                                                      height: 40,
                                                      errorBuilder: (context,
                                                          error, stackTrace) {
                                                        print(
                                                            'Backup icon load failed for ${app['id']}: $error');

                                                        // Final fallback - use direct CDN URL
                                                        final fallbackUrl =
                                                            'https://play-lh.googleusercontent.com/icon?id=${app['id']}&w=48';
                                                        return Image.network(
                                                          fallbackUrl,
                                                          width: 40,
                                                          height: 40,
                                                          errorBuilder:
                                                              (context, error,
                                                                  stackTrace) {
                                                            return Container(
                                                              width: 40,
                                                              height: 40,
                                                              decoration:
                                                                  BoxDecoration(
                                                                color: Colors
                                                                    .grey[200],
                                                                borderRadius:
                                                                    BorderRadius
                                                                        .circular(
                                                                            8),
                                                              ),
                                                              child: const Icon(
                                                                  Icons.android,
                                                                  color: Colors
                                                                      .grey),
                                                            );
                                                          },
                                                        );
                                                      },
                                                    );
                                                  }
                                                  return Container(
                                                    width: 40,
                                                    height: 40,
                                                    decoration: BoxDecoration(
                                                      color: Colors.grey[200],
                                                      borderRadius:
                                                          BorderRadius.circular(
                                                              8),
                                                    ),
                                                    child: const Icon(
                                                        Icons.android,
                                                        color: Colors.grey),
                                                  );
                                                },
                                              )
                                            : const Icon(Icons.android)
                                        : app['icon'] != null
                                            ? Image.memory(base64Decode(
                                                app['icon']?.trim() ?? ''))
                                            : const Icon(Icons.android),
                                    title: Text(
                                      app['name'] ?? 'Unknown Name',
                                      style: const TextStyle(
                                          fontWeight: FontWeight.bold),
                                    ),
                                    trailing: secureInstallers
                                            .contains(app['installer'])
                                        ? IconButton(
                                            icon: const Icon(Icons.open_in_new),
                                            onPressed: () =>
                                                _launchURL(app['storeLink']),
                                          )
                                        : null,
                                    subtitle: Builder(
                                      builder: (context) {
                                        print('App: ${app['name']}');
                                        print(
                                            'Permissions: ${app['permissions']}');
                                        return Row(
                                          children: _buildUniquePermissionIcons(
                                              app['permissions']
                                                      as List<dynamic>? ??
                                                  []),
                                        );
                                      },
                                    )),
                              ));
                        },
                      ),
          ),
        ],
      ),
      bottomNavigationBar: Padding(
        padding: const EdgeInsets.all(8.0),
        child: ElevatedButton(
          onPressed: () async {
            await _getSpywareApps();
          },
          style: ElevatedButton.styleFrom(
            foregroundColor: Theme.of(context).colorScheme.onPrimary,
            backgroundColor: Theme.of(context).colorScheme.primary,
          ),
          child: const Text('List Detected Spyware Applications'),
        ),
      ),
    );
  }
}

Future<void> _launchURL(String? urlString) async {
  if (urlString != null) {
    final Uri url = Uri.parse(urlString);
    if (await canLaunchUrl(url)) {
      await launchUrl(url);
    } else {
      // Handle error
    }
  }
}

int _getSortWeight(String type, String installer) {
  if (installer != 'com.android.vending' && installer != 'com.amazon.venezia') {
    return 1;
  } else {
    if (type == 'offstore') {
      return 2;
    } else if (type == 'spyware' || type == 'Unknown') {
      return 3;
    } else if (type == 'dual-use') {
      return 4;
    } else {
      return 5;
    }
  }
}

Color _getColorForType(String type) {
  switch (type.toUpperCase()) {
    case 'SPYWARE':
      return Colors.yellow;
    case 'DUAL_USE':
      return Colors.blue;
    case 'OFFSTORE':
      return Colors.red;
    default:
      return Colors.grey;
  }
}

List<Widget> _buildUniquePermissionIcons(List<dynamic> permissions) {
  print('Building icons for permissions: $permissions');
  Set<String> uniquePermissions = Set<String>();
  List<Widget> uniqueIcons = [];

  for (var perm in permissions) {
    print('Processing permission: $perm');
    // Handle both string permissions and map permissions
    String permType = '';
    if (perm is String) {
      permType = perm;
    } else if (perm is Map) {
      permType =
          perm['permission']?.toString().split('.').last.toLowerCase() ?? '';
      if (permType.contains('location'))
        permType = 'location';
      else if (permType.contains('camera'))
        permType = 'camera';
      else if (permType.contains('audio') || permType.contains('microphone'))
        permType = 'microphone';
      else if (permType.contains('storage') || permType.contains('media'))
        permType = 'storage';
    }

    print('Extracted permission type: $permType');

    if (permType.isNotEmpty && !uniquePermissions.contains(permType)) {
      uniquePermissions.add(permType);
      IconData iconData;
      switch (permType) {
        case 'location':
          iconData = Icons.location_on;
          break;
        case 'camera':
          iconData = Icons.camera_alt;
          break;
        case 'microphone':
          iconData = Icons.mic;
          break;
        case 'storage':
          iconData = Icons.folder;
          break;
        default:
          print('Unknown permission type: $permType');
          iconData = Icons.security;
          break;
      }
      uniqueIcons.add(
        Padding(
          padding: const EdgeInsets.only(right: 8.0),
          child: Icon(
            iconData,
            size: 20.0,
            color: Colors.grey[700],
          ),
        ),
      );
    }
  }

  print('Generated unique icons: ${uniqueIcons.length}');
  return uniqueIcons;
}

Widget _buildAppIcon(Map<String, dynamic> app) {
  if (app['iconUrl'] == null) return const Icon(Icons.android);

  return FadeInImage.assetNetwork(
    placeholder:
        'assets/app_icon_placeholder.png', // You'll need to add this asset
    image: app['iconUrl'],
    width: 40,
    height: 40,
    imageErrorBuilder: (context, error, stackTrace) {
      print('Primary icon load failed for ${app['id']}: $error');

      // Try backup URL if available
      if (app['backupIconUrl'] != null) {
        return FadeInImage.assetNetwork(
          placeholder: 'assets/app_icon_placeholder.png',
          image: app['backupIconUrl'],
          width: 40,
          height: 40,
          imageErrorBuilder: (context, error, stackTrace) {
            print('Backup icon load failed for ${app['id']}: $error');

            // Try Play Store URL as final fallback
            final playStoreUrl =
                'https://play-lh.googleusercontent.com/icon?id=${app['id']}&w=96';
            return Image.network(
              playStoreUrl,
              width: 40,
              height: 40,
              errorBuilder: (context, error, stackTrace) {
                return Container(
                  width: 40,
                  height: 40,
                  decoration: BoxDecoration(
                    color: Colors.grey[200],
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: const Icon(Icons.android, color: Colors.grey),
                );
              },
            );
          },
        );
      }
      return const SizedBox(
        width: 40,
        height: 40,
        child: Center(
          child: CircularProgressIndicator(
            strokeWidth: 2.0,
          ),
        ),
      );
    },
  );
}
