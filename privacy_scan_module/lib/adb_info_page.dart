import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import 'app_state.dart';

class AdbInfoPage extends StatelessWidget {
  static const platform = MethodChannel('com.htetznaing.adbotg/main_activity');

  const AdbInfoPage({super.key});

  @override
  Widget build(BuildContext context) {
    final appState = Provider.of<AppState>(context);

    return Scaffold(
      appBar: AppBar(
        title: const Text('ADB Connection Guide'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
        actions: [
          // Retry connection button
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
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(16.0),
              decoration: BoxDecoration(
                color:
                    appState.isConnected ? Colors.green[100] : Colors.red[100],
                borderRadius: BorderRadius.circular(8.0),
              ),
              child: Row(
                children: [
                  Icon(
                    appState.isConnected ? Icons.check_circle : Icons.error,
                    color: appState.isConnected ? Colors.green : Colors.red,
                  ),
                  const SizedBox(width: 8.0),
                  Text(
                    'Status: ${appState.isConnected ? "Connected" : "Disconnected"}',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),
            Text(
              'How to Connect Devices',
              style: Theme.of(context).textTheme.headlineSmall,
            ),
            const SizedBox(height: 16),
            _buildStep(
              context,
              '1. Enable Developer Options',
              [
                '• Go to Settings > About Phone',
                '• Find "Build Number"',
                '• Tap it 7 times',
                '• Enter your device PIN if prompted'
              ],
            ),
            _buildStep(
              context,
              '2. Enable USB Debugging',
              [
                '• Go to Settings > System > Developer Options',
                '• Find "USB Debugging"',
                '• Toggle it ON',
              ],
            ),
            _buildStep(
              context,
              '3. Connect Devices',
              [
                '• Connect target device to this device using USB OTG cable',
                '• Accept USB debugging prompt on target device',
                '• Use retry button above if needed',
              ],
            ),
            const SizedBox(height: 24),
            if (!appState.isConnected)
              Container(
                padding: const EdgeInsets.all(16.0),
                decoration: BoxDecoration(
                  color: Colors.yellow[100],
                  borderRadius: BorderRadius.circular(8.0),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.info_outline, color: Colors.orange),
                    const SizedBox(width: 8.0),
                    Expanded(
                      child: Text(
                        'If connection fails, try disconnecting and reconnecting the USB cable, '
                        'then use the retry button above.',
                        style: Theme.of(context).textTheme.bodyLarge,
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }

  Widget _buildStep(BuildContext context, String title, List<String> steps) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          const SizedBox(height: 8.0),
          ...steps.map((step) => Padding(
                padding: const EdgeInsets.only(left: 16.0, bottom: 4.0),
                child: Text(step),
              )),
        ],
      ),
    );
  }
}
