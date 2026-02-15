param(
    [string]$BindAddress = "127.0.0.1",
    [int]$Port = 17856,
    [string]$AuthToken = "",
    [string]$RepositoryRoot = "",
    [string]$WorkDir = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RepositoryRoot)) {
    $RepositoryRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}
if ([string]::IsNullOrWhiteSpace($WorkDir)) {
    $WorkDir = Join-Path $RepositoryRoot ".tmp\runtime-packager"
}

$script:BindAddress = $BindAddress
$script:Port = $Port
$script:AuthToken = $AuthToken
$script:RepositoryRoot = $RepositoryRoot
$script:WorkDir = $WorkDir
$script:RequestDir = Join-Path $WorkDir "requests"
$script:ArtifactDir = Join-Path $WorkDir "artifacts"
$script:JobRegistry = @{}

New-Item -ItemType Directory -Force -Path $script:RequestDir | Out-Null
New-Item -ItemType Directory -Force -Path $script:ArtifactDir | Out-Null

function Get-NowMs {
    return [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
}

function Normalize-PackageSuffix {
    param([string]$Value)
    $normalized = ([string]$Value).Trim().ToLowerInvariant() -replace '[^a-z0-9_]', '_'
    $normalized = $normalized.Trim('_')
    if ([string]::IsNullOrWhiteSpace($normalized)) {
        $normalized = "app"
    }
    if ($normalized.Length -gt 32) {
        $normalized = $normalized.Substring(0, 32)
    }
    return $normalized
}

function Get-StatePath {
    param([string]$RequestId)
    return Join-Path $script:RequestDir "$RequestId.json"
}

function Load-State {
    param([string]$RequestId)
    $path = Get-StatePath -RequestId $RequestId
    if (-not (Test-Path $path)) {
        return $null
    }
    $raw = Get-Content -Path $path -Raw
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return $null
    }
    return $raw | ConvertFrom-Json
}

function Save-State {
    param($State)
    $path = Get-StatePath -RequestId ([string]$State.requestId)
    $State.updatedAt = Get-NowMs
    $State | ConvertTo-Json -Depth 20 | Set-Content -Path $path -Encoding UTF8
}

function Get-StringProperty {
    param(
        $Object,
        [string]$Name,
        [string]$DefaultValue = ""
    )
    if ($null -eq $Object) {
        return $DefaultValue
    }
    $prop = $Object.PSObject.Properties[$Name]
    if ($null -eq $prop) {
        return $DefaultValue
    }
    $value = [string]$prop.Value
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $DefaultValue
    }
    return $value.Trim()
}

function Get-IntProperty {
    param(
        $Object,
        [string]$Name,
        [int]$DefaultValue = 0
    )
    if ($null -eq $Object) {
        return $DefaultValue
    }
    $prop = $Object.PSObject.Properties[$Name]
    if ($null -eq $prop) {
        return $DefaultValue
    }
    $parsed = 0
    if ([int]::TryParse(([string]$prop.Value), [ref]$parsed)) {
        return $parsed
    }
    return $DefaultValue
}

function Get-LongProperty {
    param(
        $Object,
        [string]$Name,
        [long]$DefaultValue = 0
    )
    if ($null -eq $Object) {
        return $DefaultValue
    }
    $prop = $Object.PSObject.Properties[$Name]
    if ($null -eq $prop) {
        return $DefaultValue
    }
    $parsed = [long]0
    if ([long]::TryParse(([string]$prop.Value), [ref]$parsed)) {
        return $parsed
    }
    return $DefaultValue
}

function Write-JsonResponse {
    param(
        $Context,
        [int]$StatusCode,
        $Body
    )
    $json = $Body | ConvertTo-Json -Depth 20
    $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
    $response = $Context.Response
    $response.StatusCode = $StatusCode
    $response.ContentType = "application/json; charset=utf-8"
    $response.ContentEncoding = [System.Text.Encoding]::UTF8
    $response.ContentLength64 = $bytes.Length
    $response.OutputStream.Write($bytes, 0, $bytes.Length)
    $response.Close()
}

function Write-FileResponse {
    param(
        $Context,
        [string]$FilePath,
        [string]$FileName
    )
    $response = $Context.Response
    $bytes = [System.IO.File]::ReadAllBytes($FilePath)
    $response.StatusCode = 200
    $response.ContentType = "application/vnd.android.package-archive"
    $response.AddHeader("Content-Disposition", "attachment; filename=`"$FileName`"")
    $response.ContentLength64 = $bytes.Length
    $response.OutputStream.Write($bytes, 0, $bytes.Length)
    $response.Close()
}

function Read-RequestJson {
    param($Context)
    $request = $Context.Request
    $encoding = if ($null -ne $request.ContentEncoding) { $request.ContentEncoding } else { [System.Text.Encoding]::UTF8 }
    $reader = New-Object System.IO.StreamReader($request.InputStream, $encoding)
    try {
        $raw = $reader.ReadToEnd()
    } finally {
        $reader.Dispose()
    }
    if ([string]::IsNullOrWhiteSpace($raw)) {
        return $null
    }
    return $raw | ConvertFrom-Json
}

function Test-Authorized {
    param($Context)
    if ([string]::IsNullOrWhiteSpace($script:AuthToken)) {
        return $true
    }
    $token = [string]$Context.Request.Headers["X-Zion-Packager-Token"]
    if ([string]::IsNullOrWhiteSpace($token)) {
        return $false
    }
    return $token.Trim() -eq $script:AuthToken
}

function Get-StateResponse {
    param($State)
    $requestId = [string]$State.requestId
    $statusUrl = "http://$($script:BindAddress):$($script:Port)/v1/runtime/builds/$requestId"
    return [ordered]@{
        requestId = $requestId
        status = [string]$State.status
        statusUrl = $statusUrl
        artifactName = (Get-StringProperty -Object $State -Name "artifactName" -DefaultValue "")
        artifactUrl = (Get-StringProperty -Object $State -Name "artifactUrl" -DefaultValue "")
        error = (Get-StringProperty -Object $State -Name "error" -DefaultValue "")
        versionName = (Get-StringProperty -Object $State -Name "versionName" -DefaultValue "1.0.0")
        versionCode = Get-IntProperty -Object $State -Name "versionCode" -DefaultValue 1
        versionModel = Get-IntProperty -Object $State -Name "versionModel" -DefaultValue 1
        runtimeTemplate = (Get-StringProperty -Object $State -Name "runtimeTemplate" -DefaultValue "runtime_module")
        runtimeShellPackage = (Get-StringProperty -Object $State -Name "runtimeShellPackage" -DefaultValue "")
        runtimeShellDownloadUrl = (Get-StringProperty -Object $State -Name "runtimeShellDownloadUrl" -DefaultValue "")
        updatedAt = Get-LongProperty -Object $State -Name "updatedAt" -DefaultValue (Get-NowMs)
    }
}

function Find-PendingState {
    param(
        [string]$AppId,
        [string]$AppUrl,
        [int]$VersionCode,
        [int]$VersionModel
    )
    if ([string]::IsNullOrWhiteSpace($AppId)) {
        return $null
    }
    foreach ($file in Get-ChildItem -Path $script:RequestDir -Filter *.json -ErrorAction SilentlyContinue) {
        $raw = Get-Content -Path $file.FullName -Raw
        if ([string]::IsNullOrWhiteSpace($raw)) {
            continue
        }
        $state = $raw | ConvertFrom-Json
        $status = (Get-StringProperty -Object $state -Name "status").ToLowerInvariant()
        if ($status -ne "queued" -and $status -ne "in_progress") {
            continue
        }
        $stateAppId = Get-StringProperty -Object $state -Name "appId"
        $stateUrl = Get-StringProperty -Object $state -Name "appUrl"
        $stateVersionCode = Get-IntProperty -Object $state -Name "versionCode" -DefaultValue 0
        $stateVersionModel = Get-IntProperty -Object $state -Name "versionModel" -DefaultValue 0
        if ($stateAppId -eq $AppId -and $stateUrl -eq $AppUrl -and $stateVersionCode -eq $VersionCode -and $stateVersionModel -eq $VersionModel) {
            return $state
        }
    }
    return $null
}

function Start-PackagingJob {
    param($State)
    $statePath = Get-StatePath -RequestId ([string]$State.requestId)
    $repoRoot = $script:RepositoryRoot
    $artifactDir = $script:ArtifactDir
    $bindAddress = $script:BindAddress
    $port = $script:Port

    return Start-Job -Name ("runtime-pack-" + [string]$State.requestId) -ArgumentList $statePath, $repoRoot, $artifactDir, $bindAddress, $port -ScriptBlock {
        param(
            [string]$StatePath,
            [string]$RepoRoot,
            [string]$ArtifactDir,
            [string]$BindAddress,
            [int]$Port
        )
        $ErrorActionPreference = "Stop"

        function Get-NowMs {
            return [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        }

        function Load-StateFromPath {
            param([string]$Path)
            if (-not (Test-Path $Path)) {
                return $null
            }
            $raw = Get-Content -Path $Path -Raw
            if ([string]::IsNullOrWhiteSpace($raw)) {
                return $null
            }
            return $raw | ConvertFrom-Json
        }

        function Save-StateToPath {
            param(
                $State,
                [string]$Path
            )
            $State.updatedAt = Get-NowMs
            $State | ConvertTo-Json -Depth 20 | Set-Content -Path $Path -Encoding UTF8
        }

        $state = Load-StateFromPath -Path $StatePath
        if ($null -eq $state) {
            return
        }

        $state.status = "in_progress"
        Save-StateToPath -State $state -Path $StatePath

        $logPath = Join-Path $ArtifactDir ([string]$state.requestId + ".log")
        try {
            Push-Location $RepoRoot
            $packScriptPath = Join-Path $RepoRoot "_ci\package-runtime.ps1"
            if (-not (Test-Path $packScriptPath)) {
                throw "package-runtime.ps1 not found."
            }

            & $packScriptPath `
                -AppName ([string]$state.appName) `
                -AppUrl ([string]$state.appUrl) `
                -PackageSuffix ([string]$state.packageSuffix) `
                -VersionName ([string]$state.versionName) `
                -VersionCode ([int]$state.versionCode) *>&1 | Tee-Object -FilePath $logPath | Out-Null

            $apkDir = Join-Path $RepoRoot "runtime\build\outputs\apk\release"
            if (-not (Test-Path $apkDir)) {
                throw "APK output directory was not found."
            }
            $apkFile = Get-ChildItem -Path $apkDir -Filter *.apk -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($null -eq $apkFile) {
                throw "APK output file was not found."
            }

            $artifactName = ("{0}-{1}.apk" -f [string]$state.packageSuffix, [string]$state.versionName)
            $artifactPath = Join-Path $ArtifactDir $artifactName
            Copy-Item -Path $apkFile.FullName -Destination $artifactPath -Force

            $state.status = "success"
            $state.error = $null
            $state.artifactPath = $artifactPath
            $state.artifactName = $artifactName
            $state.artifactUrl = "http://$($BindAddress):$($Port)/v1/runtime/builds/$([string]$state.requestId)/artifact"
            $state.logPath = $logPath
            Save-StateToPath -State $state -Path $StatePath
        } catch {
            $state.status = "failed"
            $errorMessage = $_.Exception.Message
            if ([string]::IsNullOrWhiteSpace($errorMessage)) {
                $errorMessage = ($_ | Out-String).Trim()
            }
            if ([string]::IsNullOrWhiteSpace($errorMessage) -and (Test-Path $logPath)) {
                $errorMessage = (Get-Content -Path $logPath -Tail 30 | Out-String).Trim()
            }
            if ([string]::IsNullOrWhiteSpace($errorMessage)) {
                $errorMessage = "Runtime packaging failed. See logPath for details."
            }
            $state.error = $errorMessage
            $state.logPath = $logPath
            Save-StateToPath -State $state -Path $StatePath
        } finally {
            Pop-Location -ErrorAction SilentlyContinue
        }
    }
}

function Cleanup-Jobs {
    foreach ($entry in @($script:JobRegistry.GetEnumerator())) {
        $jobId = [int]$entry.Value
        $job = Get-Job -Id $jobId -ErrorAction SilentlyContinue
        if ($null -eq $job) {
            $script:JobRegistry.Remove($entry.Key)
            continue
        }
        if ($job.State -in @("Completed", "Failed", "Stopped")) {
            Receive-Job -Id $jobId -ErrorAction SilentlyContinue | Out-Null
            Remove-Job -Id $jobId -Force -ErrorAction SilentlyContinue
            $script:JobRegistry.Remove($entry.Key)
        }
    }
}

$listener = New-Object System.Net.HttpListener
$listener.Prefixes.Add("http://$($BindAddress):$($Port)/")
$listener.Start()

Write-Host "Runtime packager bridge started."
Write-Host "Listening: http://$($BindAddress):$($Port)"
Write-Host "Repository: $RepositoryRoot"
Write-Host "Work dir : $WorkDir"

try {
    while ($listener.IsListening) {
        Cleanup-Jobs
        $context = $listener.GetContext()

        try {
            if (-not (Test-Authorized -Context $context)) {
                Write-JsonResponse -Context $context -StatusCode 401 -Body ([ordered]@{
                        error = "Unauthorized"
                    })
                continue
            }

            $request = $context.Request
            $method = $request.HttpMethod.ToUpperInvariant()
            $path = $request.Url.AbsolutePath.TrimEnd("/")
            if ([string]::IsNullOrWhiteSpace($path)) {
                $path = "/"
            }

            if ($method -eq "GET" -and $path -eq "/health") {
                Write-JsonResponse -Context $context -StatusCode 200 -Body ([ordered]@{
                        status = "ok"
                        time = Get-NowMs
                    })
                continue
            }

            if ($method -eq "POST" -and $path -eq "/v1/runtime/builds") {
                $payload = Read-RequestJson -Context $context
                $appId = Get-StringProperty -Object $payload -Name "appId"
                $appName = Get-StringProperty -Object $payload -Name "appName" -DefaultValue "Runtime App"
                $appUrl = Get-StringProperty -Object $payload -Name "appUrl"
                $runtimeTemplate = Get-StringProperty -Object $payload -Name "runtimeTemplate" -DefaultValue "runtime_module"
                $runtimeShellPackage = Get-StringProperty -Object $payload -Name "runtimeShellPackage"
                $runtimeShellDownloadUrl = Get-StringProperty -Object $payload -Name "runtimeShellDownloadUrl"
                $versionName = Get-StringProperty -Object $payload -Name "versionName" -DefaultValue "1.0.0"
                $versionCode = [Math]::Max((Get-IntProperty -Object $payload -Name "versionCode" -DefaultValue 1), 1)
                $versionModel = [Math]::Max((Get-IntProperty -Object $payload -Name "versionModel" -DefaultValue 1), 1)
                $packageSuffix = Normalize-PackageSuffix -Value (Get-StringProperty -Object $payload -Name "packageSuffix")

                if ([string]::IsNullOrWhiteSpace($appUrl)) {
                    Write-JsonResponse -Context $context -StatusCode 400 -Body ([ordered]@{
                            error = "appUrl is required"
                        })
                    continue
                }
                if (-not ($appUrl.StartsWith("https://") -or $appUrl.StartsWith("http://"))) {
                    Write-JsonResponse -Context $context -StatusCode 400 -Body ([ordered]@{
                            error = "appUrl must start with http:// or https://"
                        })
                    continue
                }
                if ($runtimeTemplate -ne "runtime_module" -and $runtimeTemplate -ne "builtin_shell_plugin") {
                    Write-JsonResponse -Context $context -StatusCode 400 -Body ([ordered]@{
                            error = "runtimeTemplate must be runtime_module or builtin_shell_plugin"
                        })
                    continue
                }
                if ($runtimeTemplate -eq "builtin_shell_plugin" -and [string]::IsNullOrWhiteSpace($runtimeShellPackage)) {
                    Write-JsonResponse -Context $context -StatusCode 400 -Body ([ordered]@{
                            error = "runtimeShellPackage is required when runtimeTemplate is builtin_shell_plugin"
                        })
                    continue
                }

                $pending = Find-PendingState -AppId $appId -AppUrl $appUrl -VersionCode $versionCode -VersionModel $versionModel
                if ($null -ne $pending) {
                    Write-JsonResponse -Context $context -StatusCode 200 -Body (Get-StateResponse -State $pending)
                    continue
                }

                $requestId = "rt-" + ([guid]::NewGuid().ToString("N"))
                $state = [pscustomobject]@{
                    requestId = $requestId
                    status = "queued"
                    appId = $appId
                    appName = $appName
                    appUrl = $appUrl
                    packageSuffix = $packageSuffix
                    runtimeTemplate = $runtimeTemplate
                    runtimeShellPackage = $runtimeShellPackage
                    runtimeShellDownloadUrl = $runtimeShellDownloadUrl
                    versionName = $versionName
                    versionCode = $versionCode
                    versionModel = $versionModel
                    createdAt = Get-NowMs
                    updatedAt = Get-NowMs
                    artifactPath = $null
                    artifactName = $null
                    artifactUrl = $null
                    error = $null
                    logPath = $null
                }
                Save-State -State $state
                $job = Start-PackagingJob -State $state
                if ($null -ne $job) {
                    $script:JobRegistry[$requestId] = $job.Id
                }
                Start-Sleep -Milliseconds 120
                $latest = Load-State -RequestId $requestId
                Write-JsonResponse -Context $context -StatusCode 202 -Body (Get-StateResponse -State $latest)
                continue
            }

            if ($method -eq "GET" -and $path -match '^/v1/runtime/builds/([^/]+)$') {
                $requestId = [System.Uri]::UnescapeDataString($Matches[1])
                $state = Load-State -RequestId $requestId
                if ($null -eq $state) {
                    Write-JsonResponse -Context $context -StatusCode 404 -Body ([ordered]@{
                            error = "Request not found"
                        })
                    continue
                }
                Write-JsonResponse -Context $context -StatusCode 200 -Body (Get-StateResponse -State $state)
                continue
            }

            if ($method -eq "GET" -and $path -match '^/v1/runtime/builds/([^/]+)/artifact$') {
                $requestId = [System.Uri]::UnescapeDataString($Matches[1])
                $state = Load-State -RequestId $requestId
                if ($null -eq $state) {
                    Write-JsonResponse -Context $context -StatusCode 404 -Body ([ordered]@{
                            error = "Request not found"
                        })
                    continue
                }
                $artifactPath = Get-StringProperty -Object $state -Name "artifactPath"
                $artifactName = Get-StringProperty -Object $state -Name "artifactName" -DefaultValue "runtime.apk"
                if ([string]::IsNullOrWhiteSpace($artifactPath) -or -not (Test-Path $artifactPath)) {
                    Write-JsonResponse -Context $context -StatusCode 404 -Body ([ordered]@{
                            error = "Artifact not available"
                        })
                    continue
                }
                Write-FileResponse -Context $context -FilePath $artifactPath -FileName $artifactName
                continue
            }

            Write-JsonResponse -Context $context -StatusCode 404 -Body ([ordered]@{
                    error = "Not found"
                })
        } catch {
            Write-JsonResponse -Context $context -StatusCode 500 -Body ([ordered]@{
                    error = $_.Exception.Message
                })
        }
    }
} finally {
    if ($listener.IsListening) {
        $listener.Stop()
    }
    $listener.Close()
    Cleanup-Jobs
    Write-Host "Runtime packager bridge stopped."
}
