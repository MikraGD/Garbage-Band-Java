# GarageStudio: Mobile Multi-Track DAW & Cloud Sync Studio

A mobile digital audio workstation (DAW) for Android inspired by GarageBand, featuring playable instruments (Piano synth, Organ, Acoustic Guitar, Electric Guitar, Drums), multi-track timeline recording, a studio mixing console with real-time effects (reverb, delay, distortion, pan), an integrated sound effects library, and cross-platform Firebase cloud project synchronization with a Web Audio companion player.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following product requirements and architecture decisions were confirmed during the Phase 1 clarification interview:

- **Confirmed Instruments**: Piano synthesizer, Organ (tonewheel drawbar harmonic voicing), Acoustic Guitar (strumming & picking), Electric Guitar (overdrive tone & power chords), and Drums (drum pads & percussion kit).
- **Confirmed Cloud & Web Experience**: Firebase cloud synchronization for real-time project backup and cross-platform sharing, paired with an exportable/viewable Web Audio companion player for browser playback across devices.
- **Confirmed Audio Effects & Mixing**: Full multi-track mixer with volume faders, stereo panning, solo/mute channels, master bus controls, and real-time DSP effects: algorithmic Reverb, Echo Delay, Overdrive Distortion, and Master EQ.

---

## 1. Overview & Core Concept

### What It Does
GarageStudio transforms an Android device into a pocket recording studio. Users can tap to play five distinct virtual instruments with ultra-low latency, record tracks into a multi-track arranger timeline, layer loop presets and one-shot sound effects, fine-tune individual track volume, pan, and DSP effects (reverb, delay, drive), and sync their projects to Firebase so they can be reopened on any device or previewed in a web browser.

### Target Audience & Persona
- **Mobile Musicians & Songwriters**: Rapidly sketching melody ideas, chord progressions, and drum beats on the go without carrying heavy equipment.
- **Podcasters & Content Creators**: Creating theme songs, intro jingles, ambient soundscapes, and layering sound effects.
- **Music Students & Producers**: Learning multi-track arrangement, sound design, and mixing concepts in an intuitive tactile interface.

### Key Value
- **Zero-Friction Tactile Playability**: Fluid multi-touch instruments with realistic touch feedback and responsive acoustic/synthesized response.
- **Full Production Workflow**: Play -> Record -> Layer -> Mix with Effects -> Cloud Sync & Share.
- **Multi-Device Freedom**: Songs authored on the phone can be stored in the cloud, shared via project codes, and played back instantly via the Web Audio companion.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Studio Timeline & Arranger View**:
   - The central workspace displays horizontal color-coded tracks (Piano, Organ, Guitar, Electric Guitar, Drums, and FX tracks).
   - Top transport bar provides Play/Pause, Record, Stop, Loop, BPM adjuster (60–200 BPM), Time Signature selector, Metronome toggle, and current beat/time counter.
   - Users can scrub the playhead, select clips, split/delete, add new tracks, and solo/mute individual layers.
2. **Interactive Instrument View**:
   - Tapping an instrument track header or the "+" button opens that instrument's dedicated playable interface:
     - **Piano Synth**: Scrollable multi-octave 88-key touch keyboard with sustain toggle, pitch bend, octave shift, and attack/decay tone sliders.
     - **Organ**: Vintage tonewheel organ drawbar sliders (16', 8', 5 1/3', 4'), vibrato/chorus toggle, and keyboard interface with sustained organ tone.
     - **Acoustic Guitar**: Dual mode: Chord Strummer (tap chords like C, G, Am, F, Em, D with dynamic strum gestures) and Fretboard/String Picker for manual plucking.
     - **Electric Guitar**: Overdrive toggle, power chords, heavy riff buttons, palm-mute toggle, and lead solo scale notes with amp distortion warmth.
     - **Drums & Percussion**: 8 responsive velocity-sensitive drum pads (Kick, Snare, Hi-Hat Closed, Hi-Hat Open, Crash, Ride, High Tom, Floor Tom) plus a live tap quantizer and tempo sync.
   - Recording mode counts in with a metronome (1-2-3-4), then records played note events directly into the selected track timeline.
3. **Studio Mixer & Sound FX Console**:
   - Dedicated mixing deck showing vertical channel strips for each track plus the Master bus.
   - Each channel strip includes a Volume fader, dB meter indicator, Pan rotary dial, Mute (M), Solo (S), and an FX Insert button.
   - Tapping FX Insert expands real-time DSP knobs:
     - **Reverb**: Room Size, Damping, Wet/Dry mix.
     - **Echo Delay**: Delay Time (ms / beat subdivision), Feedback %, Wet/Dry mix.
     - **Distortion / Drive**: Gain overdrive, Tone cutoff, Output level.
4. **Sound Effects Library**:
   - Built-in library organized by categories: *Rhythms & Loops*, *Ambient & Risers*, *Drum One-Shots*, *Retro Arcade*, *Percussive Transitions*.
   - Audition preview with one tap, or drag/insert directly onto the timeline at the playhead position.
5. **Project Management & Cloud Sync**:
   - Save projects locally and sync to Firebase Firestore.
   - Google Sign-In with Credential Manager for user account security.
   - Project sharing: Generates a project JSON / link that can be opened on another device or viewed via the embedded Web Audio companion player.

### Visual Identity & Theme
- **Aesthetic Direction**: High-end Studio Hardware Dark Mode. Sleek metallic slate backgrounds, warm brushed aluminum accents, glowing LED indicators, and illuminated neon track badges (Amber, Electric Cyan, Emerald Green, Hot Magenta, Sunset Orange).
- **Color Palette & Semantic Tokens**:
  - `Background`: Deep Charcoal / Studio Void (`#121316`)
  - `Surface / Console`: Brushed Dark Slate (`#1E2026`)
  - `Surface Elevated`: Hardware Rack Gray (`#292C35`)
  - `Primary / Accent`: Studio Amber Gold (`#F59E0B`)
  - `Track Piano`: Cyan Glow (`#06B6D4`)
  - `Track Organ`: Warm Purple (`#A855F7`)
  - `Track Acoustic Guitar`: Amber Bronze (`#D97706`)
  - `Track Electric Guitar`: Fiery Crimson (`#EF4444`)
  - `Track Drums`: Emerald Pulse (`#10B981`)
  - `Track FX`: Electric Indigo (`#6366F1`)
  - `Record Button`: Glowing Studio Red (`#DC2626`)
- **Typography & Hierarchy**:
  - Digital Display / Clock: Monospace high-legibility clock font for BPM, bar/beat markers, and time counters (`01:04:12`).
  - Section Headers: Bold, compact uppercase tracking for mixing channel labels (`TRACK 1 - PIANO`, `MASTER BUS`, `FX RACK`).
  - Body & Settings: Clean sans-serif with accessible touch target padding.
- **Motion & Visual Feedback**:
  - VU Meters: Smooth animated LED bar levels responding to track volume and audio playback.
  - Playhead: Silky smooth canvas scrubbing with millisecond precision.
  - Keys and Pads: Immediate haptic & visual lighting when touched (active glow state).

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Low-Latency Sound Generation Engine
- **Chosen Approach**: Built-in High-Performance Multi-Voice Hybrid Synthesizer & Audio Engine using Kotlin coroutine-managed PCM waveform synthesis and native `AudioTrack` playback, combined with pre-rendered acoustic sample waveforms for guitars and drums.
- **Why**: Zero external binary dependencies or obscure C++ NDK setup required; runs completely reliably on standard Android runtime, delivers sub-20ms touch-to-sound response, and allows programmatic real-time DSP effects (gain, panning, delay buffers, wave shaping distortion, and comb-filtering reverb).
- **Alternatives Considered**: Oboe C++ NDK (high build complexity in cloud container) vs. MediaPlayer (unusable latency >150ms for live instrument play).

### Decision 2: Cross-Platform Data Synchronization & Web Player
- **Chosen Approach**: Firebase Firestore structured document persistence storing project metadata, track channel settings, audio effect parameters, and time-stamped note event sequences (`NoteEvent(pitch, velocity, startMs, durationMs)`). An interactive Web Audio API HTML5 companion app (`index.html` web player bundle) is generated and synced, letting users run the exact same song in mobile or web browsers with identical synthesis algorithms.
- **Why**: Firestore allows instantaneous real-time sync across devices with zero-trust security rules and Google Sign-In; the Web Audio companion guarantees full cross-platform playback without requiring desktop native installs.
- **Alternatives Considered**: Raw WAV file cloud uploads (massive network payload, slow sync) vs. MIDI file export only (lacks custom synth and FX parameters).

### Decision 3: Multi-Track Timeline & Non-Destructive Editing
- **Chosen Approach**: Event-driven track timeline where notes, loops, and effects are non-destructive objects with millisecond timestamps, volume envelopes, and loop counts. During playback, the mixer engine multiplexes and sums active track audio buffers into a single master output stream with real-time DSP routing.
- **Why**: Allows infinite re-recording, undo/redo, tempo adjustments without pitch warping, and independent per-track effects tweaking at any stage of production.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        GarageStudio Android UI                         │
├───────────────────┬───────────────────┬────────────────────────────────┤
│  Arranger View    │  Instrument Deck  │  Mixer & FX Rack               │
│  - Track Timeline │  - Piano Synth    │  - Volume Faders & Meters      │
│  - Playhead Scrub │  - Organ Drawbars │  - Stereo Pan Dials            │
│  - Transport Bar  │  - Guitar Chords  │  - Reverb / Delay / Distortion │
│  - Sound FX Shelf │  - Drum Touchpads │  - Solo / Mute Bus             │
└─────────┬─────────┴─────────┬─────────┴───────────────┬────────────────┘
          │                   │                         │
          ▼                   ▼                         ▼
┌────────────────────────────────────────────────────────────────────────┐
│                   StudioViewModel & Project State                      │
│   • activeProject: StateFlow<Project>   • isPlaying / isRecording      │
│   • currentPositionMs: StateFlow<Long>  • selectedTrackId              │
└─────────────────────────────┬──────────────────────────────────────────┘
                              │
          ┌───────────────────┴───────────────────┐
          ▼                                       ▼
┌───────────────────────────────┐   ┌────────────────────────────────────┐
│      AudioEngine & Synth      │   │     ProjectRepository (Firestore)  │
│  - AudioTrack Output Stream   │   │  - Google Sign-In Auth Gate        │
│  - Real-time Multi-Voice Mix  │   │  - users/{uid}/projects collection │
│  - Polyphonic Synth Voices    │   │  - JSON Project Import/Export      │
│  - PCM DSP FX (Reverb, Delay, │   │  - Cross-Platform Sync             │
│    Distortion, Pan)           │   └─────────────────┬──────────────────┘
└───────────────────────────────┘                     │
                                                      ▼
                                    ┌────────────────────────────────────┐
                                    │ Web Audio Companion Export         │
                                    │  - HTML5 / Web Audio synthesizer   │
                                    │  - Cross-platform preview player   │
                                    └────────────────────────────────────┘
```

### Data Model & Firestore Schema
- **`Project`**:
  - `id: String` (Firestore Document ID)
  - `userId: String` (Owner Google UID)
  - `title: String` ("Midnight Groove")
  - `bpm: Int` (e.g., 120)
  - `timeSignature: String` ("4/4")
  - `tracks: List<StudioTrack>`
  - `masterVolume: Float` (0.0 to 1.0)
  - `createdAt: Timestamp?`
  - `updatedAt: Timestamp?`
- **`StudioTrack`**:
  - `id: String`
  - `name: String` ("Electric Guitar", "Beats", etc.)
  - `instrumentType: String` ("PIANO", "ORGAN", "ACOUSTIC_GUITAR", "ELECTRIC_GUITAR", "DRUMS", "SFX")
  - `colorHex: String`
  - `volume: Float` (0.0 to 1.0)
  - `pan: Float` (-1.0 Left to +1.0 Right)
  - `isMuted: Boolean`
  - `isSoloed: Boolean`
  - `effects: TrackEffects` (reverbWet, delayTimeMs, delayFeedback, distortionDrive)
  - `notes: List<NoteEvent>`
- **`NoteEvent`**:
  - `pitch: Int` (MIDI note 0–127)
  - `velocity: Float` (0.0 to 1.0)
  - `startMs: Long` (position in project timeline)
  - `durationMs: Long` (length of note)
  - `sampleId: String?` (for one-shot SFX or drum hits)

### Interactive Component & State Mapping
- **Transport Bar**: Starts/stops `AudioEngine` loop clock, updates `currentPositionMs` Flow at 60fps for silky playhead animation.
- **Instrument Play**: Touching keys or pads immediately triggers `AudioEngine.playNote(...)` for zero latency; if `isRecording == true`, simultaneously appends a new `NoteEvent` with current timeline position.
- **Mixer Controls**: Sliders directly update `track.volume`, `track.pan`, and DSP parameters in real time without audio glitching.
- **Firebase Sync**: Saves locally on every user action; pushes to Firestore with `FieldValue.serverTimestamp()` when user taps Sync or auto-saves periodically.
- **Web Export**: Single-click "Export for Web" generates an HTML file / JSON data URI with Web Audio API code that accurately replicates the song playback in Google Chrome, Safari, or mobile browsers.

---

## 5. Phased Execution Plan (Post-Approval)

1. **Phase 1: Project Identity & Backend Setup**
   - Update `metadata.json` (App Name: "GarageStudio", Description: "Mobile multi-track DAW with playable instruments, mixer, FX, and cloud sync").
   - Update `app/src/main/res/values/strings.xml` with matching `app_name`.
   - Setup Firebase with Google Sign-In & Firestore via `show_aistudio_ui` and `rpc_action`.
   - Create `firebase-blueprint.json` and hardened `firestore.rules`.
2. **Phase 2: Sound Engine & DSP Audio Synthesis**
   - Implement `AudioEngine.kt`: Real-time multitrack PCM audio mixer, multi-voice oscillator synthesizer (sine, triangle, saw, pulse, square), FM synthesis for organ drawbars, pluck synthesis for guitars, sample-based percussion synthesizers for drums, and real-time DSP filters (reverb, delay, drive distortion, pan).
3. **Phase 3: Multi-Track Arranger & Timeline View**
   - Implement timeline UI: Horizontal track lanes, zoomable grid, playhead, clip blocks, transport bar, metronome, and BPM controls.
   - Built-in Sound Effects library drawer with preview and drag-to-timeline functionality.
4. **Phase 4: Playable Virtual Instruments Deck**
   - High-fidelity Piano keyboard with octave shifting.
   - Organ with interactive drawbars and harmonic control.
   - Acoustic Guitar with chord strumming and single note picking.
   - Electric Guitar with distortion amp toggle, power chords, and solo mode.
   - Drums touch pads with responsive velocity and touch animations.
5. **Phase 5: Studio Mixer & FX Rack**
   - Multi-channel mixing board with animated VU meters, volume faders, pan dials, solo/mute buttons, and DSP effects parameter editors.
6. **Phase 6: Cloud Sync & Web Audio Player Companion**
   - Google Sign-In auth gate with Credential Manager.
   - Firestore repository with real-time project sync and sharing.
   - Web Audio player companion exporter (complete standalone HTML5/JS Web Audio synthesis engine) for cross-platform playback.
7. **Phase 7: Verification & Polishing**
   - Verify complete build with `compile_applet`.
   - Ensure edge-to-edge support, touch targets (>=48dp), and smooth performance.
