import axios from 'axios'
import { clearAuth } from '@/stores/auth'

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
  if (res.config.responseType === 'blob') return res
  const body = res.data as Result<unknown>
  if (body.code !== 0) {
    return Promise.reject(new Error(body.message || 'request failed'))
  }
  return res
}, (error) => {
  if (error.response?.status === 401 && localStorage.getItem('whaa_token')) {
    const redirect = `${window.location.pathname}${window.location.search}${window.location.hash}`
    clearAuth()
    window.location.replace(`/login?redirect=${encodeURIComponent(redirect)}`)
  }
  return Promise.reject(error)
})

export async function getData<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const res = await client.get<Result<T>>(url, { params })
  return res.data.data
}

export async function postData<T>(url: string, data?: unknown): Promise<T> {
  const res = await client.post<Result<T>>(url, data)
  return res.data.data
}

export async function deleteData<T = void>(url: string): Promise<T> {
  const res = await client.delete<Result<T>>(url)
  return res.data.data
}

export async function uploadFile<T>(url: string, file: File): Promise<T> {
  const form = new FormData()
  form.append('file', file)
  const res = await client.post<Result<T>>(url, form, { timeout: 120000 })
  return res.data.data
}

export async function downloadFile(url: string): Promise<Blob> {
  const res = await client.get<Blob>(url, { responseType: 'blob' })
  return res.data
}

export default client
