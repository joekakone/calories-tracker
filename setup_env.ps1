$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$jdkUrl = "https://aka.ms/download-jdk/microsoft-jdk-17.0.12-windows-x64.zip"
$cmdToolsUrl = "https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip"

$jdkZip = "jdk-17.zip"
$cmdToolsZip = "cmdtools.zip"

# Write-Host "Downloading JDK 17..."
# curl.exe -L -o $jdkZip $jdkUrl
# Write-Host "Extracting JDK 17..."
# Expand-Archive -Path $jdkZip -DestinationPath "jdk-17" -Force
# # Remove-Item -Path $jdkZip

# Write-Host "Downloading Android Command Line Tools..."
# curl.exe -L -o $cmdToolsZip $cmdToolsUrl
# Write-Host "Extracting Android Command Line Tools..."
# Expand-Archive -Path $cmdToolsZip -DestinationPath "android-sdk\cmdline-tools" -Force
# # Remove-Item -Path $cmdToolsZip

# # Rearrange cmdline-tools to be in latest/
# Rename-Item -Path "android-sdk\cmdline-tools\cmdline-tools" -NewName "latest"

Write-Host "Environment setup downloaded."
