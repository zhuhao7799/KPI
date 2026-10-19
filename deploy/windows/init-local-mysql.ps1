param(
  [string]$Mysql = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe',
  [string]$RootUser = 'root',
  [string]$RootPassword = 'change-me-root',
  [string]$Database = 'kpi',
  [string]$AppUser = 'kpi_app',
  [string]$AppPassword = 'change-me-local'
)
$ErrorActionPreference = 'Stop'
if (-not (Test-Path $Mysql)) { throw "找不到 mysql.exe：$Mysql。请修改 -Mysql 参数。" }
$repo = (Resolve-Path (Join-Path $PSScriptRoot '../..')).Path
$env:MYSQL_PWD = $RootPassword
$sql = 'CREATE DATABASE IF NOT EXISTS {0} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS ''{1}''@''localhost'' IDENTIFIED BY ''{2}''; GRANT ALL PRIVILEGES ON {0}.* TO ''{1}''@''localhost''; FLUSH PRIVILEGES;' -f $Database, $AppUser, $AppPassword
& $Mysql -u $RootUser -e $sql
$baseline = Join-Path $repo 'backend/ruoyi-vue-plus/script/sql/ry_vue.sql'
& $Mysql -u $RootUser $Database -e ('SET SESSION sql_mode='''' ; SOURCE "{0}";' -f $baseline)
Get-ChildItem (Join-Path $repo 'backend/kpi-storage/migrations') -Filter 'V*.sql' | Sort-Object Name | ForEach-Object {
  & $Mysql -u $RootUser $Database -e ('SOURCE "{0}";' -f $_.FullName)
}
Remove-Item Env:MYSQL_PWD
Write-Host "MySQL 初始化完成：$Database（RuoYi 基础表 + KPI V1-V7）"
