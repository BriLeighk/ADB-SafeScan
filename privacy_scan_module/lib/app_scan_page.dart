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

class PermissionIcon {
  final String permission;
  PermissionIcon({required this.permission});

  IconData getIcon() {
    switch (permission) {
      case "location":
        return Icons.location_on;
      case "camera":
        return Icons.camera_alt;
      case "microphone":
        return Icons.mic;
      case "storage":
        return Icons.folder;
      default:
        return Icons.security;
    }
  }
}

class AppScanPage extends StatefulWidget {
  final bool scanTarget;

  const AppScanPage({super.key, required this.scanTarget});

  @override
  State<AppScanPage> createState() => _AppScanPageState();
}

class _AppScanPageState extends State<AppScanPage> {
  static const platform = MethodChannel('samples.flutter.dev/spyware');
  static const settingsChannel = MethodChannel('com.example.spyware/settings');
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

  List<Widget> _buildPermissionsInfo() {
    return _permissionsInfo.map((PermissionInfo info) {
      return ExpansionTile(
        leading: Icon(info.icon),
        title: Text(info.name),
        children: <Widget>[
          ListTile(
            title: Text(info.description),
          ),
        ],
      );
    }).toList();
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
    if (installer != 'com.android.vending') {
      return const Color.fromARGB(255, 255, 177, 177);
    } else {
      if (type == 'offstore') {
        return const Color.fromARGB(255, 255, 177, 177);
      } else if (type == 'spyware' || type == 'Unknown') {
        return const Color.fromARGB(255, 255, 255, 173);
      } else if (type == 'dual-use') {
        return const Color.fromARGB(255, 175, 230, 255);
      } else {
        return Colors.grey;
      }
    }
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
            child: Column(
              children: _buildPermissionsInfo(),
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
                          List<PermissionIcon> permissions =
                              (app['permissions'] as List<dynamic>? ?? [])
                                  .map((perm) {
                            return PermissionIcon(
                              permission: perm['icon'],
                            );
                          }).toList();
                          return TextButton(
                              onPressed: () async {
                                await _openAppSettings(app['id']);
                              },
                              child: Container(
                                margin: const EdgeInsets.all(.1),
                                decoration: BoxDecoration(
                                  color: baseColor,
                                  borderRadius: BorderRadius.circular(10.0),
                                ),
                                child: ListTile(
                                    tileColor: Colors.transparent,
                                    leading: app['icon'] != null
                                        ? Image.memory(base64Decode(
                                            app['icon']?.trim() ?? ''))
                                        : null,
                                    title: RichText(
                                      text: TextSpan(
                                        style:
                                            DefaultTextStyle.of(context).style,
                                        children: <TextSpan>[
                                          TextSpan(
                                            text:
                                                '${app['name'] ?? 'Unknown Name'}  ',
                                            style: const TextStyle(
                                                fontWeight: FontWeight.bold),
                                          ),
                                          TextSpan(
                                            text:
                                                '(${app['id'] ?? 'Unknown ID'})',
                                          ),
                                        ],
                                      ),
                                    ),
                                    trailing: secureInstallers
                                            .contains(app['installer'])
                                        ? IconButton(
                                            icon: const Icon(Icons.open_in_new),
                                            onPressed: () =>
                                                _launchURL(app['storeLink']),
                                          )
                                        : null,
                                    subtitle: Row(
                                      children: permissions.map((permIcon) {
                                        return Icon(permIcon.getIcon(),
                                            size: 18.0);
                                      }).toList(),
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
