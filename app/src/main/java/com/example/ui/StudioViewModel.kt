package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioEngine
import com.example.data.model.InstrumentType
import com.example.data.model.NoteEvent
import com.example.data.model.Project
import com.example.data.model.StudioTrack
import com.example.data.model.TrackEffects
import com.example.data.repository.ProjectRepository
import com.example.web.WebDawExport
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class StudioTab {
  ARRANGER,
  INSTRUMENT,
  MIXER,
  PROJECTS,
  WEB_EXPORT
}

class StudioViewModel(application: Application) : AndroidViewModel(application) {

  val audioEngine = AudioEngine(viewModelScope)
  val repository = ProjectRepository(application)

  private val _currentTab = MutableStateFlow(StudioTab.ARRANGER)
  val currentTab = _currentTab.asStateFlow()

  private val _activeProject = MutableStateFlow(createDefaultStarterProject())
  val activeProject = _activeProject.asStateFlow()

  private val _selectedTrackId = MutableStateFlow<String>(_activeProject.value.tracks.first().id)
  val selectedTrackId = _selectedTrackId.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  val statusMessage = _statusMessage.asStateFlow()

  private val _isSyncing = MutableStateFlow(false)
  val isSyncing = _isSyncing.asStateFlow()

  private val _shareIdResult = MutableStateFlow<String?>(null)
  val shareIdResult = _shareIdResult.asStateFlow()

  // Cloud projects flow
  val cloudProjects: StateFlow<List<Project>> = repository.observeProjects()
    .catch { emit(emptyList()) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000L),
      initialValue = emptyList()
    )

  init {
    audioEngine.start()
    audioEngine.currentProject = _activeProject.value

    // Auto-update AudioEngine when activeProject changes
    viewModelScope.launch {
      _activeProject.collect { proj ->
        audioEngine.currentProject = proj
      }
    }
  }

  override fun onCleared() {
    super.onCleared()
    audioEngine.stop()
  }

  fun switchTab(tab: StudioTab) {
    _currentTab.value = tab
  }

  fun selectTrack(trackId: String) {
    _selectedTrackId.value = trackId
  }

  fun getSelectedTrack(): StudioTrack? {
    return _activeProject.value.tracks.find { it.id == _selectedTrackId.value }
  }

  // --- Transport Controls ---

  fun togglePlay() {
    if (audioEngine.isPlaying.value) {
      if (audioEngine.isRecording.value) {
        commitRecordedNotes()
      }
      audioEngine.pause()
    } else {
      audioEngine.play()
    }
  }

  fun toggleRecord() {
    if (audioEngine.isRecording.value) {
      commitRecordedNotes()
      audioEngine.pause()
    } else {
      audioEngine.record()
    }
  }

  fun rewind() {
    audioEngine.rewind()
  }

  fun seekTo(positionMs: Long) {
    audioEngine.seekTo(positionMs)
  }

  fun toggleMetronome() {
    audioEngine.toggleMetronome()
  }

  fun toggleLoop() {
    audioEngine.isLooping = !audioEngine.isLooping
  }

  fun setBpm(newBpm: Int) {
    val clamped = newBpm.coerceIn(40, 240)
    _activeProject.value = _activeProject.value.copy(bpm = clamped)
  }

  fun setMasterVolume(vol: Float) {
    _activeProject.value = _activeProject.value.copy(masterVolume = vol.coerceIn(0f, 1f))
  }

  // --- Live Instrument Playing & Recording ---

  fun playNote(
    pitch: Int,
    velocity: Float = 0.85f,
    durationMs: Long = 600L,
    sampleId: String? = null
  ) {
    val track = getSelectedTrack()
    val instType = try {
      InstrumentType.valueOf(track?.instrumentType ?: InstrumentType.PIANO.name)
    } catch (_: Exception) {
      InstrumentType.PIANO
    }

    audioEngine.playNote(
      instrumentType = instType,
      pitch = pitch,
      velocity = velocity,
      durationMs = durationMs,
      track = track,
      sampleId = sampleId
    )
  }

  private fun commitRecordedNotes() {
    val recorded = audioEngine.recordedEvents.toList()
    audioEngine.recordedEvents.clear()
    if (recorded.isEmpty()) return

    val targetTrackId = _selectedTrackId.value
    val updatedTracks = _activeProject.value.tracks.map { track ->
      if (track.id == targetTrackId) {
        track.copy(notes = track.notes + recorded)
      } else {
        track
      }
    }
    _activeProject.value = _activeProject.value.copy(tracks = updatedTracks)
    showMessage("Recorded ${recorded.size} notes to ${getSelectedTrack()?.name}")
  }

  // --- Track Operations ---

  fun addTrack(type: InstrumentType) {
    val newTrack = StudioTrack(
      id = UUID.randomUUID().toString(),
      name = "${type.displayName} ${(_activeProject.value.tracks.count { it.instrumentType == type.name } + 1)}",
      instrumentType = type.name,
      colorHex = type.defaultColor,
      volume = 0.8f,
      pan = 0f,
      effects = TrackEffects()
    )
    val updatedList = _activeProject.value.tracks + newTrack
    _activeProject.value = _activeProject.value.copy(tracks = updatedList)
    _selectedTrackId.value = newTrack.id
    showMessage("Added ${newTrack.name}")
  }

  fun deleteTrack(trackId: String) {
    if (_activeProject.value.tracks.size <= 1) {
      showMessage("Project must have at least one track.")
      return
    }
    val updatedList = _activeProject.value.tracks.filter { it.id != trackId }
    _activeProject.value = _activeProject.value.copy(tracks = updatedList)
    if (_selectedTrackId.value == trackId) {
      _selectedTrackId.value = updatedList.first().id
    }
  }

  fun updateTrackVolume(trackId: String, volume: Float) {
    val updatedTracks = _activeProject.value.tracks.map {
      if (it.id == trackId) it.copy(volume = volume.coerceIn(0f, 1f)) else it
    }
    _activeProject.value = _activeProject.value.copy(tracks = updatedTracks)
  }

  fun updateTrackPan(trackId: String, pan: Float) {
    val updatedTracks = _activeProject.value.tracks.map {
      if (it.id == trackId) it.copy(pan = pan.coerceIn(-1f, 1f)) else it
    }
    _activeProject.value = _activeProject.value.copy(tracks = updatedTracks)
  }

  fun toggleMute(trackId: String) {
    val updatedTracks = _activeProject.value.tracks.map {
      if (it.id == trackId) it.copy(isMuted = !it.isMuted) else it
    }
    _activeProject.value = _activeProject.value.copy(tracks = updatedTracks)
  }

  fun toggleSolo(trackId: String) {
    val updatedTracks = _activeProject.value.tracks.map {
      if (it.id == trackId) it.copy(isSoloed = !it.isSoloed) else it
    }
    _activeProject.value = _activeProject.value.copy(tracks = updatedTracks)
  }

  fun updateTrackEffects(trackId: String, effects: TrackEffects) {
    val updatedTracks = _activeProject.value.tracks.map {
      if (it.id == trackId) it.copy(effects = effects) else it
    }
    _activeProject.value = _activeProject.value.copy(tracks = updatedTracks)
  }

  fun insertSoundEffect(sampleId: String, name: String) {
    val currentMs = audioEngine.playbackPositionMs.value
    val sfxNote = NoteEvent(
      pitch = 60,
      velocity = 0.9f,
      startMs = currentMs,
      durationMs = 1200L,
      sampleId = sampleId
    )

    // Find or create SFX track
    var sfxTrack = _activeProject.value.tracks.find { it.instrumentType == InstrumentType.SFX.name }
    if (sfxTrack == null) {
      sfxTrack = StudioTrack(
        id = UUID.randomUUID().toString(),
        name = "Sound FX",
        instrumentType = InstrumentType.SFX.name,
        colorHex = InstrumentType.SFX.defaultColor,
        notes = listOf(sfxNote)
      )
      _activeProject.value = _activeProject.value.copy(tracks = _activeProject.value.tracks + sfxTrack)
    } else {
      val updatedTracks = _activeProject.value.tracks.map {
        if (it.id == sfxTrack.id) it.copy(notes = it.notes + sfxNote) else it
      }
      _activeProject.value = _activeProject.value.copy(tracks = updatedTracks)
    }

    // Audition sound effect
    audioEngine.playNote(
      instrumentType = InstrumentType.SFX,
      pitch = 60,
      velocity = 0.9f,
      durationMs = 1200L,
      sampleId = sampleId
    )
    showMessage("Inserted $name at ${currentMs / 1000}s")
  }

  // --- Cloud Sync & Sharing ---

  fun syncToCloud() {
    val user = Firebase.auth.currentUser
    if (user == null) {
      showMessage("Please sign in to save and sync projects to cloud.")
      return
    }

    _isSyncing.value = true
    viewModelScope.launch {
      val projectToSave = _activeProject.value.copy(userId = user.uid)
      val result = repository.saveProject(projectToSave)
      _isSyncing.value = false
      result.onSuccess { savedId ->
        _activeProject.value = _activeProject.value.copy(id = savedId, userId = user.uid)
        showMessage("Project synced to cloud!")
      }.onFailure { err ->
        showMessage("Cloud sync failed: ${err.message}")
      }
    }
  }

  fun shareProject() {
    val user = Firebase.auth.currentUser
    if (user == null) {
      showMessage("Please sign in to share projects.")
      return
    }

    _isSyncing.value = true
    viewModelScope.launch {
      val result = repository.shareProject(_activeProject.value)
      _isSyncing.value = false
      result.onSuccess { shareId ->
        _shareIdResult.value = shareId
        showMessage("Project shared! Share code: $shareId")
      }.onFailure { err ->
        showMessage("Share failed: ${err.message}")
      }
    }
  }

  fun loadProject(project: Project) {
    audioEngine.pause()
    _activeProject.value = project
    _selectedTrackId.value = project.tracks.firstOrNull()?.id ?: ""
    audioEngine.rewind()
    showMessage("Loaded '${project.title}'")
    _currentTab.value = StudioTab.ARRANGER
  }

  fun createNewProject(title: String = "New Song") {
    audioEngine.pause()
    val fresh = createDefaultStarterProject().copy(
      id = UUID.randomUUID().toString(),
      title = title,
      userId = Firebase.auth.currentUser?.uid ?: ""
    )
    _activeProject.value = fresh
    _selectedTrackId.value = fresh.tracks.first().id
    audioEngine.rewind()
    showMessage("Created '$title'")
    _currentTab.value = StudioTab.ARRANGER
  }

  fun getWebDawHtml(): String {
    return WebDawExport.generateWebPlayerHtml(_activeProject.value)
  }

  fun showMessage(msg: String) {
    _statusMessage.value = msg
  }

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  companion object {
    fun createDefaultStarterProject(): Project {
      // 120 BPM: 1 beat = 500ms, 1 bar (4 beats) = 2000ms
      // 4-bar groove with all 5 confirmed instruments!

      // Piano chords (Cmaj7 -> Am7 -> Fmaj7 -> G7)
      val pianoNotes = listOf(
        // Bar 1: C - E - G - B
        NoteEvent(pitch = 60, velocity = 0.8f, startMs = 0L, durationMs = 1800L),
        NoteEvent(pitch = 64, velocity = 0.75f, startMs = 0L, durationMs = 1800L),
        NoteEvent(pitch = 67, velocity = 0.75f, startMs = 0L, durationMs = 1800L),
        NoteEvent(pitch = 71, velocity = 0.7f, startMs = 0L, durationMs = 1800L),
        // Bar 2: A - C - E - G
        NoteEvent(pitch = 57, velocity = 0.8f, startMs = 2000L, durationMs = 1800L),
        NoteEvent(pitch = 60, velocity = 0.75f, startMs = 2000L, durationMs = 1800L),
        NoteEvent(pitch = 64, velocity = 0.75f, startMs = 2000L, durationMs = 1800L),
        NoteEvent(pitch = 67, velocity = 0.7f, startMs = 2000L, durationMs = 1800L),
        // Bar 3: F - A - C - E
        NoteEvent(pitch = 53, velocity = 0.8f, startMs = 4000L, durationMs = 1800L),
        NoteEvent(pitch = 57, velocity = 0.75f, startMs = 4000L, durationMs = 1800L),
        NoteEvent(pitch = 60, velocity = 0.75f, startMs = 4000L, durationMs = 1800L),
        NoteEvent(pitch = 64, velocity = 0.7f, startMs = 4000L, durationMs = 1800L),
        // Bar 4: G - B - D - F
        NoteEvent(pitch = 55, velocity = 0.8f, startMs = 6000L, durationMs = 1800L),
        NoteEvent(pitch = 59, velocity = 0.75f, startMs = 6000L, durationMs = 1800L),
        NoteEvent(pitch = 62, velocity = 0.75f, startMs = 6000L, durationMs = 1800L),
        NoteEvent(pitch = 65, velocity = 0.7f, startMs = 6000L, durationMs = 1800L)
      )

      // Organ sustaining pads
      val organNotes = listOf(
        NoteEvent(pitch = 60, velocity = 0.7f, startMs = 0L, durationMs = 3800L),
        NoteEvent(pitch = 67, velocity = 0.65f, startMs = 0L, durationMs = 3800L),
        NoteEvent(pitch = 57, velocity = 0.7f, startMs = 4000L, durationMs = 3800L),
        NoteEvent(pitch = 64, velocity = 0.65f, startMs = 4000L, durationMs = 3800L)
      )

      // Acoustic Guitar strumming arpeggios
      val guitarNotes = mutableListOf<NoteEvent>()
      val pattern = listOf(0, 4, 7, 12, 7, 4)
      for (bar in 0..3) {
        val root = when (bar) {
          0 -> 48 // C
          1 -> 45 // A
          2 -> 41 // F
          else -> 43 // G
        }
        for (i in pattern.indices) {
          guitarNotes.add(
            NoteEvent(
              pitch = root + pattern[i],
              velocity = 0.75f,
              startMs = bar * 2000L + i * 300L,
              durationMs = 450L
            )
          )
        }
      }

      // Electric Guitar lead riffs in Bar 3 & 4
      val electricGuitarNotes = listOf(
        NoteEvent(pitch = 72, velocity = 0.85f, startMs = 4000L, durationMs = 600L),
        NoteEvent(pitch = 74, velocity = 0.85f, startMs = 4600L, durationMs = 600L),
        NoteEvent(pitch = 76, velocity = 0.9f, startMs = 5200L, durationMs = 900L),
        NoteEvent(pitch = 79, velocity = 0.95f, startMs = 6200L, durationMs = 1200L),
        NoteEvent(pitch = 76, velocity = 0.85f, startMs = 7400L, durationMs = 500L)
      )

      // Drum pattern (Kick on 1 & 3, Snare on 2 & 4, Hi-hat every 8th note)
      val drumNotes = mutableListOf<NoteEvent>()
      for (bar in 0..3) {
        val barStart = bar * 2000L
        // Kick on beats 0 and 2 (0ms and 1000ms)
        drumNotes.add(NoteEvent(pitch = 36, velocity = 0.9f, startMs = barStart, durationMs = 300L, sampleId = "kick"))
        drumNotes.add(NoteEvent(pitch = 36, velocity = 0.85f, startMs = barStart + 1000L, durationMs = 300L, sampleId = "kick"))

        // Snare on beats 1 and 3 (500ms and 1500ms)
        drumNotes.add(NoteEvent(pitch = 38, velocity = 0.9f, startMs = barStart + 500L, durationMs = 250L, sampleId = "snare"))
        drumNotes.add(NoteEvent(pitch = 38, velocity = 0.9f, startMs = barStart + 1500L, durationMs = 250L, sampleId = "snare"))

        // Hi-hat on every 8th note (0, 250, 500, 750, 1000, 1250, 1500, 1750)
        for (step in 0..7) {
          val isOp = (step == 7) // Open on last 8th note
          drumNotes.add(
            NoteEvent(
              pitch = if (isOp) 46 else 42,
              velocity = if (step % 2 == 0) 0.8f else 0.6f,
              startMs = barStart + step * 250L,
              durationMs = if (isOp) 350L else 100L,
              sampleId = if (isOp) "hihat_open" else "hihat_closed"
            )
          )
        }
      }

      val track1 = StudioTrack(
        id = "t_piano",
        name = "Piano Synth",
        instrumentType = InstrumentType.PIANO.name,
        colorHex = InstrumentType.PIANO.defaultColor,
        volume = 0.85f,
        pan = -0.2f,
        effects = TrackEffects(reverbWet = 0.25f, delayTimeMs = 250, delayFeedback = 0.2f),
        notes = pianoNotes
      )

      val track2 = StudioTrack(
        id = "t_organ",
        name = "Tonewheel Organ",
        instrumentType = InstrumentType.ORGAN.name,
        colorHex = InstrumentType.ORGAN.defaultColor,
        volume = 0.7f,
        pan = 0.3f,
        effects = TrackEffects(reverbWet = 0.35f, delayTimeMs = 300, delayFeedback = 0.15f),
        notes = organNotes
      )

      val track3 = StudioTrack(
        id = "t_acoustic_guitar",
        name = "Acoustic Guitar",
        instrumentType = InstrumentType.ACOUSTIC_GUITAR.name,
        colorHex = InstrumentType.ACOUSTIC_GUITAR.defaultColor,
        volume = 0.8f,
        pan = -0.35f,
        effects = TrackEffects(reverbWet = 0.2f, delayTimeMs = 200, delayFeedback = 0.1f),
        notes = guitarNotes
      )

      val track4 = StudioTrack(
        id = "t_electric_guitar",
        name = "Electric Guitar",
        instrumentType = InstrumentType.ELECTRIC_GUITAR.name,
        colorHex = InstrumentType.ELECTRIC_GUITAR.defaultColor,
        volume = 0.85f,
        pan = 0.4f,
        effects = TrackEffects(reverbWet = 0.3f, distortionDrive = 0.4f),
        notes = electricGuitarNotes
      )

      val track5 = StudioTrack(
        id = "t_drums",
        name = "Studio Drums",
        instrumentType = InstrumentType.DRUMS.name,
        colorHex = InstrumentType.DRUMS.defaultColor,
        volume = 0.9f,
        pan = 0.0f,
        effects = TrackEffects(reverbWet = 0.1f),
        notes = drumNotes
      )

      return Project(
        id = "starter_groove",
        title = "Midnight Jam",
        bpm = 120,
        timeSignature = "4/4",
        masterVolume = 0.85f,
        tracks = listOf(track1, track2, track3, track4, track5)
      )
    }
  }
}
