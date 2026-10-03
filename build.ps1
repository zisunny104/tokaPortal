param(
    [string]$JdkHome = "$env:USERPROFILE/.jdks/jbr-21.0.11",
    [string]$Maven = 'C:/Program Files/JetBrains/IntelliJ IDEA 2026.2.3/plugins/maven-plugin/lib/maven3/bin/mvn.cmd'
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
    New-Item -ItemType Directory -Force .build-tools | Out-Null
    $core = '.build-tools/Nukkit-PM1E.jar'
    if (!(Test-Path $core)) {
        Invoke-WebRequest 'https://github.com/PetteriM1/NukkitPetteriM1Edition/releases/download/4511/Nukkit-PM1E.jar' -OutFile $core
    }
    $expected = '67CEC0084ED161BB558AB2C103DD15B0939A258F39A0FF5CB98E5DE896A86ACD'
    if ((Get-FileHash $core -Algorithm SHA256).Hash -ne $expected) { throw 'PM1E 4511 checksum mismatch.' }
    $common = @('-B', '-ntp', '-Dmaven.repo.local=.build-tools/m2')
    & $Maven @common 'org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file' "-Dfile=$core" '-DgroupId=cn.nukkit' '-DartifactId=Nukkit' '-Dversion=PM1E-4511' '-Dpackaging=jar' '-DgeneratePom=true'
    if ($LASTEXITCODE) { throw 'PM1E dependency installation failed.' }
    & $Maven @common package
    if ($LASTEXITCODE) { throw 'Build failed. See Maven errors above.' }
    New-Item -ItemType Directory -Force output | Out-Null
    Copy-Item -LiteralPath 'target/tokaPortal-2.0.0.jar' -Destination 'output/tokaPortal-2.0.0.jar'
} finally {
    $env:JAVA_HOME = $previousJava
    $env:PATH = $previousPath
    Pop-Location
}
