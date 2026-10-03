$ErrorActionPreference = 'Stop'
Set-Location (Split-Path -Parent $PSScriptRoot)
$env:JAVA_HOME = 'C:/Program Files/Java/jdk-21'
if ($env:TEST_DB_URL -and $env:TEST_DB_URL -notmatch '(?i);databaseName=TourismIntegrationTest(?:;|$)') {
    throw 'Integration tests require a separate TourismIntegrationTest database, never TourismDemo.'
}
# Never load the demo credentials as test credentials. SQL tests skip unless explicitly configured.
& ./mvnw.cmd -B -ntp verify
if ($LASTEXITCODE -ne 0) { throw 'Verification failed' }
