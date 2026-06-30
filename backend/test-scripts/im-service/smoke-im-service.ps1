param(
    [string]$BaseUrl = "http://localhost:8084/api/v1/im",
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
    Write-Host "smoke-im-service.ps1 syntax/options validation passed."
    exit 0
}

$runId = Get-Date -Format "yyyyMMddHHmmss"

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

Write-Host "IM smoke started. BaseUrl=$BaseUrl"

$userA = New-TokenUser -Email "im.a.$runId@example.com" -Nickname ("iA" + $runId.Substring($runId.Length - 8))
$userB = New-TokenUser -Email "im.b.$runId@example.com" -Nickname ("iB" + $runId.Substring($runId.Length - 8))
Write-Host "Users ready: A=$($userA.id), B=$($userB.id)"

Invoke-Json -Method "Post" -Url "http://localhost:8083/api/v1/friends/$($userB.id)/applies" -Headers $userA.headers -Body @{
    message = "im smoke friend"
} | Out-Null
$apply = Invoke-Json -Method "Get" -Url "http://localhost:8083/api/v1/friends/applies?type=received&page=1&size=10" -Headers $userB.headers
$applyId = $apply.data.records[0].applyId
Invoke-Json -Method "Put" -Url "http://localhost:8083/api/v1/friends/applies/$applyId" -Headers $userB.headers -Body @{
    action = 1
    reason = "approved"
} | Out-Null
Write-Host "Friend relation ready"

$privateMsg = Invoke-Json -Method "Post" -Url "$BaseUrl/messages/private" -Headers $userA.headers -Body @{
    receiverId = $userB.id
    msgType = 1
    content = "hello from im smoke"
}
Write-Host "Private message OK, msgId=$($privateMsg.data.msgId)"

$team = Invoke-Json -Method "Post" -Url "http://localhost:8083/api/v1/teams" -Headers $userA.headers -Body @{
    name = "IM Smoke Team $runId"
    description = "im smoke team"
    tags = @("smoke")
    joinType = 0
    maxMembers = 20
}
$teamId = $team.data.teamId
Invoke-Json -Method "Post" -Url "http://localhost:8083/api/v1/teams/$teamId/join" -Headers $userB.headers -Body @{
    message = "join"
} | Out-Null

Invoke-Json -Method "Post" -Url "$BaseUrl/messages/group" -Headers $userA.headers -Body @{
    teamId = $teamId
    msgType = 1
    content = "hello team"
    mentionAll = $true
} | Out-Null
Write-Host "Group message OK"

$conversations = Invoke-Json -Method "Get" -Url "$BaseUrl/conversations" -Headers $userA.headers
Write-Host "Conversations OK, count=$($conversations.data.Count)"

Invoke-Json -Method "Post" -Url "$BaseUrl/conversations/$($privateMsg.data.convId)/read" -Headers $userB.headers -Body @{
    lastReadMsgId = $privateMsg.data.msgId
} | Out-Null
Write-Host "Read receipt OK"

Write-Host "IM smoke test completed."
