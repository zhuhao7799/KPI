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
& $Mysql -u $RootUser -e "CREATE DATABASE IF NOT EXISTS ${Database} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci; CREATE USER IF NOT EXISTS '$AppUser'@'localhost' IDENTIFIED BY '$AppPassword'; GRANT ALL PRIVILEGES ON ${Database}.* TO '$AppUser'@'localhost'; FLUSH PRIVILEGES;"
& $Mysql -u $RootUser $Database -e "SET SESSION sql_mode=''; SOURCE $repo\backend\ruoyi-vue-plus\script\sql\ry_vue.sql;"
Get-ChildItem (Join-Path $repo 'backend/kpi-storage/migrations') -Filter 'V*.sql' | Sort-Object Name | ForEach-Object {
  & $Mysql -u $RootUser $Database -e "SOURCE $($_.FullName);"
}
Remove-Item Env:MYSQL_PWD
Write-Host "MySQL 初始化完成：$Database（RuoYi 基础表 + KPI V1-V7）"
