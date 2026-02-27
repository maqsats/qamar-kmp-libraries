# How to Get Credentials & Publish

You **don't** upload files on the Sonatype website. Gradle uploads them when you run `./gradlew publish`. The "Upload Your File" / "Choose File" screen is for manual uploads; ignore it for this project.

---

## 1. What You Need (Credentials Summary)

| What | Where to get it | Used as |
|------|------------------|--------|
| **OSSRH_USERNAME** | Central Portal user token | Token username from central.sonatype.com |
| **OSSRH_PASSWORD** | Central Portal user token | Token password from central.sonatype.com |
| **SIGNING_KEY_ID** | From `gpg --list-keys` | The key ID (e.g. `ABC123DEF456`) |
| **SIGNING_PASSWORD** | You chose when creating the GPG key | Passphrase for that key |
| **SIGNING_KEY** | From `gpg --export-secret-keys ... \| base64` | Base64 blob of your private key |

You already have `io.github.maqsats` verified and SNAPSHOTs enabled, so Sonatype access is set.

---

## 2. Get Sonatype Username & Password (OSSRH_*)

1. Go to **https://central.sonatype.com/** and log in (or create an account).
2. Navigate to **Account → Generate User Token**.
3. **OSSRH_USERNAME** = the token username (not your login email).
4. **OSSRH_PASSWORD** = the token password.

Use these **only** as env vars or in GitHub Secrets. Don't put them in `gradle.properties` that you commit.

---

## 3. Get GPG Signing Keys (SIGNING_KEY_ID, SIGNING_PASSWORD, SIGNING_KEY)

### 3.1 Create a GPG key (if you don't have one)

```bash
gpg --gen-key
```

- Kind: **RSA and RSA**, **4096** bits.
- Set email/name as you like.
- Set a **passphrase** → this is **SIGNING_PASSWORD**.

### 3.2 Get the Key ID (SIGNING_KEY_ID)

```bash
gpg --list-keys
```

You'll see something like:

```
pub   rsa4096 2024-01-15 [SC]
      XXXXYYYYZZZZ1234567890ABCDEF1234567890
uid           [ultimate] Your Name <you@example.com>
```

The long hex string (e.g. `XXXXYYYYZZZZ...`) is your **SIGNING_KEY_ID**. You can use either the **full fingerprint** or the **last 8 characters**; this project's build normalizes to the last 8 for Gradle's signing plugin.

### 3.3 Export private key as base64 (SIGNING_KEY)

**macOS:**

```bash
gpg --export-secret-keys --armor YOUR_KEY_ID | base64
```

**Linux:**

```bash
gpg --export-secret-keys --armor YOUR_KEY_ID | base64 -w 0
```

Replace `YOUR_KEY_ID` with the key ID from the previous step.  
Copy the **entire** output (one long base64 string). That's **SIGNING_KEY**.

### 3.4 Publish the public key (required for Sonatype)

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
```

---

## 4. Publish From Your Machine (Local)

Set the variables in your shell (no spaces around `=`), then run the publish script.

```bash
# Required
export OSSRH_USERNAME="your-central-portal-token-username"
export OSSRH_PASSWORD="your-central-portal-token-password"
export SIGNING_KEY_ID="your-key-id-from-gpg-list-keys"
export SIGNING_PASSWORD="passphrase-you-set-for-gpg-key"
export SIGNING_KEY="paste-the-full-base64-output-here"

# Optional (defaults exist in the project)
# export PUBLISHING_DEVELOPER_ID="maqsats"
# export PUBLISHING_DEVELOPER_NAME="Maksat Inkar"
# export PUBLISHING_DEVELOPER_EMAIL="your@email.com"
```

Then, from the project root:

**SNAPSHOT (good for testing; you said SNAPSHOTs are enabled):**

```bash
./scripts/publish.sh 1.0.0 true
```

**Release (real version, e.g. 1.0.0):**

```bash
./scripts/publish.sh 1.0.0 false
```

Or, without the script:

```bash
# Set VERSION_NAME in gradle.properties to 1.0.0 or 1.0.0-SNAPSHOT, then:
./gradlew publish --no-daemon
```

After a **release** publish, check your deployment status on the Central Portal (see below). For **SNAPSHOT** it goes straight to the snapshot repo.

---

## 5. Publish Via GitHub Actions (CI)

1. In your repo: **Settings → Secrets and variables → Actions**.
2. Add these **Repository secrets** (names must match exactly):

   | Secret name         | Value |
   |---------------------|--------|
   | `OSSRH_USERNAME`    | Central Portal token username |
   | `OSSRH_PASSWORD`    | Central Portal token password |
   | `SIGNING_KEY_ID`   | GPG key ID |
   | `SIGNING_PASSWORD` | GPG key passphrase |
   | `SIGNING_KEY`      | Full base64 private key (one line; newlines are OK, it is passed via env) |

   Optional (defaults are used if omitted): `PUBLISHING_DEVELOPER_ID`, `PUBLISHING_DEVELOPER_NAME`, `PUBLISHING_DEVELOPER_EMAIL`.

3. Trigger the workflow:
   - **From a release:** create a Release, tag e.g. `v1.0.0` → workflow runs and publishes that version.
   - **Manual run:** **Actions → "Publish to Maven Central" → Run workflow**, then enter version (e.g. `1.0.0`) and "snapshot" (true/false).

Note: because this project publishes iOS artifacts (`iosArm64`, `iosX64`, `iosSimulatorArm64`), the publish workflow runs on **macOS** runners.

Again: **no file upload on the website.** The workflow runs `./gradlew publish` and uploads everything for you.

---

## 6. After a **Release** Publish: Verify on Central Portal

Only for **non-SNAPSHOT** publishes (e.g. `1.0.0`).

1. Open **https://central.sonatype.com/** and log in.
2. Go to **Publishing → Deployments**.
3. Find your deployment and verify its status.
4. The Central Portal validates and publishes automatically; wait 10–30 minutes for Maven Central sync.

Your publish is done by Gradle (or the publish script / GitHub Action). The new Central Portal handles validation and release automatically.

---

## 7. Verify It Worked

- **Central Portal Deployments:**  
  https://central.sonatype.com/publishing/deployments
- **Release (after sync):**  
  https://repo1.maven.org/maven2/io/github/maqsats/
- **Search on Maven Central:**  
  https://central.sonatype.com/search?q=io.github.maqsats

---

## Quick Reference: "API keys" = these five

- **OSSRH_USERNAME** → Central Portal token username  
- **OSSRH_PASSWORD** → Central Portal token password  
- **SIGNING_KEY_ID** → `gpg --list-keys` → key ID  
- **SIGNING_PASSWORD** → GPG key passphrase  
- **SIGNING_KEY** → `gpg --export-secret-keys --armor KEY_ID | base64`  

You don't create "API keys" in a separate screen; you use your Central Portal token + GPG key as above.
