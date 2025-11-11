# CSV Authentication File Format

## File Location
Place your `clinics_2025.csv` file in the `app/src/main/assets/` directory.

## CSV Format
The CSV file must have the following structure:

### Header Row (will be skipped):
```
id,name,short_name,password
```

### Data Rows:
```
363,MNR Dental College,MNR,password123
364,Another Clinic,AC,password456
```

### Format Details:
- **id**: Clinic ID (String/Number) - used for login
- **name**: Full clinic name (String)
- **short_name**: Short name or abbreviation (String)
- **password**: Login password (String)

### Important Notes:
1. The first line (header) will be automatically skipped
2. Each field should be separated by commas
3. If a field contains commas, it should be wrapped in quotes: `"Clinic, Name"`
4. No spaces around commas (or they will be included in the field value)
5. Empty lines will be skipped

### Example CSV Content:
```csv
id,name,short_name,password
363,MNR Dental College,MNR,password123
364,City Dental Clinic,CDC,securepass
365,Regional Medical Center,RMC,admin2025
```

