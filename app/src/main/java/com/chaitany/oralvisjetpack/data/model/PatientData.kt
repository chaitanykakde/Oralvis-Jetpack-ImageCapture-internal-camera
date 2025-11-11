package com.chaitany.oralvisjetpack.data.model

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable

@DynamoDBTable(tableName = "OralVis_Patients")
data class PatientData(
    @DynamoDBHashKey(attributeName = "patientId")
    var patientId: String? = null,
    
    @DynamoDBAttribute(attributeName = "clinicId")
    var clinicId: Int? = null,
    
    @DynamoDBAttribute(attributeName = "name")
    var name: String? = null,
    
    @DynamoDBAttribute(attributeName = "age")
    var age: String? = null,
    
    @DynamoDBAttribute(attributeName = "gender")
    var gender: String? = null,
    
    @DynamoDBAttribute(attributeName = "phone")
    var phone: String? = null,
    
    @DynamoDBAttribute(attributeName = "imagePaths")
    var imagePaths: Map<String, String>? = null
)






