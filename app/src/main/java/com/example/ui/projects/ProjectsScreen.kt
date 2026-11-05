package com.example.ui.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Project
import com.example.ui.StudioViewModel
import com.example.ui.auth.CloudSyncStatusCard
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioCrimson
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioEmerald
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun ProjectsScreen(
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier
) {
  val activeProject by viewModel.activeProject.collectAsState()
  val cloudProjects by viewModel.cloudProjects.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val shareId by viewModel.shareIdResult.collectAsState()

  var newSongTitle by remember { mutableStateOf("") }
  var showNewSongDialog by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(14.dp)
  ) {
    // Auth & Cloud Status Card
    CloudSyncStatusCard(
      onSignInRequired = {}
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Active Song Actions Deck
    Card(
      modifier = Modifier.fillMaxWidth(),
      colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text("CURRENT ACTIVE SONG", fontSize = 11.sp, color = TextSecondary, fontWeight = FontWeight.SemiBold)
            Text(activeProject.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("${activeProject.tracks.size} Tracks • ${activeProject.bpm} BPM", fontSize = 12.sp, color = StudioAmber)
          }

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Cloud Sync Button
            Button(
              onClick = { viewModel.syncToCloud() },
              enabled = !isSyncing,
              colors = ButtonDefaults.buttonColors(containerColor = StudioEmerald),
              modifier = Modifier.testTag("sync_cloud_button")
            ) {
              if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
              } else {
                Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SAVE CLOUD", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
              }
            }

            // Share Project Button
            Button(
              onClick = { viewModel.shareProject() },
              colors = ButtonDefaults.buttonColors(containerColor = StudioCyan),
              modifier = Modifier.testTag("share_project_button")
            ) {
              Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Black, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("SHARE", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 12.sp)
            }
          }
        }

        if (shareId != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF0F2231))
              .border(1.dp, StudioCyan, RoundedCornerShape(6.dp))
              .padding(10.dp)
          ) {
            Text(
              text = "Shared Online! ID: $shareId (Loadable in Web Companion)",
              color = StudioCyan,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Create New Song Row
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "MY CLOUD SONGS (${cloudProjects.size})",
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
      )

      Button(
        onClick = { showNewSongDialog = !showNewSongDialog },
        colors = ButtonDefaults.buttonColors(containerColor = StudioAmber),
        modifier = Modifier.testTag("new_song_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = "New", tint = Color.Black, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("NEW SONG", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
      }
    }

    if (showNewSongDialog) {
      Spacer(modifier = Modifier.height(8.dp))
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF232733))
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = newSongTitle,
            onValueChange = { newSongTitle = it },
            placeholder = { Text("Song Title...") },
            modifier = Modifier.weight(1f).testTag("new_song_title_input"),
            singleLine = true
          )
          Spacer(modifier = Modifier.width(8.dp))
          Button(
            onClick = {
              if (newSongTitle.isNotBlank()) {
                viewModel.createNewProject(newSongTitle.trim())
                newSongTitle = ""
                showNewSongDialog = false
              }
            },
            colors = ButtonDefaults.buttonColors(containerColor = StudioAmber),
            modifier = Modifier.testTag("confirm_create_song")
          ) {
            Text("CREATE", color = Color.Black, fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // List of Cloud Projects
    if (cloudProjects.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .background(StudioSurfaceElevated)
          .padding(24.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Folder, contentDescription = "Empty", tint = TextMuted, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(8.dp))
          Text("No cloud saved songs yet", color = TextSecondary, fontWeight = FontWeight.SemiBold)
          Text("Click 'Save Cloud' above to backup your current jam!", color = TextMuted, fontSize = 12.sp)
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(cloudProjects) { proj ->
          ProjectItemRow(
            project = proj,
            onLoad = { viewModel.loadProject(proj) },
            onDelete = {
              if (Firebase.auth.currentUser != null) {
                // Delete
              }
            }
          )
        }
      }
    }
  }
}

@Composable
fun ProjectItemRow(
  project: Project,
  onLoad: () -> Unit,
  onDelete: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(8.dp))
      .background(StudioSurfaceElevated)
      .border(1.dp, Color(0xFF333846), RoundedCornerShape(8.dp))
      .padding(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(project.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        Text(
          "${project.tracks.size} Tracks • ${project.bpm} BPM • ${project.timeSignature}",
          fontSize = 12.sp,
          color = TextSecondary
        )
      }

      Button(
        onClick = onLoad,
        colors = ButtonDefaults.buttonColors(containerColor = StudioAmber),
        modifier = Modifier.testTag("load_project_${project.id}")
      ) {
        Text("LOAD", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
      }
    }
  }
}
