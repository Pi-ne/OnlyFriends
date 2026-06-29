$env:JWT_SECRET = "replace-with-at-least-32-bytes-random-secret"

$mysqlPort = if ($env:MYSQL_PORT) { $env:MYSQL_PORT } else { "3306" }
$env:MYSQL_PORT = $mysqlPort

$env:USER_DB_USERNAME = "root"
$env:USER_DB_PASSWORD = "onlyfriends_root_password"
$env:USER_DB_URL = "jdbc:mysql://localhost:$mysqlPort/onlyfriends_user?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

$env:ACTIVITY_DB_USERNAME = "root"
$env:ACTIVITY_DB_PASSWORD = "onlyfriends_root_password"
$env:ACTIVITY_DB_URL = "jdbc:mysql://localhost:$mysqlPort/onlyfriends_activity?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

$env:SOCIAL_DB_USERNAME = "root"
$env:SOCIAL_DB_PASSWORD = "onlyfriends_root_password"
$env:SOCIAL_DB_URL = "jdbc:mysql://localhost:$mysqlPort/onlyfriends_social?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

$env:IM_DB_USERNAME = "root"
$env:IM_DB_PASSWORD = "onlyfriends_root_password"
$env:IM_DB_URL = "jdbc:mysql://localhost:$mysqlPort/onlyfriends_im?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

$env:ADMIN_DB_USERNAME = "root"
$env:ADMIN_DB_PASSWORD = "onlyfriends_root_password"
$env:ADMIN_DB_URL = "jdbc:mysql://localhost:$mysqlPort/onlyfriends_admin?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"

$env:USER_SERVICE_BASE_URL = "http://localhost:8081"
$env:ACTIVITY_SERVICE_BASE_URL = "http://localhost:8082"
$env:SOCIAL_SERVICE_BASE_URL = "http://localhost:8083"
$env:IM_SERVICE_BASE_URL = "http://localhost:8084"
$env:ADMIN_SERVICE_BASE_URL = "http://localhost:8085"

$env:USER_SERVICE_URI = "http://localhost:8081"
$env:ACTIVITY_SERVICE_URI = "http://localhost:8082"
$env:SOCIAL_SERVICE_URI = "http://localhost:8083"
$env:IM_SERVICE_URI = "http://localhost:8084"
$env:IM_SERVICE_WS_URI = "ws://localhost:8084"
$env:ADMIN_SERVICE_URI = "http://localhost:8085"

$env:NACOS_ENABLED = "false"
$env:NACOS_CONFIG_ENABLED = "false"
$env:AI_MODE = "local"

Write-Host "Local backend environment variables have been set for this PowerShell process."
