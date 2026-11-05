const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

// --- SECURITY BOUNDS TESTS ---

test("Unauthenticated user: cannot read user projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
});

test("Authenticated user: can create valid project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const projRef = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  await assertSucceeds(projRef.set({
    userId: ALICE_UID,
    title: "Midnight Jam",
    bpm: 120,
    timeSignature: "4/4",
    masterVolume: 0.8,
    tracks: [
      { id: "track_1", name: "Piano Synth", instrumentType: "PIANO", volume: 0.8 }
    ],
    createdAt: new Date(),
  }));
});

test("Cross-user access: Bob cannot read Alice's project", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("projects").doc("alice_proj").set({
      userId: ALICE_UID,
      title: "Secret Song",
      bpm: 128,
      timeSignature: "4/4",
      masterVolume: 1.0,
      tracks: [],
      createdAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("alice_proj").get());
});

test("Public shared project: unauthenticated user can get shared project by ID", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("shared_projects").doc("share_123").set({
      shareCode: "GRG123",
      userId: ALICE_UID,
      title: "Public Jam",
      projectJson: "{}",
      createdAt: new Date(),
    });
  });

  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauthDb.collection("shared_projects").doc("share_123").get());
});

test("Public shared project: unauthenticated user cannot list all shared projects", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shared_projects").get());
});
