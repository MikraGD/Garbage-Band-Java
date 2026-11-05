package com.example.ui.sfx

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstrumentType
import com.example.ui.StudioViewModel
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

data class SfxItem(
  val id: String,
  val name: String,
  val category: String,
  val description: String,
  val duration: String
)

val SFX_LIBRARY = listOf(
  SfxItem("riser", "Cosmic Synth Riser", "Transitions", "150Hz to 1200Hz tension build", "1.2s"),
  SfxItem("sub_drop", "808 Sub Drop", "Bass & Impacts", "Deep sub bass drop down to 32Hz", "1.5s"),
  SfxItem("vinyl_crackle", "Lo-Fi Vinyl Dust", "Textures", "Warm vintage turntable dust and pops", "1.0s"),
  SfxItem("laser", "Retro Laser Zap", "Arcade", "Rapid pitch plunge with resonance", "0.4s"),
  SfxItem("tape_stop", "Vintage Tape Stop", "Transitions", "Pitch and speed decaying deceleration", "1.0s"),
  SfxItem("clap", "Layered Studio Clap", "Percussion", "Snappy room multi-burst clap", "0.5s"),
  SfxItem("crash", "Metallic Crash Cymbal", "Percussion", "Rich high-frequency shimmer", "1.8s")
)

@Composable
fun SoundEffectsDrawer(
  viewModel: StudioViewModel,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .fillMaxWidth()
      .padding(12.dp),
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
    shape = RoundedCornerShape(12.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3F4452))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.GraphicEq, contentDescription = "SFX", tint = StudioAmber, modifier = Modifier.size(22.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "SOUND EFFECTS LIBRARY",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White
          )
        }

        IconButton(
          onClick = onDismiss,
          modifier = Modifier.size(32.dp).testTag("close_sfx_drawer")
        ) {
          Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(SFX_LIBRARY) { sfx ->
          SfxRowItem(
            item = sfx,
            onPreview = {
              viewModel.audioEngine.playNote(
                instrumentType = InstrumentType.SFX,
                pitch = 60,
                velocity = 0.9f,
                durationMs = 1200L,
                sampleId = sfx.id
              )
            },
            onInsert = {
              viewModel.insertSoundEffect(sfx.id, sfx.name)
              onDismiss()
            }
          )
        }
      }
    }
  }
}

@Composable
fun SfxRowItem(
  item: SfxItem,
  onPreview: () -> Unit,
  onInsert: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(Color(0xFF181A20))
      .border(1.dp, Color(0xFF2B2F3A), RoundedCornerShape(8.dp))
      .padding(10.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(item.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(Color(0xFF282C37))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(item.category, fontSize = 9.sp, color = StudioAmber, fontWeight = FontWeight.Bold)
          }
        }
        Text(item.description, fontSize = 11.sp, color = TextMuted)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        // Preview button
        Button(
          onClick = onPreview,
          modifier = Modifier.size(36.dp).testTag("preview_sfx_${item.id}"),
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF292E3B)),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = StudioCyan, modifier = Modifier.size(18.dp))
        }

        // Insert button
        Button(
          onClick = onInsert,
          modifier = Modifier.height(36.dp).testTag("insert_sfx_${item.id}"),
          colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Add, contentDescription = "Insert", tint = Color.Black, modifier = Modifier.size(16.dp))
          Text("INSERT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
      }
    }
  }
}
