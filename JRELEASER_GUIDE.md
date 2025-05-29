# JReleaser Guide for Maven Central Publishing

This guide explains how to use JReleaser to publish the `jackson-module-protobuf` and `springdoc-bridge-protobuf` modules to Maven Central using the Portal Publisher API.

## Prerequisites

Before you can publish to Maven Central, you need:

1. A Sonatype account with access to the Maven Central Repository
2. GPG keys for signing the artifacts

## Environment Variables

Set the following environment variables before running the deployment:

```bash
# Maven Central credentials
export MAVENCENTRAL_USERNAME=your_sonatype_username
export MAVENCENTRAL_PASSWORD=your_sonatype_password

# GPG signing keys
export GPG_PUBLIC_KEY=your_gpg_public_key
export GPG_SECRET_KEY=your_gpg_secret_key
export GPG_PASSPHRASE=your_gpg_passphrase
```

## Publishing a Release

To publish a release version:

1. Set the `RELEASE` environment variable to remove the `-SNAPSHOT` suffix from the version:

```bash
export RELEASE=true
```

2. Run the JReleaser deploy task:

```bash
./gradlew jreleaserDeploy
```

This will:
- Build the artifacts
- Sign them with your GPG key
- Deploy them to Maven Central using the Portal Publisher API

## Publishing a Snapshot

To publish a snapshot version:

1. Make sure the version in `gradle.properties` ends with `-SNAPSHOT`
2. Run the JReleaser deploy task:

```bash
./gradlew jreleaserDeploy
```

This will deploy the snapshot artifacts to the Maven Central snapshot repository.

## Configuration Details

The JReleaser configuration is defined in `gradle/deploy.gradle`. It includes:

- Maven publication setup with POM information
- Signing configuration for release artifacts
- Maven Central deployment configuration using the Portal Publisher API
- Nexus2 configuration for snapshot deployments

## Troubleshooting

If you encounter issues during deployment:

1. Run with the `--dry-run` flag to test the configuration:

```bash
./gradlew jreleaserDeploy --dry-run
```

2. Run with the `--info` or `--debug` flag for more detailed logs:

```bash
./gradlew jreleaserDeploy --info
```

3. Check the JReleaser documentation for more information: https://jreleaser.org/guide/latest/index.html