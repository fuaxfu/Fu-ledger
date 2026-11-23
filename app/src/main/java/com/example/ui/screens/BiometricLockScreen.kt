package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.security.BiometricAvailability
import com.example.ui.theme.ExpenseCoral

@Composable
fun BiometricLockScreen(
  availability: BiometricAvailability,
  errorMessage: String?,
  onUnlockClick: () -> Unit,
  onOpenSecuritySettings: () -> Unit,
  onBypassWhenUnenrolled: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.background,
    modifier = Modifier.fillMaxSize(),
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        modifier = Modifier
          .widthIn(max = 420.dp)
          .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_local_vault_1795435746950),
          contentDescription = "Protected local financial vault",
          contentScale = ContentScale.Crop,
          modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(24.dp)),
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
          modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = if (availability == BiometricAvailability.READY) {
              Icons.Rounded.Fingerprint
            } else {
              Icons.Rounded.Lock
            },
            contentDescription = "Biometric Vault Lock",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "BirrLedger Vault Locked",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = when (availability) {
            BiometricAvailability.READY ->
              "Authenticate with your fingerprint, face, or device PIN to view your CBE & M-Pesa balances and spending history."
            BiometricAvailability.NONE_ENROLLED ->
              "No fingerprint, face, or device PIN is enrolled on this device yet. Enroll a lock in Android Settings to protect your ledger."
            BiometricAvailability.UNAVAILABLE ->
              "Biometric hardware is unavailable on this device."
          },
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        if (!errorMessage.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.labelLarge,
            color = ExpenseCoral,
            textAlign = TextAlign.Center,
          )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (availability == BiometricAvailability.READY) {
          Button(
            onClick = onUnlockClick,
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("biometric_unlock_button"),
          ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Unlock with Biometrics / PIN")
          }
        } else {
          Button(
            onClick = onOpenSecuritySettings,
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp),
          ) {
            Icon(Icons.Rounded.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Set Up Device Lock")
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedButton(
            onClick = onBypassWhenUnenrolled,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("biometric_bypass_button"),
          ) {
            Text("Continue Without Lock")
          }
        }
      }
    }
  }
}
