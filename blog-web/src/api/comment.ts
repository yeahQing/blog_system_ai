import request from '../utils/request'

export function listArticleComments(articleId: number | string) {
  return request({
    url: `/public/comments/article/${articleId}`,
    method: 'get'
  })
}

export function createComment(data: {
  articleId: number
  parentId?: number | null
  nickname: string
  email?: string
  content: string
}) {
  return request({ url: '/public/comments', method: 'post', data })
}

export function likeComment(id: number | string) {
  return request({ url: `/public/comments/${id}/like`, method: 'post' })
}
