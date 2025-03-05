import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class AppState extends ChangeNotifier {
  bool _isConnected = false;

  // Use private static constants
  static const _kSourceDevice = 'Source';
  static const _kTargetDevice = 'Target';
  static const platform = MethodChannel('com.htetznaing.adbotg/usb_receiver');

  // Initialize with Source as default
  String _selectedDevice = _kSourceDevice;

  AppState() {
    _initUsbConnectionListener();
  }

  bool get isConnected => _isConnected;
  String get selectedDevice => _selectedDevice;
  String get sourceDeviceName => _kSourceDevice;
  String get targetDeviceName => _kTargetDevice;

  void _initUsbConnectionListener() {
    platform.setMethodCallHandler((call) async {
      if (call.method == 'usbConnected') {
        _setConnectedState(true);
        setSelectedDevice(
            _kTargetDevice); // Set default to target when connected
      } else if (call.method == 'usbDisconnected') {
        _setConnectedState(false);
        setSelectedDevice(
            _kSourceDevice); // Default to source when disconnected
      }
    });
  }

  void _setConnectedState(bool isConnected) {
    if (_isConnected != isConnected) {
      _isConnected = isConnected;
      notifyListeners();
    }
  }

  void setSelectedDevice(String device) {
    if (device != _selectedDevice) {
      _selectedDevice = device;
      notifyListeners();
    }
  }

  // Optional: Reset to source device when no target is connected
  void resetToSource() {
    setSelectedDevice(_kSourceDevice);
  }
}
