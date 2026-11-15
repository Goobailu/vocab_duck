import axios from 'axios'

const api = axios.create({
  baseURL: 'https://vocab-duck.dockhosting.cloud',
  timeout: 90000,
  headers: { 'Content-Type': 'application/json' },
})

export function setAuthToken(token) {
  if (token) {
    api.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    delete api.defaults.headers.common['Authorization']
  }
}

export { api }
export default api
