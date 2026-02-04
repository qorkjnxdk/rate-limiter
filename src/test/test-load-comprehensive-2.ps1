Write-Host "=== SEQUENTIAL PERFORMANCE TEST ===" -ForegroundColor Cyan
Write-Host "Testing actual app performance (no PowerShell overhead)" -ForegroundColor Yellow
Write-Host ""

$url = "http://localhost:8080/api/check-limit"
$requestCount = 100

Write-Host "Sending $requestCount sequential requests..." -ForegroundColor Yellow
$start = Get-Date

for ($i = 1; $i -le $requestCount; $i++) {
    $body = @{
        resource = "api/test"
        userId = "user-$($i % 10)"
    } | ConvertTo-Json

    try {
        $response = Invoke-RestMethod `
            -Uri $url `
            -Method POST `
            -Body $body `
            -ContentType "application/json" `
            -TimeoutSec 5
    } catch {
        Write-Host "Request $i failed: $_" -ForegroundColor Red
    }

    # Progress indicator
    if ($i % 10 -eq 0) {
        Write-Host "  Completed $i requests..." -ForegroundColor Gray
    }
}

$end = Get-Date
$duration = ($end - $start).TotalSeconds
$throughput = [math]::Round($requestCount / $duration, 2)
$avgLatency = [math]::Round(($duration * 1000) / $requestCount, 2)

Write-Host ""
Write-Host "=== RESULTS ===" -ForegroundColor Cyan
Write-Host "Total Requests:     $requestCount" -ForegroundColor White
Write-Host "Duration:           $([math]::Round($duration, 2))s" -ForegroundColor White
Write-Host "Throughput:         $throughput req/s" -ForegroundColor $(if ($throughput -gt 50) { "Green" } else { "Red" })
Write-Host "Avg Latency:        ${avgLatency}ms" -ForegroundColor $(if ($avgLatency -lt 30) { "Green" } else { "Red" })

Write-Host ""
if ($throughput -lt 50) {
    Write-Host "⚠️  WARNING: Throughput is LOW. Expected 60-100 req/s." -ForegroundColor Red
    Write-Host "This indicates an app performance issue, not PowerShell overhead." -ForegroundColor Yellow
} else {
    Write-Host "✅ GOOD: App performance is healthy!" -ForegroundColor Green
    Write-Host "Your original test was limited by PowerShell job creation overhead." -ForegroundColor Yellow
}