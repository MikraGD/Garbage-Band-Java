// GarageStudio Web Audio DAW Engine & UI Controller

class WebAudioEngine {
  constructor() {
    this.ctx = null;
    this.masterGain = null;
    this.analyser = null;
    this.reverbNode = null;
    this.delayNode = null;
    this.distortionNode = null;

    this.isPlaying = false;
    this.isRecording = false;
    this.isLooping = true;
    this.isMetronomeOn = false;

    this.bpm = 120;
    this.playheadMs = 0;
    this.lastClockTime = 0;
    this.playbackTimer = null;

    this.activeVoices = [];
    this.recordedNotes = [];

    // Default 4-bar starter groove matching Android project
    this.project = {
      title: "Midnight Jam",
      bpm: 120,
      timeSignature: "4/4",
      masterVolume: 0.85,
      tracks: [
        {
          id: "t_piano",
          name: "Piano Synth",
          instrumentType: "PIANO",
          colorHex: "#06B6D4",
          volume: 0.85,
          pan: -0.2,
          isMuted: false,
          isSoloed: false,
          effects: { reverbWet: 0.25, delayTimeMs: 250, delayFeedback: 0.2, distortionDrive: 0.0 },
          notes: [
            { pitch: 60, velocity: 0.8, startMs: 0, durationMs: 1800 },
            { pitch: 64, velocity: 0.75, startMs: 0, durationMs: 1800 },
            { pitch: 67, velocity: 0.75, startMs: 0, durationMs: 1800 },
            { pitch: 71, velocity: 0.7, startMs: 0, durationMs: 1800 },
            { pitch: 57, velocity: 0.8, startMs: 2000, durationMs: 1800 },
            { pitch: 60, velocity: 0.75, startMs: 2000, durationMs: 1800 },
            { pitch: 64, velocity: 0.75, startMs: 2000, durationMs: 1800 },
            { pitch: 67, velocity: 0.7, startMs: 2000, durationMs: 1800 },
            { pitch: 53, velocity: 0.8, startMs: 4000, durationMs: 1800 },
            { pitch: 57, velocity: 0.75, startMs: 4000, durationMs: 1800 },
            { pitch: 60, velocity: 0.75, startMs: 4000, durationMs: 1800 },
            { pitch: 64, velocity: 0.7, startMs: 4000, durationMs: 1800 },
            { pitch: 55, velocity: 0.8, startMs: 6000, durationMs: 1800 },
            { pitch: 59, velocity: 0.75, startMs: 6000, durationMs: 1800 },
            { pitch: 62, velocity: 0.75, startMs: 6000, durationMs: 1800 },
            { pitch: 65, velocity: 0.7, startMs: 6000, durationMs: 1800 }
          ]
        },
        {
          id: "t_organ",
          name: "Tonewheel Organ",
          instrumentType: "ORGAN",
          colorHex: "#A855F7",
          volume: 0.75,
          pan: 0.3,
          isMuted: false,
          isSoloed: false,
          effects: { reverbWet: 0.35, delayTimeMs: 300, delayFeedback: 0.15, distortionDrive: 0.0 },
          notes: [
            { pitch: 60, velocity: 0.7, startMs: 0, durationMs: 3800 },
            { pitch: 67, velocity: 0.65, startMs: 0, durationMs: 3800 },
            { pitch: 57, velocity: 0.7, startMs: 4000, durationMs: 3800 },
            { pitch: 64, velocity: 0.65, startMs: 4000, durationMs: 3800 }
          ]
        },
        {
          id: "t_acoustic_guitar",
          name: "Acoustic Guitar",
          instrumentType: "ACOUSTIC_GUITAR",
          colorHex: "#D97706",
          volume: 0.8,
          pan: -0.35,
          isMuted: false,
          isSoloed: false,
          effects: { reverbWet: 0.2, delayTimeMs: 200, delayFeedback: 0.1, distortionDrive: 0.0 },
          notes: [
            { pitch: 48, velocity: 0.75, startMs: 0, durationMs: 450 },
            { pitch: 52, velocity: 0.75, startMs: 300, durationMs: 450 },
            { pitch: 55, velocity: 0.75, startMs: 600, durationMs: 450 },
            { pitch: 60, velocity: 0.75, startMs: 900, durationMs: 450 },
            { pitch: 55, velocity: 0.75, startMs: 1200, durationMs: 450 },
            { pitch: 52, velocity: 0.75, startMs: 1500, durationMs: 450 },
            { pitch: 45, velocity: 0.75, startMs: 2000, durationMs: 450 },
            { pitch: 52, velocity: 0.75, startMs: 2300, durationMs: 450 },
            { pitch: 57, velocity: 0.75, startMs: 2600, durationMs: 450 },
            { pitch: 60, velocity: 0.75, startMs: 2900, durationMs: 450 },
            { pitch: 41, velocity: 0.75, startMs: 4000, durationMs: 450 },
            { pitch: 48, velocity: 0.75, startMs: 4300, durationMs: 450 },
            { pitch: 53, velocity: 0.75, startMs: 4600, durationMs: 450 },
            { pitch: 57, velocity: 0.75, startMs: 4900, durationMs: 450 },
            { pitch: 43, velocity: 0.75, startMs: 6000, durationMs: 450 },
            { pitch: 50, velocity: 0.75, startMs: 6300, durationMs: 450 },
            { pitch: 55, velocity: 0.75, startMs: 6600, durationMs: 450 },
            { pitch: 59, velocity: 0.75, startMs: 6900, durationMs: 450 }
          ]
        },
        {
          id: "t_electric_guitar",
          name: "Electric Guitar",
          instrumentType: "ELECTRIC_GUITAR",
          colorHex: "#EF4444",
          volume: 0.85,
          pan: 0.4,
          isMuted: false,
          isSoloed: false,
          effects: { reverbWet: 0.3, delayTimeMs: 250, delayFeedback: 0.2, distortionDrive: 0.5 },
          notes: [
            { pitch: 72, velocity: 0.85, startMs: 4000, durationMs: 600 },
            { pitch: 74, velocity: 0.85, startMs: 4600, durationMs: 600 },
            { pitch: 76, velocity: 0.9, startMs: 5200, durationMs: 900 },
            { pitch: 79, velocity: 0.95, startMs: 6200, durationMs: 1200 },
            { pitch: 76, velocity: 0.85, startMs: 7400, durationMs: 500 }
          ]
        },
        {
          id: "t_drums",
          name: "Studio Drums",
          instrumentType: "DRUMS",
          colorHex: "#10B981",
          volume: 0.9,
          pan: 0.0,
          isMuted: false,
          isSoloed: false,
          effects: { reverbWet: 0.1, delayTimeMs: 200, delayFeedback: 0.0, distortionDrive: 0.0 },
          notes: [
            // Bar 1
            { pitch: 36, velocity: 0.9, startMs: 0, durationMs: 300, sampleId: "kick" },
            { pitch: 42, velocity: 0.8, startMs: 0, durationMs: 100, sampleId: "hihat_closed" },
            { pitch: 42, velocity: 0.6, startMs: 250, durationMs: 100, sampleId: "hihat_closed" },
            { pitch: 38, velocity: 0.9, startMs: 500, durationMs: 250, sampleId: "snare" },
            { pitch: 42, velocity: 0.8, startMs: 500, durationMs: 100, sampleId: "hihat_closed" },
            { pitch: 42, velocity: 0.6, startMs: 750, durationMs: 100, sampleId: "hihat_closed" },
            { pitch: 36, velocity: 0.85, startMs: 1000, durationMs: 300, sampleId: "kick" },
            { pitch: 42, velocity: 0.8, startMs: 1000, durationMs: 100, sampleId: "hihat_closed" },
            { pitch: 42, velocity: 0.6, startMs: 1250, durationMs: 100, sampleId: "hihat_closed" },
            { pitch: 38, velocity: 0.9, startMs: 1500, durationMs: 250, sampleId: "snare" },
            { pitch: 46, velocity: 0.7, startMs: 1750, durationMs: 350, sampleId: "hihat_open" },
            // Bar 2
            { pitch: 36, velocity: 0.9, startMs: 2000, durationMs: 300, sampleId: "kick" },
            { pitch: 42, velocity: 0.8, startMs: 2000, durationMs: 100, sampleId: "hihat_closed" },
            { pitch: 38, velocity: 0.9, startMs: 2500, durationMs: 250, sampleId: "snare" },
            { pitch: 36, velocity: 0.85, startMs: 3000, durationMs: 300, sampleId: "kick" },
            { pitch: 38, velocity: 0.9, startMs: 3500, durationMs: 250, sampleId: "snare" },
            // Bar 3
            { pitch: 36, velocity: 0.9, startMs: 4000, durationMs: 300, sampleId: "kick" },
            { pitch: 38, velocity: 0.9, startMs: 4500, durationMs: 250, sampleId: "snare" },
            { pitch: 36, velocity: 0.85, startMs: 5000, durationMs: 300, sampleId: "kick" },
            { pitch: 38, velocity: 0.9, startMs: 5500, durationMs: 250, sampleId: "snare" },
            // Bar 4
            { pitch: 36, velocity: 0.9, startMs: 6000, durationMs: 300, sampleId: "kick" },
            { pitch: 38, velocity: 0.9, startMs: 6500, durationMs: 250, sampleId: "snare" },
            { pitch: 48, velocity: 0.85, startMs: 7000, durationMs: 200, sampleId: "tom_high" },
            { pitch: 43, velocity: 0.85, startMs: 7250, durationMs: 200, sampleId: "tom_floor" },
            { pitch: 38, velocity: 0.9, startMs: 7500, durationMs: 300, sampleId: "snare" },
            { pitch: 49, velocity: 0.9, startMs: 7750, durationMs: 600, sampleId: "crash" }
          ]
        }
      ]
    };
  }

  initAudio() {
    if (this.ctx) return;
    const AudioContext = window.AudioContext || window.webkitAudioContext;
    this.ctx = new AudioContext();

    this.masterGain = this.ctx.createGain();
    this.masterGain.gain.setValueAtTime(this.project.masterVolume, this.ctx.currentTime);

    this.analyser = this.ctx.createAnalyser();
    this.analyser.fftSize = 64;

    this.masterGain.connect(this.analyser);
    this.analyser.connect(this.ctx.destination);
  }

  ensureContext() {
    this.initAudio();
    if (this.ctx.state === "suspended") {
      this.ctx.resume();
    }
  }

  midiToFreq(midi) {
    return 440 * Math.pow(2, (midi - 69) / 12);
  }

  // --- Sound Generation Voice Algorithms ---

  playPiano(freq, velocity = 0.8, duration = 0.6) {
    this.ensureContext();
    const t = this.ctx.currentTime;
    const osc = this.ctx.createOscillator();
    const osc2 = this.ctx.createOscillator();
    const gain = this.ctx.createGain();

    osc.type = "sine";
    osc.frequency.setValueAtTime(freq, t);

    osc2.type = "triangle";
    osc2.frequency.setValueAtTime(freq * 2, t);

    const gainVal = Math.max(0.01, velocity * 0.7);
    gain.gain.setValueAtTime(gainVal, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + duration);

    osc.connect(gain);
    osc2.connect(gain);
    gain.connect(this.masterGain);

    osc.start(t);
    osc2.start(t);
    osc.stop(t + duration);
    osc2.stop(t + duration);
  }

  playOrgan(freq, velocity = 0.8, duration = 1.0, drawbars = [8, 8, 6, 7], vibrato = true) {
    this.ensureContext();
    const t = this.ctx.currentTime;

    const outGain = this.ctx.createGain();
    outGain.gain.setValueAtTime(velocity * 0.5, t);
    outGain.gain.exponentialRampToValueAtTime(0.001, t + duration);
    outGain.connect(this.masterGain);

    const ratios = [0.5, 1.0, 1.5, 2.0];
    ratios.forEach((ratio, i) => {
      const dbVal = drawbars[i] || 0;
      if (dbVal <= 0) return;

      const osc = this.ctx.createOscillator();
      const oscGain = this.ctx.createGain();

      osc.type = "sine";
      let baseFreq = freq * ratio;

      if (vibrato) {
        // Vibrato LFO
        const lfo = this.ctx.createOscillator();
        const lfoGain = this.ctx.createGain();
        lfo.frequency.setValueAtTime(6.5, t);
        lfoGain.gain.setValueAtTime(3.0, t);
        lfo.connect(osc.frequency);
        lfo.start(t);
        lfo.stop(t + duration);
      }

      osc.frequency.setValueAtTime(baseFreq, t);
      oscGain.gain.setValueAtTime((dbVal / 8) * 0.25, t);

      osc.connect(oscGain);
      oscGain.connect(outGain);

      osc.start(t);
      osc.stop(t + duration);
    });
  }

  playAcousticGuitar(freq, velocity = 0.8, duration = 0.8) {
    this.ensureContext();
    const t = this.ctx.currentTime;

    // Pluck simulation using triangle oscillator + noise transient
    const osc = this.ctx.createOscillator();
    const filter = this.ctx.createBiquadFilter();
    const gain = this.ctx.createGain();

    osc.type = "triangle";
    osc.frequency.setValueAtTime(freq, t);

    filter.type = "lowpass";
    filter.frequency.setValueAtTime(3500, t);
    filter.frequency.exponentialRampToValueAtTime(freq * 1.5, t + duration);

    gain.gain.setValueAtTime(velocity * 0.8, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + duration);

    osc.connect(filter);
    filter.connect(gain);
    gain.connect(this.masterGain);

    osc.start(t);
    osc.stop(t + duration);
  }

  playElectricGuitar(freq, velocity = 0.8, duration = 1.0, drive = 0.5) {
    this.ensureContext();
    const t = this.ctx.currentTime;

    const osc = this.ctx.createOscillator();
    const waveShaper = this.ctx.createWaveShaper();
    const filter = this.ctx.createBiquadFilter();
    const gain = this.ctx.createGain();

    osc.type = "sawtooth";
    osc.frequency.setValueAtTime(freq, t);

    // Distortion curve
    const k = drive * 50;
    const n = 22050;
    const curve = new Float32Array(n);
    const deg = Math.PI / 180;
    for (let i = 0; i < n; ++i) {
      const x = (i * 2) / n - 1;
      curve[i] = ((3 + k) * x * 20 * deg) / (Math.PI + k * Math.abs(x));
    }
    waveShaper.curve = curve;

    filter.type = "lowpass";
    filter.frequency.setValueAtTime(2800, t);

    gain.gain.setValueAtTime(velocity * 0.6, t);
    gain.gain.exponentialRampToValueAtTime(0.001, t + duration);

    osc.connect(waveShaper);
    waveShaper.connect(filter);
    filter.connect(gain);
    gain.connect(this.masterGain);

    osc.start(t);
    osc.stop(t + duration);
  }

  playDrum(type, velocity = 0.9) {
    this.ensureContext();
    const t = this.ctx.currentTime;

    switch (type.toLowerCase()) {
      case "kick": {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.frequency.setValueAtTime(160, t);
        osc.frequency.exponentialRampToValueAtTime(45, t + 0.15);
        gain.gain.setValueAtTime(velocity * 1.0, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.35);
        osc.connect(gain);
        gain.connect(this.masterGain);
        osc.start(t);
        osc.stop(t + 0.35);
        break;
      }
      case "snare": {
        // Tone
        const osc = this.ctx.createOscillator();
        const oscGain = this.ctx.createGain();
        osc.frequency.setValueAtTime(180, t);
        oscGain.gain.setValueAtTime(velocity * 0.6, t);
        oscGain.gain.exponentialRampToValueAtTime(0.001, t + 0.15);
        osc.connect(oscGain);
        oscGain.connect(this.masterGain);
        osc.start(t);
        osc.stop(t + 0.15);

        // Snap Noise
        const bufferSize = this.ctx.sampleRate * 0.2;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) data[i] = Math.random() * 2 - 1;
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;
        const noiseGain = this.ctx.createGain();
        noiseGain.gain.setValueAtTime(velocity * 0.7, t);
        noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 0.2);
        noise.connect(noiseGain);
        noiseGain.connect(this.masterGain);
        noise.start(t);
        break;
      }
      case "hihat_closed": {
        const bufferSize = this.ctx.sampleRate * 0.05;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) data[i] = Math.random() * 2 - 1;
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;
        const filter = this.ctx.createBiquadFilter();
        filter.type = "highpass";
        filter.frequency.setValueAtTime(7000, t);
        const gain = this.ctx.createGain();
        gain.gain.setValueAtTime(velocity * 0.5, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.05);
        noise.connect(filter);
        filter.connect(gain);
        gain.connect(this.masterGain);
        noise.start(t);
        break;
      }
      case "hihat_open": {
        const bufferSize = this.ctx.sampleRate * 0.35;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) data[i] = Math.random() * 2 - 1;
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;
        const filter = this.ctx.createBiquadFilter();
        filter.type = "highpass";
        filter.frequency.setValueAtTime(6500, t);
        const gain = this.ctx.createGain();
        gain.gain.setValueAtTime(velocity * 0.6, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.35);
        noise.connect(filter);
        filter.connect(gain);
        gain.connect(this.masterGain);
        noise.start(t);
        break;
      }
      case "tom_high": {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.frequency.setValueAtTime(220, t);
        osc.frequency.exponentialRampToValueAtTime(110, t + 0.2);
        gain.gain.setValueAtTime(velocity * 0.8, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.25);
        osc.connect(gain);
        gain.connect(this.masterGain);
        osc.start(t);
        osc.stop(t + 0.25);
        break;
      }
      case "tom_floor": {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.frequency.setValueAtTime(130, t);
        osc.frequency.exponentialRampToValueAtTime(65, t + 0.25);
        gain.gain.setValueAtTime(velocity * 0.8, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.3);
        osc.connect(gain);
        gain.connect(this.masterGain);
        osc.start(t);
        osc.stop(t + 0.3);
        break;
      }
      case "clap": {
        // Multi-burst clap
        [0, 0.015, 0.03].forEach(offset => {
          const bufferSize = this.ctx.sampleRate * 0.15;
          const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
          const data = buffer.getChannelData(0);
          for (let i = 0; i < bufferSize; i++) data[i] = Math.random() * 2 - 1;
          const noise = this.ctx.createBufferSource();
          noise.buffer = buffer;
          const filter = this.ctx.createBiquadFilter();
          filter.type = "bandpass";
          filter.frequency.setValueAtTime(1200, t + offset);
          const gain = this.ctx.createGain();
          gain.gain.setValueAtTime(velocity * 0.7, t + offset);
          gain.gain.exponentialRampToValueAtTime(0.001, t + offset + 0.15);
          noise.connect(filter);
          filter.connect(gain);
          gain.connect(this.masterGain);
          noise.start(t + offset);
        });
        break;
      }
      case "crash": {
        const bufferSize = this.ctx.sampleRate * 1.2;
        const buffer = this.ctx.createBuffer(1, bufferSize, this.ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) data[i] = Math.random() * 2 - 1;
        const noise = this.ctx.createBufferSource();
        noise.buffer = buffer;
        const filter = this.ctx.createBiquadFilter();
        filter.type = "highpass";
        filter.frequency.setValueAtTime(4500, t);
        const gain = this.ctx.createGain();
        gain.gain.setValueAtTime(velocity * 0.7, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 1.2);
        noise.connect(filter);
        filter.connect(gain);
        gain.connect(this.masterGain);
        noise.start(t);
        break;
      }
    }
  }

  playSfx(sfxId) {
    this.ensureContext();
    const t = this.ctx.currentTime;
    switch (sfxId) {
      case "riser": {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.frequency.setValueAtTime(150, t);
        osc.frequency.exponentialRampToValueAtTime(1200, t + 1.2);
        gain.gain.setValueAtTime(0.01, t);
        gain.gain.linearRampToValueAtTime(0.8, t + 1.1);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 1.2);
        osc.connect(gain);
        gain.connect(this.masterGain);
        osc.start(t);
        osc.stop(t + 1.2);
        break;
      }
      case "sub_drop": {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.frequency.setValueAtTime(130, t);
        osc.frequency.exponentialRampToValueAtTime(32, t + 1.5);
        gain.gain.setValueAtTime(0.9, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 1.5);
        osc.connect(gain);
        gain.connect(this.masterGain);
        osc.start(t);
        osc.stop(t + 1.5);
        break;
      }
      case "laser": {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.frequency.setValueAtTime(2000, t);
        osc.frequency.exponentialRampToValueAtTime(200, t + 0.35);
        gain.gain.setValueAtTime(0.8, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.35);
        osc.connect(gain);
        gain.connect(this.masterGain);
        osc.start(t);
        osc.stop(t + 0.35);
        break;
      }
      case "tape_stop": {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.frequency.setValueAtTime(440, t);
        osc.frequency.exponentialRampToValueAtTime(60, t + 1.0);
        gain.gain.setValueAtTime(0.7, t);
        gain.gain.linearRampToValueAtTime(0.001, t + 1.0);
        osc.connect(gain);
        gain.connect(this.masterGain);
        osc.start(t);
        osc.stop(t + 1.0);
        break;
      }
      default:
        this.playPiano(440, 0.7, 0.4);
    }
  }

  // --- Sequencer Playback ---

  play() {
    this.ensureContext();
    if (this.isPlaying) return;

    this.isPlaying = true;
    this.lastClockTime = performance.now() - this.playheadMs;

    this.playbackTimer = setInterval(() => {
      this.tick();
    }, 25);
  }

  pause() {
    this.isPlaying = false;
    this.isRecording = false;
    if (this.playbackTimer) {
      clearInterval(this.playbackTimer);
      this.playbackTimer = null;
    }
  }

  rewind() {
    this.playheadMs = 0;
    this.lastClockTime = performance.now();
  }

  tick() {
    if (!this.isPlaying) return;
    const now = performance.now();
    const prevMs = this.playheadMs;
    this.playheadMs = now - this.lastClockTime;

    const maxMs = 8000; // 4 bars at 120bpm
    if (this.isLooping && this.playheadMs >= maxMs) {
      this.playheadMs = 0;
      this.lastClockTime = performance.now();
    } else if (!this.isLooping && this.playheadMs >= maxMs) {
      this.pause();
      return;
    }

    // Trigger scheduled notes between prevMs and playheadMs
    const curMs = this.playheadMs;
    const anySolo = this.project.tracks.some(t => t.isSoloed);

    this.project.tracks.forEach(track => {
      if (track.isMuted) return;
      if (anySolo && !track.isSoloed) return;

      track.notes.forEach(note => {
        if (note.startMs >= prevMs && note.startMs < curMs) {
          const freq = this.midiToFreq(note.pitch);
          const durSec = note.durationMs / 1000;
          const vel = note.velocity * track.volume;

          switch (track.instrumentType) {
            case "PIANO":
              this.playPiano(freq, vel, durSec);
              break;
            case "ORGAN":
              this.playOrgan(freq, vel, durSec);
              break;
            case "ACOUSTIC_GUITAR":
              this.playAcousticGuitar(freq, vel, durSec);
              break;
            case "ELECTRIC_GUITAR":
              this.playElectricGuitar(freq, vel, durSec, track.effects?.distortionDrive || 0.4);
              break;
            case "DRUMS":
              this.playDrum(note.sampleId || "kick", vel);
              break;
            case "SFX":
              this.playSfx(note.sampleId || "riser");
              break;
          }
        }
      });
    });
  }
}

// Global App State
const engine = new WebAudioEngine();
let activeTrackId = engine.project.tracks[0].id;
let activeInstrumentTab = "piano";
let activeChordIdx = 0;

// Guitar Chords
const CHORDS = [
  { name: "C Maj", pitches: [48, 52, 55, 60, 64] },
  { name: "G Maj", pitches: [43, 47, 50, 55, 59, 67] },
  { name: "Am", pitches: [45, 52, 57, 60, 64] },
  { name: "F Maj", pitches: [41, 48, 53, 57, 60, 65] },
  { name: "Em", pitches: [40, 47, 52, 55, 59, 64] },
  { name: "D Maj", pitches: [50, 57, 62, 66] },
  { name: "Dm", pitches: [50, 57, 62, 65] },
  { name: "E Maj", pitches: [40, 47, 52, 56, 59, 64] }
];

document.addEventListener("DOMContentLoaded", () => {
  renderTimeline();
  renderMixer();
  setupTransport();
  setupPiano();
  setupOrgan();
  setupGuitar();
  setupElectricGuitar();
  setupDrums();
  setupTabs();
  startMeterLoop();
});

// Setup Transport Controls
function setupTransport() {
  const btnPlay = document.getElementById("btnPlay");
  const btnRec = document.getElementById("btnRecord");
  const btnRewind = document.getElementById("btnRewind");
  const btnLoop = document.getElementById("btnLoop");
  const bpmInput = document.getElementById("bpmInput");

  btnPlay.addEventListener("click", () => {
    if (engine.isPlaying) {
      engine.pause();
      btnPlay.innerHTML = "▶";
      btnPlay.classList.remove("active");
    } else {
      engine.play();
      btnPlay.innerHTML = "⏸";
      btnPlay.classList.add("active");
    }
  });

  btnRec.addEventListener("click", () => {
    engine.isRecording = !engine.isRecording;
    if (engine.isRecording) {
      btnRec.classList.add("active");
      if (!engine.isPlaying) {
        engine.play();
        btnPlay.innerHTML = "⏸";
        btnPlay.classList.add("active");
      }
    } else {
      btnRec.classList.remove("active");
    }
  });

  btnRewind.addEventListener("click", () => {
    engine.rewind();
  });

  btnLoop.addEventListener("click", () => {
    engine.isLooping = !engine.isLooping;
    btnLoop.style.color = engine.isLooping ? "var(--amber)" : "var(--text-muted)";
  });

  bpmInput.addEventListener("change", (e) => {
    const val = parseInt(e.target.value, 10);
    if (!isNaN(val)) {
      engine.bpm = Math.max(40, Math.min(240, val));
      engine.project.bpm = engine.bpm;
    }
  });
}

// Render Multitrack Timeline
function renderTimeline() {
  const headersCol = document.getElementById("trackHeaders");
  const lanesArea = document.getElementById("timelineLanes");

  headersCol.innerHTML = "";
  lanesArea.innerHTML = "";

  const scale = 70; // 70px per second

  engine.project.tracks.forEach(track => {
    // 1. Header
    const hItem = document.createElement("div");
    hItem.className = `track-header-item ${track.id === activeTrackId ? "selected" : ""}`;
    hItem.innerHTML = `
      <div class="track-header-title">
        <span class="track-color-dot" style="background:${track.colorHex}"></span>
        <span>${track.name}</span>
      </div>
      <div class="track-quick-buttons">
        <button class="pill-btn mute ${track.isMuted ? "active" : ""}" data-id="${track.id}">M</button>
        <button class="pill-btn solo ${track.isSoloed ? "active" : ""}" data-id="${track.id}">S</button>
        <span style="font-size:10px;color:var(--text-muted);">${track.instrumentType}</span>
      </div>
    `;

    hItem.addEventListener("click", (e) => {
      if (e.target.classList.contains("mute")) {
        track.isMuted = !track.isMuted;
        renderTimeline();
        renderMixer();
        return;
      }
      if (e.target.classList.contains("solo")) {
        track.isSoloed = !track.isSoloed;
        renderTimeline();
        renderMixer();
        return;
      }
      activeTrackId = track.id;
      renderTimeline();
    });

    headersCol.appendChild(hItem);

    // 2. Lane with note blocks
    const lane = document.createElement("div");
    lane.className = "timeline-lane";

    track.notes.forEach(note => {
      const block = document.createElement("div");
      block.className = "note-block";
      block.style.background = track.colorHex;
      block.style.left = ((note.startMs / 1000) * scale) + "px";
      block.style.width = Math.max(16, (note.durationMs / 1000) * scale) + "px";
      block.innerText = note.sampleId ? note.sampleId.toUpperCase() : "N" + note.pitch;
      lane.appendChild(block);
    });

    lanesArea.appendChild(lane);
  });
}

// Render Mixer Console
function renderMixer() {
  const container = document.getElementById("mixerContainer");
  if (!container) return;
  container.innerHTML = "";

  engine.project.tracks.forEach(track => {
    const strip = document.createElement("div");
    strip.className = "channel-strip";
    strip.innerHTML = `
      <div class="channel-header" style="background:${track.colorHex}22; border:1px solid ${track.colorHex}; color:#FFF;">
        ${track.name}
      </div>
      <div style="display:flex;justify-content:center;gap:6px;">
        <button class="pill-btn mute ${track.isMuted ? "active" : ""}" data-id="${track.id}">MUTE</button>
        <button class="pill-btn solo ${track.isSoloed ? "active" : ""}" data-id="${track.id}">SOLO</button>
      </div>
      <div style="text-align:center;font-size:11px;color:var(--text-secondary);">
        PAN: ${(track.pan * 100).toFixed(0)}%
        <input type="range" min="-1" max="1" step="0.1" value="${track.pan}" class="pan-slider" style="width:100%;margin-top:4px;" data-id="${track.id}">
      </div>
      <div class="strip-fader-row">
        <div class="strip-meter">
          <div class="strip-meter-fill" id="meter_${track.id}"></div>
        </div>
        <input type="range" min="0" max="1" step="0.05" value="${track.volume}" class="channel-fader" data-id="${track.id}">
      </div>
      <div style="text-align:center;font-size:12px;font-weight:700;">
        ${(track.volume * 100).toFixed(0)}%
      </div>
    `;

    // Listeners
    strip.querySelector(".mute").addEventListener("click", () => {
      track.isMuted = !track.isMuted;
      renderTimeline();
      renderMixer();
    });

    strip.querySelector(".solo").addEventListener("click", () => {
      track.isSoloed = !track.isSoloed;
      renderTimeline();
      renderMixer();
    });

    strip.querySelector(".pan-slider").addEventListener("input", (e) => {
      track.pan = parseFloat(e.target.value);
    });

    strip.querySelector(".channel-fader").addEventListener("input", (e) => {
      track.volume = parseFloat(e.target.value);
      renderMixer();
    });

    container.appendChild(strip);
  });
}

// Setup Piano Keyboard
function setupPiano() {
  const pianoContainer = document.getElementById("pianoKeysContainer");
  if (!pianoContainer) return;
  pianoContainer.innerHTML = "";

  let baseOctave = 4;
  const whiteNotes = [
    { name: "C", midiOffset: 0 },
    { name: "D", midiOffset: 2 },
    { name: "E", midiOffset: 4 },
    { name: "F", midiOffset: 5 },
    { name: "G", midiOffset: 7 },
    { name: "A", midiOffset: 9 },
    { name: "B", midiOffset: 11 }
  ];

  const blackOffsets = [
    { name: "C#", midiOffset: 1, left: 36 },
    { name: "D#", midiOffset: 3, left: 88 },
    { name: "F#", midiOffset: 6, left: 192 },
    { name: "G#", midiOffset: 8, left: 244 },
    { name: "A#", midiOffset: 10, left: 296 }
  ];

  for (let oct = 3; oct <= 5; oct++) {
    const octStartMidi = 12 + oct * 12;

    whiteNotes.forEach(note => {
      const midi = octStartMidi + note.midiOffset;
      const key = document.createElement("div");
      key.className = "white-key";
      key.innerHTML = `${note.name}<span style="font-size:9px;">${oct}</span>`;
      key.addEventListener("mousedown", () => {
        key.classList.add("pressed");
        engine.playPiano(engine.midiToFreq(midi), 0.85, 0.6);
      });
      key.addEventListener("mouseup", () => key.classList.remove("pressed"));
      key.addEventListener("mouseleave", () => key.classList.remove("pressed"));
      pianoContainer.appendChild(key);
    });

    blackOffsets.forEach(bn => {
      const midi = octStartMidi + bn.midiOffset;
      const bkey = document.createElement("div");
      bkey.className = "black-key";
      const octOffset = (oct - 3) * (7 * 52);
      bkey.style.left = (octOffset + bn.left) + "px";
      bkey.addEventListener("mousedown", () => {
        bkey.classList.add("pressed");
        engine.playPiano(engine.midiToFreq(midi), 0.9, 0.6);
      });
      bkey.addEventListener("mouseup", () => bkey.classList.remove("pressed"));
      bkey.addEventListener("mouseleave", () => bkey.classList.remove("pressed"));
      pianoContainer.appendChild(bkey);
    });
  }
}

// Setup Organ Drawbars
function setupOrgan() {
  const organKeys = document.getElementById("organKeysContainer");
  if (!organKeys) return;
  organKeys.innerHTML = "";

  for (let oct = 3; oct <= 4; oct++) {
    const octStartMidi = 12 + oct * 12;
    ["C", "D", "E", "F", "G", "A", "B"].forEach((noteName, idx) => {
      const midi = octStartMidi + [0, 2, 4, 5, 7, 9, 11][idx];
      const key = document.createElement("div");
      key.className = "white-key";
      key.style.height = "160px";
      key.innerHTML = `${noteName}${oct}`;
      key.addEventListener("mousedown", () => {
        key.classList.add("pressed");
        const d16 = parseInt(document.getElementById("db16").value, 10);
        const d8 = parseInt(document.getElementById("db8").value, 10);
        const d5 = parseInt(document.getElementById("db5").value, 10);
        const d4 = parseInt(document.getElementById("db4").value, 10);
        engine.playOrgan(engine.midiToFreq(midi), 0.8, 1.2, [d16, d8, d5, d4], true);
      });
      key.addEventListener("mouseup", () => key.classList.remove("pressed"));
      key.addEventListener("mouseleave", () => key.classList.remove("pressed"));
      organKeys.appendChild(key);
    });
  }
}

// Setup Acoustic Guitar
function setupGuitar() {
  const chordsContainer = document.getElementById("guitarChordsContainer");
  const strumPad = document.getElementById("guitarStrumPad");
  const stringsContainer = document.getElementById("guitarStringsContainer");

  if (!chordsContainer) return;
  chordsContainer.innerHTML = "";
  stringsContainer.innerHTML = "";

  CHORDS.forEach((c, i) => {
    const btn = document.createElement("button");
    btn.className = `chord-btn ${i === activeChordIdx ? "active" : ""}`;
    btn.innerText = c.name;
    btn.addEventListener("click", () => {
      activeChordIdx = i;
      document.querySelectorAll(".chord-btn").forEach((b, idx) => {
        b.classList.toggle("active", idx === activeChordIdx);
      });
      strumCurrentChord();
    });
    chordsContainer.appendChild(btn);
  });

  function strumCurrentChord() {
    const chord = CHORDS[activeChordIdx];
    chord.pitches.forEach((p, idx) => {
      setTimeout(() => {
        engine.playAcousticGuitar(engine.midiToFreq(p), 0.85, 0.9);
      }, idx * 35);
    });
  }

  strumPad.addEventListener("click", () => strumCurrentChord());

  const stringLabels = ["String 1 (High E)", "String 2 (B)", "String 3 (G)", "String 4 (D)", "String 5 (A)", "String 6 (Low E)"];
  stringLabels.forEach((label, idx) => {
    const btn = document.createElement("div");
    btn.className = "string-btn";
    btn.innerHTML = `<span>${label}</span><span>PICK</span>`;
    btn.addEventListener("click", () => {
      const chord = CHORDS[activeChordIdx];
      const p = chord.pitches[idx % chord.pitches.length];
      engine.playAcousticGuitar(engine.midiToFreq(p), 0.9, 0.9);
    });
    stringsContainer.appendChild(btn);
  });
}

// Setup Electric Guitar
function setupElectricGuitar() {
  const powerRow = document.getElementById("powerChordsRow");
  const soloGrid = document.getElementById("soloScaleGrid");
  if (!powerRow) return;

  const powerChords = [
    { name: "E5", root: 40, fifth: 47 },
    { name: "G5", root: 43, fifth: 50 },
    { name: "A5", root: 45, fifth: 52 },
    { name: "C5", root: 48, fifth: 55 },
    { name: "D5", root: 50, fifth: 57 },
    { name: "F5", root: 41, fifth: 48 }
  ];

  powerRow.innerHTML = "";
  powerChords.forEach(c => {
    const btn = document.createElement("button");
    btn.className = "chord-btn";
    btn.style.padding = "14px";
    btn.innerText = c.name;
    btn.addEventListener("click", () => {
      const drive = parseFloat(document.getElementById("electricDriveSlider").value);
      engine.playElectricGuitar(engine.midiToFreq(c.root), 0.9, 1.0, drive);
      engine.playElectricGuitar(engine.midiToFreq(c.fifth), 0.85, 1.0, drive);
    });
    powerRow.appendChild(btn);
  });

  const soloNotes = [
    { name: "E4 (Root)", pitch: 64 },
    { name: "G4 (b3)", pitch: 67 },
    { name: "A4 (4th)", pitch: 69 },
    { name: "B4 (5th)", pitch: 71 },
    { name: "D5 (b7)", pitch: 74 },
    { name: "E5 (Octave)", pitch: 76 }
  ];

  soloGrid.innerHTML = "";
  soloNotes.forEach(sn => {
    const btn = document.createElement("button");
    btn.className = "solo-note-btn";
    btn.innerHTML = `${sn.name}<br><span style="font-size:11px;color:var(--crimson);">SOLO FRET</span>`;
    btn.addEventListener("click", () => {
      const drive = parseFloat(document.getElementById("electricDriveSlider").value);
      engine.playElectricGuitar(engine.midiToFreq(sn.pitch), 0.9, 0.8, drive);
    });
    soloGrid.appendChild(btn);
  });
}

// Setup Drum Pads
function setupDrums() {
  const pads = document.querySelectorAll(".drum-pad");
  pads.forEach(pad => {
    pad.addEventListener("mousedown", () => {
      const type = pad.dataset.drum;
      pad.classList.add("hit");
      engine.playDrum(type, 0.9);
      setTimeout(() => pad.classList.remove("hit"), 100);
    });
  });

  // Computer Keyboard mapping for drums
  window.addEventListener("keydown", (e) => {
    const keyMap = {
      "1": "kick",
      "2": "snare",
      "3": "hihat_closed",
      "4": "hihat_open",
      "5": "tom_high",
      "6": "tom_floor",
      "7": "clap",
      "8": "crash"
    };
    if (keyMap[e.key]) {
      const targetPad = document.querySelector(`[data-drum="${keyMap[e.key]}"]`);
      if (targetPad) {
        targetPad.classList.add("hit");
        engine.playDrum(keyMap[e.key], 0.95);
        setTimeout(() => targetPad.classList.remove("hit"), 100);
      }
    }
  });
}

// Setup Tabs Navigation
function setupTabs() {
  const tabs = document.querySelectorAll(".tab-btn");
  tabs.forEach(tab => {
    tab.addEventListener("click", () => {
      tabs.forEach(t => t.classList.remove("active"));
      tab.classList.add("active");
      const targetView = tab.dataset.view;
      document.querySelectorAll(".tab-view").forEach(v => {
        v.classList.toggle("active", v.id === `view_${targetView}`);
      });
    });
  });

  // Instrument Sub-Selector Tabs
  const instTabs = document.querySelectorAll(".inst-tab-btn");
  instTabs.forEach(it => {
    it.addEventListener("click", () => {
      instTabs.forEach(t => t.classList.remove("btn-primary"));
      it.classList.add("btn-primary");
      const targetInst = it.dataset.inst;
      document.querySelectorAll(".instrument-panel").forEach(p => {
        p.classList.toggle("active", p.id === `inst_${targetInst}`);
      });
    });
  });
}

// Start Meter & Playhead Loop
function startMeterLoop() {
  const playhead = document.getElementById("playheadLine");
  const timeDisplay = document.getElementById("lcdTime");
  const masterMeter = document.getElementById("masterMeterFill");

  function loop() {
    // 1. Playhead position
    if (playhead && engine.isPlaying) {
      const scale = 70; // 70px per second
      const sec = engine.playheadMs / 1000;
      const x = 220 + (sec * scale);
      playhead.style.left = x + "px";

      const mins = Math.floor(sec / 60);
      const s = Math.floor(sec % 60);
      const ms = Math.floor((sec % 1) * 100);
      timeDisplay.innerText = `${String(mins).padStart(2, "0")}:${String(s).padStart(2, "0")}.${String(ms).padStart(2, "0")}`;
    }

    // 2. VU meter level
    if (engine.analyser && masterMeter) {
      const dataArray = new Uint8Array(engine.analyser.frequencyBinCount);
      engine.analyser.getByteTimeDomainData(dataArray);
      let sum = 0;
      for (let i = 0; i < dataArray.length; i++) {
        const val = (dataArray[i] - 128) / 128;
        sum += val * val;
      }
      const rms = Math.sqrt(sum / dataArray.length);
      const peakPercent = Math.min(100, Math.floor(rms * 280));
      masterMeter.style.width = peakPercent + "%";
    }

    requestAnimationFrame(loop);
  }
  requestAnimationFrame(loop);
}

// Save & Export Project JSON
window.exportProjectJson = function() {
  const dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(engine.project, null, 2));
  const downloadAnchor = document.createElement("a");
  downloadAnchor.setAttribute("href", dataStr);
  downloadAnchor.setAttribute("download", `${engine.project.title.replace(/\s+/g, "_")}.garagestudio.json`);
  document.body.appendChild(downloadAnchor);
  downloadAnchor.click();
  downloadAnchor.remove();
};

// Import Project JSON
window.importProjectJson = function(fileInput) {
  const file = fileInput.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => {
    try {
      const imported = JSON.parse(e.target.result);
      if (imported.tracks) {
        engine.project = imported;
        engine.bpm = imported.bpm || 120;
        document.getElementById("projectTitle").value = imported.title || "Imported Song";
        document.getElementById("bpmInput").value = engine.bpm;
        renderTimeline();
        renderMixer();
        alert("Project loaded successfully!");
      }
    } catch (err) {
      alert("Invalid GarageStudio project file.");
    }
  };
  reader.readAsText(file);
};
