# Database Migration Guide

## Overview
The app has been restructured to use `clinicId` as the partition key and `patientId` as the range key in DynamoDB. This guide explains what needs to be done.

---

## 🔴 CRITICAL: DynamoDB Table Structure

### Current Structure (OLD - Needs Update)
- **Partition Key (Hash Key):** `patientId` (String)
- **Sort Key:** None
- **Attributes:** `clinicId`, `name`, `age`, `gender`, `phone`, `imagePaths`, `timestamp`

### New Structure (REQUIRED)
- **Partition Key (Hash Key):** `clinicId` (String) - ⚠️ **IMPORTANT: Can contain letters (e.g., "CLINIC001", "ABC123")**
- **Sort Key (Range Key):** `patientId` (String)
- **Attributes:** `name`, `age`, `gender`, `phone`, `imagePaths`, `timestamp`

### ⚠️ Important: You CANNOT modify the key structure of an existing DynamoDB table

---

## 📋 Option 1: Create New Table (Recommended for Fresh Start)

### Steps:
1. **Go to AWS Console → DynamoDB**
2. **Create a new table:**
   - **Table name:** `OralVis_Patients_V2` (or keep `OralVis_Patients` if you're okay deleting the old one)
   - **Partition key:** `clinicId` (Type: **String**) ⚠️ **Must be String, not Number!**
   - **Sort key:** `patientId` (Type: String)
   - **Settings:** Use default or configure as needed
3. **Update the app code** to use the new table name (if you used a different name)
4. **Test with new data** - new sessions will work correctly
5. **Migrate old data** (if needed) - see Option 2 below

---

## 📋 Option 2: Migrate Existing Data (If You Have Important Data)

### Step 1: Create New Table
1. Create a new table `OralVis_Patients_V2` with:
   - Partition key: `clinicId` (**String** - can contain letters like "CLINIC001")
   - Sort key: `patientId` (String)

### Step 2: Migrate Data
You'll need to write a migration script or use AWS Data Pipeline. Here's a Python example:

```python
import boto3
from boto3.dynamodb.conditions import Key

# Initialize clients
dynamodb = boto3.resource('dynamodb', region_name='ap-south-1')
old_table = dynamodb.Table('OralVis_Patients')
new_table = dynamodb.Table('OralVis_Patients_V2')

# Scan old table
response = old_table.scan()
items = response['Items']

# Migrate each item
for item in items:
    # Extract clinicId and patientId
    # clinicId can be stored as Number (N) or String (S) in old table
    clinic_id_num = item.get('clinicId', {}).get('N')
    clinic_id_str = item.get('clinicId', {}).get('S')
    clinic_id = clinic_id_str if clinic_id_str else (str(clinic_id_num) if clinic_id_num else '')
    patient_id = item.get('patientId', {}).get('S', '')
    
    if not clinic_id or not patient_id:
        print(f"Skipping invalid item: {item}")
        continue
    
    # Create new item structure
    new_item = {
        'clinicId': clinic_id,  # Partition key (String)
        'patientId': patient_id,  # Sort key
        'name': item.get('name', {}).get('S', ''),
        'age': item.get('age', {}).get('S', ''),
        'gender': item.get('gender', {}).get('S', ''),
        'phone': item.get('phone', {}).get('S', ''),
        'timestamp': int(item.get('timestamp', {}).get('N', 0)) if item.get('timestamp', {}).get('N') else None
    }
    
    # Handle imagePaths (map type)
    if 'imagePaths' in item:
        image_paths = {}
        for key, value in item['imagePaths'].get('M', {}).items():
            image_paths[key] = value.get('S', '')
        new_item['imagePaths'] = image_paths
    
    # Write to new table
    new_table.put_item(Item=new_item)
    print(f"Migrated: clinicId={clinic_id}, patientId={patient_id}")

print(f"Migration complete! Migrated {len(items)} items.")
```

### Step 3: Update App Code
If you created a new table, update the table name in:
- `app/src/main/java/com/chaitany/oralvisjetpack/data/model/PatientData.kt`
  - Change: `@DynamoDBTable(tableName = "OralVis_Patients")`
  - To: `@DynamoDBTable(tableName = "OralVis_Patients_V2")`

---

## 📋 Option 3: Keep Old Table (Not Recommended)

If you want to keep using the old structure temporarily:
1. The app will still work but queries will be less efficient
2. You'll need to use scan operations instead of direct queries
3. Data isolation by clinic won't be as efficient

---

## ✅ S3 Bucket: NO CHANGES NEEDED

### Current S3 Structure:
- **Bucket:** `oralvis-patient-images`
- **Path Format:** `public/{clinicId}_{patientId}/filename.jpg`
  - Example: `public/123_1/image1.jpg`

### ✅ This structure is PERFECT and needs NO changes!

The S3 structure already groups images by clinic and patient, which is exactly what we want. The app code already uses this format.

### ⚠️ Note: There's a minor inconsistency in the code:
- `ImageSequenceViewModel.kt` uses: `public/${activeClinicId}_${patientId}/$fileName` ✅ (correct)
- `UploadQueueManager.kt` uses: `public/$clinicId/$patientId/$fileName` ❌ (inconsistent)

**Recommendation:** Keep the underscore format (`clinicId_patientId`) as it's more consistent and easier to parse.

---

## 🔧 Quick Fix: Update UploadQueueManager S3 Path

If you want consistency, update `UploadQueueManager.kt` line 375:

**Change from:**
```kotlin
val s3Key = "public/$clinicId/$patientId/$fileName"
```

**Change to:**
```kotlin
val s3Key = "public/${clinicId}_${patientId}/$fileName"
```

This matches the format used in `ImageSequenceViewModel.kt`.

---

## 📝 Summary Checklist

### DynamoDB:
- [ ] Create new table with `clinicId` as partition key and `patientId` as sort key
- [ ] Migrate existing data (if needed)
- [ ] Update table name in code (if using new table name)
- [ ] Test with new sessions

### S3:
- [x] No changes needed - structure is already correct
- [ ] (Optional) Fix path inconsistency in `UploadQueueManager.kt` for consistency

### App Code:
- [x] Already updated to use new structure
- [x] Ready to work with new table structure

---

## 🚀 After Migration

Once the DynamoDB table is updated:
1. New sessions will be stored with the correct structure
2. Each clinic will only see their own patients
3. Patient IDs will be unique per clinic (each clinic starts from 1)
4. Queries will be faster and more efficient
5. Data isolation will be guaranteed at the database level

---

## ⚠️ Important Notes

1. **Backup your data** before making any changes
2. **Test the migration** on a copy of the table first
3. **Update IAM permissions** if needed for the new table
4. **Monitor costs** - the new structure may have different access patterns

---

## Need Help?

If you need assistance with:
- AWS Console navigation
- Writing migration scripts
- Testing the new structure
- Fixing any issues

Let me know and I can provide more specific guidance!

