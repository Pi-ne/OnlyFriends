param(
    [string]$BaseUrl = "http://localhost:8083/api/v1",
    [string]$Password = "Abc123456",
    [switch]$ValidateOnly
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

if ($ValidateOnly) {
    Write-Host "smoke-social-service.ps1 syntax/options validation passed."
    exit 0
}

$runId = Get-Date -Format "yyyyMMddHHmmss"
$userAEmail = "social.a.$runId@example.com"
$userBEmail = "social.b.$runId@example.com"

function New-TokenUser {
    param([string]$Email, [string]$Nickname)
    $register = Invoke-Json -Method "Post" -Url "http://localhost:8081/api/v1/auth/register" -Body @{
        email = $Email
        password = $Password
        nickname = $Nickname.Substring(0, [Math]::Min($Nickname.Length, 20))
    }
    $tokenQuery = "SELECT activate_token FROM onlyfriends_user.user WHERE email='$Email';"
    $activateToken = (docker exec -i onlyfriends-mysql mysql -uroot -ponlyfriends_root_password --default-character-set=utf8mb4 -N -e $tokenQuery).Trim()
    Invoke-Json -Method "Get" -Url "http://localhost:8081/api/v1/auth/activate?token=$activateToken" | Out-Null
    $login = Invoke-Json -Method "Post" -Url "http://localhost:8081/api/v1/auth/login" -Body @{
        email = $Email
        password = $Password
    }
    return @{
        id = $login.data.userInfo.userId
        headers = @{ Authorization = "Bearer $($login.data.accessToken)" }
    }
}

Write-Host "Social smoke started. BaseUrl=$BaseUrl"

$userA = New-TokenUser -Email $userAEmail -Nickname ("sA" + $runId.Substring($runId.Length - 8))
$userB = New-TokenUser -Email $userBEmail -Nickname ("sB" + $runId.Substring($runId.Length - 8))
Write-Host "Users ready: A=$($userA.id), B=$($userB.id)"

Invoke-Json -Method "Post" -Url "$BaseUrl/follows/$($userB.id)" -Headers $userA.headers | Out-Null
$following = Invoke-Json -Method "Get" -Url "$BaseUrl/follows/following?page=1&size=10" -Headers $userA.headers
Write-Host "Follow OK, following total=$($following.data.total)"

$friendApply = Invoke-Json -Method "Post" -Url "$BaseUrl/friends/$($userB.id)/applies" -Headers $userA.headers -Body @{
    message = "social smoke friend apply"
}
Invoke-Json -Method "Put" -Url "$BaseUrl/friends/applies/$($friendApply.data.applyId)" -Headers $userB.headers -Body @{
    action = 1
    reason = "approved"
} | Out-Null
$friends = Invoke-Json -Method "Get" -Url "$BaseUrl/friends?page=1&size=10" -Headers $userA.headers
Write-Host "Friend OK, friends total=$($friends.data.total)"

$team = Invoke-Json -Method "Post" -Url "$BaseUrl/teams" -Headers $userA.headers -Body @{
    name = "Social Smoke Team $runId"
    description = "social smoke team"
    tags = @("smoke", "social")
    joinType = 0
    maxMembers = 20
}
$teamId = $team.data.teamId
Invoke-Json -Method "Post" -Url "$BaseUrl/teams/$teamId/join" -Headers $userB.headers -Body @{
    message = "join team"
} | Out-Null
Invoke-Json -Method "Post" -Url "$BaseUrl/teams/$teamId/announcements" -Headers $userA.headers -Body @{
    title = "Smoke announcement"
    content = "Welcome to team."
} | Out-Null
Write-Host "Team flow OK, teamId=$teamId"

Write-Host "Social smoke test completed."
