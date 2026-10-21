# Android Release Signing Guide for Sinq (ስንቅ)

This guide documents the Google Play App Signing architecture, upload keystore generation, local developer setup via `keystore.properties`, and automated CI/CD signing via GitHub Actions.

---

## 1. Google Play App Signing Architecture

Sinq uses **Google Play App Signing**:
1. **Upload Key**: Generated locally by the maintainer and used exclusively to sign release Android App Bundles (`.aab`) before uploading to Google Play Console.
2. **App Signing Key**: Securely generated and managed in Google's Cloud Key Management Service (KMS). Google Play strips the upload signature and re-signs user-downloaded APKs with the production app signing key.
3. **Key Recovery**: If the upload key is lost or compromised, it can be reset via Google Play Console without stranding existing users or changing the application ID (`com.sinq.app`).

---

## 2. Generating the Upload Keystore

Generate an RSA 2048-bit (or 4096-bit) upload key valid for 25+ years (10,000 days):

```bash
keytool -genkey -v \
  -keystore sinq-upload-keystore.jks \
  -alias sinq-upload-key \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000 \
  -storetype JKS
```

When prompted:
- Enter a strong keystore password.
- Fill in organizational details (e.g., CN=Sinq Devotional, O=EOTC).
- Securely store the resulting `sinq-upload-keystore.jks` file in an offline encrypted vault (e.g. 1Password, Bitwarden, or encrypted USB drive).

> [!CAUTION]
> Never commit `*.jks`, `*.keystore`, or secret properties files to version control. Confirm `*.jks` and `keystore.properties` are in `.gitignore`.

---

## 3. Local Developer Setup (`keystore.properties`)

Create a local `keystore.properties` file in the root directory (or `app/keystore.properties`):

```properties
# keystore.properties (gitignored)
storeFile=/path/to/sinq-upload-keystore.jks
storePassword=YOUR_SECURE_STORE_PASSWORD
keyAlias=sinq-upload-key
keyPassword=YOUR_SECURE_KEY_PASSWORD
```

`app/build.gradle.kts` automatically loads these credentials when building release artifacts:

```bash
# Build release bundle locally
./gradlew bundleRelease

# Verify bundle signature
jarsigner -verify -verbose -certs app/build/outputs/bundle/release/app-release.aab
```

---

## 4. GitHub Actions CI/CD Configuration

For automated GitHub Actions release builds, encode the keystore into base64:

```bash
base64 -w 0 sinq-upload-keystore.jks > sinq-keystore-base64.txt
```

Add the following repository secrets under **GitHub Repository -> Settings -> Secrets and variables -> Actions**:

| Secret Name | Description |
| :--- | :--- |
| `SINQ_KEYSTORE_BASE64` | Base64-encoded string of `sinq-upload-keystore.jks` |
| `SINQ_KEYSTORE_PASSWORD` | Passphrase for the keystore |
| `SINQ_KEY_ALIAS` | Key alias (e.g. `sinq-upload-key`) |
| `SINQ_KEY_PASSWORD` | Passphrase for the specific key alias |

### GitHub Actions Workflow Step Example (`.github/workflows/release.yml`)

```yaml
- name: Decode Upload Keystore
  run: |
    echo "${{ secrets.SINQ_KEYSTORE_BASE64 }}" | base64 -d > app/sinq-release.jks

- name: Build Release Bundle
  env:
    SINQ_STORE_FILE: sinq-release.jks
    SINQ_STORE_PASSWORD: ${{ secrets.SINQ_KEYSTORE_PASSWORD }}
    SINQ_KEY_ALIAS: ${{ secrets.SINQ_KEY_ALIAS }}
    SINQ_KEY_PASSWORD: ${{ secrets.SINQ_KEY_PASSWORD }}
  run: |
    ./gradlew bundleRelease

- name: Clean Keystore Artifact
  if: always()
  run: rm -f app/sinq-release.jks
```
