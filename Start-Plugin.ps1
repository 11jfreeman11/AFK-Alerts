param([string]$DataDirectory = (Join-Path $env:LOCALAPPDATA 'AfkAlerts'), [switch]$PrepareOnly)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Set-Location $PSScriptRoot
$workspace = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$portableJdk = Get-ChildItem (Join-Path $workspace 'work/jdk11') -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
if ($portableJdk) { $env:JAVA_HOME = $portableJdk.FullName }
else {
    $bankJava = Join-Path $env:LOCALAPPDATA 'BankSlotBorders/temurin-11.0.32.1/jdk-11.0.32.1+1'
    $ownJava = Join-Path $DataDirectory 'temurin-11.0.32.1/jdk-11.0.32.1+1'
    if (Test-Path -LiteralPath (Join-Path $bankJava 'bin/javac.exe')) { $env:JAVA_HOME = $bankJava }
    elseif (Test-Path -LiteralPath (Join-Path $ownJava 'bin/javac.exe')) { $env:JAVA_HOME = $ownJava }
    else {
        if (-not [Environment]::Is64BitOperatingSystem -or $env:PROCESSOR_ARCHITECTURE -eq 'ARM64' -or $env:PROCESSOR_ARCHITEW6432 -eq 'ARM64') { throw 'This launcher requires Intel/AMD 64-bit Windows.' }
        $javaHash = 'd5008f02174c1ad21c10407cbf104815cb390ec4c263d53946e8d0507d7a77f9'
        $javaUrl = 'https://github.com/adoptium/temurin11-binaries/releases/download/jdk-11.0.32.1%2B1/OpenJDK11U-jdk_x64_windows_hotspot_11.0.32.1_1.zip'
        $downloads = Join-Path $DataDirectory 'downloads'
        New-Item -ItemType Directory -Path $downloads -Force | Out-Null
        $archive = Join-Path $downloads 'temurin11.zip'
        if (-not (Test-Path -LiteralPath $archive)) {
            Write-Host 'Downloading portable Java (about 200 MB, first start only)...'
            $partial = "$archive.part"
            Invoke-WebRequest -UseBasicParsing -Uri $javaUrl -OutFile $partial
            if ((Get-FileHash -LiteralPath $partial -Algorithm SHA256).Hash -ne $javaHash) { throw 'Java download failed verification. Nothing from it was run.' }
            Move-Item -LiteralPath $partial -Destination $archive -Force
        }
        if ((Get-FileHash -LiteralPath $archive -Algorithm SHA256).Hash -ne $javaHash) { throw "Cached Java failed verification. Rename $archive and try again." }
        Expand-Archive -LiteralPath $archive -DestinationPath (Join-Path $DataDirectory 'temurin-11.0.32.1') -Force
        $env:JAVA_HOME = $ownJava
    }
}
if (-not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/javac.exe'))) { throw 'Portable Java was not found or did not extract correctly.' }
$workspaceCache = Join-Path $workspace 'work/gradle-home'
$bankCache = Join-Path $env:LOCALAPPDATA 'BankSlotBorders/gradle'
if (Test-Path -LiteralPath $workspaceCache) { $env:GRADLE_USER_HOME = $workspaceCache }
elseif (Test-Path -LiteralPath $bankCache) { $env:GRADLE_USER_HOME = $bankCache }
else { $env:GRADLE_USER_HOME = Join-Path $DataDirectory 'gradle' }
Write-Host "Java ready: $env:JAVA_HOME"
if ($PrepareOnly) { Write-Host 'Preparation passed. RuneLite was not launched.'; exit 0 }
Write-Host 'Building RuneLite. The first start can take several minutes.'
& "$PSScriptRoot/gradlew.bat" run --no-daemon
exit $LASTEXITCODE
