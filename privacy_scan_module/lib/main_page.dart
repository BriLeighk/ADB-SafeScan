import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'app_state.dart';
import 'app_scan_page.dart';
import 'privacy_scan_page.dart';
import 'package:flutter/services.dart';
import 'adb_info_page.dart';

class MainPage extends StatefulWidget {
  const MainPage({super.key, required this.title});

  final String title;

  @override
  State<MainPage> createState() => _MainPageState();
}

class _MainPageState extends State<MainPage> {
  static const platform = MethodChannel('com.htetznaing.adbotg/main_activity');

  Future<void> _openMainActivity() async {
    try {
      await platform.invokeMethod('openMainActivity');
    } on PlatformException catch (e) {
      print("Failed to open MainActivity: '${e.message}'.");
    }
  }

  void _openAdbInfo() {
    Navigator.push(
      context,
      MaterialPageRoute(builder: (context) => const AdbInfoPage()),
    );
  }

  @override
  Widget build(BuildContext context) {
    final appState = Provider.of<AppState>(context);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        title: Row(
          children: [
            Text(widget.title),
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
        actions: [
          IconButton(
            icon: Icon(
              appState.isConnected ? Icons.usb : Icons.usb_off,
              color: appState.isConnected ? Colors.green : Colors.red,
            ),
            onPressed: _openAdbInfo,
            tooltip: 'ADB Connection Info',
          ),
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () async {
              try {
                await platform.invokeMethod('retryConnection');
              } catch (e) {
                print("Failed to retry connection: $e");
              }
            },
            tooltip: 'Retry Connection',
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.start,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: <Widget>[
                const SizedBox(height: 40),
                const Padding(
                  padding:
                      EdgeInsets.symmetric(vertical: 16.0, horizontal: 8.0),
                  child: Text(
                    'SafeScan',
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                ),
                const Padding(
                  padding:
                      EdgeInsets.symmetric(vertical: 16.0, horizontal: 8.0),
                  child: Text(
                    'SafeScan is here to help ensure your digital privacy. This app '
                    'gently checks for any potentially harmful apps on your device '
                    'and guides you on how to best adjust your privacy settings.\n'
                    '\nPlease select an option below to begin:',
                    style: TextStyle(fontSize: 18),
                    textAlign: TextAlign.center,
                  ),
                ),
                const Padding(
                  padding: EdgeInsets.only(top: 0, bottom: 50),
                  child: Divider(
                    height: 5,
                    thickness: 2,
                    indent: 20,
                    endIndent: 20,
                  ),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const AppScanPage(scanTarget: true),
                    ),
                  ),
                  child: const Text('Perform App Scan'),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) => const PrivacyScanPage(),
                    ),
                  ),
                  child: const Text('Perform Privacy Scan'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
