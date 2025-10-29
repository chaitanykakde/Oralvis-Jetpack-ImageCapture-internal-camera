import 'package:flutter/material.dart';
import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';
import 'welcome_page.dart';

class ClinicEntryPage extends StatefulWidget {
  const ClinicEntryPage({super.key});

  @override
  State<ClinicEntryPage> createState() => _ClinicEntryPageState();
}

class _ClinicEntryPageState extends State<ClinicEntryPage> {
  final TextEditingController _clinicNameController = TextEditingController();
  final TextEditingController _clinicIdController = TextEditingController();

  Future<void> _saveClinicInfo() async {
    final name = _clinicNameController.text.trim();
    final idText = _clinicIdController.text.trim();
    final int? clinicId = int.tryParse(idText);

    if (name.isEmpty || clinicId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please enter valid clinic name and ID')),
      );
      return;
    }

    final db = await openDatabase(
      p.join(await getDatabasesPath(), 'oralvis.db'),
      onCreate: (db, version) {
        return db.execute(
            'CREATE TABLE clinic(id INTEGER PRIMARY KEY, name TEXT, clinicId INTEGER)');
      },
      version: 1,
    );

    // Delete existing clinic (optional, so only one entry is saved)
    await db.delete('clinic');

    // Insert new clinic
    await db.insert('clinic', {'id': clinicId, 'name': name});

    if (context.mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(
          builder: (_) => WelcomePage(clinicName: name, clinicId: clinicId),
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Enter Clinic Info')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          children: [
            TextField(
              controller: _clinicNameController,
              decoration: const InputDecoration(labelText: 'Clinic Name'),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _clinicIdController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Clinic ID'),
            ),
            const SizedBox(height: 24),
            ElevatedButton(
              onPressed: _saveClinicInfo,
              child: const Text('Save & Proceed'),
            ),
          ],
        ),
      ),
    );
  }
}
