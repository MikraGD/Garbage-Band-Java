package com.example.ui.arranger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstrumentType
import com.example.data.model.StudioTrack
import com.example.ui.StudioTab
import com.example.ui.StudioViewModel
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioRed
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary

@Composable
fun ArrangerScreen(
  viewModel: StudioViewModel,
  onOpenSfxDrawer: () -> Unit,
  modifier: Modifier = Modifier
) {
  val project by viewModel.activeProject.collectAsState()
  val isPlaying by viewModel.audioEngine.isPlaying.collectAsState()
  val isRecording by viewModel.audioEngine.isRecording.collectAsState()
  val playheadMs by viewModel.audioEngine.playbackPositionMs.collectAsState()
  val isMetronomeOn by viewModel.audioEngine.isMetronomeEnabled.collectAsState()
  val selectedTrackId by viewModel.selectedTrackId.collectAsState()

  var showAddTrackMenu by remember { mutableStateOf(false) }

  // Scale: 60dp per second of audio
  val pxPerSec = 70.dp
  val totalDurationSec = (project.getDurationMs() / 1000f).coerceAtLeast(8f)

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // 1. Studio Transport Deck Bar
    TransportBar(
      title = project.title,
      bpm = project.bpm,
      isPlaying = isPlaying,
      isRecording = isRecording,
      isLooping = viewModel.audioEngine.isLooping,
      isMetronomeOn = isMetronomeOn,
      playheadMs = playheadMs,
      onPlayPause = { viewModel.togglePlay() },
      onRecord = { viewModel.toggleRecord() },
      onRewind = { viewModel.rewind() },
      onToggleLoop = { viewModel.toggleLoop() },
      onToggleMetronome = { viewModel.toggleMetronome() },
      onBpmChange = { viewModel.setBpm(it) },
      onAddTrackClick = { showAddTrackMenu = true },
      onOpenSfx = onOpenSfxDrawer
    )

    // Add Track Menu
    DropdownMenu(
      expanded = showAddTrackMenu,
      onDismissRequest = { showAddTrackMenu = false },
      modifier = Modifier.background(StudioSurfaceElevated)
    ) {
      InstrumentType.values().forEach { type ->
        DropdownMenuItem(
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(Color(android.graphics.Color.parseColor(type.defaultColor)))
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(type.displayName, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
          },
          onClick = {
            viewModel.addTrack(type)
            showAddTrackMenu = false
          }
        )
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // 2. Multitrack Arranger Timeline Lanes
    val horizontalScrollState = rememberScrollState()

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      Row(modifier = Modifier.fillMaxSize()) {
        // Track Controls Left Column (Fixed Width 140dp)
        Column(
          modifier = Modifier
            .width(140.dp)
            .fillMaxHeight()
            .background(StudioSurface)
            .border(1.dp, Color(0xFF2D3139))
        ) {
          // Track List Header
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(34.dp)
              .background(StudioSurfaceElevated)
              .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
          ) {
            Text("TRACKS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TextSecondary)
          }

          LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(project.tracks) { track ->
              TrackHeaderItem(
                track = track,
                isSelected = (track.id == selectedTrackId),
                onSelect = {
                  viewModel.selectTrack(track.id)
                  viewModel.switchTab(StudioTab.INSTRUMENT)
                },
                onToggleMute = { viewModel.toggleMute(track.id) },
                onToggleSolo = { viewModel.toggleSolo(track.id) }
              )
            }
          }
        }

        // Timeline Lanes Right Area (Horizontally Scrollable)
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .horizontalScroll(horizontalScrollState)
            .background(Color(0xFF13151A))
        ) {
          // Playhead Red Line
          val playheadOffsetDp = ((playheadMs / 1000f) * 70).dp
          Box(
            modifier = Modifier
              .offset(x = playheadOffsetDp)
              .width(2.dp)
              .fillMaxHeight()
              .background(StudioRed)
          )

          Column(
            modifier = Modifier
              .width(playheadOffsetDp + 600.dp)
              .fillMaxHeight()
          ) {
            // Beat Markers Ruler Top
            TimelineRuler(totalDurationSec = totalDurationSec)

            // Track Note Lanes
            project.tracks.forEach { track ->
              TrackLaneContent(
                track = track,
                isSelected = (track.id == selectedTrackId),
                onLaneClick = {
                  viewModel.selectTrack(track.id)
                }
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun TransportBar(
  title: String,
  bpm: Int,
  isPlaying: Boolean,
  isRecording: Boolean,
  isLooping: Boolean,
  isMetronomeOn: Boolean,
  playheadMs: Long,
  onPlayPause: () -> Unit,
  onRecord: () -> Unit,
  onRewind: () -> Unit,
  onToggleLoop: () -> Unit,
  onToggleMetronome: () -> Unit,
  onBpmChange: (Int) -> Unit,
  onAddTrackClick: () -> Unit,
  onOpenSfx: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
    shape = RoundedCornerShape(0.dp)
  ) {
    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Song Title
        Text(
          text = title,
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp,
          color = StudioAmber
        )

        // Time Counter
        val secs = (playheadMs / 1000) % 60
        val mins = (playheadMs / 1000) / 60
        val millis = (playheadMs % 1000) / 10
        val timeStr = String.format("%02d:%02d.%02d", mins, secs, millis)

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color.Black)
            .border(1.dp, StudioEmerald.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
          Text(
            text = timeStr,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = StudioEmerald
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Action Buttons Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Transport Controls
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          // Rewind
          IconButton(
            onClick = onRewind,
            modifier = Modifier.size(38.dp).testTag("transport_rewind")
          ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = "Rewind", tint = Color.White)
          }

          // Play / Pause
          IconButton(
            onClick = onPlayPause,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(if (isPlaying) StudioAmber else Color(0xFF2C303B))
              .testTag("transport_play"),
            colors = IconButtonDefaults.iconButtonColors(
              contentColor = if (isPlaying) Color.Black else Color.White
            )
          ) {
            Icon(
              if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
              contentDescription = if (isPlaying) "Pause" else "Play",
              modifier = Modifier.size(24.dp)
            )
          }

          // Record
          IconButton(
            onClick = onRecord,
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(if (isRecording) StudioRed else Color(0xFF331D1D))
              .testTag("transport_record")
          ) {
            Icon(
              Icons.Default.FiberManualRecord,
              contentDescription = "Record",
              tint = if (isRecording) Color.White else StudioRed,
              modifier = Modifier.size(20.dp)
            )
          }

          // Loop
          IconButton(
            onClick = onToggleLoop,
            modifier = Modifier.size(36.dp).testTag("transport_loop")
          ) {
            Icon(
              Icons.Default.Loop,
              contentDescription = "Loop",
              tint = if (isLooping) StudioAmber else TextMuted
            )
          }

          // Metronome
          Button(
            onClick = onToggleMetronome,
            colors = ButtonDefaults.buttonColors(
              containerColor = if (isMetronomeOn) StudioAmber else Color(0xFF23262F),
              contentColor = if (isMetronomeOn) Color.Black else Color.White
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.height(30.dp).testTag("transport_metronome"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
          ) {
            Text("TICK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }

        // Right side: BPM, SFX, +Track
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          // BPM adjuster
          Button(
            onClick = {
              val nextBpm = if (bpm >= 160) 90 else bpm + 10
              onBpmChange(nextBpm)
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E222A)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.height(30.dp).testTag("bpm_adjuster"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
          ) {
            Text("$bpm BPM", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StudioCyan)
          }

          // SFX Library button
          Button(
            onClick = onOpenSfx,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.height(30.dp).testTag("open_sfx_button"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
          ) {
            Icon(Icons.Default.GraphicEq, contentDescription = "SFX", modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("SFX", fontSize = 11.sp, fontWeight = FontWeight.Bold)
          }

          // Add Track
          Button(
            onClick = onAddTrackClick,
            colors = ButtonDefaults.buttonColors(containerColor = StudioAmber),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.height(30.dp).testTag("add_track_button"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = "Add Track", tint = Color.Black, modifier = Modifier.size(16.dp))
            Text("TRACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Black)
          }
        }
      }
    }
  }
}

@Composable
fun TrackHeaderItem(
  track: StudioTrack,
  isSelected: Boolean,
  onSelect: () -> Unit,
  onToggleMute: () -> Unit,
  onToggleSolo: () -> Unit
) {
  val trackColor = try {
    Color(android.graphics.Color.parseColor(track.colorHex))
  } catch (_: Exception) {
    StudioCyan
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp)
      .background(if (isSelected) Color(0xFF262A34) else StudioSurface)
      .border(1.dp, if (isSelected) StudioAmber else Color(0xFF20232B))
      .clickable { onSelect() }
      .padding(horizontal = 8.dp, vertical = 4.dp)
      .testTag("track_header_${track.id}")
  ) {
    Column(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(trackColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = track.name,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color.White,
            maxLines = 1
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = track.instrumentType.take(6),
          fontSize = 9.sp,
          color = TextMuted,
          fontWeight = FontWeight.SemiBold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          // Mute mini
          Box(
            modifier = Modifier
              .size(20.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(if (track.isMuted) StudioRed else Color(0xFF2D3139))
              .clickable { onToggleMute() },
            contentAlignment = Alignment.Center
          ) {
            Text("M", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
          }

          // Solo mini
          Box(
            modifier = Modifier
              .size(20.dp)
              .clip(RoundedCornerShape(3.dp))
              .background(if (track.isSoloed) StudioAmber else Color(0xFF2D3139))
              .clickable { onToggleSolo() },
            contentAlignment = Alignment.Center
          ) {
            Text("S", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (track.isSoloed) Color.Black else Color.White)
          }
        }
      }
    }
  }
}

@Composable
fun TimelineRuler(totalDurationSec: Float) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(34.dp)
      .background(Color(0xFF16181F))
      .border(1.dp, Color(0xFF2D3139))
  ) {
    Row(modifier = Modifier.fillMaxSize()) {
      for (sec in 0..totalDurationSec.toInt() step 2) {
        val barNum = (sec / 2) + 1
        Box(
          modifier = Modifier
            .width(140.dp)
            .fillMaxHeight()
            .border(1.dp, Color(0xFF222630))
            .padding(start = 4.dp, top = 6.dp)
        ) {
          Text("BAR $barNum", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        }
      }
    }
  }
}

@Composable
fun TrackLaneContent(
  track: StudioTrack,
  isSelected: Boolean,
  onLaneClick: () -> Unit
) {
  val trackColor = try {
    Color(android.graphics.Color.parseColor(track.colorHex))
  } catch (_: Exception) {
    StudioCyan
  }

  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(64.dp)
      .background(if (isSelected) Color(0xFF1B1E26) else Color(0xFF13151A))
      .border(1.dp, Color(0xFF20232B))
      .clickable { onLaneClick() }
  ) {
    // Render Note Blocks
    track.notes.forEach { note ->
      val startOffset = ((note.startMs / 1000f) * 70).dp
      val width = ((note.durationMs / 1000f) * 70).dp.coerceAtLeast(16.dp)

      Box(
        modifier = Modifier
          .offset(x = startOffset, y = 14.dp)
          .width(width)
          .height(36.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(trackColor.copy(alpha = 0.85f))
          .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
          .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
      ) {
        Text(
          text = note.sampleId ?: "N${note.pitch}",
          fontSize = 9.sp,
          fontWeight = FontWeight.Bold,
          color = Color.Black,
          maxLines = 1
        )
      }
    }
  }
}
