# android_java_kiosk_moderator

## Google Sign-In setup

The app uses Firebase Auth with Google Sign-In through Android Credential Manager.

For debug builds, the Firebase Android app must be registered with:

- Package name: `com.example.teacherapp`
- Debug SHA-1: `89:DF:D1:55:26:D1:5D:CE:87:42:2D:60:43:43:03:7B:92:18:E8:76`

Firebase/Google Cloud must also have a Web OAuth client. After enabling Google as a Firebase Authentication provider and adding the OAuth clients, download the latest `google-services.json` and place it at `app/google-services.json`.

The Google Services Gradle plugin should generate `default_web_client_id` from that file. Do not hardcode an Android OAuth client ID into `strings.xml`; `GetGoogleIdOption.setServerClientId(...)` requires the Web client ID.
