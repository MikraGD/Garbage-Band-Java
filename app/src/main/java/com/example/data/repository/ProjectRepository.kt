package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Project
import com.example.data.model.SharedProject
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ProjectRepository(private val db: FirebaseFirestore) {

  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  fun observeProjects(): Flow<List<Project>> = flow {
    val uid = requireUserId()
    val collectionRef = db.collection("users").document(uid).collection("projects")
    collectionRef.snapshots()
      .map { snapshot -> snapshot.toObjects(Project::class.java) }
      .catch { error ->
        if (error is Exception) {
          handleFirestoreError(error, OperationType.LIST, collectionRef.path)
        }
        throw error
      }
      .collect { emit(it) }
  }

  suspend fun saveProject(project: Project): Result<String> {
    val uid = requireUserId()
    val projectId = if (project.id.isBlank()) UUID.randomUUID().toString() else project.id
    val docRef = db.collection("users").document(uid).collection("projects").document(projectId)

    return try {
      val tracksJsonArray = JSONArray()
      for (track in project.tracks) {
        val notesJsonArray = JSONArray()
        for (note in track.notes) {
          notesJsonArray.put(
            JSONObject().apply {
              put("id", note.id)
              put("pitch", note.pitch)
              put("velocity", note.velocity.toDouble())
              put("startMs", note.startMs)
              put("durationMs", note.durationMs)
              if (note.sampleId != null) put("sampleId", note.sampleId)
            }
          )
        }

        tracksJsonArray.put(
          JSONObject().apply {
            put("id", track.id)
            put("name", track.name)
            put("instrumentType", track.instrumentType)
            put("colorHex", track.colorHex)
            put("volume", track.volume.toDouble())
            put("pan", track.pan.toDouble())
            put("isMuted", track.isMuted)
            put("isSoloed", track.isSoloed)
            put(
              "effects",
              JSONObject().apply {
                put("reverbWet", track.effects.reverbWet.toDouble())
                put("delayTimeMs", track.effects.delayTimeMs)
                put("delayFeedback", track.effects.delayFeedback.toDouble())
                put("distortionDrive", track.effects.distortionDrive.toDouble())
              }
            )
            put("notes", notesJsonArray)
          }
        )
      }

      val payload = mutableMapOf<String, Any>(
        "userId" to uid,
        "title" to project.title,
        "bpm" to project.bpm,
        "timeSignature" to project.timeSignature,
        "masterVolume" to project.masterVolume,
        "tracks" to project.tracks.map { track ->
          mapOf(
            "id" to track.id,
            "name" to track.name,
            "instrumentType" to track.instrumentType,
            "colorHex" to track.colorHex,
            "volume" to track.volume,
            "pan" to track.pan,
            "isMuted" to track.isMuted,
            "isSoloed" to track.isSoloed,
            "effects" to mapOf(
              "reverbWet" to track.effects.reverbWet,
              "delayTimeMs" to track.effects.delayTimeMs,
              "delayFeedback" to track.effects.delayFeedback,
              "distortionDrive" to track.effects.distortionDrive
            ),
            "notes" to track.notes.map { note ->
              mapOf(
                "id" to note.id,
                "pitch" to note.pitch,
                "velocity" to note.velocity,
                "startMs" to note.startMs,
                "durationMs" to note.durationMs,
                "sampleId" to (note.sampleId ?: "")
              )
            }
          )
        }
      )

      if (project.createdAt == null) {
        payload["createdAt"] = FieldValue.serverTimestamp()
      } else {
        payload["createdAt"] = project.createdAt
        payload["updatedAt"] = FieldValue.serverTimestamp()
      }

      docRef.set(payload).await()
      Result.success(projectId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, if (project.createdAt == null) OperationType.CREATE else OperationType.UPDATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun deleteProject(projectId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun shareProject(project: Project): Result<String> {
    val uid = requireUserId()
    val shareId = "share_" + UUID.randomUUID().toString().replace("-", "").take(8)
    val shareCode = "GS" + (1000..9999).random()

    val projectJsonObj = JSONObject().apply {
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
    }

    val docRef = db.collection("shared_projects").document(shareId)
    val payload = mapOf(
      "shareCode" to shareCode,
      "userId" to uid,
      "title" to project.title,
      "projectJson" to projectJsonObj.toString(),
      "createdAt" to FieldValue.serverTimestamp()
    )

    return try {
      docRef.set(payload).await()
      Result.success(shareId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun getSharedProject(shareId: String): Result<SharedProject?> {
    val docRef = db.collection("shared_projects").document(shareId)
    return try {
      val doc = docRef.get().await()
      if (doc.exists()) {
        Result.success(doc.toObject(SharedProject::class.java))
      } else {
        Result.success(null)
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      Result.failure(e)
    }
  }
}
