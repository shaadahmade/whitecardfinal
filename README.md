# Cardoo — Digital Identity App

**Cardoo** is an Android app that lets users store, manage, and share their identity documents (Aadhaar, PAN, Driving License) digitally through a secure virtual ID card with QR code verification.

---

## Features

- **Virtual ID Card** — Interactive 3D card displaying Aadhaar, PAN, and Driving License details
- **QR Code Verification** — Generate and scan QR codes to verify identity
- **Google Sign-In & Email Auth** — Firebase-powered authentication
- **Document Verification** — Verify Aadhaar, PAN, and Driving License within the app
- **Card Templates** — Multiple visual themes (Blue Gradient, Rose Gold, Emerald, Ocean, etc.)
- **Profile Management** — Update profile photo and personal details
- **Physical Card Support** — Order a physical backup card
- **Shimmer Loading UI** — Smooth loading states throughout the app

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Backend | Firebase Firestore, Firebase Auth, Firebase Storage |
| QR Codes | ML Kit / ZXing |
| Architecture | MVVM (ViewModel + Coroutines) |
| Image Loading | Coil |
| Build System | Gradle (KTS) |

---

## Project Structure

```
app/src/main/java/com/example/whitecard/
├── MainActivity.kt
├── firebase/
│   ├── Firebasemanager.kt       # Firestore CRUD, Storage, Auth operations
│   └── checkstatus.kt
├── screens/
│   ├── login.kt                 # Email & Google login
│   ├── signup.kt                # Email & Google signup
│   ├── mainscreen.kt            # Home screen with virtual ID card
│   ├── profiledetailscreen.kt
│   ├── templatescreen.kt        # Card theme selector
│   ├── qrscreen.kt
│   ├── yourcardscreen.kt
│   ├── welcome.kt               # Onboarding
│   ├── updatescreen.kt
│   └── ...
├── verifyscreen/
│   ├── adharverifivationscreen.kt
│   ├── panverify.kt
│   └── licenseverify.kt
├── qrutils/
│   ├── ANALYXER.kt              # QR analyzer
│   ├── qt.kt
│   └── utils.kt
├── viewmodels/
│   └── mainscreenviewmlodel.kt
├── elements/
│   ├── bottombar.kt
│   └── topbar.kt
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

---

## Getting Started

### Prerequisites

- Android Studio Hedgehog or later
- Android SDK 26+
- A Firebase project

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/shaadahmade/whitecardfinal.git
   cd whitecardfinal
   ```

2. **Add Firebase config**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project (or use an existing one)
   - Add an Android app with package name `com.example.whitecard`
   - Download `google-services.json` and place it in `app/`

3. **Add your Google OAuth Web Client ID**
   - In `login.kt` and `signup.kt`, replace the placeholder with your actual Web Client ID:
   ```kotlin
   .requestIdToken("YOUR_WEB_CLIENT_ID_HERE")
   ```

4. **Set up Firebase services**
   - Enable **Authentication** (Email/Password + Google)
   - Enable **Firestore Database**
   - Enable **Firebase Storage**

5. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```


## Permissions

The app requests the following permissions:

| Permission | Reason |
|---|---|
| `INTERNET` | Firebase & API communication |
| `CAMERA` | QR code scanning |
| `USE_BIOMETRIC` | Biometric authentication |
| `POST_NOTIFICATIONS` | Push notifications |
| `READ/WRITE_EXTERNAL_STORAGE` | Card image handling |

---

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you'd like to change.

---

## License

This project is for personal/educational use. See [LICENSE](LICENSE) for details.
