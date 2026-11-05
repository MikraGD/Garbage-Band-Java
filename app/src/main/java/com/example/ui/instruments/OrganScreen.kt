package com.example.ui.instruments

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun OrganScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

  // Tonewheel Drawbars (values 0 to 8 like traditional B3 Hammond)
  var drawbar16 by remember { mutableFloatStateOf(8f) }
  var drawbar8 by remember { mutableFloatStateOf(8f) }
  var drawbar5 by remember { mutableFloatStateOf(6f) }
  var drawbar4 by remember { mutableFloatStateOf(7f) }
  var isVibratoOn by remember { mutableStateOf(true) }

  fun triggerHaptic() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
      }
    } catch (_: Exception) {}
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(12.dp)
  ) {
    // Organ Drawbars Rack
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "TONEWHEEL DRAWBARS",
            fontWeight = FontWeight.Bold,
            color = StudioPurple,
            fontSize = 13.sp
          )
          FilterChip(
            selected = isVibratoOn,
            onClick = { isVibratoOn = !isVibratoOn },
            label = { Text("LESLIE VIBRATO", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = StudioPurple,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("organ_vibrato_toggle")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4 Drawbar Sliders in horizontal grid
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          DrawbarColumn(label = "16' Sub", value = drawbar16, color = Color(0xFF9333EA)) { drawbar16 = it }
          DrawbarColumn(label = "8' Fund", value = drawbar8, color = Color(0xFFA855F7)) { drawbar8 = it }
          DrawbarColumn(label = "5⅓' 5th", value = drawbar5, color = Color(0xFFC084FC)) { drawbar5 = it }
          DrawbarColumn(label = "4' Oct", value = drawbar4, color = Color(0xFFE9D5FF)) { drawbar4 = it }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Organ Keyboard
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .clip(RoundedCornerShape(8.dp))
        .background(Color(0xFF0F1115))
        .border(1.dp, Color(0xFF2D3139), RoundedCornerShape(8.dp))
    ) {
      val scrollState = rememberScrollState()
      Row(
        modifier = Modifier
          .fillMaxSize()
          .horizontalScroll(scrollState)
          .padding(bottom = 6.dp)
      ) {
        for (oct in 3..4) {
          OrganOctaveKeys(
            octave = oct,
            onKeyPress = { pitch ->
              triggerHaptic()
              viewModel.playNote(pitch = pitch, velocity = 0.8f, durationMs = 1200L)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun DrawbarColumn(
  label: String,
  value: Float,
  color: Color,
  onValueChange: (Float) -> Unit
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.width(72.dp)
  ) {
    Text(text = label, fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
    Text(text = "${value.toInt()}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    Slider(
      value = value,
      onValueChange = onValueChange,
      valueRange = 0f..8f,
      steps = 7,
      colors = SliderDefaults.colors(
        thumbColor = color,
        activeTrackColor = color,
        inactiveTrackColor = Color(0xFF2E333D)
      ),
      modifier = Modifier.padding(horizontal = 4.dp)
    )
  }
}

@Composable
private fun OrganOctaveKeys(
  octave: Int,
  onKeyPress: (Int) -> Unit
) {
  val startMidi = 12 + octave * 12
  val whiteKeyNotes = listOf(
    Pair("C", startMidi + 0),
    Pair("D", startMidi + 2),
    Pair("E", startMidi + 4),
    Pair("F", startMidi + 5),
    Pair("G", startMidi + 7),
    Pair("A", startMidi + 9),
    Pair("B", startMidi + 11)
  )

  val blackKeyOffsets = listOf(
    Pair(startMidi + 1, 38.dp),
    Pair(startMidi + 3, 94.dp),
    Pair(startMidi + 6, 206.dp),
    Pair(startMidi + 8, 262.dp),
    Pair(startMidi + 10, 318.dp)
  )

  Box(modifier = Modifier.width(392.dp)) {
    Row(modifier = Modifier.fillMaxSize()) {
      whiteKeyNotes.forEach { (name, midi) ->
        var isPressed by remember { mutableStateOf(false) }
        Box(
          modifier = Modifier
            .width(56.dp)
            .fillMaxSize()
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(if (isPressed) Color(0xFFDDD6FE) else Color(0xFFF3F4F6))
            .border(1.dp, Color(0xFF9CA3AF), RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .pointerInput(midi) {
              detectTapGestures(
                onPress = {
                  isPressed = true
                  onKeyPress(midi)
                  tryAwaitRelease()
                  isPressed = false
                }
              )
            }
            .testTag("organ_key_$midi"),
          contentAlignment = Alignment.BottomCenter
        ) {
          Text(
            text = "$name$octave",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4B5563),
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }
      }
    }

    blackKeyOffsets.forEach { (midi, offsetLeft) ->
      var isPressed by remember { mutableStateOf(false) }
      Box(
        modifier = Modifier
          .offset(x = offsetLeft)
          .width(34.dp)
          .height(175.dp)
          .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
          .background(if (isPressed) StudioPurple else Color(0xFF1F2430))
          .border(1.dp, Color(0xFF0F1115), RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
          .pointerInput(midi) {
            detectTapGestures(
              onPress = {
                isPressed = true
                onKeyPress(midi)
                tryAwaitRelease()
                isPressed = false
              }
            )
          }
      )
    }
  }
}
