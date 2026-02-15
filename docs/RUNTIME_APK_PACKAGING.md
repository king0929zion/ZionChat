# Runtime APK Packaging

This project supports direct runtime packaging for a hosted web app URL.

## Local one-line packaging

```powershell
gradle :runtime:assembleRelease `
  -PRUNTIME_APP_NAME="My App" `
  -PRUNTIME_APP_URL="https://example.vercel.app/" `
  -PRUNTIME_PACKAGE_SUFFIX="my_app" `
  -PRUNTIME_VERSION_NAME="1.0.0" `
  -PRUNTIME_VERSION_CODE="1"
```

Output APK path:

```text
runtime/build/outputs/apk/release/
```

You can also use the helper script:

```powershell
.\_ci\package-runtime.ps1 `
  -AppName "My App" `
  -AppUrl "https://example.vercel.app/" `
  -PackageSuffix "my_app" `
  -VersionName "1.0.0" `
  -VersionCode 1
```

## GitHub Actions packaging

Use workflow **Package Runtime APK** (`.github/workflows/runtime-packager.yml`) with:

- `app_name`
- `app_url`
- `package_suffix`
- optional `version_name`
- optional `version_code`

The workflow uploads an artifact containing the packaged runtime APK.

## Notes

- `app_url` should be a stable HTTPS URL (for example, deployed by Vercel).
- `package_suffix` should be unique if you want to install multiple runtime APKs side-by-side.
