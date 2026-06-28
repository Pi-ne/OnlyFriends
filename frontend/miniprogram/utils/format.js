function pad(value) {
  return String(value).padStart(2, '0')
}

function toDate(value) {
  if (!value) return null
  if (value instanceof Date) return value
  return new Date(String(value).replace(/-/g, '/').replace('T', ' '))
}

function formatDateTime(value) {
  const date = toDate(value)
  if (!date || Number.isNaN(date.getTime())) return '时间待定'
  return `${date.getMonth() + 1}月${date.getDate()}日 ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatDate(value) {
  const date = toDate(value)
  if (!date || Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function formatFee(value) {
  const amount = Number(value || 0)
  return amount > 0 ? `${amount.toFixed(2).replace(/\.00$/, '')}元` : '免费'
}

function formatDistance(meters) {
  const value = Number(meters || 0)
  if (!value) return ''
  return value >= 1000 ? `${(value / 1000).toFixed(1)}km` : `${Math.round(value)}m`
}

function normalizePageResult(data) {
  if (Array.isArray(data)) {
    return { list: data, total: data.length, page: 1, size: data.length }
  }
  return {
    list: data && Array.isArray(data.list) ? data.list : [],
    total: Number(data && data.total ? data.total : 0),
    page: Number(data && data.page ? data.page : 1),
    size: Number(data && data.size ? data.size : 20)
  }
}

function splitTags(value) {
  if (Array.isArray(value)) return value.filter(Boolean)
  return String(value || '')
    .split(/[,\s，、]+/)
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 5)
}

module.exports = {
  formatDateTime,
  formatDate,
  formatFee,
  formatDistance,
  normalizePageResult,
  splitTags
}
