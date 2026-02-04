# Comprehensive load test with multiple scenarios

Write-Host "=== COMPREHENSIVE LOAD TEST ===" -ForegroundColor Cyan
Write-Host ""

# Test scenarios
$scenarios = @(
    @{
        Name = "Light Load"
        Requests = 100
        Concurrency = 5
        Description = "Normal traffic"
    },
    @{
        Name = "Medium Load"
        Requests = 500
        Concurrency = 20
        Description = "Peak traffic"
    },
    @{
        Name = "Heavy Load"
        Requests = 1000
        Concurrency = 50
        Description = "Stress test"
    },
    @{
        Name = "Spike Test"
        Requests = 2000
        Concurrency = 100
        Description = "Sudden traffic spike"
    }
)

$results = @()

foreach ($scenario in $scenarios) {
    Write-Host "`n=== $($scenario.Name) ===" -ForegroundColor Yellow
    Write-Host "Description: $($scenario.Description)"
    Write-Host "Requests: $($scenario.Requests), Concurrency: $($scenario.Concurrency)"
    Write-Host ""

    $start = Get-Date

    # Create jobs
    $jobs = 1..$scenario.Requests | ForEach-Object {
        Start-Job -ScriptBlock {
            param($id, $url)

            $body = @{
                resource = "api/load-test"
                userId = "load-user-$($id % 50)"
            } | ConvertTo-Json

            $start = Get-Date
            try {
                $response = Invoke-WebRequest `
                    -Uri $url `
                    -Method POST `
                    -Headers @{"Content-Type"="application/json"} `
                    -Body $body `
                    -UseBasicParsing `
                    -TimeoutSec 5

                $end = Get-Date
                $latency = ($end - $start).TotalMilliseconds

                return @{
                    Success = $true
                    StatusCode = $response.StatusCode
                    Latency = $latency
                }
            } catch {
                return @{
                    Success = $false
                    Error = $_.Exception.Message
                }
            }
        } -ArgumentList $_, "http://localhost:8080/api/check-limit"

        # Throttle based on concurrency
        if ($_ % $scenario.Concurrency -eq 0) {
            Start-Sleep -Milliseconds 50
        }
    }

    # Wait for completion
    Write-Host "Waiting for completion..." -ForegroundColor Gray
    $jobs | Wait-Job | Out-Null

    $end = Get-Date
    $duration = ($end - $start).TotalSeconds

    # Collect results
    $jobResults = $jobs | Receive-Job
    $jobs | Remove-Job

    # Calculate statistics
    $successCount = ($jobResults | Where-Object { $_.Success -eq $true }).Count
    $failureCount = ($jobResults | Where-Object { $_.Success -eq $false }).Count
    $latencies = ($jobResults | Where-Object { $_.Success -eq $true }).Latency | Sort-Object

    if ($latencies.Count -gt 0) {
        $p50Index = [math]::Floor($latencies.Count * 0.50)
        $p95Index = [math]::Floor($latencies.Count * 0.95)
        $p99Index = [math]::Floor($latencies.Count * 0.99)

        $p50 = $latencies[$p50Index]
        $p95 = $latencies[$p95Index]
        $p99 = $latencies[$p99Index]
        $avgLatency = ($latencies | Measure-Object -Average).Average
    } else {
        $p50 = $p95 = $p99 = $avgLatency = 0
    }

    $throughput = [math]::Round($scenario.Requests / $duration, 2)

    # Display results
    Write-Host ""
    Write-Host "Results:" -ForegroundColor Green
    Write-Host "  Duration:       $([math]::Round($duration, 2))s"
    Write-Host "  Throughput:     $throughput req/s"
    Write-Host "  Success:        $successCount"
    Write-Host "  Failures:       $failureCount"
    Write-Host "  Latency p50:    $([math]::Round($p50, 2))ms"
    Write-Host "  Latency p95:    $([math]::Round($p95, 2))ms"
    Write-Host "  Latency p99:    $([math]::Round($p99, 2))ms"
    Write-Host "  Latency avg:    $([math]::Round($avgLatency, 2))ms"

    # Store results
    $results += @{
        Scenario = $scenario.Name
        Throughput = $throughput
        P50 = [math]::Round($p50, 2)
        P95 = [math]::Round($p95, 2)
        P99 = [math]::Round($p99, 2)
        AvgLatency = [math]::Round($avgLatency, 2)
        SuccessRate = [math]::Round(($successCount / $scenario.Requests) * 100, 2)
    }

    Start-Sleep -Seconds 2
}

# Summary table
Write-Host "`n`n=== PERFORMANCE SUMMARY ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Scenario          | Throughput | p50    | p95    | p99    | Success"
Write-Host "------------------|------------|--------|--------|--------|--------"

foreach ($result in $results) {
    $line = "{0,-17} | {1,8} r/s | {2,5}ms | {3,5}ms | {4,5}ms | {5,6}%" -f `
        $result.Scenario, `
        $result.Throughput, `
        $result.P50, `
        $result.P95, `
        $result.P99, `
        $result.SuccessRate

    Write-Host $line
}

Write-Host ""
Write-Host "=== RESUME NUMBERS ===" -ForegroundColor Green
$bestThroughput = ($results | Sort-Object -Property Throughput -Descending)[0]
$bestLatency = ($results | Sort-Object -Property P95)[0]

Write-Host "Throughput: $($bestThroughput.Throughput)+ requests/second"
Write-Host "Latency p95: <$($bestLatency.P95)ms"
Write-Host "Latency p99: <$($bestLatency.P99)ms"