param([ValidateSet('check','create','migrate')][string]$Action = 'check', [string]$EnvFile = '.env.remote')
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
. ./scripts/load-env.ps1 -Path $EnvFile
New-Item -ItemType Directory -Force .local | Out-Null
& ./mvnw.cmd -B -ntp compile dependency:build-classpath '-Dmdep.outputFile=.local/classpath.txt'
if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
$classpath = 'target/classes;' + (Get-Content -Raw .local/classpath.txt).Trim()
& "$env:JAVA_HOME/bin/java.exe" -cp $classpath com.tourism.business.business.service.RemoteDatabase $Action
if ($LASTEXITCODE -ne 0) { throw 'Remote database operation failed; review the preceding error.' }
