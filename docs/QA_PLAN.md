# Tooliva — QA Plan

## Highest-risk areas

1. file deletion
2. Vault encryption/import/export
3. App Lock reliability
4. permissions/special access
5. MediaStore differences
6. OEM background restrictions
7. ads
8. huge media libraries

## Device matrix

Minimum:
- Pixel / Android 16
- Pixel / Android 14 or 15
- recent Samsung / One UI
- Xiaomi/Redmi / HyperOS or MIUI
- Android 11 device/emulator
- Android 8/9 compatibility device/emulator

Additional when available:
- Oppo/Realme
- Vivo

## Storage datasets

Small:
- <100 media files

Medium:
- ~5,000 media files

Large:
- 50,000+ media files where device permits

Include:
- exact duplicates
- same-size different content
- corrupt images
- file deleted during scan
- huge videos
- Unicode/weird filenames
- zero-byte files
- inaccessible SAF documents

## Destructive-operation tests

- cancel deletion
- approve deletion
- item disappears before confirmation
- permission revoked
- process killed
- low-storage state
- one item vs large multi-select
- trash supported/not supported
- verify UI bytes/count match actual result

## Vault tests

- import photo/video/document
- process death during import
- insufficient space
- wrong PIN
- biometric cancel/lockout
- export
- source deletion
- corrupted encrypted file
- update/migration behavior

## Notification tests

- notification posted/removed
- excluded app
- long text
- media notification
- listener disabled
- retention cleanup
- 10k+ stored records

## App Lock tests

Per OEM:
- chosen app opens
- rapid app switching
- recent apps
- split-screen
- picture-in-picture
- screen off/on
- reboot
- battery optimization
- overlay/usage access revoked

If normal usage can trivially bypass the lock, do not market it as secure.

## Performance

Measure:
- cold start
- dashboard render
- scan duration
- scan memory
- duplicate-hash CPU cost
- battery impact
- ANR/crash rate

## Ads

- no ad before expected content
- frequency cap works
- offline behavior
- Pro removes ads
- no ads in sensitive screens
- failed ad never blocks the tool

## Release sign-off

- [ ] build passes
- [ ] no open P0 bugs
- [ ] permissions audited
- [ ] Data Safety audited
- [ ] privacy policy current
- [ ] store screenshots match build
- [ ] App Lock go/no-go decided
- [ ] destructive actions manually verified
