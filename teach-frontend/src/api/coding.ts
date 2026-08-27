import request from '../utils/request'
import { getAuthToken } from '../utils/authStorage'

const getBaseURL = () => {
  return (request as any).defaults?.baseURL?.replace(/\/$/, '') ?? 'http://localhost:8820/api'
}

// ====== 编程题 ======

export const addCodingProblem = (data: any) => {
  return request.post('/coding/problem/add', data) as any
}

export const updateCodingProblem = (data: any) => {
  return request.post('/coding/problem/update', data) as any
}

export const getTeacherProblemList = (params?: any) => {
  return request.get('/coding/problem/teacher/list', { params }) as any
}

export const getTeacherProblemDetail = (params: { problemId: number }) => {
  return request.get('/coding/problem/teacher/detail', { params }) as any
}

export const getProblemSubmissions = (params: { problemId: number }) => {
  return request.get('/coding/problem/teacher/submissions', { params }) as any
}

export const getProblemDetail = (params: { problemId: number }) => {
  return request.get('/coding/problem/detail', { params }) as any
}

export const deleteCodingProblem = (problemId: number) => {
  return request.post(`/coding/problem/delete/${problemId}`) as any
}

export const publishCodingProblem = (data: any) => {
  return request.post('/coding/problem/publish', data) as any
}

export const getStudentProblemList = (params?: any) => {
  return request.get('/coding/problem/student/list', { params }) as any
}

// ====== 代码运行/提交 ======

export const runCode = (data: any) => {
  return request.post('/coding/submission/run', data, { timeout: 60000 }) as any
}

export const submitCode = (data: any) => {
  return request.post('/coding/submission/submit', data, { timeout: 240000 }) as any
}

/** 流式提交：返回 fetch Response，由调用方读取 SSE 事件流 */
export const submitCodeStream = (data: any) => {
  const token = getAuthToken()
  const baseURL = getBaseURL()
  return fetch(`${baseURL}/coding/submission/submit/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { 'Authorization': `Bearer ${token}` } : {})
    },
    credentials: 'include',
    body: JSON.stringify(data)
  })
}

export const getSubmissionHistory = (params: { problemId: number }) => {
  return request.get('/coding/submission/history', { params }) as any
}

export const getSubmissionDetail = (params: { submissionId: number }) => {
  return request.get('/coding/submission/detail', { params }) as any
}

export const deleteSubmission = (submissionId: number) => {
  return request.post(`/coding/submission/teacher/delete/${submissionId}`) as any
}

// ====== 当前教师授课班级（用于发布选择） ======
export const getMyTeachingClassList = () => {
  return request.get('/class/my-classes') as any
}
