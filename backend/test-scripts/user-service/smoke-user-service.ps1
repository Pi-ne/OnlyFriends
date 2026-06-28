param(
    [string]$BaseUrl = "http://localhost:8081/api/v1",
    [string]$InternalBaseUrl = "http://localhost:8081/internal",
    [string]$Email = ("user" + (Get-Date -Format "yyyyMMddHHmmss") + "@example.com"),
    [string]$Password = "Abc123456",
    [string]$Nickname = ("趣聚测试" + (Get-Random -Maximum 99999)),
    [string]$ActivationToken = ""
)

$ErrorActionPreference = "Stop"

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
Write-Host "Email: $Email"
Write-Host "Nickname: $Nickname"

$register = Invoke-Json -Method "Post" -Url "$BaseUrl/auth/register" -Body @{
    email = $Email
    password = $Password
    nickname = $Nickname
}
$userId = $register.data.userId
Write-Host "Registered userId=$userId"

if ([string]::IsNullOrWhiteSpace($ActivationToken)) {
    Write-Host ""
    Write-Host "Registration succeeded. Check user-service logs for activation token, then rerun with:"
    Write-Host ".\test-scripts\user-service\smoke-user-service.ps1 -Email `"$Email`" -Password `"$Password`" -Nickname `"$Nickname`" -ActivationToken `"TOKEN`""
    exit 0
}

Invoke-Json -Method "Get" -Url "$BaseUrl/auth/activate?token=$ActivationToken" | Out-Null
Write-Host "Activated account"

$login = Invoke-Json -Method "Post" -Url "$BaseUrl/auth/login" -Body @{
    email = $Email
    password = $Password
}
$accessToken = $login.data.accessToken
$refreshToken = $login.data.refreshToken
$authHeaders = @{ Authorization = "Bearer $accessToken" }
Write-Host "Logged in"

$refresh = Invoke-Json -Method "Post" -Url "$BaseUrl/auth/refresh" -Body @{
    refreshToken = $refreshToken
}
$accessToken = $refresh.data.accessToken
$authHeaders = @{ Authorization = "Bearer $accessToken" }
Write-Host "Refreshed token"

Invoke-Json -Method "Put" -Url "$BaseUrl/users/me/profile" -Headers $authHeaders -Body @{
    nickname = $Nickname
    gender = 1
    birthday = "2000-01-15"
    bio = "热爱户外运动"
    interestTags = @("徒步", "篮球", "桌游")
} | Out-Null
Write-Host "Updated profile"

$me = Invoke-Json -Method "Get" -Url "$BaseUrl/users/me/profile" -Headers $authHeaders
Write-Host "My profile nickname=$($me.data.nickname), credit=$($me.data.creditScore)"

$publicProfile = Invoke-Json -Method "Get" -Url "$BaseUrl/users/$userId" -Headers $authHeaders
Write-Host "Public profile nickname=$($publicProfile.data.nickname)"

$apply = Invoke-Json -Method "Post" -Url "$BaseUrl/merchant/apply" -Headers $authHeaders -Body @{
    merchantName = "趣聚测试商家"
    licenseUrl = "https://example.com/license.jpg"
    focusTags = @("运动", "户外")
}
Write-Host "Merchant applyId=$($apply.data.applyId)"

$applyStatus = Invoke-Json -Method "Get" -Url "$BaseUrl/merchant/apply/status" -Headers $authHeaders
Write-Host "Merchant apply status=$($applyStatus.data.status)"

$valid = Invoke-Json -Method "Get" -Url "$InternalBaseUrl/users/$userId/valid"
$credit = Invoke-Json -Method "Get" -Url "$InternalBaseUrl/users/$userId/credit"
Write-Host "Internal valid=$($valid.data), credit=$($credit.data)"

Write-Host "Smoke test completed."
