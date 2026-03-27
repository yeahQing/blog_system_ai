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
