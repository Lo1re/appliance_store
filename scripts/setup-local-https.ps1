[CmdletBinding()]
param([switch]$TrustCertificate)
$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$certificateDirectory = Join-Path $projectRoot 'config/certs'
$keyStore = Join-Path $certificateDirectory 'localhost.p12'
$publicCertificate = Join-Path $certificateDirectory 'localhost.cer'
if (Test-Path -LiteralPath $keyStore) { throw 'Keystore already exists. No files were overwritten.' }
New-Item -ItemType Directory -Path $certificateDirectory -Force | Out-Null

$keytool = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/keytool.exe' } else { $null }
if (!$keytool -or !(Test-Path -LiteralPath $keytool)) {
    $command = Get-Command keytool.exe -ErrorAction SilentlyContinue
    if ($command) { $keytool = $command.Source }
    elseif (Test-Path -LiteralPath 'C:\Program Files\Java\jdk-17\bin\keytool.exe') { $keytool = 'C:\Program Files\Java\jdk-17\bin\keytool.exe' }
    else { throw 'Set JAVA_HOME to your JDK installation before running this script.' }
}
$password = Read-Host 'Choose a keystore password (at least 12 characters; use the same value for SSL_KEY_STORE_PASSWORD in IDE)' -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
$temporaryVariable = 'APPLIANCE_TLS_SETUP_' + $PID
try {
    $plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    if ($plainPassword.Length -lt 12) { throw 'Use a password of at least 12 characters.' }
    [Environment]::SetEnvironmentVariable($temporaryVariable, $plainPassword, 'Process')
    & $keytool -genkeypair -alias appliance-store -keyalg RSA -keysize 3072 -sigalg SHA256withRSA -validity 365 -storetype PKCS12 -keystore $keyStore -storepass:env $temporaryVariable -dname 'CN=localhost, OU=Local Development, O=Appliance Store' -ext 'SAN=dns:localhost,ip:127.0.0.1,ip:::1' -ext 'EKU=serverAuth' -ext 'KU=digitalSignature,keyEncipherment'
    if ($LASTEXITCODE -ne 0) { throw 'Key generation failed.' }
    & $keytool -exportcert -alias appliance-store -keystore $keyStore -storepass:env $temporaryVariable -file $publicCertificate
    if ($LASTEXITCODE -ne 0) { throw 'Certificate export failed.' }
    if ($TrustCertificate) {
        Import-Certificate -FilePath $publicCertificate -CertStoreLocation 'Cert:\CurrentUser\Root' | Out-Null
        Write-Host 'The localhost certificate is trusted for this Windows user.'
    }
    Write-Host 'Certificate created. Set SSL_KEY_STORE_PASSWORD in your IDE to the password you entered.'
    Write-Host 'Website: https://localhost:8443'
    Write-Host 'Google authorized redirect URI: https://localhost:8443/login/oauth2/code/google'
} finally {
    [Environment]::SetEnvironmentVariable($temporaryVariable, $null, 'Process')
    $plainPassword = $null
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    $password.Dispose()
}
