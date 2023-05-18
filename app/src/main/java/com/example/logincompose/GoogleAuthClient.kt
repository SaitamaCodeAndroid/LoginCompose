package com.example.logincompose

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.util.Log
import com.example.logincompose.models.SignInResult
import com.example.logincompose.models.UserData
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.concurrent.CancellationException

private const val TAG = "Log in"

class GoogleAuthClient(
    private val context: Context,
) {
    private val auth = Firebase.auth
    private val oneTapClient: SignInClient = Identity.getSignInClient(context)

    private fun buildSignInRequest(): BeginSignInRequest =
        BeginSignInRequest
            .builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest
                    .GoogleIdTokenRequestOptions
                    .builder()
                    .setSupported(true)
                    .setServerClientId(context.getString(R.string.web_client_id))
                    .setFilterByAuthorizedAccounts(true)
                    .build()
            )
            .setAutoSelectEnabled(true)
            .build()

    suspend fun signIn(): IntentSender? {
        var intentSender: IntentSender? = null
        oneTapClient.beginSignIn(buildSignInRequest())
            .addOnSuccessListener { result ->
                intentSender = result.pendingIntent.intentSender
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                if (e is CancellationException) throw e
                intentSender = null
            }.await()
        return intentSender
    }

    suspend fun getSignInUserInfo(intent: Intent): SignInResult {
        val credential = oneTapClient.getSignInCredentialFromIntent(intent)
        val googleIdToken = credential.googleIdToken
        val password = credential.password
        val googleCredentials = GoogleAuthProvider.getCredential(
            googleIdToken,
            null
        )
        return try {
            val user = when {
                googleIdToken != null -> {
                    auth
                        .signInWithCredential(googleCredentials)
                        .await()
                        .user
                }
                password != null -> {
                    auth.signInWithEmailAndPassword(
                        credential.id,
                        password
                    )
                        .await()
                        .user
                }
                else -> {
                    // Shouldn't happen.
                    Log.d(TAG, "No ID token or password!")
                    null
                }
            }
            SignInResult(
                data = user?.run {
                    UserData(
                        userId = uid,
                        username = displayName,
                        pictureUrl = photoUrl?.toString(),
                    )
                },
                errorMessage = null,
            )
        } catch (e: ApiException) {
            when (e.statusCode) {
                CommonStatusCodes.CANCELED -> {

                }
            }
            e.printStackTrace()
            if (e is CancellationException) throw e
            SignInResult(
                data = null,
                errorMessage = e.localizedMessage,
            )
        }
    }

    fun getSignedInUser(): UserData? = auth.currentUser?.run {
        UserData(
            userId = uid,
            username = displayName,
            pictureUrl = photoUrl?.toString(),
        )
    }

    fun signOut() {
        oneTapClient.signOut()
            .addOnSuccessListener {
                auth.signOut()
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}
