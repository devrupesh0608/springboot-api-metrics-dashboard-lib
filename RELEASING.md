# Releasing

This page is for maintainers of the library. Application developers add the Maven Central dependency and the YAML in `README.md`.

Maven Central group: `io.github.devrupesh0608`

Published modules:

- `api-metric-core`
- `api-metric-storage-mysql`
- `api-metric-spring-boot-autoconfigure`
- `api-metric-spring-boot-starter`

`api-metric-sample-application` stays in this repository and is not published. The main branch version stays `1.0.0-SNAPSHOT`. A release tag supplies the version that is uploaded. `v1.0.0` publishes `1.0.0`.

Uploads are not released automatically. After the GitHub Action finishes, open the Central Portal deployment and click Publish.

## One-time setup

1. Sign in at [central.sonatype.com](https://central.sonatype.com) with GitHub user `devrupesh0608`.
2. Verify the namespace `io.github.devrupesh0608`. Central allows that group only after this GitHub account verifies it.
3. Create a Central Portal user token. The token username and password are not your GitHub password. Generate the token from the Central Portal account page.
4. Create a GPG key and send the public key to a key server so Central can verify signatures:

   ```bash
   gpg --full-generate-key
   gpg --keyserver keyserver.ubuntu.com --send-keys KEYID
   gpg --export-secret-keys --armor KEYID
   ```

   Replace `KEYID` with the id printed by `gpg --list-secret-keys`. The last command prints the private key. Keep that output private.

5. Make this GitHub repository public.
6. Add these Actions secrets on the repository. Do not commit them.

   | Secret | Value |
   | --- | --- |
   | `MAVEN_CENTRAL_USERNAME` | User token username |
   | `MAVEN_CENTRAL_PASSWORD` | User token password |
   | `SIGNING_KEY` | Full armored private key, including the `BEGIN` and `END` lines |
   | `SIGNING_PASSWORD` | Key passphrase. Leave the secret empty only when the key has no passphrase |

## Publish a version

The tag name must start with `v`, and the rest must be the Maven version.

```bash
git tag v1.0.0
git push origin v1.0.0
```

The Publish workflow runs `./gradlew check`, then:

```bash
./gradlew publishToMavenCentral --no-daemon -Pversion=1.0.0
```

`automaticRelease` is off, so the workflow only uploads a deployment. Open [Deployments](https://central.sonatype.com/publishing/deployments), confirm the four modules and version `1.0.0`, and click Publish.

Maven Central can take up to an hour after that click before another build can resolve:

```text
io.github.devrupesh0608:api-metric-spring-boot-starter:1.0.0
```

Do not publish the same version twice. Central rejects a second upload of a version that was already released. For the next release, tag `v1.0.1` or whatever the next version is. Leave `gradle.properties` on `1.0.0-SNAPSHOT` until you want the main branch to advertise the next snapshot.
