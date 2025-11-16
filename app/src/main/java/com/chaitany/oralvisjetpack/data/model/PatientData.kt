package com.chaitany.oralvisjetpack.data.model

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable

@DynamoDBTable(tableName = "OralVis_Patients")
data class PatientData(
    // clinicId is now the partition key (hash key) - all patients for a clinic are grouped together
    // clinicId can contain string characters (e.g., "CLINIC001", "ABC123")
    @DynamoDBHashKey(attributeName = "clinicId")
    var clinicId: String? = null,
    
    // patientId is now the range key - unique within each clinic
    @DynamoDBRangeKey(attributeName = "patientId")
    var patientId: String? = null,
    
    @DynamoDBAttribute(attributeName = "name")
    var name: String? = null,
    
    @DynamoDBAttribute(attributeName = "age")
    var age: String? = null,
    
    @DynamoDBAttribute(attributeName = "gender")
    var gender: String? = null,
    
    @DynamoDBAttribute(attributeName = "phone")
    var phone: String? = null,
    
    @DynamoDBAttribute(attributeName = "imagePaths")
    var imagePaths: Map<String, String>? = null,
    
    @DynamoDBAttribute(attributeName = "timestamp")
    var timestamp: Long? = null
)








