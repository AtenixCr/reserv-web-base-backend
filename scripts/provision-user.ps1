param([string]$EnvFile = '.env.remote')
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
. ./scripts/load-env.ps1 -Path $EnvFile
New-Item -ItemType Directory -Force .local | Out-Null
& ./mvnw.cmd -B -ntp compile dependency:build-classpath '-Dmdep.outputFile=.local/classpath.txt'
if ($LASTEXITCODE -ne 0) { throw 'User tool compilation failed' }
$classpath = 'target/classes;' + (Get-Content -Raw .local/classpath.txt).Trim()
& "$env:JAVA_HOME/bin/java.exe" -cp $classpath com.tourism.business.auth.service.ProvisionUser
if ($LASTEXITCODE -ne 0) { throw 'User provisioning failed' }
