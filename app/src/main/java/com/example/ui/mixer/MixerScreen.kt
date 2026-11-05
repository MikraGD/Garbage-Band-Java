package com.example.ui.mixer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudioTrack
import com.example.data.model.TrackEffects
import com.example.ui.StudioViewModel
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun MixerScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val project by viewModel.activeProject.collectAsState()
  val masterPeak by viewModel.audioEngine.masterPeakLevel.collectAsState()
  var expandedFxTrackId by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(12.dp)
  ) {
    // Header
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Tune, contentDescription = "Mixer", tint = StudioAmber, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "STUDIO MIXING CONSOLE",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp,
          color = Color.White
        )
      }
      Text(
        text = "${project.tracks.size} TRACKS + MASTER BUS",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
    }

    // Horizontal Scrollable Channel Strips
    val scrollState = rememberScrollState()
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .horizontalScroll(scrollState),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      // Channel Strip for each Track
      project.tracks.forEach { track ->
        val trackPeak = if (viewModel.audioEngine.isPlaying.value && !track.isMuted) {
          (masterPeak * track.volume).coerceIn(0f, 1f)
        } else {
          0f
        }

        ChannelStrip(
          track = track,
          peakLevel = trackPeak,
          isFxExpanded = (expandedFxTrackId == track.id),
          onVolumeChange = { viewModel.updateTrackVolume(track.id, it) },
          onPanChange = { viewModel.updateTrackPan(track.id, it) },
          onToggleMute = { viewModel.toggleMute(track.id) },
          onToggleSolo = { viewModel.toggleSolo(track.id) },
          onToggleFx = {
            expandedFxTrackId = if (expandedFxTrackId == track.id) null else track.id
          },
          onEffectsChange = { viewModel.updateTrackEffects(track.id, it) }
        )
      }

      // Master Output Bus Strip
      MasterChannelStrip(
        masterVolume = project.masterVolume,
        peakLevel = if (viewModel.audioEngine.isPlaying.value) masterPeak else 0f,
        onVolumeChange = { viewModel.setMasterVolume(it) }
      )
    }
  }
}

@Composable
fun ChannelStrip(
  track: StudioTrack,
  peakLevel: Float,
  isFxExpanded: Boolean,
  onVolumeChange: (Float) -> Unit,
  onPanChange: (Float) -> Unit,
  onToggleMute: () -> Unit,
  onToggleSolo: () -> Unit,
  onToggleFx: () -> Unit,
  onEffectsChange: (TrackEffects) -> Unit
) {
  val trackColor = try {
    Color(android.graphics.Color.parseColor(track.colorHex))
  } catch (_: Exception) {
    StudioCyan
  }

  Card(
    modifier = Modifier
      .width(if (isFxExpanded) 240.dp else 120.dp)
      .fillMaxHeight(),
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
    border = androidx.compose.foundation.BorderStroke(1.dp, if (isFxExpanded) StudioAmber else Color(0xFF2D3139))
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Track Name & Color Pill
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(4.dp))
          .background(trackColor.copy(alpha = 0.2f))
          .border(1.dp, trackColor, RoundedCornerShape(4.dp))
          .padding(vertical = 4.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = track.name,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          color = Color.White,
          maxLines = 1
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Solo & Mute Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        // Mute
        Button(
          onClick = onToggleMute,
          modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .padding(end = 2.dp)
            .testTag("mute_${track.id}"),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (track.isMuted) StudioCrimson else Color(0xFF23262F),
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(4.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
          Text("M", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }

        // Solo
        Button(
          onClick = onToggleSolo,
          modifier = Modifier
            .weight(1f)
            .height(30.dp)
            .padding(start = 2.dp)
            .testTag("solo_${track.id}"),
          colors = ButtonDefaults.buttonColors(
            containerColor = if (track.isSoloed) StudioAmber else Color(0xFF23262F),
            contentColor = if (track.isSoloed) Color.Black else Color.White
          ),
          shape = RoundedCornerShape(4.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
        ) {
          Text("S", fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Pan dial / slider
      Text(
        text = when {
          track.pan < -0.1f -> "L ${(Math.abs(track.pan) * 100).toInt()}%"
          track.pan > 0.1f -> "R ${(track.pan * 100).toInt()}%"
          else -> "PAN C"
        },
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
      Slider(
        value = track.pan,
        onValueChange = onPanChange,
        valueRange = -1f..1f,
        colors = SliderDefaults.colors(
          thumbColor = StudioCyan,
          activeTrackColor = StudioCyan,
          inactiveTrackColor = Color(0xFF374151)
        ),
        modifier = Modifier.height(28.dp)
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Volume & VU Meter Row
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // VU Meter Bar
        VuMeter(level = peakLevel)

        Spacer(modifier = Modifier.width(6.dp))

        // Vertical Volume Slider representation
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.weight(1f)
        ) {
          Text(
            text = "${(track.volume * 100).toInt()}%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Slider(
            value = track.volume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
              thumbColor = StudioAmber,
              activeTrackColor = StudioAmber,
              inactiveTrackColor = Color(0xFF2D3139)
            ),
            modifier = Modifier.padding(vertical = 4.dp)
          )
        }
      }

      // FX Button
      Button(
        onClick = onToggleFx,
        modifier = Modifier
          .fillMaxWidth()
          .height(32.dp)
          .testTag("fx_rack_${track.id}"),
        colors = ButtonDefaults.buttonColors(
          containerColor = if (isFxExpanded) StudioAmber else Color(0xFF2A2D36),
          contentColor = if (isFxExpanded) Color.Black else Color.White
        ),
        shape = RoundedCornerShape(4.dp)
      ) {
        Text("FX RACK", fontWeight = FontWeight.Bold, fontSize = 10.sp)
      }

      // Expanded DSP Effects Editor
      if (isFxExpanded) {
        Spacer(modifier = Modifier.height(8.dp))
        FxRackPanel(
          effects = track.effects,
          onEffectsChange = onEffectsChange
        )
      }
    }
  }
}

@Composable
fun FxRackPanel(
  effects: TrackEffects,
  onEffectsChange: (TrackEffects) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(6.dp))
      .background(Color(0xFF16181D))
      .padding(8.dp)
  ) {
    Text("REAL-TIME DSP FX", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = StudioAmber)

    // Reverb
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Reverb", fontSize = 10.sp, color = TextSecondary)
      Text("${(effects.reverbWet * 100).toInt()}%", fontSize = 10.sp, color = StudioEmerald)
    }
    Slider(
      value = effects.reverbWet,
      onValueChange = { onEffectsChange(effects.copy(reverbWet = it)) },
      valueRange = 0f..1f,
      colors = SliderDefaults.colors(thumbColor = StudioEmerald, activeTrackColor = StudioEmerald),
      modifier = Modifier.height(24.dp)
    )

    // Echo Delay
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Echo Delay", fontSize = 10.sp, color = TextSecondary)
      Text("${(effects.delayFeedback * 100).toInt()}%", fontSize = 10.sp, color = StudioCyan)
    }
    Slider(
      value = effects.delayFeedback,
      onValueChange = { onEffectsChange(effects.copy(delayFeedback = it)) },
      valueRange = 0f..0.8f,
      colors = SliderDefaults.colors(thumbColor = StudioCyan, activeTrackColor = StudioCyan),
      modifier = Modifier.height(24.dp)
    )

    // Distortion
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text("Distortion", fontSize = 10.sp, color = TextSecondary)
      Text("${(effects.distortionDrive * 100).toInt()}%", fontSize = 10.sp, color = StudioCrimson)
    }
    Slider(
      value = effects.distortionDrive,
      onValueChange = { onEffectsChange(effects.copy(distortionDrive = it)) },
      valueRange = 0f..1f,
      colors = SliderDefaults.colors(thumbColor = StudioCrimson, activeTrackColor = StudioCrimson),
      modifier = Modifier.height(24.dp)
    )
  }
}

@Composable
fun VuMeter(level: Float) {
  val animatedLevel by animateFloatAsState(targetValue = level.coerceIn(0f, 1f), label = "vu_level")

  Box(
    modifier = Modifier
      .width(12.dp)
      .fillMaxHeight(0.7f)
      .clip(RoundedCornerShape(3.dp))
      .background(Color(0xFF0F1115))
      .border(1.dp, Color(0xFF2D3139), RoundedCornerShape(3.dp)),
    contentAlignment = Alignment.BottomCenter
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(animatedLevel)
        .background(
          Brush.verticalGradient(
            listOf(StudioCrimson, StudioAmber, StudioEmerald)
          )
        )
    )
  }
}

@Composable
fun MasterChannelStrip(
  masterVolume: Float,
  peakLevel: Float,
  onVolumeChange: (Float) -> Unit
) {
  Card(
    modifier = Modifier
      .width(120.dp)
      .fillMaxHeight(),
    colors = CardDefaults.cardColors(containerColor = Color(0xFF25211B)),
    border = androidx.compose.foundation.BorderStroke(1.dp, StudioAmber)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(4.dp))
          .background(StudioAmber)
          .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
      ) {
        Text("MASTER", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.Black)
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "${(masterVolume * 100).toInt()}%",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = StudioAmber
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        horizontalArrangement = Arrangement.Center
      ) {
        VuMeter(level = peakLevel)
        Spacer(modifier = Modifier.width(6.dp))
        Slider(
          value = masterVolume,
          onValueChange = onVolumeChange,
          valueRange = 0f..1f,
          colors = SliderDefaults.colors(thumbColor = StudioAmber, activeTrackColor = StudioAmber),
          modifier = Modifier.weight(1f)
        )
      }

      Text("OUTPUT", fontSize = 10.sp, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
    }
  }
}
