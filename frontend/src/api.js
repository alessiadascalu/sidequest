// Prefixul /api e tratat de proxy-ul Vite (vezi vite.config.js) și trimis la Spring.
const BASE = '/api'

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(`${BASE}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch {
    throw new ApiError(0, 'Nu mă pot conecta la server. Pornește backend-ul (portul 8080).')
  }

  if (!response.ok) {
    // Backend-ul răspunde cu ProblemDetail: { detail, errors? }
    const problem = await response.json().catch(() => null)
    const fieldErrors = problem?.errors ? Object.values(problem.errors).join('; ') : null
    throw new ApiError(response.status, fieldErrors ?? problem?.detail ?? `Eroare ${response.status}`)
  }
  return response.json()
}

export const createUser = (username, zoneId) =>
  request('/users', { method: 'POST', body: JSON.stringify({ username, zoneId }) })

export const getTodayQuest = (userId) => request(`/users/${userId}/quest/today`)

export const completeTodayQuest = (userId) =>
  request(`/users/${userId}/quest/today/complete`, { method: 'POST' })

export const getProfile = (userId) => request(`/users/${userId}/profile`)
