# One-time setup: uploads the release-signing secrets to GitHub so the
# Release workflow (.github/workflows/release.yml) can sign builds.
# Prerequisites: `gh auth login` done once; keystore.properties + sinq-upload.jks
# present at the repo root (never committed).
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

$props = @{}
Get-Content (Join-Path $root "keystore.properties") | ForEach-Object {
    $k, $v = $_ -split "=", 2
    $props[$k] = $v
}

$keystoreB64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Join-Path $root $props.storeFile)))

$keystoreB64        | gh secret set KEYSTORE_BASE64
$props.storePassword | gh secret set KEYSTORE_PASSWORD
$props.keyAlias      | gh secret set KEY_ALIAS
$props.keyPassword   | gh secret set KEY_PASSWORD

Write-Host "All four signing secrets uploaded. Cut a release with:"
Write-Host "  git tag v<version> && git push origin v<version>"
