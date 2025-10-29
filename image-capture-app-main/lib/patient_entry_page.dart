// ✅ Updated patient_entry_page.dart
import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:excel/excel.dart' show Excel, TextCellValue;
import 'dart:typed_data';
import 'dart:io';
import 'image_sequence_page.dart';
import 'clinic_database.dart';

class PatientEntryPage extends StatefulWidget {
  final String clinicName;
  final int clinicId;

  const PatientEntryPage({
    super.key,
    required this.clinicName,
    required this.clinicId,
  });

  @override
  State<PatientEntryPage> createState() => _PatientEntryPageState();
}

class _PatientEntryPageState extends State<PatientEntryPage> {
  final TextEditingController nameController = TextEditingController();
  final TextEditingController ageController = TextEditingController();
  final TextEditingController phoneController = TextEditingController();
  String selectedGender = 'Male';

  int _patientCounter = 1; // default until loaded

  @override
  void initState() {
    super.initState();
    _loadPatientCounter();
  }

  Future<void> _loadPatientCounter() async {
    final counter = await ClinicDatabase.getPatientCounter();
    setState(() {
      _patientCounter = counter;
    });
  }

  Future<void> saveToExcel() async {
    final name = nameController.text.trim();
    final age = ageController.text.trim();
    final phone = phoneController.text.trim();
    final gender = selectedGender;

    if (name.isEmpty || age.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Please fill in name and age')),
      );
      return;
    }

    // Phone is optional; validate only if provided
    if (phone.isNotEmpty && (phone.length != 10 || !RegExp(r'^[0-9]+$').hasMatch(phone))) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Phone number must be exactly 10 digits')),
      );
      return;
    }

    if (!await _requestPermissions()) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Storage permission denied')),
      );
      return;
    }

    try {
      final String phoneSuffix =
          phone.isNotEmpty && phone.length >= 2 ? phone.substring(phone.length - 2) : 'NA';
      final folderName =
          "${name.replaceAll(' ', '_')}_$phoneSuffix";

      // ✅ Create in-memory Excel file
      final excel = Excel.createExcel();
      final sheet = excel['Patients'];
      sheet.appendRow([
        TextCellValue('Name'),
        TextCellValue('Age'),
        TextCellValue('Gender'),
        TextCellValue('Phone Number')
      ]);
      sheet.appendRow([
        TextCellValue(name),
        TextCellValue(age),
        TextCellValue(gender),
        TextCellValue(phone)
      ]);
      final Uint8List excelBytes = Uint8List.fromList(excel.encode()!);

      // ✅ Navigate to ImageSequencePage with in-memory Excel
      if (context.mounted) {
        await Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => ImageSequencePage(
              folderName: folderName,
              clinicId: widget.clinicId,
              patientId: _patientCounter,
              excelBytes: excelBytes,
            ),
          ),
        );

        final int nextCounter = _patientCounter + 1;
        await ClinicDatabase.updatePatientCounter(nextCounter);

        setState(() {
          _patientCounter = nextCounter;
        });

        nameController.clear();
        ageController.clear();
        phoneController.clear();
        setState(() {
          selectedGender = 'Male';
        });
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error preparing Excel: $e')),
      );
    }
  }

  Future<bool> _requestPermissions() async {
    if (Platform.isAndroid) {
      if (await Permission.manageExternalStorage.isGranted) return true;
      final status = await Permission.manageExternalStorage.request();
      return status.isGranted;
    }
    return true;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Patient Data Collection')),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Clinic ID: ${widget.clinicId}',
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Colors.teal,
              ),
            ),
            Text(
              'Patient ID: ${_patientCounter.toString().padLeft(3, '0')}',
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Colors.deepOrange,
              ),
            ),
            TextField(
              controller: nameController,
              decoration: const InputDecoration(labelText: 'Patient Name'),
            ),
            TextField(
              controller: ageController,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Age'),
            ),
            DropdownButtonFormField<String>(
              value: selectedGender,
              items: ['Male', 'Female', 'Other']
                  .map((gender) =>
                      DropdownMenuItem(value: gender, child: Text(gender)))
                  .toList(),
              onChanged: (value) {
                setState(() {
                  selectedGender = value!;
                });
              },
              decoration: const InputDecoration(labelText: 'Gender'),
            ),
            TextField(
              controller: phoneController,
              keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: 'Phone Number (optional)'),
            ),
            const SizedBox(height: 20),
            ElevatedButton(
              onPressed: saveToExcel,
              child: const Text('Save and Next'),
            ),
          ],
        ),
      ),
    );
  }
}
