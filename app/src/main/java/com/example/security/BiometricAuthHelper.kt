package com.example.security

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricAvailability {
  READY,
  NONE_ENROLLED,
  UNAVAILABLE,
}

object BiometricAuthHelper {

  private fun allowedAuthenticators(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
    } else {
      Authenticators.BIOMETRIC_WEAK or Authenticators.DEVICE_CREDENTIAL
    }
  }

  fun checkAvailability(context: Context): BiometricAvailability {
    val manager = BiometricManager.from(context)
    return when (manager.canAuthenticate(allowedAuthenticators())) {
      BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.READY
      BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NONE_ENROLLED
      else -> BiometricAvailability.UNAVAILABLE
    }
  }

  fun promptUnlock(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    onSuccess: () -> Unit,
    onFailureMessage: (String) -> Unit,
  ) {
    val executor = ContextCompat.getMainExecutor(activity)
    val callback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
          errorCode != BiometricPrompt.ERROR_CANCELED &&
          errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
        ) {
          onFailureMessage(errString.toString())
        } else {
          onFailureMessage("Authentication canceled. Tap Unlock to continue.")
        }
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        onFailureMessage("Biometric did not match. Try again.")
      }
    }

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setAllowedAuthenticators(allowedAuthenticators())
      .setConfirmationRequired(false)
      .build()

    BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
  }

  fun openBiometricEnrollSettings(context: Context) {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
        putExtra(
          Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
          Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL,
        )
      }
    } else {
      Intent(Settings.ACTION_SECURITY_SETTINGS)
    }.apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(intent) }
      .onFailure {
        runCatching {
          context.startActivity(
            Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
          )
        }
      }
  }
}
