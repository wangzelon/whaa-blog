import axios from 'axios'

export interface Result<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

const client = axios.create({ baseURL: '/api/v1', timeout: 15000 })

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('whaa_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

client.interceptors.response.use((res) => {
  const body = res.data as Result<unknown>
  if (body.code !== 0) {
    return Promise.reject(new Error(body.message || 'request failed'))
  }
  return res
})

export async function getData<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await client.get<Result<T>>(url, { params })
  return res.data.data
}

export async function postData<T>(url: string, data?: unknown): Promise<T> {
  const res = await client.post<Result<T>>(url, data)
  return res.data.data
}

export default client
