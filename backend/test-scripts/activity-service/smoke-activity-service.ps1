param(
    [string]$BaseUrl = "http://localhost:8082/api/v1",
    [Parameter(Mandatory = $true)]
    [string]$AccessToken
)

$ErrorActionPreference = "Stop"
$Headers = @{ Authorization = "Bearer $AccessToken" }

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{}
    )

    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }
    $response = Invoke-RestMethod @params
    if ($response.code -ne 200) {
        throw "Request failed: $Method $Url => code=$($response.code), message=$($response.message)"
    }
    return $response
}

Write-Host "BaseUrl: $BaseUrl"

$templates = Invoke-Json -Method "Get" -Url "$BaseUrl/activities/templates" -Headers $Headers
Write-Host "Templates: $($templates.data.Count)"

$draft = Invoke-Json -Method "Post" -Url "$BaseUrl/activities" -Headers $Headers -Body @{
    title = "周末朝阳公园徒步"
    description = "一起来朝阳公园徒步，感受秋天的美景！"
    tags = @("徒步", "户外", "周末")
    coverUrl = "https://example.com/cover.jpg"
    startTime = "2026-07-20T09:00:00"
    endTime = "2026-07-20T12:00:00"
    regDeadline = "2026-07-19T23:59:59"
    locationName = "朝阳公园南门"
    locationLat = 39.9289
    locationLng = 116.4833
    locationDetail = "北京市朝阳区朝阳公园南路1号"
    maxParticipants = 20
    fee = 0
    locationVerify = 1
    locationRadius = 300
    isDraft = $true
}
$activityId = $draft.data.activityId
Write-Host "Draft activityId=$activityId, status=$($draft.data.statusText)"

$updated = Invoke-Json -Method "Put" -Url "$BaseUrl/activities/$activityId" -Headers $Headers -Body @{
    title = "周末城市轻徒步"
    description = "从公园南门集合，轻松徒步。"
    tags = @("徒步", "户外")
    coverUrl = "https://example.com/cover.jpg"
    startTime = "2026-07-20T09:00:00"
    endTime = "2026-07-20T12:00:00"
    regDeadline = "2026-07-19T23:59:59"
    locationName = "朝阳公园南门"
    locationLat = 39.9289
    locationLng = 116.4833
    locationDetail = "北京市朝阳区朝阳公园南路1号"
    maxParticipants = 20
    fee = 0
    locationVerify = 1
    locationRadius = 300
    isDraft = $true
}
Write-Host "Updated draft status=$($updated.data.statusText)"

$submitted = Invoke-Json -Method "Post" -Url "$BaseUrl/activities/$activityId/submit" -Headers $Headers
Write-Host "Submitted status=$($submitted.data.statusText)"

$list = Invoke-Json -Method "Get" -Url "$BaseUrl/activities?keyword=徒步&status=2&page=1&size=10"
Write-Host "List total=$($list.data.total)"

$detail = Invoke-Json -Method "Get" -Url "$BaseUrl/activities/$activityId"
Write-Host "Detail title=$($detail.data.title), status=$($detail.data.statusText)"

$large = Invoke-Json -Method "Post" -Url "$BaseUrl/activities" -Headers $Headers -Body @{
    title = "大型公益活动"
    description = "人数超过50，进入人工审核。"
    tags = @("公益", "志愿")
    startTime = "2026-08-01T09:00:00"
    endTime = "2026-08-01T12:00:00"
    regDeadline = "2026-07-31T23:59:59"
    locationName = "城市广场"
    locationLat = 39.9000
    locationLng = 116.4000
    locationDetail = "北京市"
    maxParticipants = 80
    fee = 0
    isDraft = $false
}
Write-Host "Large activity status=$($large.data.statusText)"

Write-Host "Activity smoke test completed."
