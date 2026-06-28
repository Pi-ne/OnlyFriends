const request = require('../utils/request')

function getTeams(params) {
  return request({
    url: '/teams',
    data: params
  })
}

function createTeam(data) {
  return request({
    url: '/teams',
    method: 'POST',
    data
  })
}

function getTeamDetail(id) {
  return request({ url: `/teams/${id}` })
}

function joinTeam(id, data) {
  return request({
    url: `/teams/${id}/join`,
    method: 'POST',
    data
  })
}

function leaveTeam(id) {
  return request({
    url: `/teams/${id}/members/me`,
    method: 'DELETE'
  })
}

function getMembers(id) {
  return request({ url: `/teams/${id}/members` })
}

function getAnnouncements(id) {
  return request({ url: `/teams/${id}/announcements` })
}

function getAlbum(id) {
  return request({ url: `/teams/${id}/album` })
}

function getFiles(id) {
  return request({ url: `/teams/${id}/files` })
}

function getVotes(id) {
  return request({ url: `/teams/${id}/votes` })
}

function getScores(id) {
  return request({ url: `/teams/${id}/scores` })
}

module.exports = {
  getTeams,
  createTeam,
  getTeamDetail,
  joinTeam,
  leaveTeam,
  getMembers,
  getAnnouncements,
  getAlbum,
  getFiles,
  getVotes,
  getScores
}
