import 'package:sqflite/sqflite.dart';
import 'package:path/path.dart';

class ClinicDatabase {
  static Database? _db;

  static Future<Database> get database async {
    if (_db != null) return _db!;
    _db = await _initDB();
    return _db!;
  }

  static Future<Database> _initDB() async {
    final dbPath = await getDatabasesPath();
    final path = join(dbPath, 'clinic.db');
    return await openDatabase(path, version: 1, onCreate: (db, version) async {
      await db.execute('''
        CREATE TABLE clinic (
          id INTEGER PRIMARY KEY AUTOINCREMENT,
          name TEXT NOT NULL,
          clinicId INTEGER NOT NULL
        )
      ''');
      await db.execute('''
  CREATE TABLE patient_counter (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    counter INTEGER NOT NULL
  )
''');
      await db.insert('patient_counter', {'counter': 0});
    });
  }

  static Future<void> insertClinic(String name, int clinicId) async {
    final db = await database;
    await db.insert('clinic', {'name': name, 'clinicId': clinicId});
  }

  static Future<Map<String, dynamic>?> getClinic() async {
    final db = await database;
    final result = await db.query('clinic');
    if (result.isNotEmpty) return result.first;
    return null;
  }

  static Future<int> getPatientCounter() async {
    final db = await database;
    final result = await db.query('patient_counter', limit: 1);
    return result.first['counter'] as int? ?? 1;
  }

  static Future<void> updatePatientCounter(int newValue) async {
    final db = await database;
    await db.update('patient_counter', {'counter': newValue},
        where: 'id = ?', whereArgs: [1]);
  }
}
