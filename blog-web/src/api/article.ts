import request from '../utils/request'

export function saveArticle(data: unknown) {
  return request({
    url: '/article/save',
    method: 'post',
    data
  })
}

/** 管理端编辑用（需登录） */
export function getAdminArticleDetail(id: number | string) {
  return request({
    url: `/article/admin/${id}`,
    method: 'get'
  })
}

/** 前台阅读（无需登录） */
export function getPortalArticleDetail(id: number | string) {
  return request({
    url: `/article/portal/${id}`,
    method: 'get'
  })
}

export function getPortalArticles(params: Record<string, unknown>) {
  return request({
    url: '/article/portal/list',
    method: 'get',
    params
  })
}

export function getAdminArticles(params: Record<string, unknown>) {
  return request({
    url: '/article/admin/list',
    method: 'get',
    params
  })
}

export function deleteArticle(id: number | string) {
  return request({
    url: `/article/${id}`,
    method: 'delete'
  })
}

/** 草稿 → 已发布（列表一键发布） */
export function publishArticleAdmin(id: number | string) {
  return request({
    url: `/article/admin/${id}/publish`,
    method: 'post'
  })
}

/** 已发布 → 草稿（前台下架） */
export function unpublishArticleAdmin(id: number | string) {
  return request({
    url: `/article/admin/${id}/unpublish`,
    method: 'post'
  })
}

/** 置顶 / 取消置顶 */
export function setArticlePinned(id: number | string, pinned: boolean) {
  return request({
    url: `/article/admin/${id}/pinned`,
    method: 'post',
    params: { pinned }
  })
}

export function likePortalArticle(id: number | string) {
  return request({
    url: `/article/portal/${id}/like`,
    method: 'post'
  })
}

export function getRelatedArticles(id: number | string, limit = 5) {
  return request({
    url: `/article/portal/related/${id}`,
    method: 'get',
    params: { limit }
  })
}

/** 管理端：MiniMax 生成摘要（需登录与后端启用 AI） */
export function aiGenerateSummary(data: { title?: string; content: string }) {
  return request({
    url: '/admin/article/ai/summary',
    method: 'post',
    data,
    timeout: 120000
  })
}

/** 管理端：MiniMax 标题建议 */
export function aiSuggestTitles(data: { title?: string; content: string }) {
  return request({
    url: '/admin/article/ai/titles',
    method: 'post',
    data,
    timeout: 120000
  })
}

/** 管理端：MiniMax 文生图，仅返回 Base64 预览（不上传图床） */
export function aiGenerateCoverPreview(data: { title: string; summary?: string }) {
  return request({
    url: '/admin/article/ai/cover/preview',
    method: 'post',
    data,
    timeout: 200000
  })
}

/** 管理端：将预览图上传到闪电图床，返回封面 URL */
export function aiConfirmCoverUpload(data: { imageBase64: string }) {
  return request({
    url: '/admin/article/ai/cover/confirm',
    method: 'post',
    data,
    timeout: 120000
  })
}
