param([switch]$Demo)
$ErrorActionPreference = 'Stop'
Get-Command java -ErrorAction Stop | Out-Null
$backend = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$repositorio = Join-Path $env:USERPROFILE '.m2\repository'
$patron = if ($Demo) { 'h2-*.jar' } else { 'mysql-connector-j-*.jar' }
$driver = Get-ChildItem $repositorio -Recurse -Filter $patron -File |
    Where-Object { $_.Name -notmatch '-(sources|javadoc)\.jar$' } |
    Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $driver) { throw 'No se encontró el controlador JDBC. Arranca los servicios de destino una vez con Maven.' }

$anteriores = @{}
try {
    if ($Demo) {
        foreach ($servicio in @('catalogo', 'inventario')) {
            $prefijo = "${servicio}".ToUpperInvariant() + '_DB_'
            $base = Join-Path $backend "$servicio\data\$servicio"
            foreach ($nombre in @('URL', 'USERNAME', 'PASSWORD')) {
                $variable = $prefijo + $nombre
                $anteriores[$variable] = [Environment]::GetEnvironmentVariable($variable, 'Process')
            }
            [Environment]::SetEnvironmentVariable($prefijo + 'URL', "jdbc:h2:file:${base};MODE=MySQL;IFEXISTS=TRUE", 'Process')
            [Environment]::SetEnvironmentVariable($prefijo + 'USERNAME', 'sa', 'Process')
            [Environment]::SetEnvironmentVariable($prefijo + 'PASSWORD', '', 'Process')
        }
    }
    & java --class-path $driver.FullName (Join-Path $PSScriptRoot 'MigrarInventario.java')
    if ($LASTEXITCODE -ne 0) { throw 'No se completó la migración.' }
} finally {
    foreach ($variable in $anteriores.Keys) {
        [Environment]::SetEnvironmentVariable($variable, $anteriores[$variable], 'Process')
    }
}
