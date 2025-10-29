import 'package:flutter/material.dart';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';
import 'clinic_entry_page.dart';
import 'patient_entry_page.dart';
import 'welcome_page.dart';

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  Future<Widget> _determineStartPage() async {
    try {
      final database = await openDatabase(
        join(await getDatabasesPath(), 'oralvis.db'),
        onCreate: (db, version) {
          return db.execute('''
    CREATE TABLE clinic(
      id INTEGER PRIMARY KEY,
      name TEXT,
      clinicId INTEGER
    )
  ''');
        },
        version: 1,
      );

      final List<Map<String, dynamic>> clinics = await database.query('clinic');
      await database.close();

      if (clinics.isNotEmpty) {
        final name = clinics.first['name'] as String;
        final id = clinics.first['id'] as int;
        return WelcomePage(clinicName: name, clinicId: id);
      } else {
        return const ClinicEntryPage();
      }
    } catch (e) {
      return Scaffold(
        body: Center(child: Text('Error initializing app: $e')),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'OralVis App',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        useMaterial3: true,
      ),
      home: FutureBuilder<Widget>(
        future: _determineStartPage(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Scaffold(
              body: Center(child: CircularProgressIndicator()),
            );
          } else if (snapshot.hasError) {
            return Scaffold(
              body: Center(child: Text('Error: ${snapshot.error}')),
            );
          } else if (snapshot.hasData) {
            return snapshot.data!;
          } else {
            return const Scaffold(
              body: Center(child: Text('Unable to determine start page')),
            );
          }
        },
      ),
    );
  }
}
