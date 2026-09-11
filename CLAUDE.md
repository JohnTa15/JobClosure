# JobClosure

## Changelog: required for every user-visible change

`shared/src/commonMain/kotlin/gr/gtar/jobclosure/shared/changelog/Changelog.kt` is
what the apps show as the "what's new" popup after an update, and it is the only
place the user finds out anything changed. A change that ships without a bullet
there is invisible to them, so treat the changelog as part of the change, not as
follow-up work: add it in the same commit.

- Add a new `ChangelogEntry` with the next id for each batch of work you push.
  Every push publishes a release, so never edit an entry that is already pushed -
  both platforms store the last id they displayed and rely on ids only going up.
- Bullets are Greek, written for a wedding photographer rather than a developer:
  what they can now do, not which class changed. Skip refactors, CI fixes and
  anything with no visible effect.
- Follow the tone of the existing entries: one sentence per bullet, no version
  numbers, no "fixed a bug in X".

## Building

The Android Gradle plugin is fetched from `dl.google.com`, which the Claude Code
web sandbox cannot reach, so `./gradlew` cannot compile there. The GitHub Actions
workflow `.github/workflows/build-apk.yml` builds Android, Windows and Linux on
every push to any branch and publishes the APK to the `debug-latest` release -
that run is the check that the code compiles. Check its conclusion after pushing
rather than reporting the change as verified.
