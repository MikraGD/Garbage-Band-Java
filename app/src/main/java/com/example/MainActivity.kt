package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InstrumentType
import com.example.ui.StudioTab
import com.example.ui.StudioViewModel
import com.example.ui.arranger.ArrangerScreen
import com.example.ui.instruments.AcousticGuitarScreen
import com.example.ui.instruments.DrumsScreen
import com.example.ui.instruments.ElectricGuitarScreen
import com.example.ui.instruments.OrganScreen
import com.example.ui.instruments.PianoScreen
import com.example.ui.mixer.MixerScreen
import com.example.ui.projects.ProjectsScreen
import com.example.ui.sfx.SoundEffectsDrawer
import com.example.ui.theme.GarageStudioTheme
import com.example.ui.theme.StudioAmber
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioVoid
import com.example.ui.web.WebExportScreen

class MainActivity : ComponentActivity() {

  private val viewModel: StudioViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      GarageStudioTheme {
        MainAppContent(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun MainAppContent(viewModel: StudioViewModel) {
  val currentTab by viewModel.currentTab.collectAsState()
  val statusMsg by viewModel.statusMessage.collectAsState()
  val selectedTrack = viewModel.getSelectedTrack()
  val context = LocalContext.current
  var showSfxDrawer by remember { mutableStateOf(false) }

  // Back handling
  BackHandler(enabled = currentTab != StudioTab.ARRANGER || showSfxDrawer) {
    if (showSfxDrawer) {
      showSfxDrawer = false
    } else {
      viewModel.switchTab(StudioTab.ARRANGER)
    }
  }

  // Toast status notifications
  LaunchedEffect(statusMsg) {
    statusMsg?.let {
      Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
      viewModel.clearStatusMessage()
    }
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding(),
    bottomBar = {
      NavigationBar(
        containerColor = StudioSurfaceElevated,
        modifier = Modifier.testTag("main_bottom_nav")
      ) {
        // Arranger Timeline Tab
        NavigationBarItem(
          selected = currentTab == StudioTab.ARRANGER,
          onClick = {
            showSfxDrawer = false
            viewModel.switchTab(StudioTab.ARRANGER)
          },
          icon = { Icon(Icons.Default.MusicNote, contentDescription = "Arranger") },
          label = { Text("Arranger", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = StudioAmber,
            indicatorColor = StudioAmber
          ),
          modifier = Modifier.testTag("nav_arranger")
        )

        // Play Instrument Tab
        NavigationBarItem(
          selected = currentTab == StudioTab.INSTRUMENT,
          onClick = {
            showSfxDrawer = false
            viewModel.switchTab(StudioTab.INSTRUMENT)
          },
          icon = { Icon(Icons.Default.Piano, contentDescription = "Instrument") },
          label = { Text("Play", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = StudioAmber,
            indicatorColor = StudioAmber
          ),
          modifier = Modifier.testTag("nav_instrument")
        )

        // Mixer Console Tab
        NavigationBarItem(
          selected = currentTab == StudioTab.MIXER,
          onClick = {
            showSfxDrawer = false
            viewModel.switchTab(StudioTab.MIXER)
          },
          icon = { Icon(Icons.Default.Tune, contentDescription = "Mixer") },
          label = { Text("Mixer", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = StudioAmber,
            indicatorColor = StudioAmber
          ),
          modifier = Modifier.testTag("nav_mixer")
        )

        // Cloud Songs Tab
        NavigationBarItem(
          selected = currentTab == StudioTab.PROJECTS,
          onClick = {
            showSfxDrawer = false
            viewModel.switchTab(StudioTab.PROJECTS)
          },
          icon = { Icon(Icons.Default.Folder, contentDescription = "Projects") },
          label = { Text("Songs", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = StudioAmber,
            indicatorColor = StudioAmber
          ),
          modifier = Modifier.testTag("nav_projects")
        )

        // Web DAW Tab
        NavigationBarItem(
          selected = currentTab == StudioTab.WEB_EXPORT,
          onClick = {
            showSfxDrawer = false
            viewModel.switchTab(StudioTab.WEB_EXPORT)
          },
          icon = { Icon(Icons.Default.Language, contentDescription = "Web DAW") },
          label = { Text("Web DAW", fontSize = 11.sp) },
          colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Black,
            selectedTextColor = StudioAmber,
            indicatorColor = StudioAmber
          ),
          modifier = Modifier.testTag("nav_web_daw")
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(StudioVoid)
    ) {
      when (currentTab) {
        StudioTab.ARRANGER -> {
          ArrangerScreen(
            viewModel = viewModel,
            onOpenSfxDrawer = { showSfxDrawer = true }
          )
        }

        StudioTab.INSTRUMENT -> {
          val instType = try {
            InstrumentType.valueOf(selectedTrack?.instrumentType ?: InstrumentType.PIANO.name)
          } catch (_: Exception) {
            InstrumentType.PIANO
          }

          when (instType) {
            InstrumentType.PIANO -> PianoScreen(viewModel = viewModel)
            InstrumentType.ORGAN -> OrganScreen(viewModel = viewModel)
            InstrumentType.ACOUSTIC_GUITAR -> AcousticGuitarScreen(viewModel = viewModel)
            InstrumentType.ELECTRIC_GUITAR -> ElectricGuitarScreen(viewModel = viewModel)
            InstrumentType.DRUMS -> DrumsScreen(viewModel = viewModel)
            InstrumentType.SFX -> SoundEffectsDrawer(
              viewModel = viewModel,
              onDismiss = { viewModel.switchTab(StudioTab.ARRANGER) }
            )
          }
        }

        StudioTab.MIXER -> {
          MixerScreen(viewModel = viewModel)
        }

        StudioTab.PROJECTS -> {
          ProjectsScreen(viewModel = viewModel)
        }

        StudioTab.WEB_EXPORT -> {
          WebExportScreen(viewModel = viewModel)
        }
      }

      // SFX Drawer Overlay
      if (showSfxDrawer) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
        ) {
          SoundEffectsDrawer(
            viewModel = viewModel,
            onDismiss = { showSfxDrawer = false }
          )
        }
      }
    }
  }
}
