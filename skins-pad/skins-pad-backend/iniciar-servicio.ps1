param(
    [Parameter(Mandatory=$true)]
    [ValidateSet('usuario','inventario','catalogo','pago','ticket','reporte','carrito','calificacion','gateway')]
    [string]$Servicio,
    [switch]$Demo
)
$ErrorActionPreference = 'Stop'
# Java carga la credencial local compartida. INTERNAL_API_KEY sigue teniendo prioridad si ya existe.
$mavenArgs = @('spring-boot:run')
if ($Demo -and $Servicio -ne 'gateway') {
    $mavenArgs = @('-Pdemo', '-Dspring-boot.run.profiles=demo') + $mavenArgs
}
Push-Location (Join-Path $PSScriptRoot $Servicio)
try { & mvn @mavenArgs; if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar el servicio.' } }
finally { Pop-Location }
