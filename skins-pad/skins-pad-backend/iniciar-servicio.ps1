param([Parameter(Mandatory=$true)][ValidateSet('usuario','catalogo','pago','carrito','gateway','calificacion','steam','inventario')][string]$Servicio, [switch]$InventarioDemo, [switch]$Demo)
$ErrorActionPreference = 'Stop'
if ($InventarioDemo -and $Servicio -eq 'inventario') { $env:INVENTARIO_DEMO = 'true' }
Get-Command mvn -ErrorAction Stop | Out-Null
$mavenArgs = @('spring-boot:run')
if ($Demo -and $Servicio -notin @('gateway')) {
    $mavenArgs = @('-Pdemo', 'spring-boot:run', '-Dspring-boot.run.profiles=demo')
}
# Java carga la credencial local compartida. INTERNAL_API_KEY sigue teniendo prioridad si ya existe.
Push-Location (Join-Path $PSScriptRoot $Servicio)
try { & mvn @mavenArgs; if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el servicio.' } }
finally { Pop-Location }
