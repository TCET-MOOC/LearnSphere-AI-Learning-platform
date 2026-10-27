<#
.SYNOPSIS
    LearnSphere AI — Unified Local Stack Runner for Windows PowerShell.
.DESCRIPTION
    Launches and manages all LearnSphere AI microservices and frontend:
      - Main API (Spring Boot):         http://localhost:8080
      - Payments Service (Spring Boot): http://localhost:8081
      - Angular Frontend:               http://localhost:4200
      - PostgreSQL Database:            localhost:5432 (MOOC, learnsphere_payments)
.EXAMPLE
    .\run.ps1                  # Runs full stack via Docker Compose (Recommended)
    .\run.ps1 -Dev             # Runs services locally in separate PowerShell windows
    .\run.ps1 -Db              # Runs only the PostgreSQL database container
    .\run.ps1 -Status          # Inspects health of all services & ports
    .\run.ps1 -Stop            # Stops all running containers and dev servers
    .\run.ps1 -Reset           # Resets database to clean seeded demo state
#>

[CmdletBinding(DefaultParameterSetName = "Default")]
param (
    [Parameter(ParameterSetName = "Default")]
    [ValidateSet("docker", "dev", "database", "status", "stop", "reset", "menu")]
    [string]$Mode = "docker",

    [Parameter(ParameterSetName = "DevMode")]
    [switch]$Dev,

    [Parameter(ParameterSetName = "DbMode")]
    [switch]$Database,

    [Parameter(ParameterSetName = "StatusMode")]
    [switch]$Status,

    [Parameter(ParameterSetName = "StopMode")]
    [switch]$Stop,

    [Parameter(ParameterSetName = "ResetMode")]
    [switch]$Reset,

    [switch]$Build,
    [switch]$NoBrowser
)

# Resolve parameter set shortcuts
if ($Dev)      { $Mode = "dev" }
if ($Database) { $Mode = "database" }
if ($Status)   { $Mode = "status" }
if ($Stop)     { $Mode = "stop" }
if ($Reset)    { $Mode = "reset" }

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $ScriptDir

# --- Color and formatting helpers ---
function Write-Header {
    param ([string]$Text)
    Write-Host ""
    Write-Host "====================================================================" -ForegroundColor DarkCyan
    Write-Host "  $Text" -ForegroundColor Cyan
    Write-Host "====================================================================" -ForegroundColor DarkCyan
}

function Write-Success { param ([string]$Text) Write-Host "[OK] $Text" -ForegroundColor Green }
function Write-Info    { param ([string]$Text) Write-Host "[i]  $Text" -ForegroundColor Gray }
function Write-Warn    { param ([string]$Text) Write-Host "[!]  $Text" -ForegroundColor Yellow }
function Write-Err     { param ([string]$Text) Write-Host "[X]  $Text" -ForegroundColor Red }

function Show-Banner {
    $banner = @'
   __                               ____        __                       ___    ____
  / /   ___  ____ _ _____ ____     / __/ ____  / /_   ___   _____ ___     /   |  /  _/
 / /   / _ \/ __ `/ ___// __ \    _\ \  / __ \/ __ \ / _ \ / ___// _ \   / /| |  / /  
/ /___/  __/ /_/ / /   / / / /   /___/ / /_/ / / / //  __// /   /  __/  / ___ |_/ /   
\____/\___/\__,_/_/   /_/ /_/   /_____// .___/_/ /_/ \___//_/    \___/  /_/  |_/___/   
                                      /_/                                             
     Enterprise Learning Management and AI Tutoring Platform (Spring Boot + Angular)
'@
    Write-Host $banner -ForegroundColor Cyan
    Write-Host ""
}

# --- Environment check and setup ---
function Ensure-EnvFile {
    $envPath = Join-Path $ScriptDir ".env"
    $envExample = Join-Path $ScriptDir ".env.example"

    if (-not (Test-Path $envPath)) {
        if (Test-Path $envExample) {
            Write-Warn ".env file missing. Initializing from .env.example..."
            Copy-Item $envExample $envPath
            Write-Success "Created .env with default local development credentials."
        } else {
            Write-Err ".env and .env.example both missing in $ScriptDir"
            exit 1
        }
    }
}

function Load-EnvFile {
    Ensure-EnvFile
    $envPath = Join-Path $ScriptDir ".env"
    Get-Content $envPath | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not ($line.StartsWith("#"))) {
            $parts = $line -split '=', 2
            if ($parts.Count -eq 2) {
                $k = $parts[0].Trim()
                $v = $parts[1].Trim()
                [System.Environment]::SetEnvironmentVariable($k, $v, [System.EnvironmentVariableTarget]::Process)
            }
        }
    }
}

# --- Port check helper ---
function Test-PortInUse {
    param ([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | Where-Object { $_.State -eq 'Listen' }
    return ($null -ne $conn)
}

function Show-ServiceSummary {
    Write-Header "LearnSphere AI - Active Service Endpoints"
    Write-Host "  * Web Application:        " -NoNewline; Write-Host "http://localhost:4200" -ForegroundColor Green
    Write-Host "  * Main REST API:          " -NoNewline; Write-Host "http://localhost:8080/api" -ForegroundColor Green
    Write-Host "  * Swagger UI (Docs):      " -NoNewline; Write-Host "http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
    Write-Host "  * Payments Microservice:  " -NoNewline; Write-Host "http://localhost:8081" -ForegroundColor Green
    Write-Host "  * PostgreSQL Databases:   " -NoNewline; Write-Host "localhost:5432 (MOOC, learnsphere_payments)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "  [Demo Seed Personas] (Password: $($env:SEED_PASSWORD))" -ForegroundColor Yellow
    Write-Host "    - Admin:     admin@learnsphere.com" -ForegroundColor Gray
    Write-Host "    - Teacher:   teacher@learnsphere.com" -ForegroundColor Gray
    Write-Host "    - Student:   student@learnsphere.com" -ForegroundColor Gray
    Write-Host "    - External:  external@learnsphere.com" -ForegroundColor Gray
    Write-Host "--------------------------------------------------------------------" -ForegroundColor DarkCyan
}

# --- MODE 1: DOCKER COMPOSE ---
function Start-DockerStack {
    Write-Header "Starting LearnSphere AI via Docker Compose"
    Load-EnvFile

    # Check Docker daemon
    try {
        $dockerCheck = docker info 2>&1
        if ($LASTEXITCODE -ne 0) {
            Write-Err "Docker Desktop is not running. Please start Docker Desktop or run with -Dev."
            exit 1
        }
    } catch {
        Write-Err "Docker is not installed or not in PATH. Run with .\run.ps1 -Dev for local processes."
        exit 1
    }

    $composeArgs = @("compose", "up", "-d")
    if ($Build) {
        $composeArgs += "--build"
        Write-Info "Rebuilding Docker images as requested..."
    }

    Write-Info "Executing: docker $($composeArgs -join ' ')"
    & docker @composeArgs

    if ($LASTEXITCODE -ne 0) {
        Write-Err "Failed to launch Docker containers. Inspect errors above."
        exit 1
    }

    Write-Info "Waiting for services to report healthy (Actuator and Postgres)..."
    $maxSeconds = 75
    $elapsed = 0
    $backendReady = $false

    while ($elapsed -lt $maxSeconds) {
        Start-Sleep -Seconds 3
        $elapsed += 3
        
        try {
            $response = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 2 -ErrorAction SilentlyContinue
            if ($response.status -eq "UP") {
                $backendReady = $true
                break
            }
        } catch {
            Write-Host "." -NoNewline -ForegroundColor DarkGray
        }
    }

    Write-Host ""
    if ($backendReady) {
        Write-Success "Backend and Database are UP and healthy! ($elapsed seconds)"
    } else {
        Write-Warn "Services started, but healthcheck is still warming up. Check 'docker compose ps' for status."
    }

    Show-ServiceSummary

    if (-not $NoBrowser) {
        Write-Info "Opening http://localhost:4200 in default browser..."
        Start-Process "http://localhost:4200"
    }
}

# --- MODE 2: BARE-METAL DEV RUN ---
function Start-DevMode {
    Write-Header "Starting LearnSphere AI in Local Development Mode"
    Load-EnvFile

    # 1. Database check: Is PostgreSQL up on 5432?
    if (-not (Test-PortInUse 5432)) {
        Write-Warn "PostgreSQL is not detected on port 5432."
        Write-Info "Starting PostgreSQL via Docker Compose container ('db')..."
        try {
            & docker compose up -d db
            Start-Sleep -Seconds 4
            if (Test-PortInUse 5432) {
                Write-Success "PostgreSQL started on localhost:5432."
            }
        } catch {
            Write-Err "Could not start PostgreSQL. Ensure Docker is running or start your local Postgres on port 5432."
        }
    } else {
        Write-Success "PostgreSQL detected on port 5432."
    }

    # 2. Start Main Backend in separate window
    $backendDir = Join-Path $ScriptDir "learnsphere-backend"
    $backendScript = @"
`$Host.UI.RawUI.WindowTitle = 'LearnSphere - Main API (Port 8080)'
Set-Location '$backendDir'
Write-Host '==================================================' -ForegroundColor Cyan
Write-Host '  Starting LearnSphere Main API (Port 8080)...     ' -ForegroundColor Cyan
Write-Host '==================================================' -ForegroundColor Cyan
`$env:SPRING_PROFILES_ACTIVE = '$($env:SPRING_PROFILES_ACTIVE)'
`$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/MOOC'
`$env:SPRING_DATASOURCE_USERNAME = '$($env:POSTGRES_USER)'
`$env:SPRING_DATASOURCE_PASSWORD = '$($env:POSTGRES_PASSWORD)'
`$env:JWT_SECRET = '$($env:JWT_SECRET)'
`$env:INTERNAL_API_KEY = '$($env:INTERNAL_API_KEY)'
`$env:SEED_PASSWORD = '$($env:SEED_PASSWORD)'
`$env:APP_CORS_ALLOWED_ORIGINS = 'http://localhost:4200'
`$env:NVIDIA_NIM_API_KEY = '$($env:NVIDIA_NIM_API_KEY)'
.\mvnw.cmd spring-boot:run
"@
    $backendTemp = Join-Path $env:TEMP "learnsphere-start-backend.ps1"
    Set-Content -Path $backendTemp -Value $backendScript -Encoding UTF8
    Write-Info "Launching Main API in background window..."
    Start-Process powershell.exe -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", $backendTemp

    # 3. Start Payments Service in separate window
    $paymentsDir = Join-Path $ScriptDir "learnsphere-payments-service"
    $paymentsScript = @"
`$Host.UI.RawUI.WindowTitle = 'LearnSphere - Payments Service (Port 8081)'
Set-Location '$paymentsDir'
Write-Host '==================================================' -ForegroundColor Cyan
Write-Host '  Starting Payments Microservice (Port 8081)...   ' -ForegroundColor Cyan
Write-Host '==================================================' -ForegroundColor Cyan
`$env:SPRING_PROFILES_ACTIVE = '$($env:SPRING_PROFILES_ACTIVE)'
`$env:DB_URL = 'jdbc:postgresql://localhost:5432/learnsphere_payments'
`$env:DB_USER = '$($env:POSTGRES_USER)'
`$env:DB_PASSWORD = '$($env:POSTGRES_PASSWORD)'
`$env:RAZORPAY_KEY_ID = '$($env:RAZORPAY_KEY_ID)'
`$env:RAZORPAY_KEY_SECRET = '$($env:RAZORPAY_KEY_SECRET)'
`$env:RAZORPAY_WEBHOOK_SECRET = '$($env:RAZORPAY_WEBHOOK_SECRET)'
`$env:INTERNAL_API_KEY = '$($env:INTERNAL_API_KEY)'
`$env:MAIN_BACKEND_URL = 'http://localhost:8080'
.\mvnw.cmd spring-boot:run
"@
    $paymentsTemp = Join-Path $env:TEMP "learnsphere-start-payments.ps1"
    Set-Content -Path $paymentsTemp -Value $paymentsScript -Encoding UTF8
    Write-Info "Launching Payments Service in background window..."
    Start-Process powershell.exe -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", $paymentsTemp

    # 4. Start Angular Frontend in separate window
    $frontendDir = Join-Path $ScriptDir "learnSphere\frontend\learnsphere"
    $frontendScript = @"
`$Host.UI.RawUI.WindowTitle = 'LearnSphere - Angular Frontend (Port 4200)'
Set-Location '$frontendDir'
Write-Host '==================================================' -ForegroundColor Cyan
Write-Host '  Starting Angular Frontend (Port 4200)...        ' -ForegroundColor Cyan
Write-Host '==================================================' -ForegroundColor Cyan
npm start
"@
    $frontendTemp = Join-Path $env:TEMP "learnsphere-start-frontend.ps1"
    Set-Content -Path $frontendTemp -Value $frontendScript -Encoding UTF8
    Write-Info "Launching Angular Dev Server in background window..."
    Start-Process powershell.exe -ArgumentList "-NoExit", "-ExecutionPolicy", "Bypass", "-File", $frontendTemp

    Show-ServiceSummary

    if (-not $NoBrowser) {
        Write-Info "Waiting a few moments for Angular dev server before opening browser..."
        Start-Sleep -Seconds 6
        Start-Process "http://localhost:4200"
    }
}

# --- MODE 3: DATABASE ONLY ---
function Start-DbOnly {
    Write-Header "Starting PostgreSQL Database Container"
    Load-EnvFile
    & docker compose up -d db
    if ($LASTEXITCODE -eq 0) {
        Write-Success "PostgreSQL running on localhost:5432 (Databases: MOOC, learnsphere_payments)"
    }
}

# --- MODE 4: STATUS & HEALTH ---
function Show-Status {
    Write-Header "LearnSphere AI - Service Status and Port Inspection"
    Load-EnvFile

    $services = @(
        @{ Name = "PostgreSQL Database"; Port = 5432 },
        @{ Name = "Main Backend API";    Port = 8080; Url = "http://localhost:8080/actuator/health" },
        @{ Name = "Payments Service";    Port = 8081 },
        @{ Name = "Angular Frontend";    Port = 4200; Url = "http://localhost:4200" }
    )

    foreach ($s in $services) {
        $inUse = Test-PortInUse $s.Port
        $statusStr = if ($inUse) { "LISTENING" } else { "OFFLINE" }
        $color = if ($inUse) { "Green" } else { "Red" }
        
        $healthDetail = ""
        if ($s.Url -and $inUse) {
            try {
                $res = Invoke-RestMethod -Uri $s.Url -TimeoutSec 1 -ErrorAction SilentlyContinue
                if ($res.status) { $healthDetail = " ($($res.status))" }
                else { $healthDetail = " (HTTP OK)" }
            } catch {
                $healthDetail = " (Starting / Unhealthy)"
            }
        }

        Write-Host "  [$statusStr] " -ForegroundColor $color -NoNewline
        Write-Host "$($s.Name.PadRight(24)) Port: $($s.Port)$healthDetail"
    }

    Write-Host "`nDocker Containers:" -ForegroundColor Cyan
    try {
        & docker compose ps
    } catch {
        Write-Host "  Docker is not running or not available." -ForegroundColor DarkGray
    }
}

# --- MODE 5: STOP ---
function Stop-AllServices {
    Write-Header "Stopping LearnSphere AI Services"
    
    # 1. Stop Docker Compose
    Write-Info "Stopping Docker Compose containers..."
    try {
        & docker compose down
        Write-Success "Docker containers stopped."
    } catch {
        Write-Warn "Docker compose down failed or Docker was not running."
    }

    # 2. Terminate any stray node/java processes on 4200, 8080, 8081 if running
    $ports = @(4200, 8080, 8081)
    foreach ($p in $ports) {
        $conns = Get-NetTCPConnection -LocalPort $p -ErrorAction SilentlyContinue | Where-Object { $_.State -eq 'Listen' }
        if ($conns) {
            foreach ($c in $conns) {
                try {
                    $proc = Get-Process -Id $c.OwningProcess -ErrorAction SilentlyContinue
                    if ($proc) {
                        Write-Info "Stopping process on port $p (PID: $($proc.Id), Name: $($proc.ProcessName))..."
                        Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
                    }
                } catch {}
            }
        }
    }
    Write-Success "All LearnSphere AI services stopped cleanly."
}

# --- MODE 6: RESET DEMO DATA ---
function Reset-DemoData {
    Write-Header "Resetting Demo Database and Volume Data"
    Write-Warn "This will wipe MOOC and learnsphere_payments database volumes and re-seed starter personas."
    
    $confirm = Read-Host "Are you sure you want to proceed? (y/N)"
    if ($confirm -eq 'y' -or $confirm -eq 'Y') {
        & docker compose down -v --remove-orphans
        Start-DockerStack
    } else {
        Write-Info "Reset cancelled."
    }
}

# --- INTERACTIVE MENU ---
function Show-Menu {
    Show-Banner
    Write-Host "Please select how you would like to run LearnSphere AI:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  [1] Docker Compose Stack (Recommended - Starts Postgres, Backend, Payments and Frontend)" -ForegroundColor Green
    Write-Host "  [2] Native Dev Mode (Spawns live Backend, Payments, and Frontend in separate windows)" -ForegroundColor White
    Write-Host "  [3] PostgreSQL Database Only (Docker container on port 5432)" -ForegroundColor White
    Write-Host "  [4] Check Service Status and Health" -ForegroundColor White
    Write-Host "  [5] Stop All Services" -ForegroundColor Yellow
    Write-Host "  [6] Reset Demo Database" -ForegroundColor Red
    Write-Host "  [Q] Quit" -ForegroundColor DarkGray
    Write-Host ""
    
    $choice = Read-Host "Select an option [1-6, Q] (Default: 1)"
    if ([string]::IsNullOrWhiteSpace($choice)) { $choice = "1" }

    switch ($choice) {
        "1" { Start-DockerStack }
        "2" { Start-DevMode }
        "3" { Start-DbOnly }
        "4" { Show-Status }
        "5" { Stop-AllServices }
        "6" { Reset-DemoData }
        "Q" { exit 0 }
        "q" { exit 0 }
        default { Start-DockerStack }
    }
}

# --- MAIN EXECUTION ---
Show-Banner

switch ($Mode.ToLower()) {
    "docker"    { Start-DockerStack }
    "dev"       { Start-DevMode }
    "database"  { Start-DbOnly }
    "db"        { Start-DbOnly }
    "status"    { Show-Status }
    "stop"      { Stop-AllServices }
    "reset"     { Reset-DemoData }
    "menu"      { Show-Menu }
    default     { Show-Menu }
}
