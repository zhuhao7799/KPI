#!/usr/bin/env bash
set -euo pipefail
script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
upstream="$script_dir/ruoyi-vue-plus"
patch_file="$script_dir/ruoyi-integration.patch"
if git -C "$upstream" apply --reverse --check "$patch_file"; then
  echo 'KPI RuoYi integration patch is already applied'
elif git -C "$upstream" apply --check "$patch_file"; then
  git -C "$upstream" apply "$patch_file"
  echo 'KPI RuoYi integration patch applied'
else
  echo 'RuoYi checkout differs from the pinned version; inspect the patch before building' >&2
  exit 1
fi
