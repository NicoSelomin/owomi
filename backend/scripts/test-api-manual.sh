#!/usr/bin/env bash
set -Eeuo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TEST_PASSWORD="${TEST_PASSWORD:-}"

TMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TMP_DIR"' EXIT

PASSED_TESTS=()
FAILED_TESTS=()
ENV_ERRORS=()
SKIPPED_TESTS=()

timestamp="$(date +%Y%m%d%H%M%S)"
EMAIL_A="owomi.manual.${timestamp}.a@example.test"
EMAIL_B="owomi.manual.${timestamp}.b@example.test"
TODAY="$(date +%F)"
MONTH_START="$(date +%Y-%m-01)"
MONTH_END="$(date -d "${MONTH_START} +1 month -1 day" +%F)"

ACCESS_TOKEN_A="${ACCESS_TOKEN_A:-}"
ACCESS_TOKEN_B="${ACCESS_TOKEN_B:-}"
REFRESH_TOKEN_A="${REFRESH_TOKEN_A:-}"
REFRESH_TOKEN_B="${REFRESH_TOKEN_B:-}"
CATEGORY_ID_A="${CATEGORY_ID_A:-}"
TRANSACTION_ID_A="${TRANSACTION_ID_A:-}"
CATEGORY_ID_B=""
TRANSACTION_ID_B=""

pass() {
  PASSED_TESTS+=("$1")
  printf '[PASS] %s\n' "$1"
}

fail() {
  FAILED_TESTS+=("$1 :: $2")
  printf '[FAIL] %s :: %s\n' "$1" "$2"
}

env_fail() {
  ENV_ERRORS+=("$1")
  printf '[ENV] %s\n' "$1"
}

skip() {
  SKIPPED_TESTS+=("$1")
  printf '[SKIP] %s\n' "$1"
}

sanitize_response() {
  local file="$1"
  if jq -e . "$file" >/dev/null 2>&1; then
    jq 'walk(
      if type == "object" then
        with_entries(if (.key | test("(?i)(token|password|secret)")) then .value = "***" else . end)
      else
        .
      end
    )' "$file"
  else
    sed -E 's/([A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,})/***JWT***/g' "$file"
  fi
}

require_tool() {
  if ! command -v "$1" >/dev/null 2>&1; then
    env_fail "Commande requise absente: $1"
  fi
}

request_json() {
  local method="$1"
  local path="$2"
  local bearer="$3"
  local body_file="$4"
  local out_file="$5"
  local content_type="${6:-application/json}"
  local -a args

  args=(-sS -o "$out_file" -w "%{http_code}" -X "$method" -H "Accept: application/json")
  if [[ -n "$bearer" ]]; then
    args+=(-H "Authorization: Bearer ${bearer}")
  fi
  if [[ -n "$body_file" ]]; then
    args+=(-H "Content-Type: ${content_type}" --data-binary "@${body_file}")
  fi
  args+=("${BASE_URL}${path}")
  curl "${args[@]}"
}

request_raw() {
  local method="$1"
  local path="$2"
  local bearer="$3"
  local out_file="$4"
  local headers_file="$5"
  local accept="${6:-text/csv}"
  local -a args

  args=(-sS -D "$headers_file" -o "$out_file" -w "%{http_code}" -X "$method" -H "Accept: ${accept}")
  if [[ -n "$bearer" ]]; then
    args+=(-H "Authorization: Bearer ${bearer}")
  fi
  args+=("${BASE_URL}${path}")
  curl "${args[@]}"
}

assert_status() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  local out_file="$4"

  if [[ "$actual" == "$expected" ]]; then
    return 0
  fi

  fail "$name" "HTTP attendu ${expected}, obtenu ${actual}. Reponse masquee: $(sanitize_response "$out_file" | tr '\n' ' ')"
  return 1
}

assert_success() {
  local name="$1"
  local out_file="$2"

  if jq -e '.success == true and .timestamp != null' "$out_file" >/dev/null; then
    return 0
  fi

  fail "$name" "ApiResponse success invalide: $(sanitize_response "$out_file" | tr '\n' ' ')"
  return 1
}

assert_error_code() {
  local name="$1"
  local out_file="$2"
  local expected_code="$3"

  if jq -e --arg code "$expected_code" '.success == false and .error.code == $code and .timestamp != null' "$out_file" >/dev/null; then
    return 0
  fi

  fail "$name" "Code erreur attendu ${expected_code}. Reponse masquee: $(sanitize_response "$out_file" | tr '\n' ' ')"
  return 1
}

assert_header_contains() {
  local name="$1"
  local headers_file="$2"
  local header_name="$3"
  local expected_value="$4"

  if awk -v header="${header_name}:" -v expected="$expected_value" '
    BEGIN { found = 0 }
    tolower($0) ~ "^" tolower(header) {
      line = $0
      sub(/\r$/, "", line)
      if (index(line, expected) > 0) {
        found = 1
      }
    }
    END { exit(found ? 0 : 1) }
  ' "$headers_file"; then
    return 0
  fi

  fail "$name" "Header ${header_name} ne contient pas ${expected_value}."
  return 1
}

check_success() {
  local name="$1"
  local status="$2"
  local expected_status="$3"
  local out_file="$4"

  if assert_status "$name" "$expected_status" "$status" "$out_file" && assert_success "$name" "$out_file"; then
    pass "$name"
  fi
}

check_error() {
  local name="$1"
  local status="$2"
  local expected_status="$3"
  local out_file="$4"
  local expected_code="$5"

  if assert_status "$name" "$expected_status" "$status" "$out_file" && assert_error_code "$name" "$out_file" "$expected_code"; then
    pass "$name"
  fi
}

write_json() {
  local out_file="$1"
  shift
  jq -n "$@" > "$out_file"
}

create_category_body() {
  local out_file="$1"
  local name="$2"
  local type="$3"
  write_json "$out_file" \
    --arg name "$name" \
    --arg icon "wallet-outline" \
    --arg color "#D49E10" \
    --arg type "$type" \
    '{name:$name,icon:$icon,color:$color,type:$type}'
}

create_transaction_body() {
  local out_file="$1"
  local amount="$2"
  local type="$3"
  local category_id="$4"
  local date="$5"
  local note="$6"
  write_json "$out_file" \
    --argjson amount "$amount" \
    --arg type "$type" \
    --argjson categoryId "$category_id" \
    --arg date "$date" \
    --arg note "$note" \
    '{amount:$amount,type:$type,categoryId:$categoryId,date:$date,note:$note}'
}

register_user() {
  local label="$1"
  local email="$2"
  local name="$3"
  local body="$TMP_DIR/register_${label}.json"
  local out="$TMP_DIR/register_${label}_out.json"
  local status

  write_json "$body" \
    --arg name "$name" \
    --arg email "$email" \
    --arg password "$TEST_PASSWORD" \
    '{name:$name,email:$email,password:$password,countryCode:"BJ",currencyCode:"XOF"}'
  status="$(request_json POST "/api/auth/register" "" "$body" "$out")"
  check_success "auth register utilisateur ${label}" "$status" "201" "$out"

  if [[ "$label" == "a" ]]; then
    ACCESS_TOKEN_A="${ACCESS_TOKEN_A:-$(jq -r '.data.accessToken // empty' "$out")}"
    REFRESH_TOKEN_A="${REFRESH_TOKEN_A:-$(jq -r '.data.refreshToken // empty' "$out")}"
  else
    ACCESS_TOKEN_B="${ACCESS_TOKEN_B:-$(jq -r '.data.accessToken // empty' "$out")}"
    REFRESH_TOKEN_B="${REFRESH_TOKEN_B:-$(jq -r '.data.refreshToken // empty' "$out")}"
  fi
}

require_tool curl
require_tool jq

if [[ -z "$TEST_PASSWORD" ]]; then
  env_fail "TEST_PASSWORD est obligatoire et ne doit pas etre affiche."
fi

if (( ${#ENV_ERRORS[@]} > 0 )); then
  printf '\nErreurs environnementales avant execution:\n'
  printf ' - %s\n' "${ENV_ERRORS[@]}"
  exit 2
fi

health_out="$TMP_DIR/health.json"
if ! health_status="$(request_json GET "/api/health" "" "" "$health_out")"; then
  env_fail "Backend injoignable sur ${BASE_URL}"
elif [[ "$health_status" != "200" ]]; then
  currencies_probe="$TMP_DIR/currencies_probe.json"
  if ! currencies_status="$(request_json GET "/api/currencies" "" "" "$currencies_probe")" || [[ "$currencies_status" != "200" ]]; then
    env_fail "Backend non pret. /api/health=${health_status}, /api/currencies=${currencies_status:-n/a}"
  fi
fi

if (( ${#ENV_ERRORS[@]} > 0 )); then
  printf '\nErreurs environnementales:\n'
  printf ' - %s\n' "${ENV_ERRORS[@]}"
  exit 2
fi

printf 'Base API: %s\n' "$BASE_URL"
printf 'Emails de test: %s, %s\n' "$EMAIL_A" "$EMAIL_B"
printf 'Les mots de passe, JWT et refresh tokens sont masques.\n\n'

out="$TMP_DIR/currencies.json"
status="$(request_json GET "/api/currencies" "" "" "$out")"
check_success "currencies public sans authentification" "$status" "200" "$out"

register_user "a" "$EMAIL_A" " Alice Manual "
register_user "b" "$EMAIL_B" "Bob Manual"

if [[ -z "$ACCESS_TOKEN_A" || -z "$ACCESS_TOKEN_B" || -z "$REFRESH_TOKEN_A" || -z "$REFRESH_TOKEN_B" ]]; then
  env_fail "La suite protegee exige des tokens. Si register ne renvoie plus de tokens, utiliser le flux de verification email/dev officiel puis fournir ACCESS_TOKEN_A/B et REFRESH_TOKEN_A/B."
  printf '\nErreurs environnementales:\n'
  printf ' - %s\n' "${ENV_ERRORS[@]}"
  exit 2
fi

body="$TMP_DIR/register_invalid.json"
write_json "$body" '{name:"A",email:"invalid-email",password:"weak",countryCode:"BEN",currencyCode:"BAD"}'
out="$TMP_DIR/register_invalid_out.json"
status="$(request_json POST "/api/auth/register" "" "$body" "$out")"
check_error "auth register validations champs" "$status" "400" "$out" "VALIDATION_ERROR"

body="$TMP_DIR/register_weak.json"
write_json "$body" --arg email "owomi.manual.${timestamp}.weak@example.test" '{name:"Weak User",email:$email,password:"weakpass",countryCode:"BJ",currencyCode:"XOF"}'
out="$TMP_DIR/register_weak_out.json"
status="$(request_json POST "/api/auth/register" "" "$body" "$out")"
check_error "auth register mot de passe faible" "$status" "400" "$out" "VALIDATION_ERROR"

body="$TMP_DIR/login_a.json"
write_json "$body" --arg email "$EMAIL_A" --arg password "$TEST_PASSWORD" '{email:$email,password:$password}'
out="$TMP_DIR/login_a_out.json"
status="$(request_json POST "/api/auth/login" "" "$body" "$out")"
check_error "auth login avant verification email" "$status" "403" "$out" "EMAIL_NOT_VERIFIED"

body="$TMP_DIR/resend.json"
write_json "$body" --arg email "$EMAIL_A" '{email:$email}'
out="$TMP_DIR/resend_out.json"
status="$(request_json POST "/api/auth/resend-verification" "" "$body" "$out")"
check_success "auth resend verification" "$status" "200" "$out"

body="$TMP_DIR/forgot_known.json"
write_json "$body" --arg email "$EMAIL_A" '{email:$email}'
out="$TMP_DIR/forgot_known_out.json"
status="$(request_json POST "/api/auth/forgot-password" "" "$body" "$out")"
check_success "auth forgot password email connu" "$status" "200" "$out"

body="$TMP_DIR/forgot_unknown.json"
write_json "$body" --arg email "unknown.${timestamp}@example.test" '{email:$email}'
out="$TMP_DIR/forgot_unknown_out.json"
status="$(request_json POST "/api/auth/forgot-password" "" "$body" "$out")"
check_success "auth forgot password email inconnu" "$status" "200" "$out"

out="$TMP_DIR/verify_invalid_out.json"
status="$(request_json GET "/api/auth/verify-email?token=invalid-token" "" "" "$out")"
check_error "auth verify-email token invalide" "$status" "400" "$out" "VERIFICATION_TOKEN_INVALID"

body="$TMP_DIR/reset_invalid.json"
write_json "$body" --arg password "$TEST_PASSWORD" '{token:"invalid-token",newPassword:$password}'
out="$TMP_DIR/reset_invalid_out.json"
status="$(request_json POST "/api/auth/reset-password" "" "$body" "$out")"
check_error "auth reset-password token invalide" "$status" "400" "$out" "RESET_TOKEN_INVALID"

body="$TMP_DIR/refresh_invalid.json"
write_json "$body" '{refreshToken:"invalid-refresh-token"}'
out="$TMP_DIR/refresh_invalid_out.json"
status="$(request_json POST "/api/auth/refresh" "" "$body" "$out")"
check_error "auth refresh token invalide" "$status" "401" "$out" "TOKEN_INVALID"

out="$TMP_DIR/protected_no_token.json"
status="$(request_json GET "/api/users/me" "" "" "$out")"
check_error "endpoint protege sans token" "$status" "401" "$out" "TOKEN_INVALID"

out="$TMP_DIR/protected_bad_token.json"
status="$(request_json GET "/api/users/me" "invalid.jwt.token" "" "$out")"
check_error "endpoint protege JWT invalide" "$status" "401" "$out" "TOKEN_INVALID"

out="$TMP_DIR/me_a.json"
status="$(request_json GET "/api/users/me" "$ACCESS_TOKEN_A" "" "$out")"
check_success "users me protege" "$status" "200" "$out"
if jq -e 'any(.. | objects; has("password") or has("passwordHash") or has("accessToken") or has("refreshToken"))' "$out" >/dev/null; then
  fail "users me absence donnees sensibles" "Champ sensible expose dans la reponse."
else
  pass "users me absence donnees sensibles"
fi

out="$TMP_DIR/categories_a.json"
status="$(request_json GET "/api/categories" "$ACCESS_TOKEN_A" "" "$out")"
check_success "categories liste" "$status" "200" "$out"
DEFAULT_EXPENSE_CATEGORY_ID="$(jq -r '.data[] | select(.isDefault == true and .type == "EXPENSE") | .id' "$out" | head -n 1)"
DEFAULT_INCOME_CATEGORY_ID="$(jq -r '.data[] | select(.isDefault == true and .type == "INCOME") | .id' "$out" | head -n 1)"
if [[ -z "$DEFAULT_EXPENSE_CATEGORY_ID" || -z "$DEFAULT_INCOME_CATEGORY_ID" ]]; then
  fail "categories seeds par defaut" "Categories EXPENSE/INCOME par defaut introuvables."
else
  pass "categories seeds par defaut"
fi

body="$TMP_DIR/category_a.json"
create_category_body "$body" "Manual A ${timestamp}" "EXPENSE"
out="$TMP_DIR/category_a_out.json"
status="$(request_json POST "/api/categories" "$ACCESS_TOKEN_A" "$body" "$out")"
check_success "categories creation A" "$status" "201" "$out"
CATEGORY_ID_A="${CATEGORY_ID_A:-$(jq -r '.data.id // empty' "$out")}"

body="$TMP_DIR/category_b.json"
create_category_body "$body" "Manual B ${timestamp}" "EXPENSE"
out="$TMP_DIR/category_b_out.json"
status="$(request_json POST "/api/categories" "$ACCESS_TOKEN_B" "$body" "$out")"
check_success "categories creation B" "$status" "201" "$out"
CATEGORY_ID_B="$(jq -r '.data.id // empty' "$out")"

body="$TMP_DIR/category_a_update.json"
create_category_body "$body" "Manual A Updated ${timestamp}" "EXPENSE"
out="$TMP_DIR/category_a_update_out.json"
status="$(request_json PUT "/api/categories/${CATEGORY_ID_A}" "$ACCESS_TOKEN_A" "$body" "$out")"
check_success "categories modification A" "$status" "200" "$out"

out="$TMP_DIR/category_b_get_a.json"
status="$(request_json GET "/api/categories/${CATEGORY_ID_A}" "$ACCESS_TOKEN_B" "" "$out")"
check_error "anti-IDOR categorie lecture B vers A" "$status" "404" "$out" "RESOURCE_NOT_FOUND"

out="$TMP_DIR/category_b_update_a.json"
status="$(request_json PUT "/api/categories/${CATEGORY_ID_A}" "$ACCESS_TOKEN_B" "$body" "$out")"
check_error "anti-IDOR categorie modification B vers A" "$status" "404" "$out" "RESOURCE_NOT_FOUND"

out="$TMP_DIR/category_b_delete_a.json"
status="$(request_json DELETE "/api/categories/${CATEGORY_ID_A}" "$ACCESS_TOKEN_B" "" "$out")"
check_error "anti-IDOR categorie suppression B vers A" "$status" "404" "$out" "RESOURCE_NOT_FOUND"

body="$TMP_DIR/category_default_update.json"
create_category_body "$body" "Default Updated ${timestamp}" "EXPENSE"
out="$TMP_DIR/category_default_update_out.json"
status="$(request_json PUT "/api/categories/${DEFAULT_EXPENSE_CATEGORY_ID}" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "categories modification defaut refusee" "$status" "409" "$out" "CATEGORY_IS_DEFAULT"

out="$TMP_DIR/category_default_delete_out.json"
status="$(request_json DELETE "/api/categories/${DEFAULT_EXPENSE_CATEGORY_ID}" "$ACCESS_TOKEN_A" "" "$out")"
check_error "categories suppression defaut refusee" "$status" "409" "$out" "CATEGORY_IS_DEFAULT"

body="$TMP_DIR/category_mass_assignment.json"
write_json "$body" --arg name "Manual Mass ${timestamp}" '{name:$name,icon:"wallet-outline",color:"#D49E10",type:"EXPENSE",userId:"00000000-0000-0000-0000-000000000000",user:{id:"00000000-0000-0000-0000-000000000000"}}'
out="$TMP_DIR/category_mass_assignment_out.json"
status="$(request_json POST "/api/categories" "$ACCESS_TOKEN_A" "$body" "$out")"
check_success "OWASP mass assignment categorie ignore" "$status" "201" "$out"
MASS_CATEGORY_ID="$(jq -r '.data.id // empty' "$out")"
if [[ -n "$MASS_CATEGORY_ID" ]]; then
  cleanup_out="$TMP_DIR/category_mass_delete.json"
  request_json DELETE "/api/categories/${MASS_CATEGORY_ID}" "$ACCESS_TOKEN_A" "" "$cleanup_out" >/dev/null || true
fi

body="$TMP_DIR/transaction_a.json"
create_transaction_body "$body" "150.25" "EXPENSE" "$CATEGORY_ID_A" "$TODAY" "Manual expense <b>html-like</b>"
out="$TMP_DIR/transaction_a_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_success "transactions creation A valide" "$status" "201" "$out"
TRANSACTION_ID_A="${TRANSACTION_ID_A:-$(jq -r '.data.id // empty' "$out")}"

body="$TMP_DIR/transaction_b.json"
create_transaction_body "$body" "9999.99" "EXPENSE" "$CATEGORY_ID_B" "$TODAY" "Isolation B"
out="$TMP_DIR/transaction_b_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_B" "$body" "$out")"
check_success "transactions creation B isolation" "$status" "201" "$out"
TRANSACTION_ID_B="$(jq -r '.data.id // empty' "$out")"

out="$TMP_DIR/transactions_list.json"
status="$(request_json GET "/api/transactions" "$ACCESS_TOKEN_A" "" "$out")"
check_success "transactions liste paginee" "$status" "200" "$out"

out="$TMP_DIR/transactions_filters.json"
status="$(request_json GET "/api/transactions?type=EXPENSE&categoryId=${CATEGORY_ID_A}&startDate=${MONTH_START}&endDate=${MONTH_END}&page=0&size=20" "$ACCESS_TOKEN_A" "" "$out")"
check_success "transactions filtres valides" "$status" "200" "$out"

csv_out="$TMP_DIR/export_no_token.csv"
csv_headers="$TMP_DIR/export_no_token_headers.txt"
status="$(request_raw GET "/api/transactions/export/csv" "" "$csv_out" "$csv_headers" "application/json")"
check_error "export CSV sans token" "$status" "401" "$csv_out" "TOKEN_INVALID"

csv_out="$TMP_DIR/export_bad_token.csv"
csv_headers="$TMP_DIR/export_bad_token_headers.txt"
status="$(request_raw GET "/api/transactions/export/csv" "invalid.jwt.token" "$csv_out" "$csv_headers" "application/json")"
check_error "export CSV token invalide" "$status" "401" "$csv_out" "TOKEN_INVALID"

csv_out="$TMP_DIR/export_a.csv"
csv_headers="$TMP_DIR/export_a_headers.txt"
status="$(request_raw GET "/api/transactions/export/csv" "$ACCESS_TOKEN_A" "$csv_out" "$csv_headers")"
if assert_status "export CSV utilisateur A" "200" "$status" "$csv_out" \
  && assert_header_contains "export CSV Content-Type" "$csv_headers" "Content-Type" "text/csv" \
  && assert_header_contains "export CSV Content-Disposition" "$csv_headers" "Content-Disposition" "attachment;" \
  && grep -Fq "Manual expense <b>html-like</b>" "$csv_out" \
  && ! grep -Fq "Isolation B" "$csv_out"; then
  pass "export CSV utilisateur A"
  pass "export CSV Content-Type"
  pass "export CSV Content-Disposition"
  pass "export CSV isolation utilisateur"
else
  fail "export CSV utilisateur A" "CSV invalide ou isolation utilisateur non respectee."
fi

csv_out="$TMP_DIR/export_period.csv"
csv_headers="$TMP_DIR/export_period_headers.txt"
status="$(request_raw GET "/api/transactions/export/csv?startDate=${MONTH_START}&endDate=${MONTH_END}&type=EXPENSE&categoryId=${CATEGORY_ID_A}" "$ACCESS_TOKEN_A" "$csv_out" "$csv_headers")"
if assert_status "export CSV periode valide" "200" "$status" "$csv_out" && grep -Fq "Manual expense <b>html-like</b>" "$csv_out"; then
  pass "export CSV periode valide"
fi

csv_out="$TMP_DIR/export_inverted.csv"
csv_headers="$TMP_DIR/export_inverted_headers.txt"
status="$(request_raw GET "/api/transactions/export/csv?startDate=${MONTH_END}&endDate=${MONTH_START}" "$ACCESS_TOKEN_A" "$csv_out" "$csv_headers" "application/json")"
check_error "export CSV periode invalide" "$status" "400" "$csv_out" "VALIDATION_ERROR"

body="$TMP_DIR/transaction_csv_injection.json"
create_transaction_body "$body" "13.00" "EXPENSE" "$CATEGORY_ID_A" "$TODAY" '=HYPERLINK("http://example.test")'
out="$TMP_DIR/transaction_csv_injection_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_success "export CSV creation note injection" "$status" "201" "$out"
CSV_INJECTION_TRANSACTION_ID="$(jq -r '.data.id // empty' "$out")"

csv_out="$TMP_DIR/export_injection.csv"
csv_headers="$TMP_DIR/export_injection_headers.txt"
status="$(request_raw GET "/api/transactions/export/csv?startDate=${MONTH_START}&endDate=${MONTH_END}&categoryId=${CATEGORY_ID_A}" "$ACCESS_TOKEN_A" "$csv_out" "$csv_headers")"
if assert_status "export CSV protection injection" "200" "$status" "$csv_out" && grep -Fq "'=HYPERLINK" "$csv_out"; then
  pass "export CSV protection injection"
fi
if [[ -n "$CSV_INJECTION_TRANSACTION_ID" ]]; then
  cleanup_out="$TMP_DIR/transaction_csv_injection_delete.json"
  request_json DELETE "/api/transactions/${CSV_INJECTION_TRANSACTION_ID}" "$ACCESS_TOKEN_A" "" "$cleanup_out" >/dev/null || true
fi

skip "export CSV depassement limite non teste manuellement sans surcharge volontaire"

out="$TMP_DIR/transaction_detail.json"
status="$(request_json GET "/api/transactions/${TRANSACTION_ID_A}" "$ACCESS_TOKEN_A" "" "$out")"
check_success "transactions detail A" "$status" "200" "$out"

body="$TMP_DIR/transaction_a_update.json"
create_transaction_body "$body" "42.00" "EXPENSE" "$CATEGORY_ID_A" "$TODAY" "Updated note"
out="$TMP_DIR/transaction_a_update_out.json"
status="$(request_json PUT "/api/transactions/${TRANSACTION_ID_A}" "$ACCESS_TOKEN_A" "$body" "$out")"
check_success "transactions modification A" "$status" "200" "$out"

body="$TMP_DIR/transaction_zero.json"
create_transaction_body "$body" "0" "EXPENSE" "$CATEGORY_ID_A" "$TODAY" "zero"
out="$TMP_DIR/transaction_zero_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "transactions montant nul" "$status" "400" "$out" "VALIDATION_ERROR"

body="$TMP_DIR/transaction_negative.json"
create_transaction_body "$body" "-1" "EXPENSE" "$CATEGORY_ID_A" "$TODAY" "negative"
out="$TMP_DIR/transaction_negative_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "transactions montant negatif" "$status" "400" "$out" "VALIDATION_ERROR"

body="$TMP_DIR/transaction_decimals.json"
create_transaction_body "$body" "12.345" "EXPENSE" "$CATEGORY_ID_A" "$TODAY" "decimals"
out="$TMP_DIR/transaction_decimals_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "transactions trop de decimales" "$status" "400" "$out" "VALIDATION_ERROR"

FUTURE_DATE="$(date -d "${TODAY} +1 day" +%F)"
body="$TMP_DIR/transaction_future.json"
create_transaction_body "$body" "12.00" "EXPENSE" "$CATEGORY_ID_A" "$FUTURE_DATE" "future"
out="$TMP_DIR/transaction_future_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "transactions date future" "$status" "400" "$out" "FUTURE_DATE"

body="$TMP_DIR/transaction_mismatch.json"
create_transaction_body "$body" "12.00" "EXPENSE" "$DEFAULT_INCOME_CATEGORY_ID" "$TODAY" "mismatch"
out="$TMP_DIR/transaction_mismatch_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "transactions type incompatible categorie" "$status" "400" "$out" "VALIDATION_ERROR"

body="$TMP_DIR/transaction_category_missing.json"
create_transaction_body "$body" "12.00" "EXPENSE" "999999999" "$TODAY" "missing"
out="$TMP_DIR/transaction_category_missing_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "transactions categorie inexistante" "$status" "404" "$out" "RESOURCE_NOT_FOUND"

body="$TMP_DIR/transaction_category_b.json"
create_transaction_body "$body" "12.00" "EXPENSE" "$CATEGORY_ID_B" "$TODAY" "category B"
out="$TMP_DIR/transaction_category_b_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "transactions categorie appartenant a B" "$status" "404" "$out" "RESOURCE_NOT_FOUND"

out="$TMP_DIR/transactions_page_negative.json"
status="$(request_json GET "/api/transactions?page=-1&size=20" "$ACCESS_TOKEN_A" "" "$out")"
check_error "transactions page negative" "$status" "400" "$out" "VALIDATION_ERROR"

out="$TMP_DIR/transactions_size_zero.json"
status="$(request_json GET "/api/transactions?page=0&size=0" "$ACCESS_TOKEN_A" "" "$out")"
check_error "transactions size zero" "$status" "400" "$out" "VALIDATION_ERROR"

out="$TMP_DIR/transactions_size_101.json"
status="$(request_json GET "/api/transactions?page=0&size=101" "$ACCESS_TOKEN_A" "" "$out")"
check_error "transactions size 101" "$status" "400" "$out" "VALIDATION_ERROR"

out="$TMP_DIR/transactions_bad_enum.json"
status="$(request_json GET "/api/transactions?type=BAD" "$ACCESS_TOKEN_A" "" "$out")"
check_error "transactions enum type invalide" "$status" "400" "$out" "VALIDATION_ERROR"

out="$TMP_DIR/transactions_period_inverted.json"
status="$(request_json GET "/api/transactions?startDate=${MONTH_END}&endDate=${MONTH_START}" "$ACCESS_TOKEN_A" "" "$out")"
check_error "transactions periode inversee" "$status" "400" "$out" "VALIDATION_ERROR"

out="$TMP_DIR/transaction_b_get_a.json"
status="$(request_json GET "/api/transactions/${TRANSACTION_ID_A}" "$ACCESS_TOKEN_B" "" "$out")"
check_error "anti-IDOR transaction lecture B vers A" "$status" "404" "$out" "RESOURCE_NOT_FOUND"

out="$TMP_DIR/transaction_b_update_a.json"
status="$(request_json PUT "/api/transactions/${TRANSACTION_ID_A}" "$ACCESS_TOKEN_B" "$body" "$out")"
check_error "anti-IDOR transaction modification B vers A" "$status" "404" "$out" "RESOURCE_NOT_FOUND"

out="$TMP_DIR/transaction_b_delete_a.json"
status="$(request_json DELETE "/api/transactions/${TRANSACTION_ID_A}" "$ACCESS_TOKEN_B" "" "$out")"
check_error "anti-IDOR transaction suppression B vers A" "$status" "404" "$out" "RESOURCE_NOT_FOUND"

LONG_NOTE="$(printf 'x%.0s' {1..1001})"
body="$TMP_DIR/transaction_long_note.json"
create_transaction_body "$body" "12.00" "EXPENSE" "$CATEGORY_ID_A" "$TODAY" "$LONG_NOTE"
out="$TMP_DIR/transaction_long_note_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "OWASP champ texte trop long" "$status" "400" "$out" "VALIDATION_ERROR"

body="$TMP_DIR/transaction_control_chars.json"
create_transaction_body "$body" "12.00" "EXPENSE" "$CATEGORY_ID_A" "$TODAY" $'Line1\nLine2\tTabbed'
out="$TMP_DIR/transaction_control_chars_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_success "OWASP caracteres controle JSON echappes" "$status" "201" "$out"
CONTROL_TRANSACTION_ID="$(jq -r '.data.id // empty' "$out")"
if [[ -n "$CONTROL_TRANSACTION_ID" ]]; then
  cleanup_out="$TMP_DIR/transaction_control_delete.json"
  request_json DELETE "/api/transactions/${CONTROL_TRANSACTION_ID}" "$ACCESS_TOKEN_A" "" "$cleanup_out" >/dev/null || true
fi

body="$TMP_DIR/transaction_mass_assignment.json"
write_json "$body" --argjson categoryId "$CATEGORY_ID_A" --arg date "$TODAY" '{amount:12.00,type:"EXPENSE",categoryId:$categoryId,date:$date,note:"mass",userId:"00000000-0000-0000-0000-000000000000",user:{id:"00000000-0000-0000-0000-000000000000"},category:{id:999999999}}'
out="$TMP_DIR/transaction_mass_assignment_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_success "OWASP mass assignment transaction ignore" "$status" "201" "$out"
MASS_TRANSACTION_ID="$(jq -r '.data.id // empty' "$out")"
if [[ -n "$MASS_TRANSACTION_ID" ]]; then
  cleanup_out="$TMP_DIR/transaction_mass_delete.json"
  request_json DELETE "/api/transactions/${MASS_TRANSACTION_ID}" "$ACCESS_TOKEN_A" "" "$cleanup_out" >/dev/null || true
fi

body="$TMP_DIR/transaction_bad_content_type.json"
create_transaction_body "$body" "12.00" "EXPENSE" "$CATEGORY_ID_A" "$TODAY" "bad content type"
out="$TMP_DIR/transaction_bad_content_type_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out" "text/plain")"
check_error "OWASP content-type incorrect" "$status" "415" "$out" "UNSUPPORTED_MEDIA_TYPE"

body="$TMP_DIR/malformed.json"
printf '{"amount":12.00,"type":"EXPENSE","categoryId":%s,' "$CATEGORY_ID_A" > "$body"
out="$TMP_DIR/malformed_out.json"
status="$(request_json POST "/api/transactions" "$ACCESS_TOKEN_A" "$body" "$out")"
check_error "OWASP JSON malforme" "$status" "400" "$out" "VALIDATION_ERROR"

out="$TMP_DIR/dashboard_summary_default.json"
status="$(request_json GET "/api/dashboard/summary" "$ACCESS_TOKEN_A" "" "$out")"
check_success "dashboard summary sans dates" "$status" "200" "$out"

out="$TMP_DIR/dashboard_summary_period.json"
status="$(request_json GET "/api/dashboard/summary?startDate=${MONTH_START}&endDate=${MONTH_END}" "$ACCESS_TOKEN_A" "" "$out")"
check_success "dashboard summary periode valide" "$status" "200" "$out"
if jq -e '.data.expenseTotal | tonumber < 9999.99' "$out" >/dev/null; then
  pass "dashboard isolation donnees utilisateur"
else
  fail "dashboard isolation donnees utilisateur" "Le total A semble inclure les donnees de B."
fi

out="$TMP_DIR/dashboard_monthly.json"
status="$(request_json GET "/api/dashboard/monthly-balances?startDate=${MONTH_START}&endDate=${MONTH_END}" "$ACCESS_TOKEN_A" "" "$out")"
check_success "dashboard monthly-balances" "$status" "200" "$out"

out="$TMP_DIR/dashboard_category.json"
status="$(request_json GET "/api/dashboard/category-expenses?startDate=${MONTH_START}&endDate=${MONTH_END}" "$ACCESS_TOKEN_A" "" "$out")"
check_success "dashboard category-expenses" "$status" "200" "$out"

out="$TMP_DIR/dashboard_one_date.json"
status="$(request_json GET "/api/dashboard/summary?startDate=${MONTH_START}" "$ACCESS_TOKEN_A" "" "$out")"
check_error "dashboard une seule date fournie" "$status" "400" "$out" "VALIDATION_ERROR"

out="$TMP_DIR/dashboard_inverted.json"
status="$(request_json GET "/api/dashboard/summary?startDate=${MONTH_END}&endDate=${MONTH_START}" "$ACCESS_TOKEN_A" "" "$out")"
check_error "dashboard periode inversee" "$status" "400" "$out" "VALIDATION_ERROR"

out="$TMP_DIR/dashboard_over_24.json"
status="$(request_json GET "/api/dashboard/summary?startDate=2024-01-01&endDate=2026-01-01" "$ACCESS_TOKEN_A" "" "$out")"
check_error "dashboard periode superieure 24 mois" "$status" "400" "$out" "VALIDATION_ERROR"

out="$TMP_DIR/dashboard_exact_24.json"
status="$(request_json GET "/api/dashboard/summary?startDate=2024-01-01&endDate=2025-12-31" "$ACCESS_TOKEN_A" "" "$out")"
check_success "dashboard periode exactement 24 mois" "$status" "200" "$out"

out="$TMP_DIR/dashboard_no_token.json"
status="$(request_json GET "/api/dashboard/summary" "" "" "$out")"
check_error "dashboard sans token" "$status" "401" "$out" "TOKEN_INVALID"

out="$TMP_DIR/dashboard_bad_token.json"
status="$(request_json GET "/api/dashboard/summary" "invalid.jwt.token" "" "$out")"
check_error "dashboard token invalide" "$status" "401" "$out" "TOKEN_INVALID"

out="$TMP_DIR/transaction_delete_a.json"
status="$(request_json DELETE "/api/transactions/${TRANSACTION_ID_A}" "$ACCESS_TOKEN_A" "" "$out")"
check_success "transactions suppression A" "$status" "200" "$out"

if [[ -n "$TRANSACTION_ID_B" ]]; then
  cleanup_out="$TMP_DIR/transaction_delete_b.json"
  request_json DELETE "/api/transactions/${TRANSACTION_ID_B}" "$ACCESS_TOKEN_B" "" "$cleanup_out" >/dev/null || true
fi

body="$TMP_DIR/logout_a.json"
write_json "$body" --arg refreshToken "$REFRESH_TOKEN_A" '{refreshToken:$refreshToken}'
out="$TMP_DIR/logout_a_out.json"
status="$(request_json POST "/api/auth/logout" "" "$body" "$out")"
check_success "auth logout" "$status" "200" "$out"

out="$TMP_DIR/refresh_after_logout_out.json"
status="$(request_json POST "/api/auth/refresh" "" "$body" "$out")"
check_error "auth refresh apres logout" "$status" "401" "$out" "TOKEN_EXPIRED"

printf '\nResume tests manuels OWOMI:\n'
printf ' - Reussis: %d\n' "${#PASSED_TESTS[@]}"
printf ' - Echoues: %d\n' "${#FAILED_TESTS[@]}"
printf ' - Erreurs environnement: %d\n' "${#ENV_ERRORS[@]}"
printf ' - Ignores: %d\n' "${#SKIPPED_TESTS[@]}"

if (( ${#FAILED_TESTS[@]} > 0 )); then
  printf '\nTests echoues:\n'
  printf ' - %s\n' "${FAILED_TESTS[@]}"
  exit 1
fi

printf '\nTous les tests manuels automatises du script sont passes.\n'
