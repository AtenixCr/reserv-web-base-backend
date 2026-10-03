param([string]$EnvFile = '.env.remote', [switch]$LocalHttp)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
. ./scripts/load-env.ps1 -Path $EnvFile
if ($LocalHttp) {
    $env:SERVER_ADDRESS = '127.0.0.1'
    $env:SERVER_SERVLET_SESSION_COOKIE_SECURE = 'false'
} else {
    $env:SERVER_SERVLET_SESSION_COOKIE_SECURE = 'true'
}
& ./mvnw.cmd -B -ntp spring-boot:run
if ($LASTEXITCODE -ne 0) { throw 'Application startup failed' }
