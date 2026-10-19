$ErrorActionPreference = 'Stop'

$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$jdk = if ($env:KPI_JDK_HOME) { $env:KPI_JDK_HOME } else { '' }
if (-not $jdk) {
  $java = Get-Command javac -ErrorAction SilentlyContinue
  if (-not $java) { throw '需要安装 JDK 21，并将 javac 加入 PATH，或设置 KPI_JDK_HOME。' }
  $jdk = Split-Path (Split-Path $java.Source)
}
$javac = Join-Path $jdk 'bin/javac.exe'
$javaExe = Join-Path $jdk 'bin/java.exe'
if (-not (Test-Path $javac)) { throw "找不到 JDK 21: $javac" }

$build = Join-Path $env:TEMP 'kpi-demo-classes'
$data = Join-Path $env:TEMP 'kpi-module-preview-data'
Remove-Item $build -Recurse -Force -ErrorAction SilentlyContinue
New-Item $build -ItemType Directory -Force | Out-Null
$sources = @(
  Get-ChildItem (Join-Path $repo 'backend/kpi-domain/src/main/java') -Filter '*.java' -Recurse
  Get-ChildItem (Join-Path $repo 'backend/kpi-app/src/main/java') -Filter '*.java' -Recurse
)
& $javac --release 21 --add-modules jdk.httpserver -d $build ($sources.FullName)
Copy-Item (Join-Path $repo 'backend/kpi-app/src/main/resources/*') $build -Recurse -Force

$api = Start-Process -FilePath $javaExe -ArgumentList '--add-modules','jdk.httpserver',('-Dkpi.data.dir=' + $data),'-cp',$build,'cn.edu.kpi.app.KpiDemoServer' -PassThru
try {
  for ($i = 0; $i -lt 30; $i++) {
    try { Invoke-WebRequest 'http://127.0.0.1:18080/api/health' -UseBasicParsing | Out-Null; break } catch { Start-Sleep -Milliseconds 300 }
  }
  Set-Location (Join-Path $repo 'frontend')
  $env:VITE_APP_API_TARGET = 'http://127.0.0.1:18080'
  $env:VITE_APP_PORT = if ($env:VITE_APP_PORT) { $env:VITE_APP_PORT } else { '5173' }
  $env:VITE_KPI_DEMO_PREVIEW = 'true'
  corepack pnpm@10.34.5 dev --host 0.0.0.0
}
finally {
  if ($api -and -not $api.HasExited) { Stop-Process $api.Id -Force }
}
