const request = require('../utils/request')
const auth = require('../utils/auth')

async function login(data) {
  const session = await request({
    url: '/auth/login',
    method: 'POST',
    data
  })
  auth.setSession(session)
  return session
}

function register(data) {
  return request({
    url: '/auth/register',
    method: 'POST',
    data
  })
}

function refresh(refreshToken) {
  return request({
    url: '/auth/refresh',
    method: 'POST',
    data: { refreshToken }
  })
}

function logout() {
  auth.clearSession()
}

module.exports = {
  login,
  register,
  refresh,
  logout
}
