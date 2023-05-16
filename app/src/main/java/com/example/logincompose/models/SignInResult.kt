package com.example.logincompose.models

data class SignInResult(
    val data: UserData?,
    val errorMessage: String?,
)

data class UserData(
    val userId: String,
    val username: String?,
    val pictureUrl: String?,
)
