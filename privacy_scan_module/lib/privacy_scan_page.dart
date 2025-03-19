import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:url_launcher/url_launcher.dart';
import 'app_state.dart';
import 'privacy_settings.dart';
import 'package:flutter/services.dart';

class PrivacyScanPage extends StatefulWidget {
  const PrivacyScanPage({super.key});

  @override
  State<PrivacyScanPage> createState() => _PrivacyScanPageState();
}

class _PrivacyScanPageState extends State<PrivacyScanPage> {
  static const platform =
      MethodChannel('com.htetznaing.adbotg/privacy_settings');
  List<SocialMediaApp> socialMediaApps = [];
  bool isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadApps();
  }

  Future<void> _loadApps() async {
    final appState = Provider.of<AppState>(context, listen: false);
    setState(() => isLoading = true);

    try {
      if (appState.selectedDevice == appState.sourceDeviceName) {
        // Load source device apps
        socialMediaApps = await SocialMediaApp.getInstalledApps(context, false);
      } else {
        // Load target device apps
        socialMediaApps = await SocialMediaApp.getInstalledApps(context, true);
      }
    } catch (e) {
      print('Error loading apps: $e');
    } finally {
      setState(() => isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final appState = Provider.of<AppState>(context);
    final isSourceDevice = appState.selectedDevice == appState.sourceDeviceName;

    // Add debug prints
    print('Selected device: ${appState.selectedDevice}');
    print('Source device name: ${appState.sourceDeviceName}');
    print('Is source device: $isSourceDevice');

    return Scaffold(
      appBar: AppBar(
        title: const Text('Privacy Scan'),
        actions: [
          // Only show dropdown if devices are connected via USB
          if (appState.isConnected) // Check for active USB connection
            Padding(
              padding: const EdgeInsets.only(right: 16.0),
              child: DropdownButton<String>(
                value: appState.selectedDevice,
                items: [
                  DropdownMenuItem(
                    value: appState.sourceDeviceName,
                    child: const Text('Source'),
                  ),
                  DropdownMenuItem(
                    value: 'Target',
                    child: const Text('Target'),
                  ),
                ],
                onChanged: (String? newValue) {
                  if (newValue != null) {
                    appState.setSelectedDevice(newValue);
                    _loadApps();
                  }
                },
              ),
            ),
        ],
      ),
      body: isLoading
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    if (isSourceDevice) ...[
                      const Text(
                        'Google Privacy Checkup',
                        style: TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 8),
                      const Text(
                        'Review and adjust your privacy settings across Google services',
                        style: TextStyle(fontSize: 16),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton(
                        onPressed: _launchGooglePrivacyCheckup,
                        child: const Text('Open Google Privacy Checkup'),
                      ),
                      const SizedBox(height: 24),
                      const Divider(),
                      const SizedBox(height: 16),
                    ],
                    const Text(
                      'Installed Social Media & Privacy-Sensitive Apps',
                      style: TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 16),
                    if (socialMediaApps.isEmpty)
                      const Text('No monitored apps found on this device')
                    else
                      ListView.builder(
                        shrinkWrap: true,
                        physics: const NeverScrollableScrollPhysics(),
                        itemCount: socialMediaApps.length,
                        itemBuilder: (context, index) =>
                            _buildAppCard(socialMediaApps[index]),
                      ),
                  ],
                ),
              ),
            ),
    );
  }

  Widget _buildAppCard(SocialMediaApp app) {
    return Card(
      margin: const EdgeInsets.only(bottom: 8.0),
      child: ListTile(
        leading: Image.memory(app.icon, width: 40, height: 40),
        title: Text(app.name),
        subtitle: Text(app.description),
        onTap: () => _showRecommendationsDialog(context, app),
      ),
    );
  }

  void _showRecommendationsDialog(BuildContext context, SocialMediaApp app) {
    final appState = Provider.of<AppState>(context, listen: false);
    final isSourceDevice = appState.selectedDevice == appState.sourceDeviceName;

    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('${app.name} Privacy Recommendations'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (isSourceDevice) ...[
                const Text(
                  'Active Permissions:',
                  style: TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 16,
                  ),
                ),
                const SizedBox(height: 8),
                Row(
                  children: _buildUniquePermissionIcons(app.permissions),
                ),
                const SizedBox(height: 16),
                const Divider(),
                const SizedBox(height: 16),
              ],
              const Text(
                'Recommendations:',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  fontSize: 18,
                ),
              ),
              const SizedBox(height: 12),
              ...app.getAllRecommendations().map((rec) {
                // Section headers (emoji headers)
                if (rec.startsWith('⚠️') ||
                    rec.startsWith('📱') ||
                    rec.startsWith('🔒')) {
                  return Padding(
                    padding: const EdgeInsets.only(top: 16.0, bottom: 8.0),
                    child: Text(
                      rec,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 16,
                        color: Colors.black87,
                      ),
                    ),
                  );
                }
                // Permission type headers (with emojis 📍, 📸, 🎤, 📁)
                else if (rec.startsWith('📍') ||
                    rec.startsWith('📸') ||
                    rec.startsWith('🎤') ||
                    rec.startsWith('📁')) {
                  return Padding(
                    padding: const EdgeInsets.only(top: 8.0, bottom: 4.0),
                    child: Text(
                      rec,
                      style: const TextStyle(
                        fontWeight: FontWeight.w600,
                        fontSize: 15,
                        color: Colors.black87,
                      ),
                    ),
                  );
                }
                // Empty spacing lines
                else if (rec.isEmpty) {
                  return const SizedBox(height: 8.0);
                }
                // Regular recommendation items
                else {
                  return Padding(
                    padding: EdgeInsets.only(
                      left: rec.startsWith('  •') ? 32.0 : 16.0,
                      top: 2.0,
                      bottom: 2.0,
                    ),
                    child: Text(
                      rec,
                      style: TextStyle(
                        fontSize: 14,
                        color: rec.startsWith('  •')
                            ? Colors.black54
                            : Colors.black87,
                        height: 1.4,
                      ),
                    ),
                  );
                }
              }).toList(),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Close'),
          ),
          if (isSourceDevice)
            TextButton(
              onPressed: () => _openAppSettings(app.packageName),
              child: const Text('Open Settings'),
            ),
        ],
      ),
    );
  }

  List<Widget> _buildUniquePermissionIcons(List<dynamic> permissions) {
    Set<String> uniquePermissions = Set<String>();
    List<Widget> uniqueIcons = [];

    for (var perm in permissions) {
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

      if (permType.isNotEmpty && !uniquePermissions.contains(permType)) {
        uniquePermissions.add(permType);
        IconData iconData;
        String tooltip;
        switch (permType) {
          case 'location':
            iconData = Icons.location_on;
            tooltip = 'Location Access';
            break;
          case 'camera':
            iconData = Icons.camera_alt;
            tooltip = 'Camera Access';
            break;
          case 'microphone':
            iconData = Icons.mic;
            tooltip = 'Microphone Access';
            break;
          case 'storage':
            iconData = Icons.folder;
            tooltip = 'Storage Access';
            break;
          default:
            iconData = Icons.security;
            tooltip = 'Other Permission';
            break;
        }
        uniqueIcons.add(
          Tooltip(
            message: tooltip,
            child: Padding(
              padding: const EdgeInsets.only(right: 8.0),
              child: Icon(
                iconData,
                size: 24.0,
                color: Colors.grey[700],
              ),
            ),
          ),
        );
      }
    }

    return uniqueIcons;
  }

  Future<void> _launchGooglePrivacyCheckup() async {
    final Uri url = Uri.parse('https://myaccount.google.com/privacycheckup');
    if (!await launchUrl(url)) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not open Privacy Checkup')),
        );
      }
    }
  }

  Future<void> _openAppSettings(String packageName) async {
    try {
      await platform.invokeMethod('openAppSettings', {'package': packageName});
    } catch (e) {
      print('Error opening app settings: $e');
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Could not open app settings')),
        );
      }
    }
  }
}
