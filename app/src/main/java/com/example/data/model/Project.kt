package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.UUID

enum class InstrumentType(val displayName: String, val defaultColor: String) {
  PIANO("Piano Synth", "#06B6D4"),
  ORGAN("Tonewheel Organ", "#A855F7"),
  ACOUSTIC_GUITAR("Acoustic Guitar", "#D97706"),
  ELECTRIC_GUITAR("Electric Guitar", "#EF4444"),
  DRUMS("Studio Drums", "#10B981"),
  SFX("Sound FX", "#6366F1")
}

@IgnoreExtraProperties
data class NoteEvent(
  val id: String = UUID.randomUUID().toString(),
  val pitch: Int = 60,            // MIDI pitch (e.g. 60 = Middle C)
  val velocity: Float = 0.8f,     // 0.0 to 1.0
  val startMs: Long = 0L,         // Start offset in project timeline
  val durationMs: Long = 500L,    // Duration
  val sampleId: String? = null    // Optional sound effect or drum hit identifier
)

@IgnoreExtraProperties
data class TrackEffects(
  val reverbWet: Float = 0.15f,       // 0.0 to 1.0
  val delayTimeMs: Int = 250,         // 50 to 1000 ms
  val delayFeedback: Float = 0.25f,   // 0.0 to 0.85
  val distortionDrive: Float = 0.0f   // 0.0 to 1.0
)

@IgnoreExtraProperties
data class StudioTrack(
  val id: String = UUID.randomUUID().toString(),
  val name: String = "Track",
  val instrumentType: String = InstrumentType.PIANO.name,
  val colorHex: String = "#06B6D4",
  val volume: Float = 0.8f,           // 0.0 to 1.0
  val pan: Float = 0.0f,              // -1.0 (Left) to +1.0 (Right)
  val isMuted: Boolean = false,
  val isSoloed: Boolean = false,
  val effects: TrackEffects = TrackEffects(),
  val notes: List<NoteEvent> = emptyList()
)

@IgnoreExtraProperties
data class Project(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "Untitled Song",
  val bpm: Int = 120,
  val timeSignature: String = "4/4",
  val masterVolume: Float = 0.85f,
  val tracks: List<StudioTrack> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
) {
  // Model helper for total project duration in milliseconds
  fun getDurationMs(): Long {
    var maxEndMs = 16000L // Minimum 4 bars at 120bpm
    for (track in tracks) {
      for (note in track.notes) {
        val noteEnd = note.startMs + note.durationMs
        if (noteEnd > maxEndMs) {
          maxEndMs = noteEnd
        }
      }
    }
    return maxEndMs
  }
}

@IgnoreExtraProperties
data class SharedProject(
  @DocumentId val id: String = "",
  val shareCode: String = "",
  val userId: String = "",
  val title: String = "",
  val projectJson: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null
)
