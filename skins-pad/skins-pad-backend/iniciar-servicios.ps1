param([switch]$Demo)
$ErrorActionPreference = 'Stop'
Get-Command mvn -ErrorAction Stop | Out-Null
$script = Join-Path $PSScriptRoot 'iniciar-servicio.ps1'
$perfil = if ($Demo) { ' -Demo' } else { '' }

function Iniciar([string]$servicio) {
    Start-Process powershell.exe -PassThru -ArgumentList "-NoExit -File `"$script`" -Servicio $servicio$perfil"
}

$null = Iniciar 'usuario'
if ($Demo) {
    $usuarioUrl = if ($env:USUARIO_URL) { $env:USUARIO_URL.TrimEnd('/') } else { 'http://localhost:8087' }
    $limite = (Get-Date).AddMinutes(5)
    $listo = $false
    do {
        try {
            $cuentas = Invoke-RestMethod -Uri "$usuarioUrl/api/usuarios/publicos" -TimeoutSec 3
            $listo = @($cuentas | Where-Object { $_.rol -eq 'cliente' }).Count -gt 0
        } catch {
            $listo = $false
        }
        if (-not $listo) { Start-Sleep -Seconds 2 }
    } until ($listo -or (Get-Date) -ge $limite)
    if (-not $listo) { throw 'El servicio usuario no respondió con cuentas demo. Revisa su consola.' }
}

foreach ($servicio in @('inventario', 'catalogo', 'carrito', 'pago', 'calificacion', 'gateway')) {
    $null = Iniciar $servicio
}
