import axios from 'axios'

export interface Result<T> {
  code: number
  message: string
  data: T
}

const client = axios.create({ baseURL: '/api/v1', timeout: 20000 })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('whaa_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

client.interceptors.response.use(
  (res) => {
    const body = res.data as Result<unknown>
    if (body.code !== 0) {
      return Promise.reject(new Error(body.message || 'error'))
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.removeItem('whaa_token')
    }
    return Promise.reject(err)
  },
)

export async function apiGet<T>(url: string, params?: Record<string, unknown>) {
  const res = await client.get<Result<T>>(url, { params })
  return res.data.data
}

export async function apiPost<T>(url: string, data?: unknown) {
  const res = await client.post<Result<T>>(url, data)
  return res.data.data
}

export async function apiPut<T>(url: string, data?: unknown) {
  const res = await client.put<Result<T>>(url, data)
  return res.data.data
}

export async function apiDelete(url: string) {
  await client.delete(url)
}

export default client
