# ============================================================================
# LOAD BALANCED MULTI-INSTANCE PERFORMANCE TEST
# ============================================================================
# Tests rate limiter through Nginx load balancer distributing across 3 instances

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "    LOAD BALANCED RATE LIMITER PERFORMANCE TEST" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Configuration
$nginxUrl = "http://localhost:8080"
$testScenarios = @(
    @{
        Name = "Warm-up"
        Requests = 50
        Concurrency = 10
        Description = "Initialize connections and caches"
    },
    @{
        Name = "Light Load"
        Requests = 200
        Concurrency = 20
        Description = "Normal traffic pattern"
    },
    @{
        Name = "Medium Load"
        Requests = 500
        Concurrency = 50
        Description = "Peak traffic simulation"
    },
    @{
        Name = "Heavy Load"
        Requests = 1000
        Concurrency = 100
        Description = "Stress test with high concurrency"
    },
    @{
        Name = "Sustained Load"
        Requests = 1500
        Concurrency = 150
        Description = "Extended high-load scenario"
    }
)

# Results storage
$allResults = @()

# ============================================================================
# HELPER FUNCTION: Run Load Test
# ============================================================================
function Run-LoadTest {
    param(
        [string]$Url,
        [int]$TotalRequests,
        [int]$Concurrency,
        [string]$TestName
    )

    Write-Host "Running $TestName..." -ForegroundColor Yellow
    Write-Host "  Requests: $TotalRequests | Concurrency: $Concurrency" -ForegroundColor Gray

    # Create runspace pool for true parallelism
    $runspacePool = [runspacefactory]::CreateRunspacePool(1, $Concurrency)
    $runspacePool.Open()

    $startTime = Get-Date

    # Dispatch all requests
    $jobs = 1..$TotalRequests | ForEach-Object {
        $powershell = [powershell]::Create().AddScript({
            param($requestId, $url)

            $endpoint = "$url/api/check-limit"
            $body = @{
                resource = "api/load-test"
                userId = "user-$($requestId % 100)"  # 100 different users
            } | ConvertTo-Json

            $requestStart = Get-Date

            try {
                $response = Invoke-RestMethod `
                    -Uri $endpoint `
                    -Method POST `
                    -Body $body `
                    -ContentType "application/json" `
                    -TimeoutSec 10 `
                    -ErrorAction Stop

                $requestEnd = Get-Date
                $latency = ($requestEnd - $requestStart).TotalMilliseconds

                return @{
                    Success = $true
                    Latency = $latency
                    StatusCode = 200
                }
            } catch {
                $requestEnd = Get-Date
                $latency = ($requestEnd - $requestStart).TotalMilliseconds

                return @{
                    Success = $false
                    Latency = $latency
                    Error = $_.Exception.Message
                    StatusCode = 0
                }
            }
        }).AddArgument($_).AddArgument($Url)

        $powershell.RunspacePool = $runspacePool

        @{
            PowerShell = $powershell
            Handle = $powershell.BeginInvoke()
        }

        # Progress indicator every 100 requests
        if ($_ % 100 -eq 0) {
            Write-Host "    Dispatched $_/$TotalRequests requests..." -ForegroundColor DarkGray
        }
    }

    # Wait for all requests to complete
    Write-Host "    Waiting for completion..." -ForegroundColor DarkGray

    $results = $jobs | ForEach-Object {
        $result = $_.PowerShell.EndInvoke($_.Handle)
        $_.PowerShell.Dispose()
        $result
    }

    $endTime = Get-Date
    $totalDuration = ($endTime - $startTime).TotalSeconds

    # Clean up
    $runspacePool.Close()
    $runspacePool.Dispose()

    # Analyze results
    $successful = ($results | Where-Object { $_.Success -eq $true }).Count
    $failed = ($results | Where-Object { $_.Success -eq $false }).Count
    $successRate = [math]::Round(($successful / $TotalRequests) * 100, 2)

    $latencies = ($results | Where-Object { $_.Success -eq $true }).Latency | Sort-Object

    if ($latencies.Count -gt 0) {
        $p50Index = [math]::Floor($latencies.Count * 0.50)
        $p95Index = [math]::Floor($latencies.Count * 0.95)
        $p99Index = [math]::Floor($latencies.Count * 0.99)

        $p50 = [math]::Round($latencies[$p50Index], 2)
        $p95 = [math]::Round($latencies[$p95Index], 2)
        $p99 = [math]::Round($latencies[$p99Index], 2)
        $min = [math]::Round(($latencies | Measure-Object -Minimum).Minimum, 2)
        $max = [math]::Round(($latencies | Measure-Object -Maximum).Maximum, 2)
        $avg = [math]::Round(($latencies | Measure-Object -Average).Average, 2)
    } else {
        $p50 = $p95 = $p99 = $min = $max = $avg = 0
    }

    $throughput = [math]::Round($successful / $totalDuration, 2)

    return @{
        TestName = $TestName
        TotalRequests = $TotalRequests
        Concurrency = $Concurrency
        Successful = $successful
        Failed = $failed
        SuccessRate = $successRate
        Duration = [math]::Round($totalDuration, 2)
        Throughput = $throughput
        LatencyMin = $min
        LatencyP50 = $p50
        LatencyP95 = $p95
        LatencyP99 = $p99
        LatencyMax = $max
        LatencyAvg = $avg
    }
}

# ============================================================================
# PRE-FLIGHT CHECK
# ============================================================================
Write-Host "Pre-flight checks..." -ForegroundColor Yellow

try {
    $healthCheck = Invoke-RestMethod -Uri "$nginxUrl/api/health" -TimeoutSec 5
    Write-Host "  ✓ Nginx accessible at $nginxUrl" -ForegroundColor Green
} catch {
    Write-Host "  ✗ ERROR: Cannot reach Nginx at $nginxUrl" -ForegroundColor Red
    Write-Host "    Make sure Docker containers are running:" -ForegroundColor Yellow
    Write-Host "    docker-compose ps" -ForegroundColor Gray
    exit 1
}

try {
    $metrics = Invoke-RestMethod -Uri "$nginxUrl/api/metrics/summary" -TimeoutSec 5
    Write-Host "  ✓ Metrics endpoint accessible" -ForegroundColor Green
} catch {
    Write-Host "  ⚠ Warning: Metrics endpoint not responding (non-critical)" -ForegroundColor Yellow
}

Write-Host ""
Start-Sleep -Seconds 2

# ============================================================================
# RUN TEST SCENARIOS
# ============================================================================

foreach ($scenario in $testScenarios) {
    Write-Host ""
    Write-Host "===============================================================" -ForegroundColor DarkGray
    Write-Host "  Scenario: $($scenario.Name)" -ForegroundColor Cyan
    Write-Host "  Description: $($scenario.Description)" -ForegroundColor Gray
    Write-Host "===============================================================" -ForegroundColor DarkGray
    Write-Host ""

    $result = Run-LoadTest `
        -Url $nginxUrl `
        -TotalRequests $scenario.Requests `
        -Concurrency $scenario.Concurrency `
        -TestName $scenario.Name

    $allResults += $result

    # Display results
    Write-Host ""
    Write-Host "  Results:" -ForegroundColor Green
    Write-Host "    Duration:        $($result.Duration)s" -ForegroundColor White
    Write-Host "    Throughput:      $($result.Throughput) req/s" -ForegroundColor $(if ($result.Throughput -gt 200) { "Green" } elseif ($result.Throughput -gt 100) { "Yellow" } else { "Red" })
    Write-Host "    Success Rate:    $($result.SuccessRate)%" -ForegroundColor $(if ($result.SuccessRate -gt 99) { "Green" } elseif ($result.SuccessRate -gt 95) { "Yellow" } else { "Red" })
    Write-Host "    Successful:      $($result.Successful)/$($result.TotalRequests)" -ForegroundColor White
    Write-Host "    Failed:          $($result.Failed)" -ForegroundColor $(if ($result.Failed -eq 0) { "Green" } else { "Red" })
    Write-Host ""
    Write-Host "    Latency:" -ForegroundColor White
    Write-Host "      Min:     $($result.LatencyMin)ms" -ForegroundColor Gray
    Write-Host "      p50:     $($result.LatencyP50)ms" -ForegroundColor White
    Write-Host "      p95:     $($result.LatencyP95)ms" -ForegroundColor White
    Write-Host "      p99:     $($result.LatencyP99)ms" -ForegroundColor White
    Write-Host "      Max:     $($result.LatencyMax)ms" -ForegroundColor Gray
    Write-Host "      Average: $($result.LatencyAvg)ms" -ForegroundColor White

    # Cool down between tests (except for warm-up)
    if ($scenario.Name -ne "Warm-up") {
        Write-Host ""
        Write-Host "  Cooling down for 3 seconds..." -ForegroundColor DarkGray
        Start-Sleep -Seconds 3
    }
}

# ============================================================================
# SUMMARY TABLE
# ============================================================================

Write-Host ""
Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "                    PERFORMANCE SUMMARY" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

# Filter out warm-up from summary
$summaryResults = $allResults | Where-Object { $_.TestName -ne "Warm-up" }

Write-Host "Scenario          | Requests | Throughput  | p50     | p95     | p99     | Success"
Write-Host "------------------|----------|-------------|---------|---------|---------|--------"

foreach ($result in $summaryResults) {
    $line = "{0,-17} | {1,8} | {2,9} r/s | {3,6}ms | {4,6}ms | {5,6}ms | {6,6}%" -f `
        $result.TestName, `
        $result.TotalRequests, `
        $result.Throughput, `
        $result.LatencyP50, `
        $result.LatencyP95, `
        $result.LatencyP99, `
        $result.SuccessRate

    # Color code based on performance
    $color = "White"
    if ($result.Throughput -gt 300) { $color = "Green" }
    elseif ($result.Throughput -gt 200) { $color = "Yellow" }

    Write-Host $line -ForegroundColor $color
}

# ============================================================================
# KEY METRICS FOR RESUME
# ============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Green
Write-Host "                    RESUME NUMBERS" -ForegroundColor Green
Write-Host "===============================================================" -ForegroundColor Green
Write-Host ""

$bestThroughput = ($summaryResults | Sort-Object -Property Throughput -Descending)[0]
$bestLatency = ($summaryResults | Sort-Object -Property LatencyP95)[0]
$avgThroughput = [math]::Round(($summaryResults | Measure-Object -Property Throughput -Average).Average, 2)

Write-Host "Peak Throughput:" -ForegroundColor White
Write-Host "  $($bestThroughput.Throughput)+ requests/second" -ForegroundColor Green
Write-Host "  (Achieved in: $($bestThroughput.TestName))" -ForegroundColor Gray

Write-Host ""
Write-Host "Best Latency (p95):" -ForegroundColor White
Write-Host "  <$($bestLatency.LatencyP95)ms" -ForegroundColor Green
Write-Host "  (Achieved in: $($bestLatency.TestName))" -ForegroundColor Gray

Write-Host ""
Write-Host "Best Latency (p99):" -ForegroundColor White
Write-Host "  <$($bestLatency.LatencyP99)ms" -ForegroundColor Green

Write-Host ""
Write-Host "Average Throughput:" -ForegroundColor White
Write-Host "  $avgThroughput req/s across all tests" -ForegroundColor Yellow

Write-Host ""
Write-Host "Architecture:" -ForegroundColor White
Write-Host "  3 Spring Boot instances" -ForegroundColor Gray
Write-Host "  Nginx load balancer (round-robin)" -ForegroundColor Gray
Write-Host "  Redis distributed state management" -ForegroundColor Gray
Write-Host "  PostgreSQL configuration storage" -ForegroundColor Gray

# ============================================================================
# SYSTEM METRICS
# ============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "                    SYSTEM METRICS" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

try {
    $metrics = Invoke-RestMethod -Uri "$nginxUrl/api/metrics/summary" -TimeoutSec 5

    Write-Host "Rate Limiter Metrics:" -ForegroundColor White
    Write-Host "  Total Requests:    $($metrics.totalRequests)" -ForegroundColor Gray
    Write-Host "  Allowed:           $($metrics.allowedRequests)" -ForegroundColor Green
    Write-Host "  Denied:            $($metrics.deniedRequests)" -ForegroundColor Red
    Write-Host "  Block Rate:        $([math]::Round($metrics.blockRatePercent, 2))%" -ForegroundColor Yellow
    Write-Host "  Lua Success:       $($metrics.luaScriptSuccesses)" -ForegroundColor Green
    Write-Host "  Lua Failures:      $($metrics.luaScriptFailures)" -ForegroundColor $(if ($metrics.luaScriptFailures -gt 0) { "Red" } else { "Green" })
} catch {
    Write-Host "  Could not fetch metrics" -ForegroundColor Yellow
}

# ============================================================================
# DOCKER CONTAINER STATUS
# ============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "                 CONTAINER STATUS" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Checking Docker containers..." -ForegroundColor Yellow

try {
    docker-compose ps --format table
} catch {
    Write-Host "  Could not fetch container status" -ForegroundColor Yellow
}

# ============================================================================
# RECOMMENDATIONS
# ============================================================================

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "                   RECOMMENDATIONS" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""

$maxThroughput = ($summaryResults | Measure-Object -Property Throughput -Maximum).Maximum

if ($maxThroughput -lt 200) {
    Write-Host "Throughput is lower than expected (<200 req/s)" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "Potential improvements:" -ForegroundColor White
    Write-Host "  1. Increase database connection pool size" -ForegroundColor Gray
    Write-Host "  2. Add config caching (reduce DB queries)" -ForegroundColor Gray
    Write-Host "  3. Increase container resources (CPU/memory)" -ForegroundColor Gray
    Write-Host "  4. Check for slow queries in PostgreSQL" -ForegroundColor Gray
} elseif ($maxThroughput -lt 400) {
    Write-Host "Good performance (200-400 req/s)" -ForegroundColor Green
    Write-Host ""
    Write-Host "Potential improvements:" -ForegroundColor White
    Write-Host "  1. Add config caching to reduce DB load" -ForegroundColor Gray
    Write-Host "  2. Consider Redis connection pooling" -ForegroundColor Gray
} else {
    Write-Host "Excellent performance (400+ req/s)!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Your system is performing very well!" -ForegroundColor White
}

Write-Host ""
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host "                   TEST COMPLETE" -ForegroundColor Cyan
Write-Host "===============================================================" -ForegroundColor Cyan
Write-Host ""