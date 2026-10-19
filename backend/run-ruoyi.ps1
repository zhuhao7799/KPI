$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$ruoyi = Join-Path $PSScriptRoot 'ruoyi-vue-plus'
$jdk = if ($env:KPI_JDK_HOME) { $env:KPI_JDK_HOME } else { Split-Path (Split-Path (Get-Command javac).Source) }
$mvn = if ($env:KPI_MAVEN_HOME) { Join-Path $env:KPI_MAVEN_HOME 'bin/mvn.cmd' } else { (Get-Command mvn).Source }
if (-not (Test-Path $mvn)) { throw '找不到 Maven 3.9+，请设置 KPI_MAVEN_HOME。' }
if (-not (Test-Path (Join-Path $jdk 'bin/java.exe'))) { throw '找不到 JDK 21，请设置 KPI_JDK_HOME。' }
& bash (Join-Path $PSScriptRoot 'apply-ruoyi-integration.sh')
Set-Location $ruoyi
$env:JAVA_HOME = $jdk
& $mvn '-pl' 'ruoyi-admin' '-am' 'spring-boot:run' '-Dspring-boot.run.arguments=--spring.config.additional-location=optional:file:../../deploy/backend-config/'
