import 'dart:math' as math;
import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

void main() {
  runApp(const SmartNamazApp());
}

// ==========================================
// 1. DATA MODELS & CALCULATION ENGINE
// ==========================================

class City {
  final String id;
  final String name;
  final double lat;
  final double lng;
  final double timeZoneOffset;

  const City({
    required this.id,
    required this.name,
    required this.lat,
    required this.lng,
    this.timeZoneOffset = 5.0,
  });
}

class PrayerTimes {
  final City city;
  final DateTime date;
  final DateTime fajr;
  final DateTime sunrise;
  final DateTime dhuhr;
  final DateTime asr;
  final DateTime maghrib;
  final DateTime isha;

  PrayerTimes({
    required this.city,
    required this.date,
    required this.fajr,
    required this.sunrise,
    required this.dhuhr,
    required this.asr,
    required this.maghrib,
    required this.isha,
  });
}

class CustomAlarm {
  final String id;
  final String title;
  final int hour;
  final int minute;
  final int durationMinutes;
  final bool isEnabled;

  CustomAlarm({
    required this.id,
    required this.title,
    required this.hour,
    required this.minute,
    required this.durationMinutes,
    this.isEnabled = true,
  });

  CustomAlarm copyWith({
    String? id,
    String? title,
    int? hour,
    int? minute,
    int? durationMinutes,
    bool? isEnabled,
  }) {
    return CustomAlarm(
      id: id ?? this.id,
      title: title ?? this.title,
      hour: hour ?? this.hour,
      minute: minute ?? this.minute,
      durationMinutes: durationMinutes ?? this.durationMinutes,
      isEnabled: isEnabled ?? this.isEnabled,
    );
  }
}

class PrayerCalculator {
  static const List<City> punjabCities = [
    City(id: 'multan', name: 'Multan', lat: 30.1575, lng: 71.5249),
    City(id: 'lahore', name: 'Lahore', lat: 31.5204, lng: 74.3587),
    City(id: 'gujrat', name: 'Gujrat', lat: 32.5742, lng: 74.0754),
    City(id: 'lalamusa', name: 'Lalamusa', lat: 32.7011, lng: 73.9592),
    City(id: 'bahawalpur', name: 'Bahawalpur', lat: 29.3956, lng: 71.6836),
    City(id: 'rawalpindi', name: 'Rawalpindi', lat: 33.5651, lng: 73.0169),
    City(id: 'faisalabad', name: 'Faisalabad', lat: 31.4504, lng: 73.1350),
    City(id: 'sialkot', name: 'Sialkot', lat: 32.4945, lng: 74.5229),
    City(id: 'sargodha', name: 'Sargodha', lat: 32.0836, lng: 72.6711),
    City(id: 'sheikhupura', name: 'Sheikhupura', lat: 31.7167, lng: 73.9850),
    City(id: 'rahim_yar_khan', name: 'Rahim Yar Khan', lat: 28.4212, lng: 70.2989),
    City(id: 'gujranwala', name: 'Gujranwala', lat: 32.1877, lng: 74.1945),
    City(id: 'jhelum', name: 'Jhelum', lat: 32.9405, lng: 73.7276),
    City(id: 'attock', name: 'Attock', lat: 33.7680, lng: 72.3607),
    City(id: 'dg_khan', name: 'Dera Ghazi Khan', lat: 30.0561, lng: 70.6348),
    City(id: 'sahiwal', name: 'Sahiwal', lat: 30.6682, lng: 73.1014),
  ];

  static PrayerTimes calculate(City city, DateTime date) {
    final double lat = city.lat;
    final double lng = city.lng;
    final double tz = city.timeZoneOffset;

    final int dayOfYear = _getDayOfYear(date);
    final double gamma = 2 * math.pi / 365 * (dayOfYear - 1 + (12 - 12) / 24);
    
    final double eqtime = 229.18 * (0.000075 + 0.001868 * math.cos(gamma) - 0.032077 * math.sin(gamma) - 0.014615 * math.cos(2 * gamma) - 0.040849 * math.sin(2 * gamma));
    final double decl = 0.006918 - 0.399912 * math.cos(gamma) + 0.070257 * math.sin(gamma) - 0.006758 * math.cos(2 * gamma) + 0.000907 * math.sin(2 * gamma) - 0.002697 * math.cos(3 * gamma) + 0.00148 * math.sin(3 * gamma);

    final double dhuhrMinutes = 720 - 4 * lng - eqtime + tz * 60 + 2.0; // +2 min buffer

    final double latRad = _degreesToRadians(lat);
    
    // Karachi Fajr Angle: 18 deg
    final double fajrHA = _calculateHourAngle(latRad, decl, -18.0);
    // Sunrise Angle: -0.833 deg
    final double sunriseHA = _calculateHourAngle(latRad, decl, -0.833);
    // Hanafi Asr (shadow factor = 2 + tan(|lat - decl|))
    final double asrAngleRad = math.atan(1.0 / (2.0 + math.tan((latRad - decl).abs())));
    final double asrHA = _calculateHourAngle(latRad, decl, _radiansToDegrees(asrAngleRad));
    // Maghrib Angle: -0.833 deg
    final double maghribHA = _calculateHourAngle(latRad, decl, -0.833);
    // Karachi Isha Angle: 18 deg
    final double ishaHA = _calculateHourAngle(latRad, decl, -18.0);

    final DateTime fajr = _minutesToDateTime(date, dhuhrMinutes - fajrHA * 4);
    final DateTime sunrise = _minutesToDateTime(date, dhuhrMinutes - sunriseHA * 4);
    final DateTime dhuhr = _minutesToDateTime(date, dhuhrMinutes);
    final DateTime asr = _minutesToDateTime(date, dhuhrMinutes + asrHA * 4);
    final DateTime maghrib = _minutesToDateTime(date, dhuhrMinutes + maghribHA * 4);
    final DateTime isha = _minutesToDateTime(date, dhuhrMinutes + ishaHA * 4);

    return PrayerTimes(
      city: city,
      date: date,
      fajr: fajr,
      sunrise: sunrise,
      dhuhr: dhuhr,
      asr: asr,
      maghrib: maghrib,
      isha: isha,
    );
  }

  static double _calculateHourAngle(double latRad, double decl, double angleDeg) {
    final double angleRad = _degreesToRadians(angleDeg);
    final double cosHA = (math.sin(angleRad) - math.sin(latRad) * math.sin(decl)) / (math.cos(latRad) * math.cos(decl));
    final double clampedCosHA = cosHA.clamp(-1.0, 1.0);
    return _radiansToDegrees(math.acos(clampedCosHA));
  }

  static double _degreesToRadians(double deg) => deg * math.pi / 180.0;
  static double _radiansToDegrees(double rad) => rad * 180.0 / math.pi;

  static int _getDayOfYear(DateTime date) {
    final DateTime diff = date.difference(DateTime(date.year, 1, 1));
    return diff.inDays + 1;
  }

  static DateTime _minutesToDateTime(DateTime baseDate, double minutesFromMidnight) {
    int totalMinutes = minutesFromMidnight.round();
    int hour = (totalMinutes ~/ 60) % 24;
    int minute = totalMinutes % 60;
    return DateTime(baseDate.year, baseDate.month, baseDate.day, hour, minute);
  }
}

// ==========================================
// 2. STATE MANAGEMENT & NOTIFIER
// ==========================================

class NamazAppState extends ChangeNotifier {
  City _selectedCity = PrayerCalculator.punjabCities.first; // Default: Multan
  bool _isManualSilent = false;
  bool _isJummahModeEnabled = true;
  int _jummahDurationMinutes = 90;
  bool _isEidModeEnabled = true;
  int _prayerSilenceDurationMinutes = 25;
  String _autoReplyMessage = "I am currently offering Namaz. I will respond to your internet message shortly. JazaakAllah.";
  bool _isInternetConnected = true;
  bool _hasDndPermission = true;

  final List<CustomAlarm> _customAlarms = [
    CustomAlarm(id: '1', title: 'Tahajjud Routine', hour: 4, minute: 15, durationMinutes: 45, isEnabled: true),
    CustomAlarm(id: '2', title: 'Ishraq Nawafil', hour: 6, minute: 30, durationMinutes: 20, isEnabled: false),
    CustomAlarm(id: '3', title: 'Taraweeh / Special', hour: 20, minute: 45, durationMinutes: 60, isEnabled: false),
  ];

  City get selectedCity => _selectedCity;
  bool get isManualSilent => _isManualSilent;
  bool get isJummahModeEnabled => _isJummahModeEnabled;
  int get jummahDurationMinutes => _jummahDurationMinutes;
  bool get isEidModeEnabled => _isEidModeEnabled;
  int get prayerSilenceDurationMinutes => _prayerSilenceDurationMinutes;
  String get autoReplyMessage => _autoReplyMessage;
  bool get isInternetConnected => _isInternetConnected;
  bool get hasDndPermission => _hasDndPermission;
  List<CustomAlarm> get customAlarms => List.unmodifiable(_customAlarms);

  PrayerTimes get currentPrayerTimes => PrayerCalculator.calculate(_selectedCity, DateTime.now());

  bool get isNamazModeActive => _isManualSilent || _isScheduledWindowActive();

  void setSelectedCity(City city) {
    _selectedCity = city;
    notifyListeners();
  }

  void toggleManualSilent() {
    _isManualSilent = !_isManualSilent;
    notifyListeners();
  }

  void setJummahModeEnabled(bool enabled) {
    _isJummahModeEnabled = enabled;
    notifyListeners();
  }

  void setJummahDuration(int minutes) {
    _jummahDurationMinutes = minutes;
    notifyListeners();
  }

  void setEidModeEnabled(bool enabled) {
    _isEidModeEnabled = enabled;
    notifyListeners();
  }

  void setSilenceDuration(int minutes) {
    _prayerSilenceDurationMinutes = minutes;
    notifyListeners();
  }

  void setAutoReplyMessage(String message) {
    _autoReplyMessage = message;
    notifyListeners();
  }

  void toggleInternetConnected() {
    _isInternetConnected = !_isInternetConnected;
    notifyListeners();
  }

  void toggleDndPermission() {
    _hasDndPermission = !_hasDndPermission;
    notifyListeners();
  }

  void addCustomAlarm(CustomAlarm alarm) {
    _customAlarms.add(alarm);
    notifyListeners();
  }

  void toggleAlarmEnabled(String id) {
    final index = _customAlarms.indexWhere((a) => a.id == id);
    if (index != -1) {
      _customAlarms[index] = _customAlarms[index].copyWith(isEnabled: !_customAlarms[index].isEnabled);
      notifyListeners();
    }
  }

  void deleteCustomAlarm(String id) {
    _customAlarms.removeWhere((a) => a.id == id);
    notifyListeners();
  }

  bool _isScheduledWindowActive() {
    final now = DateTime.now();
    final pt = currentPrayerTimes;

    final List<DateTime> starts = [pt.fajr, pt.dhuhr, pt.asr, pt.maghrib, pt.isha];
    for (var start in starts) {
      final end = start.add(Duration(minutes: _prayerSilenceDurationMinutes));
      if (now.isAfter(start) && now.isBefore(end)) {
        return true;
      }
    }
    return false;
  }
}

// ==========================================
// 3. APPLICATION & THEME DEFINITION
// ==========================================

class SmartNamazApp extends StatefulWidget {
  const SmartNamazApp({super.key});

  @override
  State<SmartNamazApp> createState() => _SmartNamazAppState();
}

class _SmartNamazAppState extends State<SmartNamazApp> {
  final NamazAppState appState = NamazAppState();

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: appState,
      builder: (context, child) {
        return MaterialApp(
          title: 'Smart Namaz Mode',
          debugShowCheckedModeBanner: false,
          theme: ThemeData(
            useMaterial3: true,
            colorScheme: ColorScheme.fromSeed(
              seedColor: const Color(0xFF0F5257),
              primary: const Color(0xFF0F5257),
              secondary: const Color(0xFFC6A15B),
              surface: const Color(0xFFF7F9F9),
              onPrimary: Colors.white,
            ),
            cardTheme: CardTheme(
              elevation: 2,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              margin: const EdgeInsets.symmetric(vertical: 8, horizontal: 16),
            ),
          ),
          home: MainHomeScreen(appState: appState),
        );
      },
    );
  }
}

// ==========================================
// 4. MAIN HOME SCREEN WITH HERO & TABS
// ==========================================

class MainHomeScreen extends StatefulWidget {
  final NamazAppState appState;
  const MainHomeScreen({super.key, required this.appState});

  @override
  State<MainHomeScreen> createState() => _MainHomeScreenState();
}

class _MainHomeScreenState extends State<MainHomeScreen> {
  int _currentTab = 0;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF2F5F5),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeroHeader(widget.appState),
            Expanded(
              child: IndexedStack(
                index: _currentTab,
                children: [
                  PrayersTab(appState: widget.appState),
                  AutoReplyTab(appState: widget.appState),
                  IbadatTab(appState: widget.appState),
                  SettingsTab(appState: widget.appState),
                ],
              ),
            ),
          ],
        ),
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _currentTab,
        onDestinationSelected: (index) => setState(() => _currentTab = index),
        indicatorColor: const Color(0xFFC6A15B).withOpacity(0.3),
        destinations: const [
          NavigationDestination(icon: Icon(Icons.access_time_filled), label: 'Prayers'),
          NavigationDestination(icon: Icon(Icons.mark_chat_unread), label: 'Auto Reply'),
          NavigationDestination(icon: Icon(Icons.stars), label: 'Ibadat'),
          NavigationDestination(icon: Icon(Icons.settings), label: 'Settings'),
        ],
      ),
    );
  }

  Widget _buildHeroHeader(NamazAppState state) {
    final bool isActive = state.isNamazModeActive;
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          colors: [Color(0xFF0B3C40), Color(0xFF1D6A70)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.only(
          bottomLeft: Radius.circular(24),
          bottomRight: Radius.circular(24),
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Row(
                children: [
                  const Icon(Icons.location_on, color: Color(0xFFC6A15B), size: 20),
                  const SizedBox(width: 4),
                  Text(
                    '${state.selectedCity.name}, Punjab',
                    style: const TextStyle(color: Colors.white, fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                decoration: BoxDecoration(
                  color: isActive ? Colors.redAccent : const Color(0xFF2ECC71),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Row(
                  children: [
                    Icon(isActive ? Icons.do_not_disturb_on : Icons.check_circle, color: Colors.white, size: 14),
                    const SizedBox(width: 4),
                    Text(
                      isActive ? 'SILENT ACTIVE' : 'READY',
                      style: const TextStyle(color: Colors.white, fontSize: 11, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Text(
            isActive ? '🕌 Silent Namaz Mode Active' : '🟢 Phone Sound Normal',
            style: const TextStyle(color: Colors.white, fontSize: 22, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          Text(
            isActive ? 'DND priority filter enabled for current prayer time' : 'Auto-silence scheduled for next prayer window',
            style: const TextStyle(color: Colors.white70, fontSize: 12),
          ),
          const SizedBox(height: 16),
          Row(
            children: [
              Expanded(
                child: ElevatedButton.icon(
                  onPressed: () => state.toggleManualSilent(),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: isActive ? Colors.red.shade700 : const Color(0xFFC6A15B),
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                  ),
                  icon: Icon(isActive ? Icons.volume_up : Icons.volume_off),
                  label: Text(isActive ? 'End Silence Now' : 'Quick Silence Now'),
                ),
              ),
              const SizedBox(width: 12),
              GestureDetector(
                onTap: () => state.toggleInternetConnected(),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
                  decoration: BoxDecoration(
                    color: Colors.white.withOpacity(0.15),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        state.isInternetConnected ? Icons.wifi : Icons.wifi_off,
                        color: state.isInternetConnected ? const Color(0xFF2ECC71) : Colors.orangeAccent,
                        size: 18,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        state.isInternetConnected ? 'Net Auto-Reply' : 'No Net',
                        style: const TextStyle(color: Colors.white, fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

// ==========================================
// 5. TAB 1: PRAYERS & CITY TIMES
// ==========================================

class PrayersTab extends StatelessWidget {
  final NamazAppState appState;
  const PrayersTab({super.key, required this.appState});

  @override
  Widget build(BuildContext context) {
    final pt = appState.currentPrayerTimes;
    final DateFormat formatter = DateFormat('hh:mm a');

    final prayers = [
      {'name': 'Fajr', 'time': formatter.format(pt.fajr), 'icon': Icons.wb_twilight},
      {'name': 'Sunrise', 'time': formatter.format(pt.sunrise), 'icon': Icons.wb_sunny_outlined},
      {'name': 'Dhuhr', 'time': formatter.format(pt.dhuhr), 'icon': Icons.wb_sunny},
      {'name': 'Asr (Hanafi)', 'time': formatter.format(pt.asr), 'icon': Icons.wb_cloudy},
      {'name': 'Maghrib', 'time': formatter.format(pt.maghrib), 'icon': Icons.nights_stay_outlined},
      {'name': 'Isha', 'time': formatter.format(pt.isha), 'icon': Icons.nights_stay},
    ];

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 4),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text('Select Punjab City:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
                    DropdownButton<City>(
                      value: appState.selectedCity,
                      underline: const SizedBox(),
                      items: PrayerCalculator.punjabCities.map((City c) {
                        return DropdownMenuItem<City>(
                          value: c,
                          child: Text(c.name, style: const TextStyle(fontWeight: FontWeight.w600, color: Color(0xFF0F5257))),
                        );
                      }).toList(),
                      onChanged: (City? newCity) {
                        if (newCity != null) appState.setSelectedCity(newCity);
                      },
                    ),
                  ],
                ),
              ),
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  'Today\'s Prayer Times',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17, color: Colors.grey.shade800),
                ),
                Text(
                  DateFormat('EEE, dd MMM yyyy').format(DateTime.now()),
                  style: const TextStyle(color: Color(0xFF0F5257), fontWeight: FontWeight.bold),
                ),
              ],
            ),
          ),
          ...prayers.map((p) => Card(
            child: ListTile(
              leading: CircleAvatar(
                backgroundColor: const Color(0xFF0F5257).withOpacity(0.1),
                child: Icon(p['icon'] as IconData, color: const Color(0xFF0F5257)),
              ),
              title: Text(p['name'] as String, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
              trailing: Text(
                p['time'] as String,
                style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, color: Color(0xFFC6A15B)),
              ),
            ),
          )),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Silence Duration Settings', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Standard Prayer Silence:'),
                      Text('${appState.prayerSilenceDurationMinutes} mins', style: const TextStyle(fontWeight: FontWeight.bold, color: Color(0xFF0F5257))),
                    ],
                  ),
                  Slider(
                    value: appState.prayerSilenceDurationMinutes.toDouble(),
                    min: 15,
                    max: 60,
                    divisions: 9,
                    activeColor: const Color(0xFF0F5257),
                    onChanged: (val) => appState.setSilenceDuration(val.round()),
                  ),
                  const Divider(),
                  SwitchListTile(
                    title: const Text('Jummah Extended Mode (Friday)', style: TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: Text('Extends silence window up to ${appState.jummahDurationMinutes} mins'),
                    value: appState.isJummahModeEnabled,
                    activeColor: const Color(0xFF0F5257),
                    onChanged: (val) => appState.setJummahModeEnabled(val),
                  ),
                  if (appState.isJummahModeEnabled)
                    Slider(
                      value: appState.jummahDurationMinutes.toDouble(),
                      min: 45,
                      max: 120,
                      divisions: 15,
                      activeColor: const Color(0xFFC6A15B),
                      onChanged: (val) => appState.setJummahDuration(val.round()),
                    ),
                  const Divider(),
                  SwitchListTile(
                    title: const Text('Eid Prayer Mode Profile', style: TextStyle(fontWeight: FontWeight.bold)),
                    subtitle: const Text('Automatic silence timing for Eid-ul-Fitr & Eid-ul-Adha'),
                    value: appState.isEidModeEnabled,
                    activeColor: const Color(0xFF0F5257),
                    onChanged: (val) => appState.setEidModeEnabled(val),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }
}

// ==========================================
// 6. TAB 2: INTERNET-ONLY AUTO REPLY
// ==========================================

class AutoReplyTab extends StatefulWidget {
  final NamazAppState appState;
  const AutoReplyTab({super.key, required this.appState});

  @override
  State<AutoReplyTab> createState() => _AutoReplyTabState();
}

class _AutoReplyTabState extends State<AutoReplyTab> {
  late TextEditingController _controller;

  @override
  void initState() {
    super.initState();
    _controller = TextEditingController(text: widget.appState.autoReplyMessage);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: Colors.amber.shade50,
              border: Border.all(color: Colors.amber.shade700),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              children: [
                Icon(Icons.shield, color: Colors.amber.shade900, size: 28),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Strict Non-Cellular Internet-Only Policy',
                        style: TextStyle(fontWeight: FontWeight.bold, color: Colors.amber.shade900, fontSize: 14),
                      ),
                      const SizedBox(height: 2),
                      const Text(
                        'This app contains NO SMS or Call permissions. Auto-replies are triggered strictly over active Wi-Fi/Mobile Data for social messaging apps during Namaz DND windows.',
                        style: TextStyle(fontSize: 12, color: Colors.black87),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Customize Internet Auto-Reply Text', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                  const SizedBox(height: 8),
                  TextField(
                    controller: _controller,
                    maxLines: 4,
                    decoration: const InputDecoration(
                      border: OutlineInputBorder(),
                      hintText: 'Enter custom auto-reply message...',
                    ),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: double.infinity,
                    child: ElevatedButton.icon(
                      onPressed: () {
                        widget.appState.setAutoReplyMessage(_controller.text);
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Auto-reply message saved successfully!')),
                        );
                      },
                      style: ElevatedButton.styleFrom(
                        backgroundColor: const Color(0xFF0F5257),
                        foregroundColor: Colors.white,
                      ),
                      icon: const Icon(Icons.save),
                      label: const Text('Save Auto-Reply Template'),
                    ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 4, vertical: 4),
            child: Text('Supported Social Messaging Apps', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
          ),
          Card(
            child: Column(
              children: const [
                ListTile(
                  leading: Icon(Icons.chat_bubble, color: Colors.green),
                  title: Text('WhatsApp & WhatsApp Business'),
                  subtitle: Text('Notification Quick Reply listener enabled'),
                  trailing: Icon(Icons.check_circle, color: Colors.green, size: 20),
                ),
                Divider(height: 1),
                ListTile(
                  leading: Icon(Icons.send, color: Colors.blue),
                  title: Text('Telegram & Signal'),
                  subtitle: Text('Internet message listener active'),
                  trailing: Icon(Icons.check_circle, color: Colors.green, size: 20),
                ),
                Divider(height: 1),
                ListTile(
                  leading: Icon(Icons.forum, color: Colors.purple),
                  title: Text('Messenger & Instagram Direct'),
                  subtitle: Text('Social notification listener active'),
                  trailing: Icon(Icons.check_circle, color: Colors.green, size: 20),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

// ==========================================
// 7. TAB 3: CUSTOM IBADAT ALARMS
// ==========================================

class IbadatTab extends StatelessWidget {
  final NamazAppState appState;
  const IbadatTab({super.key, required this.appState});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Padding(
              padding: EdgeInsets.symmetric(horizontal: 4, vertical: 4),
              child: Text('Custom Ibadat & Nawafil Windows', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
            ),
            ...appState.customAlarms.map((alarm) => Card(
              child: ListTile(
                leading: CircleAvatar(
                  backgroundColor: alarm.isEnabled ? const Color(0xFFC6A15B) : Colors.grey.shade300,
                  child: Icon(Icons.alarm, color: alarm.isEnabled ? Colors.white : Colors.grey.shade600),
                ),
                title: Text(alarm.title, style: const TextStyle(fontWeight: FontWeight.bold)),
                subtitle: Text(
                  '${_formatTime(alarm.hour, alarm.minute)} (${alarm.durationMinutes} mins silence)',
                  style: const TextStyle(fontSize: 13),
                ),
                trailing: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Switch(
                      value: alarm.isEnabled,
                      activeColor: const Color(0xFF0F5257),
                      onChanged: (_) => appState.toggleAlarmEnabled(alarm.id),
                    ),
                    IconButton(
                      icon: const Icon(Icons.delete_outline, color: Colors.red),
                      onPressed: () => appState.deleteCustomAlarm(alarm.id),
                    ),
                  ],
                ),
              ),
            )),
          ],
        ),
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _showAddAlarmDialog(context, appState),
        backgroundColor: const Color(0xFF0F5257),
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add_alarm),
        label: const Text('Add Ibadat Alarm'),
      ),
    );
  }

  static String _formatTime(int hour, int minute) {
    final TimeOfDay tod = TimeOfDay(hour: hour, minute: minute);
    final String minuteStr = tod.minute.toString().padLeft(2, '0');
    final String period = tod.period == DayPeriod.am ? 'AM' : 'PM';
    final int hour12 = tod.hourOfPeriod == 0 ? 12 : tod.hourOfPeriod;
    return '$hour12:$minuteStr $period';
  }

  void _showAddAlarmDialog(BuildContext context, NamazAppState state) {
    final titleController = TextEditingController();
    TimeOfDay selectedTime = TimeOfDay.now();
    int durationMinutes = 30;

    showDialog(
      context: context,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              title: const Text('New Ibadat Alarm Profile'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  TextField(
                    controller: titleController,
                    decoration: const InputDecoration(labelText: 'Title (e.g. Tahajjud, Itikaf)'),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text('Time: ${_formatTime(selectedTime.hour, selectedTime.minute)}'),
                      TextButton(
                        onPressed: () async {
                          final t = await showTimePicker(context: context, initialTime: selectedTime);
                          if (t != null) setDialogState(() => selectedTime = t);
                        },
                        child: const Text('Pick Time'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text('Silence Duration:'),
                      Text('$durationMinutes mins', style: const TextStyle(fontWeight: FontWeight.bold)),
                    ],
                  ),
                  Slider(
                    value: durationMinutes.toDouble(),
                    min: 15,
                    max: 120,
                    divisions: 7,
                    onChanged: (val) => setDialogState(() => durationMinutes = val.round()),
                  ),
                ],
              ),
              actions: [
                TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancel')),
                ElevatedButton(
                  onPressed: () {
                    if (titleController.text.isNotEmpty) {
                      state.addCustomAlarm(
                        CustomAlarm(
                          id: DateTime.now().millisecondsSinceEpoch.toString(),
                          title: titleController.text,
                          hour: selectedTime.hour,
                          minute: selectedTime.minute,
                          durationMinutes: durationMinutes,
                        ),
                      );
                      Navigator.pop(ctx);
                    }
                  },
                  style: ElevatedButton.styleFrom(backgroundColor: const Color(0xFF0F5257), foregroundColor: Colors.white),
                  child: const Text('Add Alarm'),
                ),
              ],
            );
          },
        );
      },
    );
  }
}

// ==========================================
// 8. TAB 4: SYSTEM PERMISSIONS & SETTINGS
// ==========================================

class SettingsTab extends StatelessWidget {
  final NamazAppState appState;
  const SettingsTab({super.key, required this.appState});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 4, vertical: 4),
            child: Text('System Access & Permissions', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
          ),
          Card(
            child: ListTile(
              leading: Icon(
                appState.hasDndPermission ? Icons.do_not_disturb_on : Icons.warning_amber,
                color: appState.hasDndPermission ? Colors.green : Colors.orange,
              ),
              title: const Text('Do Not Disturb Access'),
              subtitle: Text(appState.hasDndPermission ? 'Permission Granted' : 'Required to toggle silent mode automatically'),
              trailing: TextButton(
                onPressed: () => appState.toggleDndPermission(),
                child: Text(appState.hasDndPermission ? 'Granted' : 'Grant'),
              ),
            ),
          ),
          Card(
            child: ListTile(
              leading: const Icon(Icons.notifications_active, color: Colors.blue),
              title: const Text('Notification Listener Access'),
              subtitle: const Text('Required for auto-reply to incoming social messages'),
              trailing: const Text('Active', style: TextStyle(color: Colors.green, fontWeight: FontWeight.bold)),
            ),
          ),
          const SizedBox(height: 12),
          const Padding(
            padding: EdgeInsets.symmetric(horizontal: 4, vertical: 4),
            child: Text('Quick Settings Tile Guide', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 17)),
          ),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Text('How to add "Namaz Silence" Quick Tile:', style: TextStyle(fontWeight: FontWeight.bold)),
                  SizedBox(height: 6),
                  Text('1. Swipe down twice from top of Android screen.'),
                  Text('2. Tap Edit / Pencil icon.'),
                  Text('3. Drag "Namaz Silence" tile to top quick settings.'),
                  Text('4. Toggle silent mode anytime with 1 tap!'),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Card(
            color: const Color(0xFF0F5257),
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Row(
                children: const [
                  Icon(Icons.verified_user, color: Color(0xFFC6A15B), size: 32),
                  SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      '100% Cellular-Blind Guarantee\nNo READ_PHONE_STATE, CALL_PHONE, or SEND_SMS permissions are used.',
                      style: TextStyle(color: Colors.white, fontSize: 12, height: 1.4),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
