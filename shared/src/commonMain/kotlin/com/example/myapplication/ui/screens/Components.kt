package com.example.myapplication.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.DarkNavy
import com.example.myapplication.ui.theme.NeonBlue
import com.example.myapplication.ui.theme.NeonPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeWatchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onVisibilityToggle: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        placeholder = {
            Text(
                text = placeholder,
                color = Color.LightGray,
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.Gray
            )
        },
        trailingIcon = if (isPassword) {
            {
                IconButton(onClick = onVisibilityToggle) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            cursorColor = Color.Black
        ),
        textStyle = TextStyle(
            color = Color.Black,
            fontSize = 16.sp
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp)),
        contentPadding = PaddingValues(),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(NeonBlue, NeonPurple)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun SecurityGridBackground() {
    Canvas(modifier = Modifier.fillMaxSize().alpha(0.15f)) {
        val gridSpacing = 40.dp.toPx()
        val color = NeonBlue
        
        for (x in 0..size.width.toInt() step gridSpacing.toInt()) {
            drawLine(
                color = color,
                start = Offset(x.toFloat(), 0f),
                end = Offset(x.toFloat(), size.height),
                strokeWidth = 1f
            )
        }
        
        for (y in 0..size.height.toInt() step gridSpacing.toInt()) {
            drawLine(
                color = color,
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 1f
            )
        }
    }
}
