param(
    [Parameter(Mandatory = $true)]
    [string]$AppName,

    [Parameter(Mandatory = $true)]
    [string]$AppUrl,

    [Parameter(Mandatory = $true)]
    [string]$PackageSuffix,

    [string]$VersionName = "1.0.0",
    [int]$VersionCode = 1
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AppUrl)) {
    throw "AppUrl is required."
}
if (-not $AppUrl.StartsWith("https://") -and -not $AppUrl.StartsWith("http://")) {
    throw "AppUrl must start with http:// or https://."
}
if ([string]::IsNullOrWhiteSpace($PackageSuffix)) {
    throw "PackageSuffix is required."
}

$args = @(
    ":runtime:assembleRelease",
    "--stacktrace",
    "-PRUNTIME_APP_NAME=$AppName",
    "-PRUNTIME_APP_URL=$AppUrl",
    "-PRUNTIME_PACKAGE_SUFFIX=$PackageSuffix",
    "-PRUNTIME_VERSION_NAME=$VersionName",
    "-PRUNTIME_VERSION_CODE=$VersionCode"
)

Write-Host "Packaging runtime APK..."
Write-Host "Name: $AppName"
Write-Host "URL : $AppUrl"
Write-Host "ID  : $PackageSuffix"

& .\gradlew.bat @args

Write-Host "Done. APK output path: runtime\build\outputs\apk\release\"

