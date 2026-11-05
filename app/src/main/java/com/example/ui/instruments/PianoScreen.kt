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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun PianoScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
  var baseOctave by remember { mutableIntStateOf(4) } // Default C4 = Middle C
  var isSustainOn by remember { mutableStateOf(false) }

  fun triggerHaptic() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(20)
      }
    } catch (_: Exception) {}
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(12.dp)
  ) {
    // Piano Header Controls
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
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "PIANO SYNTH",
            fontWeight = FontWeight.Bold,
            color = StudioCyan,
            fontSize = 15.sp
          )
          Spacer(modifier = Modifier.width(16.dp))

          // Octave controls
          IconButton(
            onClick = { if (baseOctave > 2) baseOctave-- },
            modifier = Modifier.size(36.dp).testTag("piano_octave_down")
          ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Octave Down", tint = StudioAmber)
          }

          Text(
            text = "OCTAVE: C$baseOctave",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = TextSecondary
          )

          IconButton(
            onClick = { if (baseOctave < 6) baseOctave++ },
            modifier = Modifier.size(36.dp).testTag("piano_octave_up")
          ) {
            Icon(Icons.Default.ArrowForward, contentDescription = "Octave Up", tint = StudioAmber)
          }
        }

        // Sustain pedal toggle
        FilterChip(
          selected = isSustainOn,
          onClick = { isSustainOn = !isSustainOn },
          label = { Text("SUSTAIN", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = StudioAmber,
            selectedLabelColor = Color.Black
          ),
          modifier = Modifier.testTag("sustain_toggle")
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Interactive 2-Octave Keyboard
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
        // Render 2 full octaves (from baseOctave to baseOctave + 1)
        for (oct in baseOctave..(baseOctave + 1)) {
          OctaveKeys(
            octave = oct,
            isSustain = isSustainOn,
            onKeyPress = { pitch ->
              triggerHaptic()
              val dur = if (isSustainOn) 1600L else 600L
              viewModel.playNote(pitch = pitch, velocity = 0.85f, durationMs = dur)
            }
          )
        }
      }
    }
  }
}

@Composable
private fun OctaveKeys(
  octave: Int,
  isSustain: Boolean,
  onKeyPress: (Int) -> Unit
) {
  val startMidi = 12 + octave * 12 // C of this octave
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
    Pair(startMidi + 1, 38.dp),  // C#
    Pair(startMidi + 3, 94.dp),  // D#
    Pair(startMidi + 6, 206.dp), // F#
    Pair(startMidi + 8, 262.dp), // G#
    Pair(startMidi + 10, 318.dp) // A#
  )

  Box(modifier = Modifier.width(392.dp)) {
    // White Keys Row
    Row(modifier = Modifier.fillMaxSize()) {
      whiteKeyNotes.forEach { (name, midi) ->
        var isPressed by remember { mutableStateOf(false) }

        Box(
          modifier = Modifier
            .width(56.dp)
            .fillMaxSize()
            .padding(horizontal = 1.dp)
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(if (isPressed) Color(0xFFCBD5E1) else Color(0xFFF1F5F9))
            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
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
            .testTag("white_key_$midi"),
          contentAlignment = Alignment.BottomCenter
        ) {
          Text(
            text = "$name$octave",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF334155),
            modifier = Modifier.padding(bottom = 12.dp)
          )
        }
      }
    }

    // Black Keys Overlay
    blackKeyOffsets.forEach { (midi, offsetLeft) ->
      var isPressed by remember { mutableStateOf(false) }

      Box(
        modifier = Modifier
          .offset(x = offsetLeft)
          .width(34.dp)
          .height(180.dp)
          .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
          .background(if (isPressed) StudioAmber else Color(0xFF1E222A))
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
          .testTag("black_key_$midi")
      )
    }
  }
}
