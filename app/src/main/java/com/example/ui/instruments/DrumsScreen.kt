package com.example.ui.instruments

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstrumentType
import com.example.ui.StudioViewModel
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

data class DrumPadInfo(
  val label: String,
  val sampleId: String,
  val midiPitch: Int,
  val color: Color
)

val DRUM_PADS = listOf(
  DrumPadInfo("KICK", "kick", 36, Color(0xFF10B981)),
  DrumPadInfo("SNARE", "snare", 38, Color(0xFF06B6D4)),
  DrumPadInfo("HI-HAT CL", "hihat_closed", 42, Color(0xFFF59E0B)),
  DrumPadInfo("HI-HAT OP", "hihat_open", 46, Color(0xFFFBBF24)),
  DrumPadInfo("HI TOM", "tom_high", 48, Color(0xFFA855F7)),
  DrumPadInfo("FLOOR TOM", "tom_floor", 43, Color(0xFF8B5CF6)),
  DrumPadInfo("CLAP", "clap", 39, Color(0xFFEC4899)),
  DrumPadInfo("CRASH", "crash", 49, Color(0xFFF43F5E))
)

@Composable
fun DrumsScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
  var hitVelocity by remember { mutableFloatStateOf(0.9f) }

  fun triggerHaptic(pitch: Int) {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val duration = if (pitch == 36) 40L else 20L
        vibrator?.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
      }
    } catch (_: Exception) {}
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(12.dp)
  ) {
    // Drum Kit Header
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "STUDIO DRUMS",
          fontWeight = FontWeight.Bold,
          color = StudioEmerald,
          fontSize = 14.sp
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.width(200.dp)
        ) {
          Text(text = "Velocity", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(end = 6.dp))
          Slider(
            value = hitVelocity,
            onValueChange = { hitVelocity = it },
            valueRange = 0.3f..1.0f,
            colors = SliderDefaults.colors(
              thumbColor = StudioEmerald,
              activeTrackColor = StudioEmerald,
              inactiveTrackColor = Color(0xFF374151)
            )
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 8 Velocity Touchpads (2 rows of 4)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      for (row in 0..1) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          for (col in 0..3) {
            val idx = row * 4 + col
            val pad = DRUM_PADS[idx]
            var isPressed by remember { mutableStateOf(false) }

            val padBg by animateColorAsState(
              targetValue = if (isPressed) pad.color.copy(alpha = 0.5f) else Color(0xFF1E222A),
              label = "drum_pad_bg"
            )

            Box(
              modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(padBg)
                .border(
                  width = if (isPressed) 2.dp else 1.dp,
                  color = if (isPressed) pad.color else Color(0xFF374151),
                  shape = RoundedCornerShape(10.dp)
                )
                .pointerInput(pad.sampleId) {
                  detectTapGestures(
                    onPress = {
                      isPressed = true
                      triggerHaptic(pad.midiPitch)
                      viewModel.playNote(
                        pitch = pad.midiPitch,
                        velocity = hitVelocity,
                        durationMs = 400L,
                        sampleId = pad.sampleId
                      )
                      tryAwaitRelease()
                      isPressed = false
                    }
                  )
                }
                .testTag("drum_pad_${pad.sampleId}"),
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                  modifier = Modifier
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(pad.color)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = pad.label,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.Black
                  )
                }
                Text(
                  text = pad.sampleId.uppercase(),
                  fontSize = 10.sp,
                  color = TextMuted
                )
              }
            }
          }
        }
      }
    }
  }
}
