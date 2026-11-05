package com.example.ui.instruments

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstrumentType
import com.example.ui.StudioViewModel
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

data class PowerChord(
  val name: String,
  val rootPitch: Int,
  val fifthPitch: Int
)

val POWER_CHORDS = listOf(
  PowerChord("E5", 40, 47),
  PowerChord("G5", 43, 50),
  PowerChord("A5", 45, 52),
  PowerChord("C5", 48, 55),
  PowerChord("D5", 50, 57),
  PowerChord("F5", 41, 48)
)

val SOLO_SCALE_NOTES = listOf(
  Pair("E4 (Root)", 64),
  Pair("G4 (b3)", 67),
  Pair("A4 (4th)", 69),
  Pair("B4 (5th)", 71),
  Pair("D5 (b7)", 74),
  Pair("E5 (Octave)", 76)
)

@Composable
fun ElectricGuitarScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
  var isOverdriveOn by remember { mutableStateOf(true) }
  var isPalmMuteOn by remember { mutableStateOf(false) }
  var driveLevel by remember { mutableFloatStateOf(0.6f) }

  fun triggerHaptic() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
      }
    } catch (_: Exception) {}
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(12.dp)
  ) {
    // Amp & Distortion Controls
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
            text = "ELECTRIC AMP RIG",
            fontWeight = FontWeight.Bold,
            color = StudioCrimson,
            fontSize = 13.sp
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
              selected = isPalmMuteOn,
              onClick = { isPalmMuteOn = !isPalmMuteOn },
              label = { Text("PALM MUTE", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = Color(0xFFB91C1C),
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("palm_mute_toggle")
            )

            FilterChip(
              selected = isOverdriveOn,
              onClick = { isOverdriveOn = !isOverdriveOn },
              label = { Text("OVERDRIVE", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = StudioCrimson,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("overdrive_toggle")
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Drive: ${(driveLevel * 100).toInt()}%",
            fontSize = 12.sp,
            color = TextSecondary,
            modifier = Modifier.width(80.dp)
          )
          Slider(
            value = driveLevel,
            onValueChange = { driveLevel = it },
            colors = SliderDefaults.colors(
              thumbColor = StudioCrimson,
              activeTrackColor = StudioCrimson,
              inactiveTrackColor = Color(0xFF374151)
            ),
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Power Chords Section
    Text(
      text = "HEAVY POWER CHORDS",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = TextSecondary,
      modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      POWER_CHORDS.forEach { chord ->
        Button(
          onClick = {
            triggerHaptic()
            val dur = if (isPalmMuteOn) 250L else 1100L
            // Play root + fifth for heavy rock power chord
            viewModel.playNote(pitch = chord.rootPitch, velocity = 0.9f, durationMs = dur)
            viewModel.playNote(pitch = chord.fifthPitch, velocity = 0.85f, durationMs = dur)
          },
          modifier = Modifier
            .weight(1f)
            .height(56.dp)
            .testTag("power_chord_${chord.name}"),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF261818),
            contentColor = Color(0xFFFCA5A5)
          ),
          shape = RoundedCornerShape(8.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7F1D1D))
        ) {
          Text(text = chord.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      }
    }

    Spacer(modifier = Modifier.height(18.dp))

    // Solo Lead Pentatonic Scale Section
    Text(
      text = "SOLO LEAD FRETBOARD (E PENTATONIC MINOR)",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = StudioCrimson,
      modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // 2 rows of 3 solo notes
      for (row in 0..1) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          for (col in 0..2) {
            val idx = row * 3 + col
            val note = SOLO_SCALE_NOTES[idx]

            Button(
              onClick = {
                triggerHaptic()
                viewModel.playNote(
                  pitch = note.second,
                  velocity = 0.9f,
                  durationMs = if (isPalmMuteOn) 300L else 900L
                )
              },
              modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .testTag("solo_note_${note.second}"),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1E1E24),
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4B5563))
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = note.first, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Fret Note", fontSize = 10.sp, color = StudioCrimson)
              }
            }
          }
        }
      }
    }
  }
}
