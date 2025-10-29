import 'package:flutter/material.dart';
import 'patient_entry_page.dart';

class WelcomePage extends StatelessWidget {
  final String clinicName;
  final int clinicId;

  const WelcomePage(
      {super.key, required this.clinicName, required this.clinicId});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text('Welcome, $clinicName')),
      body: Center(
        child: ElevatedButton(
          onPressed: () {
            Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => PatientEntryPage(
                    clinicName: clinicName,
                    clinicId: clinicId,
                  ),
                ));
          },
          child: const Text('Proceed to Patient Entry'),
        ),
      ),
    );
  }
}
