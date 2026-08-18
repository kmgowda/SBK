# Copyright (c) KMG. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

param(
    [ValidateSet('true', 'false')]
    [string]$InstallIfMissing = 'true',
    [Parameter(Mandatory = $true)]
    [string]$ConfigFile
)

$ErrorActionPreference = 'Stop'
$bootstrapConfig = @{}
Get-Content -LiteralPath $ConfigFile | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith('#')) {
        $parts = $line.Split('=', 2)
        if ($parts.Length -eq 2) { $bootstrapConfig[$parts[0].Trim()] = $parts[1].Trim() }
    }
}
$jdkVersion = $bootstrapConfig.SBK_JAVA_VERSION
$jdkMajor = $bootstrapConfig.SBK_JAVA_MAJOR
$baseUrl = $bootstrapConfig.SBK_JAVA_BASE_URL
$windowsX64Sha256 = $bootstrapConfig.SBK_JAVA_SHA256_WINDOWS_X64
$downloadTimeoutSeconds = [int]$bootstrapConfig.SBK_JAVA_DOWNLOAD_TIMEOUT_SECONDS
$lockTimeoutSeconds = [int]$bootstrapConfig.SBK_JAVA_LOCK_TIMEOUT_SECONDS

function Test-SbkJdk([string]$CandidateHome) {
    if (-not $CandidateHome) { return $false }
    $java = Join-Path $CandidateHome 'bin\java.exe'
    $javac = Join-Path $CandidateHome 'bin\javac.exe'
    if (-not (Test-Path $java -PathType Leaf) -or -not (Test-Path $javac -PathType Leaf)) { return $false }
    $versionText = (& $java -version 2>&1 | Out-String)
    $versionPattern = 'version "' + [Regex]::Escape($jdkMajor) + '(?:\.|"|-)'
    return $versionText -match $versionPattern
}

function Stop-SbkJava([string]$Message) {
    [Console]::Error.WriteLine("ERROR: $Message")
    exit 1
}

if ($env:SBK_JAVA_HOME) {
    $candidate = $env:SBK_JAVA_HOME.Trim('"')
    if (-not (Test-SbkJdk $candidate)) {
        Stop-SbkJava "SBK_JAVA_HOME must point to a complete JDK 25 installation: $candidate"
    }
    Write-Output $candidate
    exit 0
}

if ($env:JAVA_HOME) {
    $candidate = $env:JAVA_HOME.Trim('"')
    if (-not (Test-SbkJdk $candidate)) {
        Stop-SbkJava "JAVA_HOME must point to a complete JDK 25 installation: $candidate"
    }
    Write-Output $candidate
    exit 0
}

$pathJava = Get-Command java.exe -ErrorAction SilentlyContinue
if ($pathJava) {
    $settings = (& $pathJava.Source -XshowSettings:properties -version 2>&1) -join "`n"
    if ($settings -match '(?m)^\s*java\.home\s*=\s*(.+)$') {
        $candidate = $Matches[1].Trim()
        if (Test-SbkJdk $candidate) {
            Write-Output $candidate
            exit 0
        }
    }
}

$cacheRoot = if ($env:SBK_JAVA_CACHE_DIR) {
    $env:SBK_JAVA_CACHE_DIR
} else {
    $localAppData = [Environment]::GetFolderPath('LocalApplicationData')
    if (-not $localAppData) { $localAppData = Join-Path $env:USERPROFILE '.cache' }
    Join-Path $localAppData 'SBK\jdks'
}
$target = Join-Path $cacheRoot "openjdk-$jdkVersion-windows-x64"
if (Test-SbkJdk $target) {
    Write-Output $target
    exit 0
}
if ($InstallIfMissing -ne 'true') {
    Stop-SbkJava 'no usable JDK 25 was found. Run gradlew once to install the managed JDK, or set SBK_JAVA_HOME.'
}

if (-not [Environment]::Is64BitOperatingSystem -or $env:PROCESSOR_ARCHITECTURE -notin @('AMD64', 'x86')) {
    Stop-SbkJava "automatic JDK installation is unsupported on Windows/$env:PROCESSOR_ARCHITECTURE. Set SBK_JAVA_HOME to a JDK 25 installation."
}

New-Item -ItemType Directory -Force -Path $cacheRoot | Out-Null
$mutexName = "Local\SBK-JDK-$jdkVersion-windows-x64"
$mutex = [Threading.Mutex]::new($false, $mutexName)
if (-not $mutex.WaitOne([TimeSpan]::FromSeconds($lockTimeoutSeconds))) {
    Stop-SbkJava "timed out waiting for another SBK JDK installation: $target"
}

try {
    if (Test-SbkJdk $target) {
        Write-Output $target
        exit 0
    }
    $temp = Join-Path $cacheRoot ('.openjdk-' + [Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $temp | Out-Null
    $archive = Join-Path $temp 'openjdk.zip'
    $url = "$baseUrl/openjdk-${jdkVersion}_windows-x64_bin.zip"
    [Console]::Error.WriteLine("Downloading OpenJDK $jdkVersion for windows-x64 to the SBK user cache...")
    $ProgressPreference = 'SilentlyContinue'
    Invoke-WebRequest -Uri $url -OutFile $archive -UseBasicParsing -TimeoutSec $downloadTimeoutSeconds
    $actualHash = (Get-FileHash -Algorithm SHA256 $archive).Hash.ToLowerInvariant()
    if ($actualHash -ne $windowsX64Sha256) {
        throw "OpenJDK checksum mismatch: expected $windowsX64Sha256, found $actualHash"
    }
    Expand-Archive -Path $archive -DestinationPath $temp
    $extracted = Join-Path $temp "jdk-$jdkVersion"
    if (-not (Test-SbkJdk $extracted)) { throw 'Downloaded archive does not contain a valid JDK 25.' }
    if (Test-Path $target) {
        Move-Item $target "$target.invalid.$PID"
    }
    Move-Item $extracted $target
    Remove-Item $temp -Recurse -Force
    Write-Output $target
} catch {
    if ($temp -and (Test-Path $temp)) { Remove-Item $temp -Recurse -Force }
    Stop-SbkJava "managed JDK installation failed: $($_.Exception.Message)"
} finally {
    $mutex.ReleaseMutex()
    $mutex.Dispose()
}
