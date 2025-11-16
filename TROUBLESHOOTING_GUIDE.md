# Troubleshooting Guide: Upload and History Not Working

## ✅ What Was Fixed

### 1. Enhanced Error Handling
- Added comprehensive error messages for different failure scenarios
- Better logging to help diagnose issues
- Error messages now displayed in UI with retry option

### 2. Improved Logging
- Added detailed logs at every step of the query/upload process
- Logs include clinicId values, query parameters, and error details
- All errors are now logged with stack traces

### 3. Validation
- Added clinicId validation before attempting queries
- Clear error messages if clinicId is missing or invalid

---

## 🔍 How to Diagnose Issues

### Check Logcat for Errors

Use Android Studio Logcat or `adb logcat` to see detailed logs:

```bash
adb logcat | grep -E "HistoryViewModel|ImageSequenceVM|UploadQueueManager"
```

### Common Issues and Solutions

#### 1. **"DynamoDB table 'OralVis_Patients' not found"**
   - **Problem:** Table doesn't exist or has wrong name
   - **Solution:** 
     - Go to AWS Console → DynamoDB
     - Verify table name is exactly `OralVis_Patients`
     - Check table is in region `ap-south-1` (Mumbai)
     - Verify table structure:
       - Partition Key: `clinicId` (String)
       - Sort Key: `patientId` (String)

#### 2. **"Invalid clinic ID. Please login again."**
   - **Problem:** clinicId is empty or null
   - **Solution:**
     - Logout and login again
     - Check if clinicId is being saved correctly in SharedPreferences
     - Check logcat for: `"=== LaunchedEffect triggered with clinicId: '...'"`

#### 3. **"Access denied. Please check AWS credentials."**
   - **Problem:** AWS credentials are incorrect or expired
   - **Solution:**
     - Check `OralVisApplication.kt` for correct Cognito Identity Pool ID
     - Verify IAM permissions allow DynamoDB access
     - Check if credentials are expired

#### 4. **"Query returned 0 items"**
   - **Problem:** No data exists for this clinicId
   - **Solution:**
     - Verify clinicId matches what was used when saving data
     - Check DynamoDB table has data for this clinicId
     - Use AWS Console to query the table directly

#### 5. **History shows "No patient records found"**
   - **Possible causes:**
     - No patients exist for this clinicId
     - Patients don't have timestamps (old data)
     - Table structure mismatch
   - **Solution:**
     - Check logcat for: `"After filtering: X patients with valid timestamps"`
     - Verify patients were saved with timestamps
     - Check if patients exist in DynamoDB for this clinicId

---

## 📋 Diagnostic Checklist

When upload or history doesn't work, check:

1. **✅ Table Exists**
   - [ ] Table `OralVis_Patients` exists in DynamoDB
   - [ ] Table is in correct region (ap-south-1)
   - [ ] Table has correct structure (clinicId String, patientId String)

2. **✅ Clinic ID is Valid**
   - [ ] Check logcat: `"=== LaunchedEffect triggered with clinicId: '...'"`
   - [ ] clinicId is not empty
   - [ ] clinicId matches what was used when saving

3. **✅ AWS Credentials**
   - [ ] Cognito Identity Pool ID is correct
   - [ ] IAM permissions allow DynamoDB read/write
   - [ ] Network connection is available

4. **✅ Data Exists**
   - [ ] Check DynamoDB table has items
   - [ ] Items have correct clinicId value
   - [ ] Items have timestamps (for history to show)

---

## 🔧 Testing Steps

1. **Test History:**
   ```
   1. Login with a clinic ID
   2. Open History screen
   3. Check logcat for:
      - "=== STARTING loadPatientsForClinic with clinicId='...'"
      - "Query returned X items"
      - Any error messages
   ```

2. **Test Upload:**
   ```
   1. Create a new patient session
   2. Capture images
   3. Click Upload
   4. Check logcat for:
      - "=== SAVING PATIENT DATA TO DYNAMODB ==="
      - "✓ Successfully saved patient data"
      - Any error messages
   ```

---

## 📱 What to Check in the App

### History Screen
- If you see "No patient records found":
  - Check if error message appears (will show specific error)
  - Click "Retry" button if available
  - Check logcat for detailed error

### Upload
- If upload fails:
  - Error message will show in UI
  - Data is still saved locally
  - Check logcat for detailed error

---

## 🐛 Debugging Commands

### View App Logs
```bash
adb logcat -s HistoryViewModel:* ImageSequenceVM:* UploadQueueManager:*
```

### Check if Table Exists (AWS CLI)
```bash
aws dynamoDB describe-table --table-name OralVis_Patients --region ap-south-1
```

### Query Table Directly (AWS CLI)
```bash
aws dynamoDB query \
  --table-name OralVis_Patients \
  --key-condition-expression "clinicId = :clinicId" \
  --expression-attribute-values '{":clinicId":{"S":"YOUR_CLINIC_ID"}}' \
  --region ap-south-1
```

---

## 📞 Next Steps

If issues persist:

1. **Check Logcat** - Look for error messages with ❌ or ERROR tags
2. **Verify Table Structure** - Ensure it matches the guide
3. **Test with AWS Console** - Try querying the table directly
4. **Check Network** - Ensure device has internet connection
5. **Verify Credentials** - Check AWS credentials are correct

The app now provides detailed error messages to help identify the exact issue!

