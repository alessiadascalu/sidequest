// Adresa backend-ului:
//  - VITE_API_URL setat (producție pe Vercel, sau .env local): cererile merg direct acolo (backend-ul permite CORS);
//  - nesetat (npm run dev): "/api", pe care proxy-ul Vite îl trimite la Spring pe :8080 (vezi vite.config.js).
// Variabila e citită la BUILD: după ce o schimbi pe Vercel, e nevoie de un redeploy.
const BASE = (import.meta.env.VITE_API_URL || '/api').replace(/\/+$/, '')

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
    throw new ApiError(
      0,
      import.meta.env.DEV
        ? 'Nu mă pot conecta la server. Pornește backend-ul (portul 8080).'
        : // Pe Render (plan gratuit) serverul adoarme după 15 min fără trafic și pornește în ~1 minut.
          'Serverul nu răspunde. Dacă nu a fost folosit de un timp, se trezește: reîncearcă în câteva secunde.',
    )
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

// ---------- Grupuri ----------

export const createGroup = (name, creatorUserId) =>
  request('/groups', { method: 'POST', body: JSON.stringify({ name, creatorUserId: Number(creatorUserId) }) })

/** 404 = cod inexistent, 409 = ești deja membru. */
export const joinGroup = (userId, inviteCode) =>
  request('/groups/join', { method: 'POST', body: JSON.stringify({ userId: Number(userId), inviteCode }) })

export const getUserGroups = (userId) => request(`/users/${userId}/groups`)

export const getLeaderboard = (groupId) => request(`/groups/${groupId}/leaderboard`)

/** URL-urile de poze vin relative la API ("/uploads/..."). */
export const imageSrc = (url) => (url ? `${BASE}${url}` : null)
