# CSV File Upload Instructions

## 📋 Overview

The app now validates clinic IDs against a CSV file stored in AWS S3. The CSV file should be uploaded to S3, and the app will download it during login validation.

## 📍 Where to Upload the CSV File

### S3 Location:
- **Bucket:** `oralvisclinics`
- **Path/Key:** `clinics_2025.csv`
- **Full S3 Path:** `s3://oralvisclinics/clinics_2025.csv`

## 📝 CSV File Format

The CSV file must have this exact format:

```csv
id,name,short_name,password
MDC202501,MNR Dental College,mnr_dental,mdc@202501
AB202502,AIIMS Bibinagar,aiims_bibinagar,ab@202502
SDC202503,Swarna Dental Clinic,swarna_dental,sdc@202503
```

### Format Details:
- **Header Row:** `id,name,short_name,password` (required)
- **id:** Clinic ID (String, can contain letters/numbers)
- **name:** Full clinic name
- **short_name:** Short name or abbreviation
- **password:** Login password

## 🚀 How to Upload to S3

### Option 1: AWS Console (Easiest)

1. Go to [AWS S3 Console](https://console.aws.amazon.com/s3/)
2. Navigate to bucket: `oralvisclinics`
3. Click **"Upload"** button
4. Select your `clinics_2025.csv` file
5. **IMPORTANT:** Set the **Object key** to: `clinics_2025.csv`
   - Click "Set destination" or edit the key manually
   - The path must be exactly: `clinics_2025.csv`
6. Click **"Upload"**

**✅ Already Uploaded:** If you've already uploaded the file, verify it's at `s3://oralvisclinics/clinics_2025.csv`

### Option 2: AWS CLI

```bash
aws s3 cp clinics_2025.csv s3://oralvisclinics/clinics_2025.csv --region ap-south-1
```

### Option 3: Using Android App (One-time Setup)

You can create a one-time utility function to upload the CSV from assets to S3. See `CSV_UPLOAD_UTILITY.md` for details.

## ✅ Verification

After uploading, verify the file is accessible:

1. Go to S3 Console → `oralvis-patient-images` bucket
2. Navigate to `public/` folder
3. You should see `clinics_2025.csv`
4. Click on it and verify the content

## 🔄 How the App Works

1. **Login Attempt:** User enters clinic ID and password
2. **Download CSV:** App downloads `clinics_2025.csv` from S3 bucket `oralvisclinics`
3. **Validation:** App checks if clinic ID exists in CSV
4. **Password Check:** If ID exists, validates password
5. **Fallback:** If S3 download fails, uses CSV from assets folder

## 📱 Current CSV File Location

Your current CSV file is in:
- **Android Assets:** `app/src/main/assets/clinics_2025.csv`

This file will be used as a **fallback** if S3 download fails.

## ⚠️ Important Notes

1. **File Name:** Must be exactly `clinics_2025.csv`
2. **S3 Path:** Must be exactly `clinics_2025.csv` (root of bucket)
3. **Bucket:** Must be `oralvisclinics`
4. **Region:** Should be `ap-south-1` (Mumbai) or your bucket's region
5. **Permissions:** Ensure the app's AWS credentials have read access to this S3 object

## 🔐 Security Considerations

- The CSV file contains passwords in plain text
- Consider using S3 bucket policies to restrict access
- Only the app's AWS credentials should have read access
- Consider encrypting the CSV file at rest in S3

## 🆘 Troubleshooting

### App can't download CSV from S3:
- Check AWS credentials are correct
- Verify file exists at `s3://oralvisclinics/clinics_2025.csv`
- Check IAM permissions allow S3 read access to `oralvisclinics` bucket
- App will fallback to assets folder automatically

### Login fails even with correct credentials:
- Check CSV format matches exactly (header row, comma-separated)
- Verify clinic ID and password match exactly (case-sensitive)
- Check logcat for CSV parsing errors

## 📞 Next Steps

1. ✅ **Already Done:** CSV uploaded to `s3://oralvisclinics/clinics_2025.csv`
2. Test login with a clinic ID from the CSV
3. Verify app downloads CSV from S3 (check logcat for "✓ Successfully loaded X clinics from S3")
4. Update CSV in S3 whenever you need to add/remove clinics

---

**The app is now configured to download and validate against the S3 CSV file!** 🎉

