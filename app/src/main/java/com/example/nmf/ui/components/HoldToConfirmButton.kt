package com.example.nmf.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val reflectionMessages = listOf(
    "Los impulsos pasan. Espera un momento más.",
    "Este tiempo existe para ayudarte a tomar una decisión consciente.",
    "Respira profundamente antes de continuar.",
    "Pregúntate si realmente quieres eliminar esta protección.",
    "La mayoría de los impulsos disminuyen después de unos minutos.",
)

@Composable
fun HoldToConfirmButton(
    modifier: Modifier = Modifier,
    text: String = "Mantener presionado (60s)",
    targetDurationSeconds: Int = 60,
    containerColor: Color = MaterialTheme.colorScheme.errorContainer,
    progressColor: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onErrorContainer,
    onConfirmed: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    var elapsedMillis by remember { mutableFloatStateOf(0f) }
    var messageIndex by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    var timerJob by remember { mutableStateOf<Job?>(null) }

    val targetMillis = targetDurationSeconds * 1000f
    val progress = (elapsedMillis / targetMillis).coerceIn(0f, 1f)
    val remainingSeconds = (((targetMillis - elapsedMillis) / 1000f).toInt() + 1).coerceAtLeast(1)
    val percentage = (progress * 100).toInt()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 100),
        label = "HoldProgress",
    )

    fun startHolding() {
        isPressed = true
        elapsedMillis = 0f
        messageIndex = 0
        timerJob?.cancel()
        timerJob = scope.launch {
            val updateInterval = 50L
            while (elapsedMillis < targetMillis && isPressed) {
                delay(updateInterval)
                elapsedMillis += updateInterval
                messageIndex = ((elapsedMillis / 1000f) / 12f).toInt() % reflectionMessages.size
            }
            if (elapsedMillis >= targetMillis) {
                isPressed = false
                elapsedMillis = 0f
                onConfirmed()
            }
        }
    }

    fun stopHolding() {
        isPressed = false
        timerJob?.cancel()
        timerJob = null
        elapsedMillis = 0f
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isPressed) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = reflectionMessages[messageIndex],
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Restante: ${remainingSeconds}s",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = "$percentage%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = progressColor,
                        trackColor = containerColor,
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(containerColor)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            startHolding()
                            try {
                                awaitRelease()
                            } finally {
                                stopHolding()
                            }
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (isPressed && progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(progressColor.copy(alpha = 0.35f))
                        .align(Alignment.CenterStart),
                )
            }

            Text(
                text = if (isPressed) "Mantén presionado (${remainingSeconds}s)..." else text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}