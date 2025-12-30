# k6 테스트 실행 스크립트 (PowerShell)
# 사용법: .\run-k6.ps1 [스크립트 경로]

param(
    [Parameter(Position=0)]
    [string]$Script = "scripts/commerce-api/order-create.js",
    
    [switch]$Help
)

if ($Help) {
    Write-Host @"
k6 테스트 실행 스크립트

사용법:
  .\run-k6.ps1 [스크립트 경로] [옵션]

예제:
  .\run-k6.ps1
  .\run-k6.ps1 scripts/commerce-api/order-create-constant.js
  .\run-k6.ps1 scripts/pg-simulator/payment-api.js

사용 가능한 스크립트:
  - scripts/commerce-api/order-create.js (기본)
  - scripts/commerce-api/order-create-constant.js
  - scripts/commerce-api/order-create-spike.js
  - scripts/commerce-api/order-list.js
  - scripts/pg-simulator/payment-api.js
"@
    exit 0
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$K6Path = Join-Path $ProjectRoot "tests\k6"

# 스크립트 경로 검증
$ScriptPath = Join-Path $K6Path $Script
if (-not (Test-Path $ScriptPath)) {
    Write-Host "오류: 스크립트를 찾을 수 없습니다: $Script" -ForegroundColor Red
    Write-Host "경로: $ScriptPath" -ForegroundColor Yellow
    exit 1
}

Write-Host "k6 테스트 실행 중..." -ForegroundColor Green
Write-Host "스크립트: $Script" -ForegroundColor Cyan
Write-Host ""

docker run --rm -i `
  -v "${K6Path}:/scripts" `
  -e COMMERCE_API_BASE=http://host.docker.internal:8080/api/v1 `
  -e PG_API_BASE=http://host.docker.internal:8082/api/v1 `
  -e BASE_URL=http://host.docker.internal:8080 `
  -e PG_API_URL=http://host.docker.internal:8082 `
  grafana/k6:latest run "/scripts/$Script"

