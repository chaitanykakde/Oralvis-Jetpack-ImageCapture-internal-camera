# AWS DynamoDB Table Creation Guide

## 📋 Table Details

### Table Name
**`OralVis_Patients`**

(You can use the same name as your old table if you're okay replacing it, or use `OralVis_Patients_V2` if you want to keep the old one)

### Table Structure

| Attribute | Type | Key Type | Description |
|-----------|------|----------|-------------|
| `clinicId` | **String** | **Partition Key (Hash Key)** | Clinic ID (can contain letters like "CLINIC001") |
| `patientId` | **String** | **Sort Key (Range Key)** | Patient ID (unique within each clinic) |
| `name` | String | Attribute | Patient name |
| `age` | String | Attribute | Patient age |
| `gender` | String | Attribute | Patient gender |
| `phone` | String | Attribute | Patient phone number |
| `imagePaths` | Map | Attribute | Map of image file names to S3 paths |
| `timestamp` | Number | Attribute | Timestamp when session was created |

---

## 🚀 Step-by-Step Creation Instructions

### Step 1: Open AWS Console
1. Go to [AWS Console](https://console.aws.amazon.com/)
2. Make sure you're in the **ap-south-1 (Mumbai)** region (or your preferred region)
3. Search for **"DynamoDB"** in the search bar
4. Click on **"DynamoDB"** service

### Step 2: Create Table
1. Click the **"Create table"** button (usually a blue button at the top)
2. You'll see a form to fill out

### Step 3: Fill in Table Details

#### Basic Settings:
- **Table name:** `OralVis_Patients`
  - ⚠️ **Important:** Use this exact name (or `OralVis_Patients_V2` if keeping old table)

#### Partition Key (Primary Key):
- **Partition key:** `clinicId`
- **Data type:** Select **"String"** (NOT Number!)
  - ⚠️ **CRITICAL:** Must be String type because clinic IDs can contain letters

#### Sort Key (Optional):
- **Check the box:** "Add sort key"
- **Sort key:** `patientId`
- **Data type:** Select **"String"**

#### Table Settings:
- **Default settings** - You can use default settings for:
  - Read/write capacity (On-demand or Provisioned)
  - Encryption
  - Point-in-time recovery (optional, but recommended for production)

### Step 4: Review and Create
1. Review all settings
2. Make sure:
   - ✅ Partition key: `clinicId` (String)
   - ✅ Sort key: `patientId` (String)
   - ✅ Table name is correct
3. Click **"Create table"** button at the bottom

### Step 5: Wait for Table Creation
- The table will be created in a few seconds
- Status will show as **"Active"** when ready

---

## 📸 Visual Guide (What You Should See)

### Partition Key Section:
```
┌─────────────────────────────────────┐
│ Partition key (primary key)         │
│ ┌─────────────────────────────────┐ │
│ │ clinicId                        │ │
│ └─────────────────────────────────┘ │
│ Data type: [String ▼]              │
└─────────────────────────────────────┘
```

### Sort Key Section:
```
┌─────────────────────────────────────┐
│ ☑ Add sort key                      │
│ ┌─────────────────────────────────┐ │
│ │ patientId                        │ │
│ └─────────────────────────────────┘ │
│ Data type: [String ▼]              │
└─────────────────────────────────────┘
```

---

## ⚠️ Common Mistakes to Avoid

1. ❌ **Don't use Number type for clinicId** - Must be String!
2. ❌ **Don't forget the sort key** - You need both partition key AND sort key
3. ❌ **Don't use wrong table name** - Must match what's in the app code
4. ❌ **Don't create in wrong region** - Should be **ap-south-1** (or your app's region)

---

## ✅ Verification Checklist

After creating the table, verify:

- [ ] Table name: `OralVis_Patients` (or `OralVis_Patients_V2`)
- [ ] Partition key: `clinicId` (Type: **String**)
- [ ] Sort key: `patientId` (Type: **String**)
- [ ] Table status: **Active**
- [ ] Region: **ap-south-1** (Mumbai) or your app's region

---

## 🔧 If You Need to Update Table Name in Code

If you created the table with a different name (like `OralVis_Patients_V2`), update this file:

**File:** `app/src/main/java/com/chaitany/oralvisjetpack/data/model/PatientData.kt`

**Change:**
```kotlin
@DynamoDBTable(tableName = "OralVis_Patients")
```

**To:**
```kotlin
@DynamoDBTable(tableName = "OralVis_Patients_V2")
```

---

## 📝 Table Settings Recommendations

### For Production:
- **Capacity mode:** On-demand (pay per request) OR Provisioned (fixed capacity)
- **Point-in-time recovery:** Enabled (recommended)
- **Encryption:** AWS owned keys (default) or Customer managed keys

### For Development/Testing:
- **Capacity mode:** On-demand (simpler, no capacity planning)
- **Point-in-time recovery:** Optional
- **Encryption:** Default is fine

---

## 🎯 After Table Creation

Once the table is created:

1. ✅ The app will automatically use it
2. ✅ New sessions will be saved with the correct structure
3. ✅ Each clinic will only see their own patients
4. ✅ Patient IDs will be unique per clinic

---

## 🆘 Troubleshooting

### Table Already Exists?
- If `OralVis_Patients` already exists with wrong structure:
  - Option 1: Delete old table (⚠️ will lose data) and create new one
  - Option 2: Create `OralVis_Patients_V2` with correct structure and update code

### Wrong Key Types?
- You CANNOT modify key structure of existing table
- Must create a new table with correct structure

### Wrong Region?
- Make sure table is in same region as your app (ap-south-1)
- Check your AWS credentials configuration

---

## 📞 Need Help?

If you encounter any issues:
1. Check AWS Console → DynamoDB → Tables
2. Verify table structure matches requirements above
3. Check table is in correct region
4. Verify IAM permissions allow table access

---

## ✅ Summary

**Table Name:** `OralVis_Patients`  
**Partition Key:** `clinicId` (String)  
**Sort Key:** `patientId` (String)  
**Region:** ap-south-1 (Mumbai) or your app's region

That's it! Once created, your app will work with the new structure. 🎉

