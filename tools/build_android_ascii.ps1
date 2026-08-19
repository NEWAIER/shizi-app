param(
    [string]$Task = "assembleDebug",
    [string]$Output = "deliverables\app-debug-ascii.apk"
)

$ErrorActionPreference = "Stop"
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..\")).Path
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$stage = Join-Path $env:TEMP ("shizi-ascii-build-" + $stamp)
$stageTemp = Join-Path $stage ".tmp"
$jdk = Join-Path $repo ".tools\jdk17\jdk-17.0.20+8"

New-Item -ItemType Directory -Path $stage,$stageTemp -Force | Out-Null
robocopy $repo $stage /E /NFL /NDL /NJH /NJS /XD .git .gradle tmp deliverables chatgpt-web-review ".gradle-temp*" 压缩 | Out-Null
if ($LASTEXITCODE -gt 7) {
    throw "Failed to stage the project for an ASCII-path build. robocopy exit code: $LASTEXITCODE"
}

$env:JAVA_HOME = (Resolve-Path $jdk).Path
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:TEMP = $stageTemp
$env:TMP = $stageTemp

Push-Location $stage
try {
    & .\gradlew.bat $Task --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle task failed: $Task"
    }

    $apk = Get-ChildItem -LiteralPath (Join-Path $stage ".tmp") -Recurse -Filter "app-debug.apk" -File |
        Select-Object -First 1
    if ($null -eq $apk) {
        throw "Gradle completed but app-debug.apk was not found."
    }

    $destination = Join-Path $repo $Output
    New-Item -ItemType Directory -Path (Split-Path -Parent $destination) -Force | Out-Null
    Copy-Item -LiteralPath $apk.FullName -Destination $destination -Force
    Write-Host "APK written to $destination"
}
finally {
    Pop-Location
}
