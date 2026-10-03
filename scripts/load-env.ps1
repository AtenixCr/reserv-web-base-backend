param([string]$Path = '.env.remote')
$ErrorActionPreference = 'Stop'
foreach ($line in Get-Content -LiteralPath $Path) {
    if ($line -match '^([A-Z_]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
