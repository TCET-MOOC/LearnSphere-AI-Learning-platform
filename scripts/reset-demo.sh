#!/usr/bin/env bash
#
# Rebuild the local demo stack from scratch.
#
# This deletes BOTH PostgreSQL databases and the uploads volume, then starts
# Compose with the `demo` profile so Flyway migrates and DemoDataSeeder runs.
# The fixed insert order makes the persona ids reproducible across resets.
#
# Usage:
#   scripts/reset-demo.sh
#   ENV_FILE=.env.example scripts/reset-demo.sh
#   RESET_DEMO_SKIP_BUILD=1 scripts/reset-demo.sh
#
set -euo pipefail

repo_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_root"

env_file="${ENV_FILE:-.env}"
if [[ ! -f "$env_file" ]]; then
  cat >&2 <<EOF
Environment file not found: $env_file
Copy .env.example to .env first, or run with ENV_FILE=.env.example for a
disposable local reset.
EOF
  exit 1
fi

profile_in_env="$(sed -n 's/^[[:space:]]*SPRING_PROFILES_ACTIVE[[:space:]]*=[[:space:]]*//p' "$env_file" | tail -n 1 | tr -d '\r')"
case "${profile_in_env,,}" in
  prod | production)
    printf 'Refusing to reset demo data: %s selects %s.\n' "$env_file" "$profile_in_env" >&2
    exit 1
    ;;
esac

if ! command -v docker >/dev/null 2>&1; then
  printf 'Docker is required to reset the demo stack.\n' >&2
  exit 1
fi

compose=(docker compose --env-file "$env_file")

printf 'Removing the demo database and upload volumes...\n'
"${compose[@]}" down -v --remove-orphans

printf 'Starting the stack with the demo profile...\n'
up_args=(up -d --wait --wait-timeout 300)
if [[ "${RESET_DEMO_SKIP_BUILD:-0}" != "1" ]]; then
  up_args+=(--build)
fi
SPRING_PROFILES_ACTIVE=demo "${compose[@]}" "${up_args[@]}"

postgres_user="${POSTGRES_USER:-}"
if [[ -z "$postgres_user" ]]; then
  postgres_user="$(sed -n 's/^[[:space:]]*POSTGRES_USER[[:space:]]*=[[:space:]]*//p' "$env_file" | tail -n 1 | tr -d '\r')"
fi
postgres_user="${postgres_user:-postgres}"

printf '\nDemo personas (password: SEED_PASSWORD from %s):\n' "$env_file"
"${compose[@]}" exec -T db psql -U "$postgres_user" -d MOOC -P pager=off -c "
select u.user_id,
       u.email,
       r.name as role,
       coalesce(c.name, '(no college)') as college
from users u
join user_roles ur on ur.user_id = u.user_id
join roles r on r.role_id = ur.role_id
left join colleges c on c.id = u.college_id
order by u.user_id;
"

printf '\nDemo reset complete.\n'
