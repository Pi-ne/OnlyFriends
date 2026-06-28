param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$MysqlContainer = "ququ-mysql",
    [string]$MysqlUser = "root",
    [string]$MysqlPassword = "ququ_root_password",
    [string]$Password = "Abc123456",
    [switch]$SkipAdmin,
    [switch]$ValidateOnly
)

$ErrorActionPreference = "Stop"

function Invoke-Json {
    param(
        [string]$Method,
        [string]$Url,
        [object]$Body = $null,
        [hashtable]$Headers = @{},
        [int[]]$AllowedHttpStatus = @(200)
    )

    $params = @{
        Method = $Method
        Uri = $Url
        Headers = $Headers
    }
    if ($null -ne $Body) {
        $params.ContentType = "application/json; charset=utf-8"
        $params.Body = ($Body | ConvertTo-Json -Depth 12)
    }
    try {
        $response = Invoke-RestMethod @params
    } catch {
        $status = [int]$_.Exception.Response.StatusCode
        if ($AllowedHttpStatus -contains $status) {
            return @{ httpStatus = $status }
        }
        Write-Host "Request failed: $Method $Url"
        if ($_.Exception.Response) {
            Write-Host "HTTP status: $status"
            try {
                $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
                Write-Host "Response body: $($reader.ReadToEnd())"
            } catch {
                Write-Host "Response body could not be read."
            }
        }
        throw
    }
    if ($response.PSObject.Properties.Name -contains "code" -and $response.code -ne 200) {
        throw "Request failed: $Method $Url => code=$($response.code), message=$($response.message)"
    }
    return $response
}

function Get-ActivationToken {
    param([string]$Email)
    $query = "SELECT activate_token FROM ququ_user.user WHERE email='$Email';"
    $token = docker exec -i $MysqlContainer mysql "--user=$MysqlUser" "--password=$MysqlPassword" --default-character-set=utf8mb4 -N -e $query
    if ([string]::IsNullOrWhiteSpace($token)) {
        throw "Activation token not found for $Email. Check database initialization and user-service logs."
    }
    return $token.Trim()
}

function New-User {
    param([string]$Suffix)
    $email = "smoke.$Suffix.$RunId@example.com"
    $nickname = "smoke$Suffix$RunId"
    $register = Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/auth/register" -Body @{
        email = $email
        password = $Password
        nickname = $nickname
    }
    $token = Get-ActivationToken -Email $email
    Invoke-Json -Method "Get" -Url "$BaseUrl/api/v1/auth/activate?token=$token" | Out-Null
    $login = Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/auth/login" -Body @{
        email = $email
        password = $Password
    }
    return @{
        id = $login.data.userInfo.userId
        email = $email
        nickname = $nickname
        token = $login.data.accessToken
        headers = @{ Authorization = "Bearer $($login.data.accessToken)" }
    }
}

if ($ValidateOnly) {
    Write-Host "backend-smoke.ps1 syntax/options validation passed."
    exit 0
}

$RunId = Get-Date -Format "yyyyMMddHHmmss"
Write-Host "Backend smoke started. BaseUrl=$BaseUrl RunId=$RunId"

$publicList = Invoke-Json -Method "Get" -Url "$BaseUrl/api/v1/activities?page=1&size=1"
Write-Host "Gateway public activity list OK, total=$($publicList.data.total)"

$internalBlocked = Invoke-Json -Method "Get" -Url "$BaseUrl/internal/users/1/valid" -AllowedHttpStatus @(403, 404)
Write-Host "Gateway internal path block OK, status=$($internalBlocked.httpStatus)"

$userA = New-User -Suffix "a"
$userB = New-User -Suffix "b"
Write-Host "Users ready: A=$($userA.id), B=$($userB.id)"

Invoke-Json -Method "Put" -Url "$BaseUrl/api/v1/users/me/profile" -Headers $userA.headers -Body @{
    gender = 1
    bio = "backend smoke user A"
    interestTags = @("hiking", "boardgame")
} | Out-Null

$activity = Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/activities" -Headers $userA.headers -Body @{
    title = "Smoke Activity $RunId"
    description = "Backend smoke test activity."
    tags = @("smoke", "hiking")
    coverUrl = "https://example.com/smoke.jpg"
    startTime = "2026-07-20T09:00:00"
    endTime = "2026-07-20T12:00:00"
    regDeadline = "2026-07-19T18:00:00"
    locationName = "Smoke Park"
    locationLat = 31.2304
    locationLng = 121.4737
    locationDetail = "East gate"
    maxParticipants = 20
    fee = 0
    locationVerify = 0
    locationRadius = 500
    isDraft = $false
}
$activityId = $activity.data.activityId
Write-Host "Activity created: id=$activityId status=$($activity.data.statusText)"

$nearby = Invoke-Json -Method "Get" -Url "$BaseUrl/api/v1/activities/nearby?lat=31.2304&lng=121.4737&radius=2000&page=1&size=10" -Headers $userA.headers
Write-Host "Nearby activity query OK, total=$($nearby.data.total)"

$registration = Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/activities/$activityId/register" -Headers $userB.headers
Write-Host "Activity registration OK, status=$($registration.data.registrationStatusText)"

$qrcode = Invoke-Json -Method "Get" -Url "$BaseUrl/api/v1/activities/$activityId/checkin/qrcode" -Headers $userA.headers
$checkin = Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/activities/$activityId/checkin" -Headers $userB.headers -Body @{
    qrcodeContent = $qrcode.data.qrcodeContent
}
Write-Host "Checkin OK, checkinId=$($checkin.data.checkinId)"

Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/activities/$activityId/comments" -Headers $userB.headers -Body @{
    rating = 5
    content = "Smoke comment."
} | Out-Null
Write-Host "Activity comment OK"

Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/follows/$($userB.id)" -Headers $userA.headers | Out-Null
$friendApply = Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/friends/$($userB.id)/applies" -Headers $userA.headers -Body @{
    message = "Smoke friend apply."
}
Invoke-Json -Method "Put" -Url "$BaseUrl/api/v1/friends/applies/$($friendApply.data.applyId)" -Headers $userB.headers -Body @{
    action = 1
    reason = "approved"
} | Out-Null
Write-Host "Social follow/friend OK"

$team = Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/teams" -Headers $userA.headers -Body @{
    name = "Smoke Team $RunId"
    description = "Backend smoke team."
    tags = @("smoke", "social")
    joinType = 0
    maxMembers = 20
}
$teamId = $team.data.teamId
Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/teams/$teamId/join" -Headers $userB.headers -Body @{
    message = "join smoke team"
} | Out-Null
Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/teams/$teamId/announcements" -Headers $userA.headers -Body @{
    title = "Smoke announcement"
    content = "Welcome."
} | Out-Null
Write-Host "Team flow OK, teamId=$teamId"

$privateMsg = Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/im/messages/private" -Headers $userA.headers -Body @{
    receiverId = $userB.id
    msgType = 1
    content = "hello from smoke"
}
Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/im/messages/group" -Headers $userA.headers -Body @{
    teamId = $teamId
    msgType = 1
    content = "hello team"
    mentionAll = $true
} | Out-Null
Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/im/conversations/$($privateMsg.data.convId)/read" -Headers $userA.headers -Body @{
    lastReadMsgId = $privateMsg.data.msgId
} | Out-Null
Write-Host "IM private/group/read OK"

if (-not $SkipAdmin) {
    $adminLogin = Invoke-Json -Method "Post" -Url "$BaseUrl/api/v1/admin/auth/login" -Body @{
        username = "admin"
        password = "Admin123456"
    }
    $adminHeaders = @{ Authorization = "Bearer $($adminLogin.data.accessToken)" }
    Invoke-Json -Method "Get" -Url "$BaseUrl/api/v1/admin/users?page=1&size=5&email=smoke" -Headers $adminHeaders | Out-Null
    Invoke-Json -Method "Get" -Url "$BaseUrl/api/v1/admin/teams?page=1&size=5&status=1" -Headers $adminHeaders | Out-Null
    Write-Host "Admin gateway role flow OK"
}

Write-Host "Backend smoke completed successfully."
