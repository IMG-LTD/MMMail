param(
  [Parameter(Position = 0)]
  [string]$Mode = '',
  [Parameter(Position = 1)]
  [string]$EnvFile = '',
  [Alias('h', 'help')]
  [switch]$ShowHelp = $false,
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$RemainingArgs = @()
)

$ErrorActionPreference = 'Stop'
$RootDir = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
  $EnvFile = $env:MMMAIL_ENV_FILE
}

if ([string]::IsNullOrWhiteSpace($EnvFile)) {
  $EnvFile = Join-Path $RootDir '.env'
}

function Write-Usage {
  Write-Host 'Usage: scripts/install.ps1 [minimal|standard] [-EnvFile path] [-h|--help]'
  Write-Host 'Set MMMAIL_ENV_FILE=/path/to/.env to use a custom env file.'
}

function Resolve-RequestedMode {
  $helpTokens = @('-h', '--help', 'help')

  if ($ShowHelp -or $helpTokens -contains $Mode -or ([string]::IsNullOrWhiteSpace($Mode) -and $RemainingArgs.Count -eq 1 -and $helpTokens -contains $RemainingArgs[0])) {
    Write-Usage
    exit 0
  }

  if ($RemainingArgs.Count -gt 0) {
    Write-Usage
    throw "Unexpected arguments: $($RemainingArgs -join ' ')"
  }

  if ([string]::IsNullOrWhiteSpace($Mode)) {
    return ''
  }

  if ($Mode -eq 'minimal' -or $Mode -eq 'standard') {
    return $Mode
  }

  Write-Usage
  throw "Unknown install mode: $Mode"
}

function Require-Command {
  param([string]$Name)

  if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
    throw "Missing required command: $Name"
  }
}

function Ensure-EnvFile {
  if (Test-Path $EnvFile) {
    return
  }

  Copy-Item (Join-Path $RootDir '.env.example') $EnvFile
  Write-Host "Created $EnvFile from .env.example."
  Write-Host "Edit $EnvFile and replace required secrets before running this installer again."
  exit 1
}

function Read-EnvMap {
  $map = @{}

  Get-Content $EnvFile | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith('#')) {
      return
    }

    $separator = $line.IndexOf('=')
    if ($separator -lt 1) {
      return
    }

    $key = $line.Substring(0, $separator)
    $value = $line.Substring($separator + 1)
    $map[$key] = $value
  }

  return $map
}

function Require-EnvValue {
  param(
    [hashtable]$EnvMap,
    [string]$Key
  )

  if (-not $EnvMap.ContainsKey($Key) -or [string]::IsNullOrWhiteSpace($EnvMap[$Key])) {
    throw "$Key is missing in $EnvFile"
  }

  if ($EnvMap[$Key].StartsWith('replace-with-')) {
    throw "$Key still uses placeholder value in $EnvFile"
  }
}

function Require-EnvEquals {
  param(
    [hashtable]$EnvMap,
    [string]$Key,
    [string]$Expected
  )

  if (-not $EnvMap.ContainsKey($Key) -or $EnvMap[$Key] -ne $Expected) {
    throw "$Key must be $Expected for this install mode"
  }
}

function Select-InstallMode {
  param([string]$RequestedMode)

  if ($RequestedMode -eq 'minimal' -or $RequestedMode -eq 'standard') {
    return $RequestedMode
  }

  $selected = Read-Host 'Choose install mode [minimal/standard] (minimal)'
  if ([string]::IsNullOrWhiteSpace($selected)) {
    $selected = 'minimal'
  }

  if ($selected -ne 'minimal' -and $selected -ne 'standard') {
    Write-Usage
    throw "Unknown install mode: $selected"
  }

  return $selected
}

function Check-EnvForMode {
  param(
    [hashtable]$EnvMap,
    [string]$InstallMode
  )

  Require-EnvValue $EnvMap 'MMMAIL_JWT_SECRET'
  Require-EnvValue $EnvMap 'SPRING_DATASOURCE_PASSWORD'
  Require-EnvValue $EnvMap 'SPRING_REDIS_PASSWORD'
  Require-EnvValue $EnvMap 'MYSQL_ROOT_PASSWORD'

  if ($EnvMap.ContainsKey('SPRING_SQL_INIT_MODE') -and $EnvMap['SPRING_SQL_INIT_MODE'] -ne 'never') {
    throw 'SPRING_SQL_INIT_MODE must be never because Flyway owns schema migrations'
  }

  if ($InstallMode -eq 'minimal') {
    # Requires MMMAIL_NACOS_ENABLED=false
    Require-EnvEquals $EnvMap 'MMMAIL_NACOS_ENABLED' 'false'
  } else {
    # Requires MMMAIL_NACOS_ENABLED=true
    Require-EnvEquals $EnvMap 'MMMAIL_NACOS_ENABLED' 'true'
    Require-EnvValue $EnvMap 'NACOS_USERNAME'
    Require-EnvValue $EnvMap 'NACOS_PASSWORD'
  }
}

function Run-Compose {
  param([string]$InstallMode)

  Push-Location $RootDir
  try {
    if ($InstallMode -eq 'minimal') {
      docker compose --env-file $EnvFile -f docker-compose.minimal.yml up -d --build
    } else {
      docker compose --env-file $EnvFile up -d --build
    }
  } finally {
    Pop-Location
  }
}

$requestedMode = Resolve-RequestedMode
$installMode = Select-InstallMode $requestedMode

Require-Command 'docker'
docker compose version | Out-Null
Ensure-EnvFile
$envMap = Read-EnvMap
Check-EnvForMode $envMap $installMode
Run-Compose $installMode

Write-Host ""
Write-Host "MMMail $installMode mode is starting."
Write-Host "Frontend: http://127.0.0.1:3001"
Write-Host "Backend health: http://127.0.0.1:8080/actuator/health"
Write-Host "Boundary page: http://127.0.0.1:3001/boundary"
Write-Host "Migration status: ./scripts/db-upgrade.sh .env info"
