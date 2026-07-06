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
$Stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

if (-not (Test-Path -LiteralPath $ModsDir)) {
    throw "Mods directory not found: $ModsDir"
}

$javaProcesses = Get-Process java,javaw -ErrorAction SilentlyContinue
if ($javaProcesses) {
    $Pending = Join-Path $ModsDir "$JarName.pending-$Stamp"
    Copy-Item -LiteralPath $SourceJar.FullName -Destination $Pending
    Get-FileHash -Algorithm SHA256 -LiteralPath $Pending | Format-List
    Write-Host "Minecraft/Java is running; active jar was NOT overwritten. Pending jar written to: $Pending"
    exit 2
}

$activeJars = Get-ChildItem -LiteralPath $ModsDir -File |
    Where-Object { $_.Name -like 'temple-of-nihility*.jar' }

foreach ($jar in $activeJars) {
    Move-Item -LiteralPath $jar.FullName -Destination ($jar.FullName + ".bak-$Stamp")
}

Copy-Item -LiteralPath $SourceJar.FullName -Destination $TargetJar

$loadable = Get-ChildItem -LiteralPath $ModsDir -File |
    Where-Object { $_.Name -like 'temple-of-nihility*.jar' }
if ($loadable.Count -ne 1) {
    throw "Expected exactly one active Temple of Nihility jar, found $($loadable.Count)"
}

Get-FileHash -Algorithm SHA256 -LiteralPath $TargetJar | Format-List
Write-Host "Deployed: $TargetJar"
