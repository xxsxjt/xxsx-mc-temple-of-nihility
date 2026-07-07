$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$SourceJar = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'build\libs') -File |
    Where-Object { $_.Name -like 'temple-of-nihility-neoforge-26.2-*.jar' } |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1
if (-not $SourceJar) {
    throw 'Build output not found. Run .\gradlew.bat build first.'
}
$JarName = $SourceJar.Name
$ModsDir = 'D:\_dx\_Games\MC\xxxxxx\.minecraft\versions\26.2-NeoForge_26.2.0.7-beta\mods'
$TargetJar = Join-Path $ModsDir $JarName
$DisabledDir = Join-Path $ModsDir 'disabled-templenihility'
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

if (-not (Test-Path -LiteralPath $ModsDir)) {
    throw "Mods directory not found: $ModsDir"
}

New-Item -ItemType Directory -Force -Path $DisabledDir | Out-Null

$activeJars = Get-ChildItem -LiteralPath $ModsDir -File |
    Where-Object { $_.Name -like 'temple-of-nihility*.jar' }
$rootBackups = Get-ChildItem -LiteralPath $ModsDir -File |
    Where-Object { $_.Name -like 'temple-of-nihility*.jar.bak-*' }

try {
    $index = 0
    foreach ($jar in $activeJars) {
        $index++
        Move-Item -LiteralPath $jar.FullName -Destination (Join-Path $DisabledDir "old-$Stamp-$index.jar.bak")
    }
    foreach ($backup in $rootBackups) {
        $index++
        Move-Item -LiteralPath $backup.FullName -Destination (Join-Path $DisabledDir "root-bak-$Stamp-$index.jar.bak")
    }
} catch {
    $Pending = Join-Path $ModsDir "$JarName.pending-$Stamp"
    Copy-Item -LiteralPath $SourceJar.FullName -Destination $Pending
    Get-FileHash -Algorithm SHA256 -LiteralPath $Pending | Format-List
    Write-Host "Active jar appears locked; active jar was NOT overwritten. Pending jar written to: $Pending"
    exit 2
}

Copy-Item -LiteralPath $SourceJar.FullName -Destination $TargetJar

$pendingJars = Get-ChildItem -LiteralPath $ModsDir -File |
    Where-Object { $_.Name -like 'temple-of-nihility*.jar.pending-*' }
$index = 0
foreach ($pending in $pendingJars) {
    $index++
    Move-Item -LiteralPath $pending.FullName -Destination (Join-Path $DisabledDir "pending-$Stamp-$index.jar.bak")
}

$loadable = Get-ChildItem -LiteralPath $ModsDir -File |
    Where-Object { $_.Name -like 'temple-of-nihility*.jar' }
if ($loadable.Count -ne 1) {
    throw "Expected exactly one active Temple of Nihility jar, found $($loadable.Count)"
}

Get-FileHash -Algorithm SHA256 -LiteralPath $TargetJar | Format-List
Write-Host "Deployed: $TargetJar"
