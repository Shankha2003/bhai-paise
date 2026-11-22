# Bhai Paise

Bhai Paise is an Android bill-splitting app built with Kotlin and Jetpack Compose. It supports local group ledgers, shared groups, expense splits, settlements, invite codes, and UPI payment handoff.

## Open and run

1. Open this folder in Android Studio.
2. Let Android Studio sync the Gradle project.
3. Select an emulator or connect an Android phone with USB debugging enabled.
4. Click **Run**.

The debug build uses the project debug key. Do not remove the `debugConfig` signing assignment from `app/build.gradle.kts`.

## Firebase setup

The Android app is registered in Firebase project `bhai-paise` with package `com.aistudio.slicesplit.xkmqvr`. Cloud Firestore is configured in Mumbai (`asia-south1`), with email/password authentication and verified-email access rules.

Download the Android app's `google-services.json` from Firebase Console and place it at `app/google-services.json`. That file is intentionally ignored by Git. Configure Firebase Authentication's Email/Password provider and deploy the repository's `firestore.rules` to the same Firebase project. Users need verified email accounts to create or join synced groups.

## Build a release

The release signing key is intentionally not included. Create it once and keep a secure backup; losing it can prevent future updates from using the same signing identity.

```sh
mkdir -p "$HOME/.android"
keytool -genkeypair -v \
  -keystore "$HOME/.android/bhai-paise-upload.jks" \
  -storetype JKS -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` prompts for the keystore and key passwords. Keep them private and make `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` available to Gradle in your local environment. Do not put passwords in source files or commit them. Then build the Play Store bundle with `./gradlew bundleRelease`.

## Privacy and configuration

Do not commit `google-services.json`, `.env`, signing keys, local Android Studio settings, or build outputs. The Firebase config file contains project identifiers and an API key; Firestore security rules and Firebase Authentication must protect the data.
