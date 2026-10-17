param(
    [string]$JdkHome = "$env:USERPROFILE/.jdks/jbr-21.0.11",
    [string]$Maven = 'C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/plugins/maven-plugin/lib/maven3/bin/mvn.cmd',
    [string]$Repository = (Join-Path (Split-Path $PSScriptRoot -Parent) 'plugin-modernization/build/m2')
)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
$previousJava = $env:JAVA_HOME
$previousPath = $env:PATH
try {
    $java = Join-Path $JdkHome 'bin/java.exe'
    if (!(Test-Path $java)) { throw "JDK missing: $JdkHome" }
    $version = & $java --version | Out-String
    if ($version -notmatch '(?:openjdk|java) 21\.') { throw 'This build requires JDK 21.' }
    $env:JAVA_HOME = $JdkHome
    $env:PATH = "$JdkHome/bin;$env:PATH"
    if (!(Test-Path $Maven)) { throw "Maven missing: $Maven" }
    $core = Join-Path (Split-Path $PSScriptRoot -Parent) 'Nukkit/target/nukkit-1.0-SNAPSHOT.jar'
    if (!(Test-Path $core)) { throw "Nukkit jar missing, build it first: $core" }
    Write-Host ("Nukkit core: {0} {1}" -f $core, (Get-FileHash $core -Algorithm SHA256).Hash)
    $common = @('-B', '-ntp', "-Dmaven.repo.local=$Repository")
    & $Maven @common 'org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file' "-Dfile=$core" '-DgroupId=cn.nukkit' '-DartifactId=Nukkit' '-Dversion=1.0-SNAPSHOT' '-Dpackaging=jar' '-DgeneratePom=true'
    if ($LASTEXITCODE) { throw 'Nukkit dependency installation failed.' }
    & $Maven @common package
    if ($LASTEXITCODE) { throw 'Build failed. See Maven errors above.' }
    New-Item -ItemType Directory -Force output | Out-Null
    Copy-Item -LiteralPath 'target/tokaPortal-2.4.0.jar' -Destination 'output/tokaPortal-2.4.0.jar'
    $sharedOutput = Join-Path (Split-Path $PSScriptRoot -Parent) 'plugin-modernization/output'
    New-Item -ItemType Directory -Force $sharedOutput | Out-Null
    Copy-Item -LiteralPath 'target/tokaPortal-2.4.0.jar' -Destination (Join-Path $sharedOutput 'tokaPortal-2.4.0.jar')
} finally {
    $env:JAVA_HOME = $previousJava
    $env:PATH = $previousPath
    Pop-Location
}

