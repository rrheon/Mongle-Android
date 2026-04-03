package com.mongle.android.ui.common

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import com.mongle.android.ui.theme.MongleRadius
import com.mongle.android.ui.theme.MongleTextHint

@Composable
fun MongleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    isError: Boolean = false,
    errorMessage: String? = null,
    maxLines: Int = 1,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = MongleTextHint
            )
        },
        label = label?.let { { Text(text = it) } },
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(text = errorMessage, color = MaterialTheme.colorScheme.error) }
        } else null,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        enabled = enabled,
        shape = RoundedCornerShape(MongleRadius.medium),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        ),
        textStyle = MaterialTheme.typography.bodyLarge
    )
}
