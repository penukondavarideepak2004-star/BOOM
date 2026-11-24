package com.boom.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.boom.core.designsystem.theme.BoomAmberWarning
import com.boom.core.designsystem.theme.BoomDarkBorder
import com.boom.core.designsystem.theme.BoomDarkSurface
import com.boom.core.designsystem.theme.BoomDarkSurfaceElevated
import com.boom.core.designsystem.theme.BoomElectricIndigo
import com.boom.core.designsystem.theme.BoomErrorRed
import com.boom.core.designsystem.theme.BoomNeonCoral
import com.boom.core.designsystem.theme.BoomSuccessGreen
import com.boom.core.designsystem.theme.BoomTextPrimary
import com.boom.core.designsystem.theme.BoomTextSecondary

@Composable
fun BoomPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BoomElectricIndigo,
            contentColor = BoomTextPrimary,
            disabledContainerColor = BoomElectricIndigo.copy(alpha = 0.4f),
            disabledContentColor = BoomTextPrimary.copy(alpha = 0.5f)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = BoomTextPrimary,
                strokeWidth = 2.5.dp
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = BoomTextPrimary
            )
        }
    }
}

@Composable
fun BoomSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, BoomElectricIndigo),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = BoomElectricIndigo
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
    }
}

@Composable
fun BoomCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    border: BorderStroke? = BorderStroke(1.dp, BoomDarkBorder),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BoomDarkSurface),
        border = border
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun BoomStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (status.uppercase()) {
        "CONNECTED" -> BoomSuccessGreen to "Connected"
        "RECONNECTING" -> BoomAmberWarning to "Reconnecting"
        "DELAYED" -> BoomAmberWarning to "Delayed"
        "MUTED" -> BoomTextSecondary to "Muted"
        "DISCONNECTED" -> BoomErrorRed to "Disconnected"
        "PENDING_APPROVAL" -> BoomElectricIndigo to "Pending"
        else -> BoomTextSecondary to status
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = color,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 12.sp
        )
    }
}

@Composable
fun BoomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    isError: Boolean = false,
    errorMessage: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = BoomTextSecondary.copy(alpha = 0.6f)) },
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage, color = BoomErrorRed) }
        } else null,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BoomElectricIndigo,
            unfocusedBorderColor = BoomDarkBorder,
            focusedTextColor = BoomTextPrimary,
            unfocusedTextColor = BoomTextPrimary,
            focusedContainerColor = BoomDarkSurfaceElevated,
            unfocusedContainerColor = BoomDarkSurfaceElevated,
            errorBorderColor = BoomErrorRed,
            focusedLabelColor = BoomElectricIndigo,
            unfocusedLabelColor = BoomTextSecondary
        ),
        trailingIcon = trailingIcon,
        singleLine = true
    )
}
