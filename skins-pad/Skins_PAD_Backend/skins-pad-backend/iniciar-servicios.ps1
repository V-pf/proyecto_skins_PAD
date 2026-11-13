param([switch]$Demo)
foreach ($s in @("usuario","catalogo","carrito","pago","gateway")) {
  $dir = Join-Path $PSScriptRoot $s
  $cmd = "mvn spring-boot:run"
  if ($Demo -and $s -ne "gateway") { $cmd = 'mvn -Pdemo spring-boot:run "-Dspring-boot.run.profiles=demo"' }
  Start-Process powershell -ArgumentList @("-NoExit","-Command","Set-Location '$dir'; $cmd")
}
