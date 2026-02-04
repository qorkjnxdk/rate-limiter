# Simple performance test using PowerShell

Write-Host "=== RATE LIMITER PERFORMANCE TEST ===" -ForegroundColor Cyan
Write-Host ""

# Configuration
$url = "http://localhost:8080/api/check-limit"
$requestCount = 1000
$concurrency = 10

Write-Host "Configuration:" -ForegroundColor Yellow
Write-Host "  URL: $url"
Write-Host "  Total Requests: $requestCount"
Write-Host "  Concurrent Users: $concurrency"
Write-Host ""

# Run test
Write-Host "Running test..." -ForegroundColor Yellow

$start = Get-Date

$jobs = 1..$requestCount | ForEach-Object {
    Start-Job -ScriptBlock {
        param($id, $url)

        $body = @{
            resource = "api/perf-test"
            userId = "perf-user-$($id % 100)"  # 100 different users
        } | ConvertTo-Json

        $response = Invoke-WebRequest `
            -Uri $url `
            -Method POST `
            -Headers @{"Content-Type"="application/json"} `
            -Body $body `
            -UseBasicParsing

        return @{
            StatusCode = $response.StatusCode
            Time = (Get-Date)
        }
    } -ArgumentList $_, $url

    # Throttle job creation to avoid overwhelming PowerShell
    if ($_ % $concurrency -eq 0) {
        Start-Sleep -Milliseconds 10
    }
}

# Wait for completion
$jobs | Wait-Job | Out-Null

$end = Get-Date
$duration = ($end - $start).TotalSeconds

# Collect results
$results = $jobs | Receive-Job
$jobs | Remove-Job

# Calculate stats
$totalRequests = $results.Count
$requestsPerSecond = [math]::Round($totalRequests / $duration, 2)

Write-Host ""
Write-Host "=== RESULTS ===" -ForegroundColor Cyan
Write-Host "Total Requests:     $totalRequests" -ForegroundColor White
Write-Host "Duration:           $([math]::Round($duration, 2)) seconds" -ForegroundColor White
Write-Host "Throughput:         $requestsPerSecond req/s" -ForegroundColor Green
Write-Host "Avg Response Time:  $([math]::Round(($duration * 1000) / $totalRequests, 2)) ms" -ForegroundColor Green

# Get metrics
Write-Host ""
Write-Host "Fetching metrics..." -ForegroundColor Yellow
$metrics = Invoke-RestMethod -Uri "http://localhost:8080/api/metrics/summary"

Write-Host ""
Write-Host "=== METRICS ===" -ForegroundColor Cyan
Write-Host "Total Requests:     $($metrics.totalRequests)" -ForegroundColor White
Write-Host "Allowed:            $($metrics.allowedRequests)" -ForegroundColor Green
Write-Host "Denied:             $($metrics.deniedRequests)" -ForegroundColor Red
Write-Host "Block Rate:         $([math]::Round($metrics.blockRatePercent, 2))%" -ForegroundColor Yellow
Write-Host "Lua Success:        $($metrics.luaScriptSuccesses)" -ForegroundColor Green
Write-Host "Lua Failures:       $($metrics.luaScriptFailures)" -ForegroundColor $(if ($metrics.luaScriptFailures -gt 0) { "Red" } else { "Green" })