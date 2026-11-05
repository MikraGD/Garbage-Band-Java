🎸 Garbage-Band-Java
Java Android Firebase Gradle
Just a simple mixer written in Java! 🎵

📌 Overview
Garbage-Band-Java is an Android-native audio mixing application designed for manipulating audio tracks, adjusting playback controls, and interfacing with real-time cloud storage for user session persistence.
Whether you're layering tracks or experimenting with sound samples, this project serves as a lightweight, flexible Java-based DAW (Digital Audio Workstation) client.

✨ Features
Multi-Track Mixing: Import and layer multiple audio clips smoothly.
Real-Time Volume & FX Controls: Adjust individual track gain, pan, and playback states.
Firebase Backend Integration: Sync session states, audio metadata, and user presets seamlessly across devices.
Web Dashboard Support: Shared backend structures allowing web-based track management.

🛠️ Built With
Language: Java / Kotlin
Framework: Android SDK
Build System: Gradle with Kotlin DSL (build.gradle.kts)
Cloud Database: Google Cloud Firestore
Backend Platform: Firebase Services

📂 Repository Architecture
Garbage-Band-Java/
├── .aistudio/                # Development workspace configurations
├── app/                      # Main Android application module
│   ├── src/                  # Source code (Java/Kotlin) and UI resources
│   └── build.gradle.kts      # App-level build dependencies
├── gradle/                   # Gradle wrapper binaries & properties
├── web/                      # Web interface / companion dashboard
├── .env.example              # Sample template for private environment keys
├── .gitignore                # Git exclusions list
├── build.gradle.kts          # Top-level build configuration
├── firebase-blueprint.json   # Firebase architecture blueprint
├── firebase.json             # Firebase deployment configuration
├── firestore.rules           # Security rules for Cloud Firestore
├── firestore.test.js         # Automated testing suite for Firestore rules
├── gradle.properties         # JVM & build system optimization settings
└── settings.gradle.kts       # Repository project settings & module registration

🚀 Getting Started
Follow these instructions to get a copy of the project up and running on your local machine for development and testing.
Prerequisites
Android Studio: Electric Eel (2022.1.1) or newer recommended.
JDK: Java Development Kit 17 or higher.
Android SDK: API Level 24 (Android 7.0 Nougat) or higher.
Local Setup Instructions
Clone the Repository
git clone https://github.com/MikraGD/Garbage-Band-Java.git
cd Garbage-Band-Java
Configure Environment Variables Duplicate .env.example to generate your local .env file:
cp .env.example .env
Fill in your required credentials (Firebase keys, Database URLs, etc.).
Open Project in Android Studio
Launch Android Studio.
Click Open and select the Garbage-Band-Java directory.
Allow Gradle to resolve dependencies and build indexing automatically.
Build & Run
Connect a physical Android device via USB debugging or start an Android Virtual Device (AVD).
Press Shift + F10 (or click the Run app button).

🧪 Testing Firestore Security Rules
To execute local tests for the Firestore security rules:
npm install -g firebase-tools
npm test

🤝 Contributing
Contributions, issues, and feature requests are welcome!
Fork the Project
Create your Feature Branch (git checkout -b feature/AmazingFeature)
Commit your Changes (git commit -m 'Add some AmazingFeature')
Push to the Branch (git push origin feature/AmazingFeature)
Open a Pull Request

📜 License
This project is licensed under the MIT License — see the LICENSE file for details.
