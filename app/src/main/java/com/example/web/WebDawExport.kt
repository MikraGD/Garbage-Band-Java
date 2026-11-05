package com.example.web

import com.example.data.model.Project
import org.json.JSONArray
import org.json.JSONObject

object WebDawExport {

  /**
   * Generates a complete, single-file HTML5/CSS/JavaScript Web Audio DAW
   * that can run in any modern web browser to playback, arrange, and mix projects.
   */
  fun generateWebPlayerHtml(project: Project): String {
    val projectJson = JSONObject().apply {
      put("title", project.title)
      put("bpm", project.bpm)
      put("timeSignature", project.timeSignature)
      put("masterVolume", project.masterVolume.toDouble())
      val tracksArray = JSONArray()
      for (track in project.tracks) {
        val notesArray = JSONArray()
        for (note in track.notes) {
          notesArray.put(
            JSONObject().apply {
              put("pitch", note.pitch)
              put("velocity", note.velocity.toDouble())
              put("startMs", note.startMs)
              put("durationMs", note.durationMs)
              if (note.sampleId != null) put("sampleId", note.sampleId)
            }
          )
        }
        tracksArray.put(
          JSONObject().apply {
            put("id", track.id)
            put("name", track.name)
            put("instrumentType", track.instrumentType)
            put("colorHex", track.colorHex)
            put("volume", track.volume.toDouble())
            put("pan", track.pan.toDouble())
            put("reverb", track.effects.reverbWet.toDouble())
            put("delay", track.effects.delayFeedback.toDouble())
            put("distortion", track.effects.distortionDrive.toDouble())
            put("notes", notesArray)
          }
        )
      }
      put("tracks", tracksArray)
    }.toString()

    return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>${project.title} - GarageStudio Web</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body {
      background-color: #121316;
      color: #E2E8F0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      overflow-x: hidden;
      padding-bottom: 60px;
    }
    header {
      background: #1E2026;
      border-bottom: 1px solid #2D3139;
      padding: 16px 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      flex-wrap: wrap;
      gap: 12px;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .badge {
      background: #F59E0B;
      color: #000;
      font-weight: 700;
      font-size: 11px;
      padding: 3px 8px;
      border-radius: 4px;
      letter-spacing: 0.5px;
    }
    h1 { font-size: 1.25rem; font-weight: 700; color: #F8FAFC; }
    .transport {
      display: flex;
      align-items: center;
      gap: 12px;
      background: #18191E;
      padding: 8px 16px;
      border-radius: 8px;
      border: 1px solid #292C35;
    }
    button.btn {
      background: #2A2D36;
      color: #FFF;
      border: 1px solid #3E4350;
      padding: 8px 16px;
      border-radius: 6px;
      cursor: pointer;
      font-weight: 600;
      transition: all 0.15s;
    }
    button.btn:hover { background: #373C48; }
    button.btn-primary {
      background: #F59E0B;
      color: #000;
      border: none;
    }
    button.btn-primary:hover { background: #D97706; }
    .display {
      font-family: monospace;
      font-size: 14px;
      color: #10B981;
      background: #000;
      padding: 4px 10px;
      border-radius: 4px;
      border: 1px solid #10B98133;
    }
    .timeline {
      padding: 24px;
      max-width: 1200px;
      margin: 0 auto;
    }
    .track-row {
      background: #1E2026;
      border: 1px solid #2D3139;
      border-radius: 8px;
      margin-bottom: 12px;
      display: flex;
      overflow: hidden;
      min-height: 80px;
    }
    .track-info {
      width: 220px;
      background: #252830;
      padding: 12px 16px;
      border-right: 1px solid #2D3139;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
    }
    .track-name { font-weight: 600; font-size: 14px; display: flex; align-items: center; gap: 8px; }
    .track-type { font-size: 11px; color: #94A3B8; text-transform: uppercase; }
    .track-lane {
      flex: 1;
      position: relative;
      background: #16181D;
      overflow-x: auto;
      min-height: 80px;
    }
    .note-block {
      position: absolute;
      top: 20px;
      height: 40px;
      border-radius: 4px;
      opacity: 0.9;
      box-shadow: 0 2px 4px rgba(0,0,0,0.4);
      display: flex;
      align-items: center;
      padding: 0 6px;
      font-size: 11px;
      font-weight: 600;
      color: #FFF;
      pointer-events: none;
    }
    .playhead {
      position: absolute;
      top: 0;
      bottom: 0;
      width: 2px;
      background: #EF4444;
      box-shadow: 0 0 8px #EF4444;
      pointer-events: none;
      z-index: 10;
      left: 0;
    }
    .keys-container {
      margin-top: 24px;
      background: #1E2026;
      border-radius: 12px;
      padding: 20px;
      border: 1px solid #2D3139;
    }
    .piano-keys {
      display: flex;
      position: relative;
      height: 140px;
      user-select: none;
      overflow-x: auto;
    }
    .white-key {
      flex: 1;
      min-width: 44px;
      background: #FFF;
      border: 1px solid #CBD5E1;
      border-radius: 0 0 6px 6px;
      cursor: pointer;
      display: flex;
      align-items: flex-end;
      justify-content: center;
      padding-bottom: 8px;
      font-weight: 700;
      color: #334155;
      font-size: 12px;
    }
    .white-key:active { background: #E2E8F0; }
    .black-key {
      position: absolute;
      width: 26px;
      height: 85px;
      background: #0F172A;
      border-radius: 0 0 4px 4px;
      z-index: 2;
      cursor: pointer;
    }
    .black-key:active { background: #334155; }
    .drums-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 12px;
      margin-top: 16px;
    }
    .drum-pad {
      background: #252830;
      border: 2px solid #373C48;
      border-radius: 8px;
      padding: 20px;
      text-align: center;
      cursor: pointer;
      font-weight: 700;
      transition: all 0.1s;
    }
    .drum-pad:active {
      transform: scale(0.96);
      border-color: #10B981;
      background: #10B98133;
    }
  </style>
</head>
<body>
  <header>
    <div class="brand">
      <span class="badge">GARAGESTUDIO WEB</span>
      <h1 id="songTitle">${project.title}</h1>
    </div>
    <div class="transport">
      <button class="btn btn-primary" id="btnPlay">▶ Play</button>
      <button class="btn" id="btnStop">⏹ Stop</button>
      <div class="display" id="timeDisplay">00:00.00</div>
      <div class="display">BPM: ${project.bpm}</div>
    </div>
  </header>

  <main class="timeline">
    <div style="position: relative;" id="timelineContainer">
      <div class="playhead" id="playhead"></div>
      <div id="trackLanes"></div>
    </div>

    <div class="keys-container">
      <h3 style="margin-bottom: 12px;">Live Interactive Keyboard (Touch / Click)</h3>
      <div class="piano-keys" id="pianoKeys"></div>
    </div>

    <div class="keys-container">
      <h3 style="margin-bottom: 12px;">Live Drum Pads</h3>
      <div class="drums-grid" id="drumsGrid">
        <div class="drum-pad" data-drum="kick">KICK</div>
        <div class="drum-pad" data-drum="snare">SNARE</div>
        <div class="drum-pad" data-drum="hihat_closed">HI-HAT (CL)</div>
        <div class="drum-pad" data-drum="hihat_open">HI-HAT (OP)</div>
        <div class="drum-pad" data-drum="tom_high">HI-TOM</div>
        <div class="drum-pad" data-drum="tom_floor">FLOOR TOM</div>
        <div class="drum-pad" data-drum="clap">CLAP</div>
        <div class="drum-pad" data-drum="crash">CRASH</div>
      </div>
    </div>
  </main>

  <script>
    const projectData = $projectJson;
    let audioCtx = null;
    let isPlaying = false;
    let startTime = 0;
    let animationFrameId = null;

    function getAudioContext() {
      if (!audioCtx) {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
      }
      if (audioCtx.state === 'suspended') {
        audioCtx.resume();
      }
      return audioCtx;
    }

    function midiToFreq(m) {
      return 440 * Math.pow(2, (m - 69) / 12);
    }

    function playSynthTone(freq, duration = 0.5, type = 'sine', decayRate = 3.0) {
      const ctx = getAudioContext();
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();

      osc.type = type;
      osc.frequency.setValueAtTime(freq, ctx.currentTime);

      gain.gain.setValueAtTime(0.7, ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, ctx.currentTime + duration);

      osc.connect(gain);
      gain.connect(ctx.destination);

      osc.start();
      osc.stop(ctx.currentTime + duration);
    }

    function playDrumSound(type) {
      const ctx = getAudioContext();
      const t = ctx.currentTime;
      if (type === 'kick') {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.frequency.setValueAtTime(150, t);
        osc.frequency.exponentialRampToValueAtTime(45, t + 0.15);
        gain.gain.setValueAtTime(1.0, t);
        gain.gain.exponentialRampToValueAtTime(0.001, t + 0.35);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start(t);
        osc.stop(t + 0.35);
      } else if (type === 'snare') {
        const osc = ctx.createOscillator();
        const oscGain = ctx.createGain();
        osc.frequency.setValueAtTime(180, t);
        oscGain.gain.setValueAtTime(0.5, t);
        oscGain.gain.exponentialRampToValueAtTime(0.001, t + 0.15);
        osc.connect(oscGain);
        oscGain.connect(ctx.destination);
        osc.start(t);
        osc.stop(t + 0.15);

        // Noise buffer
        const bufferSize = ctx.sampleRate * 0.2;
        const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
        const data = buffer.getChannelData(0);
        for (let i = 0; i < bufferSize; i++) data[i] = Math.random() * 2 - 1;
        const noise = ctx.createBufferSource();
        noise.buffer = buffer;
        const noiseGain = ctx.createGain();
        noiseGain.gain.setValueAtTime(0.6, t);
        noiseGain.gain.exponentialRampToValueAtTime(0.001, t + 0.2);
        noise.connect(noiseGain);
        noiseGain.connect(ctx.destination);
        noise.start(t);
      } else {
        // High hat / cymbal
        playSynthTone(7500, 0.08, 'triangle');
      }
    }

    // Render Timeline Tracks
    const lanesContainer = document.getElementById('trackLanes');
    const scalePxPerSec = 80;

    projectData.tracks.forEach(track => {
      const row = document.createElement('div');
      row.className = 'track-row';

      const info = document.createElement('div');
      info.className = 'track-info';
      info.innerHTML = `
        <div class="track-name">
          <span style="width:10px;height:10px;border-radius:50%;background:${'$'}{track.colorHex}"></span>
          ${'$'}{track.name}
        </div>
        <div class="track-type">${'$'}{track.instrumentType}</div>
      `;

      const lane = document.createElement('div');
      lane.className = 'track-lane';

      track.notes.forEach(note => {
        const block = document.createElement('div');
        block.className = 'note-block';
        block.style.background = track.colorHex;
        block.style.left = ((note.startMs / 1000) * scalePxPerSec) + 'px';
        block.style.width = Math.max(20, (note.durationMs / 1000) * scalePxPerSec) + 'px';
        block.innerText = note.sampleId ? note.sampleId : 'Note ' + note.pitch;
        lane.appendChild(block);
      });

      row.appendChild(info);
      row.appendChild(lane);
      lanesContainer.appendChild(row);
    });

    // Interactive Piano Keyboard
    const pianoEl = document.getElementById('pianoKeys');
    const notesList = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
    for (let octave = 3; octave <= 5; octave++) {
      notesList.forEach((noteName, idx) => {
        const isBlack = noteName.includes('#');
        if (!isBlack) {
          const key = document.createElement('div');
          key.className = 'white-key';
          key.innerText = noteName + octave;
          const midi = 12 + octave * 12 + idx;
          key.addEventListener('mousedown', () => playSynthTone(midiToFreq(midi), 0.6, 'sine'));
          pianoEl.appendChild(key);
        }
      });
    }

    // Interactive Drum Pads
    document.querySelectorAll('.drum-pad').forEach(pad => {
      pad.addEventListener('mousedown', () => {
        playDrumSound(pad.dataset.drum);
      });
    });

    // Transport Play / Stop
    const playhead = document.getElementById('playhead');
    const timeDisplay = document.getElementById('timeDisplay');
    const btnPlay = document.getElementById('btnPlay');
    const btnStop = document.getElementById('btnStop');

    btnPlay.addEventListener('click', () => {
      if (isPlaying) return;
      getAudioContext();
      isPlaying = true;
      startTime = Date.now();
      btnPlay.innerText = '⏸ Pause';

      // Schedule all notes in Web Audio
      const ctx = getAudioContext();
      const now = ctx.currentTime;
      projectData.tracks.forEach(track => {
        track.notes.forEach(note => {
          const delaySec = note.startMs / 1000;
          setTimeout(() => {
            if (!isPlaying) return;
            if (track.instrumentType === 'DRUMS') {
              playDrumSound(note.sampleId || 'kick');
            } else {
              playSynthTone(midiToFreq(note.pitch), note.durationMs / 1000, 'triangle');
            }
          }, note.startMs);
        });
      });

      function updatePlayhead() {
        if (!isPlaying) return;
        const elapsedSec = (Date.now() - startTime) / 1000;
        const x = 220 + (elapsedSec * scalePxPerSec);
        playhead.style.left = x + 'px';

        const mins = Math.floor(elapsedSec / 60);
        const secs = (elapsedSec % 60).toFixed(2);
        timeDisplay.innerText = String(mins).padStart(2, '0') + ':' + String(secs).padStart(5, '0');

        animationFrameId = requestAnimationFrame(updatePlayhead);
      }
      updatePlayhead();
    });

    btnStop.addEventListener('click', () => {
      isPlaying = false;
      btnPlay.innerText = '▶ Play';
      cancelAnimationFrame(animationFrameId);
      playhead.style.left = '220px';
      timeDisplay.innerText = '00:00.00';
    });
  </script>
</body>
</html>
    """.trimIndent()
  }
}
