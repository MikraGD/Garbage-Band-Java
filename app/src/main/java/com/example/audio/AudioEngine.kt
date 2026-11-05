package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.data.model.InstrumentType
import com.example.data.model.NoteEvent
import com.example.data.model.Project
import com.example.data.model.StudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tanh
import kotlin.random.Random

class AudioEngine(private val scope: CoroutineScope) {

  companion object {
    const val SAMPLE_RATE = 44100
    private const val BUFFER_SIZE = 1024
    private const val MAX_VOICES = 32

    fun midiToFreq(midiNote: Int): Float {
      return (440.0 * Math.pow(2.0, (midiNote - 69).toDouble() / 12.0)).toFloat()
    }
  }

  // Active playing voice
  private class ActiveVoice(
    val id: Long,
    val instrumentType: InstrumentType,
    val frequency: Float,
    val velocity: Float,
    val startSample: Long,
    val durationSamples: Long,
    val volume: Float = 0.8f,
    val pan: Float = 0.0f,
    val reverbWet: Float = 0.15f,
    val delayFeedback: Float = 0.2f,
    val distortionDrive: Float = 0.0f,
    val sampleId: String? = null
  ) {
    var currentSample: Long = 0L
    var phase: Double = 0.0
    // Karplus-Strong string buffer for acoustic guitar
    var stringBuffer: FloatArray? = null
    var stringIndex: Int = 0
  }

  private var audioTrack: AudioTrack? = null
  private val isRunning = AtomicBoolean(false)
  private var renderJob: Job? = null

  // Voices currently active
  private val activeVoices = ConcurrentHashMap<Long, ActiveVoice>()
  private val voiceIdCounter = AtomicLong(1L)

  // Sequencer playback state
  private val _isPlaying = MutableStateFlow(false)
  val isPlaying = _isPlaying.asStateFlow()

  private val _isRecording = MutableStateFlow(false)
  val isRecording = _isRecording.asStateFlow()

  private val _playbackPositionMs = MutableStateFlow(0L)
  val playbackPositionMs = _playbackPositionMs.asStateFlow()

  private val _isMetronomeEnabled = MutableStateFlow(false)
  val isMetronomeEnabled = _isMetronomeEnabled.asStateFlow()

  // Peak metering for mixer
  private val _masterPeakLevel = MutableStateFlow(0f)
  val masterPeakLevel = _masterPeakLevel.asStateFlow()

  // Current active project reference for sequencer playback
  @Volatile var currentProject: Project? = null
  @Volatile var isLooping: Boolean = true

  // Recorded notes queue for active recording session
  val recordedEvents = ConcurrentLinkedQueue<NoteEvent>()
  private var recordingStartUptimeMs = 0L

  // DSP Delay buffer (stereo)
  private val delayBufferLength = SAMPLE_RATE * 2 // 2 seconds max delay
  private val delayBufferL = FloatArray(delayBufferLength)
  private val delayBufferR = FloatArray(delayBufferLength)
  private var delayWriteIndex = 0

  // DSP Reverb comb filter buffers
  private val combBuffers = Array(4) { FloatArray(1600 + it * 320) }
  private val combIndices = IntArray(4)

  fun start() {
    if (isRunning.getAndSet(true)) return

    val minBufSize = AudioTrack.getMinBufferSize(
      SAMPLE_RATE,
      AudioFormat.CHANNEL_OUT_STEREO,
      AudioFormat.ENCODING_PCM_16BIT
    )

    audioTrack = AudioTrack.Builder()
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_MEDIA)
          .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
          .build()
      )
      .setAudioFormat(
        AudioFormat.Builder()
          .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
          .setSampleRate(SAMPLE_RATE)
          .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
          .build()
      )
      .setBufferSizeInBytes(max(minBufSize, BUFFER_SIZE * 4))
      .setTransferMode(AudioTrack.MODE_STREAM)
      .build()

    audioTrack?.play()

    renderJob = scope.launch(Dispatchers.Default) {
      val pcmBuffer = ShortArray(BUFFER_SIZE * 2) // Stereo (L, R)
      val floatBufferL = FloatArray(BUFFER_SIZE)
      val floatBufferR = FloatArray(BUFFER_SIZE)

      var totalSequencerSamples = 0L
      var lastReportTimeMs = System.currentTimeMillis()

      while (isActive && isRunning.get()) {
        // Clear frame float buffers
        floatBufferL.fill(0f)
        floatBufferR.fill(0f)

        val project = currentProject
        val bpm = project?.bpm ?: 120
        val samplesPerBeat = (SAMPLE_RATE * 60.0 / bpm).toLong()

        // 1. Check Sequencer Event Scheduling if playing
        if (_isPlaying.value && project != null) {
          val currentMs = (totalSequencerSamples * 1000L) / SAMPLE_RATE
          _playbackPositionMs.value = currentMs

          val loopDurationMs = project.getDurationMs()
          if (isLooping && currentMs >= loopDurationMs) {
            totalSequencerSamples = 0L
            _playbackPositionMs.value = 0L
          } else if (!isLooping && currentMs >= loopDurationMs) {
            _isPlaying.value = false
            _isRecording.value = false
          }

          // Trigger sequencer notes within current buffer window
          val windowEndMs = ((totalSequencerSamples + BUFFER_SIZE) * 1000L) / SAMPLE_RATE
          for (track in project.tracks) {
            if (track.isMuted) continue
            // Check solo: if any track is soloed, only play soloed tracks
            val anySoloed = project.tracks.any { it.isSoloed }
            if (anySoloed && !track.isSoloed) continue

            val instType = try {
              InstrumentType.valueOf(track.instrumentType)
            } catch (e: Exception) {
              InstrumentType.PIANO
            }

            for (note in track.notes) {
              if (note.startMs in currentMs until windowEndMs) {
                val delayOffsetSamples = max(0L, (note.startMs - currentMs) * SAMPLE_RATE / 1000L)
                triggerVoiceInternal(
                  instrument = instType,
                  pitch = note.pitch,
                  velocity = note.velocity,
                  durationMs = note.durationMs,
                  trackVolume = track.volume,
                  trackPan = track.pan,
                  reverbWet = track.effects.reverbWet,
                  delayFeedback = track.effects.delayFeedback,
                  distortionDrive = track.effects.distortionDrive,
                  sampleId = note.sampleId,
                  startSampleOffset = delayOffsetSamples
                )
              }
            }
          }

          // Metronome click on beats
          if (_isMetronomeEnabled.value) {
            val beatIndexBefore = totalSequencerSamples / samplesPerBeat
            val beatIndexAfter = (totalSequencerSamples + BUFFER_SIZE) / samplesPerBeat
            if (beatIndexAfter > beatIndexBefore) {
              // Click high on beat 0, normal on other beats
              val isDownbeat = (beatIndexAfter % 4) == 0L
              triggerMetronomeClick(isDownbeat)
            }
          }

          totalSequencerSamples += BUFFER_SIZE
        }

        // 2. Synthesize audio for all active voices into frame float buffers
        val voicesToRemove = mutableListOf<Long>()
        var activeVoicesPeak = 0f

        for ((voiceId, voice) in activeVoices) {
          val samplesToProcess = BUFFER_SIZE
          var ended = false

          for (i in 0 until samplesToProcess) {
            if (voice.currentSample < 0) {
              // Note start offset
              voice.currentSample++
              continue
            }

            if (voice.currentSample >= voice.durationSamples) {
              ended = true
              break
            }

            val sampleVal = generateVoiceSample(voice)
            voice.currentSample++

            // Apply voice volume and panning
            val effectiveVol = voice.volume * voice.velocity
            val panL = if (voice.pan <= 0f) 1f else (1f - voice.pan)
            val panR = if (voice.pan >= 0f) 1f else (1f + voice.pan)

            floatBufferL[i] += sampleVal * effectiveVol * panL
            floatBufferR[i] += sampleVal * effectiveVol * panR
          }

          if (ended) {
            voicesToRemove.add(voiceId)
          }
        }

        for (id in voicesToRemove) {
          activeVoices.remove(id)
        }

        // 3. Apply Global DSP Effects (Delay and Reverb) to Frame Buffer
        applyDspEffects(floatBufferL, floatBufferR)

        // 4. Master Volume and Soft Limiter, convert to PCM 16-bit
        val masterVol = project?.masterVolume ?: 0.85f
        var framePeak = 0f

        for (i in 0 until BUFFER_SIZE) {
          var outL = floatBufferL[i] * masterVol
          var outR = floatBufferR[i] * masterVol

          // Soft hyperbolic tangent limiter to avoid harsh digital clipping
          outL = tanh(outL.toDouble()).toFloat()
          outR = tanh(outR.toDouble()).toFloat()

          val absPeak = max(Math.abs(outL), Math.abs(outR))
          if (absPeak > framePeak) framePeak = absPeak

          val pcmL = (outL * 32767f).toInt().coerceIn(-32768, 32767).toShort()
          val pcmR = (outR * 32767f).toInt().coerceIn(-32768, 32767).toShort()

          pcmBuffer[i * 2] = pcmL
          pcmBuffer[i * 2 + 1] = pcmR
        }

        // Write to AudioTrack
        audioTrack?.write(pcmBuffer, 0, pcmBuffer.size)

        // Smooth VU meter level reporting
        val now = System.currentTimeMillis()
        if (now - lastReportTimeMs >= 50) {
          _masterPeakLevel.value = framePeak
          lastReportTimeMs = now
        }
      }
    }
  }

  fun stop() {
    isRunning.set(false)
    renderJob?.cancel()
    renderJob = null
    activeVoices.clear()
    try {
      audioTrack?.stop()
      audioTrack?.release()
    } catch (_: Exception) {}
    audioTrack = null
  }

  // --- Live Instrument Play API ---

  fun playNote(
    instrumentType: InstrumentType,
    pitch: Int,
    velocity: Float = 0.85f,
    durationMs: Long = 600L,
    track: StudioTrack? = null,
    sampleId: String? = null
  ): Long {
    val trackVol = track?.volume ?: 0.8f
    val trackPan = track?.pan ?: 0.0f
    val reverb = track?.effects?.reverbWet ?: 0.15f
    val delay = track?.effects?.delayFeedback ?: 0.2f
    val dist = track?.effects?.distortionDrive ?: 0.0f

    val voiceId = triggerVoiceInternal(
      instrument = instrumentType,
      pitch = pitch,
      velocity = velocity,
      durationMs = durationMs,
      trackVolume = trackVol,
      trackPan = trackPan,
      reverbWet = reverb,
      delayFeedback = delay,
      distortionDrive = dist,
      sampleId = sampleId,
      startSampleOffset = 0L
    )

    // If recording, log NoteEvent
    if (_isRecording.value) {
      val nowUptime = System.currentTimeMillis()
      val startMs = _playbackPositionMs.value
      recordedEvents.add(
        NoteEvent(
          pitch = pitch,
          velocity = velocity,
          startMs = startMs,
          durationMs = durationMs,
          sampleId = sampleId
        )
      )
    }

    return voiceId
  }

  private fun triggerVoiceInternal(
    instrument: InstrumentType,
    pitch: Int,
    velocity: Float,
    durationMs: Long,
    trackVolume: Float,
    trackPan: Float,
    reverbWet: Float,
    delayFeedback: Float,
    distortionDrive: Float,
    sampleId: String?,
    startSampleOffset: Long
  ): Long {
    // Prevent voice overflow
    if (activeVoices.size >= MAX_VOICES) {
      val oldestKey = activeVoices.keys.firstOrNull()
      if (oldestKey != null) activeVoices.remove(oldestKey)
    }

    val id = voiceIdCounter.incrementAndGet()
    val freq = midiToFreq(pitch)
    val durSamples = (durationMs * SAMPLE_RATE / 1000L).coerceAtLeast(100L)

    val voice = ActiveVoice(
      id = id,
      instrumentType = instrument,
      frequency = freq,
      velocity = velocity,
      startSample = -startSampleOffset,
      durationSamples = durSamples,
      volume = trackVolume,
      pan = trackPan,
      reverbWet = reverbWet,
      delayFeedback = delayFeedback,
      distortionDrive = distortionDrive,
      sampleId = sampleId
    )

    // Initialize Karplus-Strong string buffer for Acoustic Guitar
    if (instrument == InstrumentType.ACOUSTIC_GUITAR) {
      val period = (SAMPLE_RATE / freq).toInt().coerceIn(10, 2048)
      val buf = FloatArray(period)
      for (k in 0 until period) {
        buf[k] = (Random.nextFloat() * 2f - 1f) * 0.9f
      }
      voice.stringBuffer = buf
      voice.stringIndex = 0
    }

    activeVoices[id] = voice
    return id
  }

  // --- Voice Synthesis Algorithmic Engine ---

  private fun generateVoiceSample(voice: ActiveVoice): Float {
    val t = voice.currentSample.toDouble() / SAMPLE_RATE
    val progress = voice.currentSample.toFloat() / voice.durationSamples
    val freq = voice.frequency.toDouble()

    var sample = when (voice.instrumentType) {
      InstrumentType.PIANO -> {
        // Multi-harmonic acoustic piano model with hammer attack
        val env = exp(-progress * 4.5).toFloat()
        val h1 = sin(2.0 * PI * freq * t)
        val h2 = 0.5 * sin(2.0 * PI * (freq * 2.0) * t)
        val h3 = 0.25 * sin(2.0 * PI * (freq * 3.0) * t)
        val h4 = 0.12 * sin(2.0 * PI * (freq * 4.0) * t)
        // Transient hammer click
        val hammer = if (voice.currentSample < 200) (Random.nextFloat() * 2f - 1f) * 0.3f else 0f
        ((h1 + h2 + h3 + h4) * 0.6f + hammer).toFloat() * env
      }

      InstrumentType.ORGAN -> {
        // Tonewheel organ additive synthesis (16', 8', 5 1/3', 4')
        val env = if (progress > 0.85f) ((1f - progress) / 0.15f) else 1.0f // Sustained with smooth release
        val vibrato = 1.0 + 0.015 * sin(2.0 * PI * 6.5 * t) // Leslie vibrato 6.5Hz
        val modFreq = freq * vibrato

        val drawbar16 = 0.8 * sin(2.0 * PI * (modFreq * 0.5) * t)
        val drawbar8 = 1.0 * sin(2.0 * PI * modFreq * t)
        val drawbar5 = 0.6 * sin(2.0 * PI * (modFreq * 1.5) * t)
        val drawbar4 = 0.7 * sin(2.0 * PI * (modFreq * 2.0) * t)
        ((drawbar16 + drawbar8 + drawbar5 + drawbar4) * 0.35).toFloat() * env.toFloat()
      }

      InstrumentType.ACOUSTIC_GUITAR -> {
        // Karplus-Strong string synthesis
        val buf = voice.stringBuffer
        if (buf != null && buf.isNotEmpty()) {
          val idx = voice.stringIndex
          val nextIdx = (idx + 1) % buf.size
          // Low-pass filter averaging with string loss decay factor (0.993)
          val filtered = 0.993f * 0.5f * (buf[idx] + buf[nextIdx])
          buf[idx] = filtered
          voice.stringIndex = nextIdx
          filtered
        } else {
          val env = exp(-progress * 3.0).toFloat()
          (sin(2.0 * PI * freq * t) * env).toFloat()
        }
      }

      InstrumentType.ELECTRIC_GUITAR -> {
        // Distorted overdrive electric guitar
        val env = exp(-progress * 2.2).toFloat()
        val raw = sin(2.0 * PI * freq * t) + 0.5 * sin(2.0 * PI * (freq * 2.0) * t)
        // Waveshaping overdrive saturation
        val driven = (raw * 3.5).toFloat()
        (tanh(driven.toDouble()) * 0.8 * env).toFloat()
      }

      InstrumentType.DRUMS -> {
        generateDrumSample(voice.sampleId ?: "kick", voice.currentSample, voice.durationSamples)
      }

      InstrumentType.SFX -> {
        generateSfxSample(voice.sampleId ?: "riser", voice.currentSample, voice.durationSamples)
      }
    }

    // Apply per-voice distortion if configured
    if (voice.distortionDrive > 0.05f) {
      val drive = 1f + voice.distortionDrive * 8f
      sample = tanh((sample * drive).toDouble()).toFloat()
    }

    return sample
  }

  private fun generateDrumSample(sampleId: String, currentSample: Long, durationSamples: Long): Float {
    val progress = currentSample.toFloat() / durationSamples
    val t = currentSample.toDouble() / SAMPLE_RATE

    return when (sampleId.lowercase()) {
      "kick" -> {
        // 160Hz -> 45Hz exponential pitch sweep
        val env = exp(-progress * 7.0).toFloat()
        val curFreq = 45.0 + 115.0 * exp(-progress * 15.0)
        (sin(2.0 * PI * curFreq * t) * env).toFloat()
      }
      "snare" -> {
        // Body (180Hz) + Snap (white noise)
        val bodyEnv = exp(-progress * 9.0).toFloat()
        val body = sin(2.0 * PI * 180.0 * t).toFloat() * bodyEnv * 0.7f
        val noiseEnv = exp(-progress * 7.5).toFloat()
        val noise = (Random.nextFloat() * 2f - 1f) * noiseEnv * 0.8f
        body + noise
      }
      "hihat_closed" -> {
        // Fast metallic noise snap (40ms)
        val env = exp(-progress * 35.0).toFloat()
        val noise = (Random.nextFloat() * 2f - 1f)
        val metallicTone = sin(2.0 * PI * 8500.0 * t).toFloat() * 0.4f
        (noise * 0.6f + metallicTone) * env
      }
      "hihat_open" -> {
        // Sustained metallic noise shimmer (350ms)
        val env = exp(-progress * 5.5).toFloat()
        val noise = (Random.nextFloat() * 2f - 1f)
        val metallicTone = sin(2.0 * PI * 9200.0 * t).toFloat() * 0.5f
        (noise * 0.5f + metallicTone) * env
      }
      "tom_high" -> {
        val env = exp(-progress * 6.0).toFloat()
        val curFreq = 120.0 + 100.0 * exp(-progress * 10.0)
        (sin(2.0 * PI * curFreq * t) * env).toFloat()
      }
      "tom_floor" -> {
        val env = exp(-progress * 5.0).toFloat()
        val curFreq = 70.0 + 80.0 * exp(-progress * 8.0)
        (sin(2.0 * PI * curFreq * t) * env).toFloat()
      }
      "clap" -> {
        // Multi-burst envelope
        val burstTime = (currentSample % 500)
        val burstEnv = exp(-burstTime.toFloat() / 150f)
        val env = exp(-progress * 5.0).toFloat()
        ((Random.nextFloat() * 2f - 1f) * burstEnv * env * 0.9f)
      }
      "crash" -> {
        val env = exp(-progress * 3.0).toFloat()
        val noise = (Random.nextFloat() * 2f - 1f)
        val ring = sin(2.0 * PI * 5800.0 * t).toFloat() * 0.3f
        (noise * 0.7f + ring) * env
      }
      else -> {
        val env = exp(-progress * 6.0).toFloat()
        (sin(2.0 * PI * 220.0 * t) * env).toFloat()
      }
    }
  }

  private fun generateSfxSample(sfxId: String, currentSample: Long, durationSamples: Long): Float {
    val progress = currentSample.toFloat() / durationSamples
    val t = currentSample.toDouble() / SAMPLE_RATE

    return when (sfxId.lowercase()) {
      "riser" -> {
        // Riser: 150Hz rising to 1200Hz
        val curFreq = 150.0 + 1050.0 * Math.pow(progress.toDouble(), 2.0)
        val env = (progress * 1.2f).coerceAtMost(1f)
        (sin(2.0 * PI * curFreq * t) * env * 0.7f).toFloat()
      }
      "sub_drop" -> {
        // 808 Sub drop: 130Hz down to 32Hz
        val curFreq = 32.0 + 98.0 * exp(-progress * 4.0)
        val env = exp(-progress * 2.5).toFloat()
        (sin(2.0 * PI * curFreq * t) * env * 0.85f).toFloat()
      }
      "vinyl_crackle" -> {
        // Lo-fi vinyl dust
        val pop = if (Random.nextFloat() < 0.05f) (Random.nextFloat() * 2f - 1f) * 0.8f else 0f
        val hum = sin(2.0 * PI * 60.0 * t).toFloat() * 0.05f
        pop + hum
      }
      "laser" -> {
        // Laser zap: 2000Hz down to 200Hz in 150ms
        val curFreq = 200.0 + 1800.0 * exp(-progress * 12.0)
        val env = exp(-progress * 8.0).toFloat()
        (sin(2.0 * PI * curFreq * t) * env * 0.75f).toFloat()
      }
      "tape_stop" -> {
        // Pitch slowing down
        val speed = max(0.05, 1.0 - progress.toDouble())
        val curFreq = 440.0 * speed
        val env = (1f - progress)
        (sin(2.0 * PI * curFreq * t) * env * 0.7f).toFloat()
      }
      else -> {
        val env = exp(-progress * 4.0).toFloat()
        (sin(2.0 * PI * 440.0 * t) * env * 0.5f).toFloat()
      }
    }
  }

  private fun triggerMetronomeClick(isDownbeat: Boolean) {
    val freq = if (isDownbeat) 1200f else 800f
    triggerVoiceInternal(
      instrument = InstrumentType.PIANO,
      pitch = if (isDownbeat) 84 else 77,
      velocity = if (isDownbeat) 0.9f else 0.6f,
      durationMs = 80L,
      trackVolume = 0.9f,
      trackPan = 0f,
      reverbWet = 0f,
      delayFeedback = 0f,
      distortionDrive = 0f,
      sampleId = null,
      startSampleOffset = 0L
    )
  }

  // --- DSP Reverb & Delay Filter Block ---

  private fun applyDspEffects(bufferL: FloatArray, bufferR: FloatArray) {
    val delaySamples = (SAMPLE_RATE * 0.25).toInt() // 250ms default delay

    for (i in bufferL.indices) {
      val inL = bufferL[i]
      val inR = bufferR[i]

      // Circular Echo Delay
      val readIdx = (delayWriteIndex - delaySamples + delayBufferLength) % delayBufferLength
      val delayedL = delayBufferL[readIdx]
      val delayedR = delayBufferR[readIdx]

      delayBufferL[delayWriteIndex] = inL + delayedL * 0.35f
      delayBufferR[delayWriteIndex] = inR + delayedR * 0.35f
      delayWriteIndex = (delayWriteIndex + 1) % delayBufferLength

      // Reverb Comb Filter Mix
      var reverbL = 0f
      var reverbR = 0f
      for (c in combBuffers.indices) {
        val cBuf = combBuffers[c]
        val cIdx = combIndices[c]
        val combVal = cBuf[cIdx]
        cBuf[cIdx] = (inL + inR) * 0.25f + combVal * 0.65f
        combIndices[c] = (cIdx + 1) % cBuf.size

        if (c % 2 == 0) reverbL += combVal * 0.15f
        else reverbR += combVal * 0.15f
      }

      // Sum Wet + Dry
      bufferL[i] = inL + delayedL * 0.25f + reverbL * 0.35f
      bufferR[i] = inR + delayedR * 0.25f + reverbR * 0.35f
    }
  }

  // --- Transport & Sequencer Control ---

  fun play() {
    _isPlaying.value = true
  }

  fun pause() {
    _isPlaying.value = false
    _isRecording.value = false
  }

  fun record() {
    recordedEvents.clear()
    recordingStartUptimeMs = System.currentTimeMillis()
    _isRecording.value = true
    _isPlaying.value = true
  }

  fun rewind() {
    _playbackPositionMs.value = 0L
  }

  fun seekTo(positionMs: Long) {
    _playbackPositionMs.value = max(0L, positionMs)
  }

  fun toggleMetronome() {
    _isMetronomeEnabled.value = !_isMetronomeEnabled.value
  }
}
