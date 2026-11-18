param([Parameter(Mandatory=$true)][ValidateSet('usuario','catalogo','pago','carrito','gateway')][string]$Servicio)
$ErrorActionPreference = 'Stop'
# Java carga la credencial local compartida. INTERNAL_API_KEY sigue teniendo prioridad si ya existe.
Push-Location (Join-Path $PSScriptRoot $Servicio)
try { & mvn spring-boot:run; if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el servicio.' } }
finally { Pop-Location }
