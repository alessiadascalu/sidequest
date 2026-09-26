// Prefixul /api e tratat de proxy-ul Vite (vezi vite.config.js) și trimis la Spring.
const BASE = '/api'

export class ApiError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

async function request(path, options = {}) {
  // Pentru FormData browserul pune singur Content-Type (cu boundary-ul multipart).
  const isForm = options.body instanceof FormData
  let response
  try {
    response = await fetch(`${BASE}${path}`, {
      ...options,
      headers: isForm || !options.body ? options.headers : { 'Content-Type': 'application/json', ...options.headers },
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

/** Login fără parolă: username existent → datele lui (created: false); nou → cont nou (created: true). */
export const loginOrCreate = (username, zoneId) =>
  request('/users', { method: 'POST', body: JSON.stringify({ username, zoneId }) })

export const getTodayQuest = (userId) => request(`/users/${userId}/quest/today`)

/** Dovada e opțională: `photo` (File) și/sau `text`. */
export function completeTodayQuest(userId, { photo, text } = {}) {
  const form = new FormData()
  if (photo) form.append('photo', photo)
  if (text?.trim()) form.append('proofText', text.trim())
  return request(`/users/${userId}/quest/today/complete`, { method: 'POST', body: form })
}

export const getProfile = (userId) => request(`/users/${userId}/profile`)

export const getHistory = (userId) => request(`/users/${userId}/history`)

/** URL-urile de poze vin relative la API ("/uploads/..."). */
export const imageSrc = (url) => (url ? `${BASE}${url}` : null)
