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

## Group-join notifications

The Android app registers verified users' Firebase Cloud Messaging tokens under `userDevices/{uid}/tokens`. The Cloud Function in `functions/` sends push alerts to existing group members when a new verified member joins. It also writes one email per existing member to the `mail` collection.

To enable delivery, first link the Firebase project to the Blaze plan. Cloud Functions requires Blaze and can incur charges based on usage. Then install the official **Trigger Email** Firebase Extension and configure it with an SMTP sender account; use the `mail` collection. The extension needs SMTP credentials, which must be entered in Firebase and must not be committed to this repository. Finally, install Firebase CLI, sign in to project `bhai-paise`, and deploy with `firebase deploy --only firestore:rules,functions`. Build and install app version 1.2.0 or newer so devices register their push tokens.

The Trigger Email extension sends emails from documents in the configured collection and requires SMTP connection details. See the [Firebase Trigger Email setup](https://firebase.google.com/docs/extensions/official/firestore-send-email) and [Cloud Functions getting started guide](https://firebase.google.com/docs/functions/get-started).

## Build a release

The release signing key is intentionally not included. Create it once and keep a secure backup; losing it can prevent future updates from using the same signing identity.

```sh
mkdir -p "$HOME/.android"
keytool -genkeypair -v \
  -keystore "$HOME/.android/bhai-paise-upload.jks" \
  -storetype JKS -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

`keytool` prompts for the keystore and key passwords. Keep them private. You can put them in a local `keystore.properties` file at the project root (this file is ignored by Git):

```properties
KEYSTORE_PATH=/absolute/path/to/my-upload-key.jks
STORE_PASSWORD=your-keystore-password
KEY_PASSWORD=your-key-password
```

If the key password matches the keystore password, `KEY_PASSWORD` can be omitted. The same values can instead be supplied as environment variables. Then build the Play Store bundle with `./gradlew bundleRelease`.

## Privacy and configuration

Do not commit `google-services.json`, `.env`, signing keys, local Android Studio settings, or build outputs. The Firebase config file contains project identifiers and an API key; Firestore security rules and Firebase Authentication must protect the data.
