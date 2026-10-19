#!/usr/bin/env bash
set -euo pipefail

if [ "$#" -ne 1 ] || [[ ! "$1" =~ ^[A-Za-z0-9_.-]+$ ]]; then
  echo 'Usage: bash deploy/bootstrap-kpi-admin.sh <existing-ruoyi-username>' >&2
  exit 2
fi

username="$1"
container="${KPI_MYSQL_CONTAINER:-kpi-mysql}"
sql="insert into kpi_user_role_binding (id,user_id,role_code,starts_at,ends_at)
select uuid(),cast(u.user_id as char),'SCHOOL_ADMIN',utc_timestamp(6),null
from sys_user u
where u.user_name='${username}' and u.status='0' and u.del_flag='0'
and not exists (
  select 1 from kpi_user_role_binding b
  where b.user_id=cast(u.user_id as char) and b.role_code='SCHOOL_ADMIN'
    and b.starts_at<=utc_timestamp(6)
    and (b.ends_at is null or b.ends_at>=utc_timestamp(6))
);
select u.user_id,u.user_name,b.role_code
from sys_user u join kpi_user_role_binding b on b.user_id=cast(u.user_id as char)
where u.user_name='${username}' and b.role_code='SCHOOL_ADMIN'
  and b.starts_at<=utc_timestamp(6)
  and (b.ends_at is null or b.ends_at>=utc_timestamp(6));"

docker exec -i "$container" sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D "$MYSQL_DATABASE"' <<< "$sql"
