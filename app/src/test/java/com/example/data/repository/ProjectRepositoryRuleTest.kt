package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.InstrumentType
import com.example.data.model.NoteEvent
import com.example.data.model.Project
import com.example.data.model.StudioTrack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ProjectRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveProject_validProject_savesSuccessfullyAndReturnsId() = runBlocking {
    val uid = signInTestUser("musician@test.com")
    val repo = ProjectRepository(firestore)

    val project = Project(
      id = "test_song_" + UUID.randomUUID().toString().take(6),
      userId = uid,
      title = "Groove 1",
      bpm = 120,
      timeSignature = "4/4",
      masterVolume = 0.85f,
      tracks = listOf(
        StudioTrack(
          id = "track_1",
          name = "Synth",
          instrumentType = InstrumentType.PIANO.name,
          volume = 0.8f,
          notes = listOf(NoteEvent(pitch = 60, velocity = 0.8f, startMs = 0L, durationMs = 500L))
        )
      )
    )

    val result = withTimeout(5000L) { repo.saveProject(project) }
    assertTrue(result.isSuccess)
    val projectId = result.getOrThrow()
    assertEquals(project.id, projectId)
  }

  @Test
  fun observeProjects_authenticatedOwner_emitsMatchingProject() = runBlocking {
    val uid = signInTestUser("artist@test.com")
    val repo = ProjectRepository(firestore)

    val project = Project(
      id = "test_observe_" + UUID.randomUUID().toString().take(6),
      userId = uid,
      title = "Groove Observe",
      bpm = 128,
      tracks = emptyList()
    )

    withTimeout(5000L) { repo.saveProject(project) }.getOrThrow()

    val projects = withTimeout(4000L) {
      repo.observeProjects().first { list -> list.any { it.id == project.id } }
    }
    assertTrue(projects.any { it.id == project.id })
  }

  @Test
  fun shareProject_publicSnapshot_canBeReadWithoutAuth() = runBlocking {
    val uid = signInTestUser("producer@test.com")
    val repo = ProjectRepository(firestore)

    val project = Project(
      id = "song_to_share",
      userId = uid,
      title = "Shareable Jam",
      bpm = 120,
      tracks = listOf(
        StudioTrack(id = "tr1", name = "Drums", instrumentType = InstrumentType.DRUMS.name)
      )
    )

    val shareResult = withTimeout(5000L) { repo.shareProject(project) }
    assertTrue(shareResult.isSuccess)
    val shareId = shareResult.getOrThrow()

    // Sign out to verify unauthenticated reading of shared project
    auth.signOut()
    val unauthRepo = ProjectRepository(firestore)
    val getResult = withTimeout(5000L) { unauthRepo.getSharedProject(shareId) }
    assertTrue(getResult.isSuccess)
    val shared = getResult.getOrThrow()
    assertNotNull(shared)
    assertEquals("Shareable Jam", shared?.title)
  }
}
