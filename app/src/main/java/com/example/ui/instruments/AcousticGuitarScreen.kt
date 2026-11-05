package com.example.ui.instruments

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class GuitarChord(
  val name: String,
  val pitches: List<Int> // 6 string pitches from Low E to High E
)

val STANDARD_GUITAR_CHORDS = listOf(
  GuitarChord("C Maj", listOf(48, 52, 55, 60, 64)), // C E G C E
  GuitarChord("G Maj", listOf(43, 47, 50, 55, 59, 67)), // G B D G B G
  GuitarChord("Am", listOf(45, 52, 57, 60, 64)), // A E A C E
  GuitarChord("F Maj", listOf(41, 48, 53, 57, 60, 65)), // F C F A C F
  GuitarChord("Em", listOf(40, 47, 52, 55, 59, 64)), // E B E G B E
  GuitarChord("D Maj", listOf(50, 57, 62, 66)), // D A D F#
  GuitarChord("Dm", listOf(50, 57, 62, 65)), // D A D F
  GuitarChord("E Maj", listOf(40, 47, 52, 56, 59, 64)) // E B E G# B E
)

@Composable
fun AcousticGuitarScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }
  var selectedChordIndex by remember { mutableIntStateOf(0) }
  val activeChord = STANDARD_GUITAR_CHORDS[selectedChordIndex]
  var lastStrumTime by remember { mutableStateOf(0L) }

  fun triggerHaptic() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
      }
    } catch (_: Exception) {}
  }

  fun strumChord(isDownStrum: Boolean) {
    val now = System.currentTimeMillis()
    if (now - lastStrumTime < 100) return
    lastStrumTime = now
    triggerHaptic()

    val chordNotes = if (isDownStrum) activeChord.pitches else activeChord.pitches.reversed()
    CoroutineScope(Dispatchers.Main).launch {
      for (i in chordNotes.indices) {
        viewModel.playNote(
          pitch = chordNotes[i],
          velocity = 0.85f,
          durationMs = 900L
        )
        delay(30) // Strum spread
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(12.dp)
  ) {
    // Chord Selection Bank
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
    ) {
      Column(modifier = Modifier.padding(12.dp)) {
        Text(
          text = "ACOUSTIC GUITAR CHORDS",
          fontWeight = FontWeight.Bold,
          color = StudioAmber,
          fontSize = 13.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        // 2 rows of 4 chords
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          for (i in 0..3) {
            ChordButton(
              chord = STANDARD_GUITAR_CHORDS[i],
              isSelected = (selectedChordIndex == i),
              onClick = { selectedChordIndex = i; strumChord(true) }
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          for (i in 4..7) {
            ChordButton(
              chord = STANDARD_GUITAR_CHORDS[i],
              isSelected = (selectedChordIndex == i),
              onClick = { selectedChordIndex = i; strumChord(true) }
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Guitar Strumming Deck & Individual Strings
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      colors = CardDefaults.cardColors(containerColor = Color(0xFF181512))
    ) {
      Row(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Dynamic Strumming Strip
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF2A1F16))
            .border(2.dp, Color(0xFF78350F), RoundedCornerShape(12.dp))
            .pointerInput(selectedChordIndex) {
              detectDragGestures(
                onDragStart = { strumChord(true) },
                onDrag = { change, dragAmount ->
                  change.consume()
                  if (Math.abs(dragAmount.y) > 20) {
                    strumChord(dragAmount.y > 0)
                  }
                }
              )
            }
            .pointerInput(selectedChordIndex) {
              detectTapGestures(onTap = { strumChord(true) })
            }
            .testTag("guitar_strum_pad"),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "STRUM STRIP",
              fontWeight = FontWeight.Bold,
              fontSize = 16.sp,
              color = StudioAmber
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Swipe Up/Down to Strum",
              fontSize = 12.sp,
              color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "Current: ${activeChord.name}",
              fontWeight = FontWeight.Bold,
              fontSize = 14.sp,
              color = Color.White
            )
          }
        }

        // Individual Strings Fingerpicking
        Column(
          modifier = Modifier
            .weight(1f)
            .fillMaxSize(),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          val stringLabels = listOf("String 1 (High E)", "String 2 (B)", "String 3 (G)", "String 4 (D)", "String 5 (A)", "String 6 (Low E)")
          val pitches = activeChord.pitches.take(6)

          for (idx in pitches.indices) {
            val p = pitches[idx]
            val label = if (idx < stringLabels.size) stringLabels[idx] else "String $idx"

            Button(
              onClick = {
                triggerHaptic()
                viewModel.playNote(pitch = p, velocity = 0.85f, durationMs = 800L)
              },
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .testTag("string_pick_$idx"),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B271A),
                contentColor = Color(0xFFFEF3C7)
              ),
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Note $p", fontSize = 11.sp, color = StudioAmber)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChordButton(
  chord: GuitarChord,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  Button(
    onClick = onClick,
    modifier = Modifier
      .width(80.dp)
      .height(48.dp)
      .testTag("chord_${chord.name}"),
    colors = ButtonDefaults.buttonColors(
      containerColor = if (isSelected) StudioAmber else Color(0xFF2C241C),
      contentColor = if (isSelected) Color.Black else Color.White
    ),
    shape = RoundedCornerShape(8.dp)
  ) {
    Text(
      text = chord.name,
      fontWeight = FontWeight.Bold,
      fontSize = 12.sp
    )
  }
}
