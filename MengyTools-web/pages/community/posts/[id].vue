<script setup lang="ts">
/**
 * 社区文章详情：正文 + 评论区（楼层 + 楼中楼）+ 互动（点赞/收藏/举报）。
 *
 * 内容安全：评论正文由服务端生成 content_html（先转义再白名单化链接与 @提及），
 * 因此这里可以安全地 v-html 渲染；正文仍沿用门户已验证的 marked 方案。
 */
import { marked } from 'marked'
import { MessageSquare, Eye, Clock, Heart, Star, Flag, Trash2, CornerDownRight } from 'lucide-vue-next'
import { COMMUNITY_SITE } from '~/community-site.config'
import { imageUrl, hasImage } from '~/utils/image'
import { useCommunitySite } from '~/composables/useCommunitySite'
import {
  useCommunity,
  formatCount,
  formatRelativeTime,
  type CommunityComment
} from '~/composables/useCommunity'

definePageMeta({ layout: 'community' })

const route = useRoute()
const router = useRouter()
const { article: fetchArticle, comments: fetchComments, createComment, deleteComment, react, reactionState, report } =
  useCommunity()
const { isLoggedIn, user, init } = useAuth()
const { siteName } = useCommunitySite()

const id = computed(() => String(route.params.id))
const cpage = computed(() => Math.max(Number(route.query.cpage) || 1, 1))
const csort = computed<'asc' | 'desc'>(() => (route.query.csort === 'desc' ? 'desc' : 'asc'))
const showLogin = ref(false)

onMounted(() => init())

const { data: article, error, refresh: refreshArticle } = await useAsyncData(
  () => `community-post-${id.value}`,
  () => fetchArticle(id.value),
  { watch: [id] }
)

const { data: commentPage, refresh: refreshComments } = await useAsyncData(
  () => `community-post-${id.value}-comments-${cpage.value}-${csort.value}`,
  () => fetchComments(id.value, { page: cpage.value, size: 20, sort: csort.value }),
  { watch: [id, cpage, csort] }
)

if (error.value || !article.value) {
  throw createError({ statusCode: 404, statusMessage: '文章不存在或未发布', fatal: true })
}

const htmlContent = computed(() => {
  const a = article.value
  if (!a) return ''
  if (a.contentHtml) return a.contentHtml
  if (!a.content) return ''
  return marked.parse(a.content, { async: false }) as string
})

// ==================== 阅读量埋点 ====================
// 与门户同一套机制：cookie 存访客ID（同一浏览器在门户与社区共用一个访客），
// 后端按「文章+访客ID」24 小时去重，命中才自增 view_count。
const api = useApi()
const visitorId = useCookie<string | undefined>('visitor_id', {
  maxAge: 60 * 60 * 24 * 365,
  sameSite: 'lax'
})
// crypto.randomUUID 在非 HTTPS（http://localhost）下不可用，需兼容回退
const genVisitorId = () => {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return crypto.randomUUID()
  }
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

onMounted(() => {
  const a = article.value
  if (!a) return
  if (!visitorId.value) {
    visitorId.value = genVisitorId()
  }
  api
    .post<boolean>('/v1/track/view', { articleId: a.id, visitorId: visitorId.value })
    .then((counted) => {
      // 本次为有效计数时本地即时 +1，无需刷新页面
      if (counted && article.value) {
        article.value.viewCount = (article.value.viewCount || 0) + 1
      }
    })
    .catch(() => {
      /* 埋点失败不影响阅读 */
    })
})

// ==================== 互动状态 ====================
const reaction = reactive({ like: false, favorite: false, likeCount: 0, favoriteCount: 0 })

const syncReactionState = async () => {
  const a = article.value
  if (!a) return
  reaction.likeCount = a.likeCount ?? 0
  reaction.favoriteCount = a.favoriteCount ?? 0
  if (!isLoggedIn.value) {
    reaction.like = false
    reaction.favorite = false
    return
  }
  try {
    const s = await reactionState('article', a.id)
    reaction.like = s.like
    reaction.favorite = s.favorite
  } catch {
    /* 忽略：状态获取失败不阻断阅读 */
  }
}
watch([isLoggedIn, article], syncReactionState, { immediate: true })

const toggleReaction = async (type: 'like' | 'favorite') => {
  if (!isLoggedIn.value) {
    showLogin.value = true
    return
  }
  const a = article.value
  if (!a) return
  try {
    const res = await react({ targetType: 'article', targetId: a.id, type })
    if (type === 'like') {
      reaction.like = res.active
      reaction.likeCount = res.count
    } else {
      reaction.favorite = res.active
      reaction.favoriteCount = res.count
    }
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '操作失败'
  }
}

// ==================== 评论点赞 ====================
/** 评论点赞状态（进入页面时批量初始化，点击后就地更新） */
const commentLikes = reactive<Record<number, { active: boolean; count: number }>>({})

const likeComment = async (c: CommunityComment) => {
  if (!isLoggedIn.value) {
    showLogin.value = true
    return
  }
  try {
    const res = await react({ targetType: 'comment', targetId: c.id, type: 'like' })
    commentLikes[c.id] = { active: res.active, count: res.count }
    c.likeCount = res.count
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '操作失败'
  }
}

/** 批量拉取「我点过赞的评论」，避免刷新后红心状态丢失 */
const syncCommentLikes = async () => {
  const ids = (commentPage.value?.records ?? []).flatMap((c) => [c.id, ...(c.replies ?? []).map((r) => r.id)])
  if (!isLoggedIn.value || !ids.length) return
  try {
    const liked = await reactedIds('comment', 'like', ids)
    const set = new Set(liked)
    for (const cid of ids) {
      const rec = (commentPage.value?.records ?? []).find((x) => x.id === cid)
      const rep = (commentPage.value?.records ?? []).flatMap((x) => x.replies ?? []).find((x) => x.id === cid)
      const count = rec?.likeCount ?? rep?.likeCount ?? 0
      commentLikes[cid] = { active: set.has(cid), count }
    }
  } catch {
    /* 忽略 */
  }
}
watch([isLoggedIn, commentPage], syncCommentLikes)

// ==================== 发表评论 / 回复 ====================
const content = ref('')
const replyTo = ref<CommunityComment | null>(null)
const submitting = ref(false)
const errMsg = ref('')
const okMsg = ref('')
const COMMENT_MAX = 1000

const startReply = (c: CommunityComment) => {
  if (!isLoggedIn.value) {
    showLogin.value = true
    return
  }
  replyTo.value = c
  errMsg.value = ''
  okMsg.value = ''
}

const cancelReply = () => {
  replyTo.value = null
}

const submit = async () => {
  errMsg.value = ''
  okMsg.value = ''
  const text = content.value.trim()
  if (text.length < 2) {
    errMsg.value = '评论至少 2 个字'
    return
  }
  submitting.value = true
  try {
    const res = await createComment({
      articleId: Number(id.value),
      content: text,
      parentId: replyTo.value?.id
    })
    content.value = ''
    replyTo.value = null
    okMsg.value =
      res?.status === 0
        ? '已提交，内容需经审核后展示'
        : '发表成功'
    await Promise.all([refreshComments(), refreshArticle()])
    await syncReactionState()
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '发表失败，请稍后再试'
  } finally {
    submitting.value = false
  }
}

const removeComment = async (c: CommunityComment) => {
  if (!confirm('确定撤回这条评论吗？')) return
  try {
    await deleteComment(c.id)
    await Promise.all([refreshComments(), refreshArticle()])
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '撤回失败'
  }
}

const canDelete = (c: CommunityComment) => isLoggedIn.value && user.value?.id === c.authorId

/** 本帖作者（作者可编辑/删除自己的帖子） */
const isAuthor = computed(() => isLoggedIn.value && !!user.value?.id && user.value.id === article.value?.authorId)

const removePost = async () => {
  if (!confirm('确定删除这篇帖子吗？删除后不可恢复。')) return
  try {
    await deletePost(Number(id.value))
    router.push('/community/my-posts')
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '删除失败'
  }
}

// ==================== 举报 ====================
const reportTarget = ref<CommunityComment | null>(null)
const reportReason = ref('广告营销')
const reportDetail = ref('')
const reportReasons = ['广告营销', '内容不友善', '垃圾信息', '违法违规', '其他']

const openReport = (c: CommunityComment) => {
  if (!isLoggedIn.value) {
    showLogin.value = true
    return
  }
  reportTarget.value = c
  reportReason.value = '广告营销'
  reportDetail.value = ''
}

const submitReport = async () => {
  const c = reportTarget.value
  if (!c) return
  try {
    await report({ targetType: 'comment', targetId: c.id, reason: reportReason.value, detail: reportDetail.value })
    reportTarget.value = null
    okMsg.value = '举报已提交，感谢反馈'
  } catch (e: any) {
    errMsg.value = e?.data?.message || e?.message || '举报失败'
    reportTarget.value = null
  }
}

// ==================== 其它 ====================
const setSort = (s: 'asc' | 'desc') => {
  router.push({ path: route.path, query: { ...route.query, csort: s, cpage: undefined } })
}
const changePage = (p: number) => {
  router.push({ path: route.path, query: { ...route.query, cpage: p } })
}

useSeoMeta({
  title: () => `${article.value?.title ?? '文章'} - ${siteName.value}`,
  description: () => article.value?.summary || COMMUNITY_SITE.tagline,
  ogTitle: () => article.value?.title ?? '',
  ogDescription: () => article.value?.summary || '',
  ogType: 'article'
})

// SEO：canonical + BlogPosting/BreadcrumbList 结构化数据
const { absolute, jsonLd, toIso } = useCommunitySeo()
useHead(() => {
  const a = article.value
  const postUrl = absolute(`/community/posts/${id.value}`)
  return {
    link: [{ rel: 'canonical', href: postUrl }],
    script: [
      jsonLd({
        '@context': 'https://schema.org',
        '@type': 'BlogPosting',
        headline: a?.title,
        description: a?.summary || undefined,
        url: postUrl,
        inLanguage: COMMUNITY_SITE.locale,
        datePublished: toIso(a?.publishTime || a?.createTime),
        dateModified: toIso(a?.updateTime || a?.publishTime || a?.createTime),
        author: a?.authorId
          ? { '@type': 'Person', name: a.authorName || '匿名', url: absolute(`/community/users/${a.authorId}`) }
          : undefined,
        commentCount: a?.commentCount ?? 0,
        interactionStatistic: [
          {
            '@type': 'InteractionCounter',
            interactionType: 'https://schema.org/CommentAction',
            userInteractionCount: a?.commentCount ?? 0
          },
          {
            '@type': 'InteractionCounter',
            interactionType: 'https://schema.org/LikeAction',
            userInteractionCount: a?.likeCount ?? 0
          }
        ]
      }),
      jsonLd({
        '@context': 'https://schema.org',
        '@type': 'BreadcrumbList',
        itemListElement: [
          { '@type': 'ListItem', position: 1, name: '社区首页', item: absolute('/community') },
          ...(a?.categoryName
            ? [{ '@type': 'ListItem', position: 2, name: a.categoryName }]
            : []),
          { '@type': 'ListItem', position: a?.categoryName ? 3 : 2, name: a?.title || '' }
        ]
      })
    ]
  }
})

const initials = (name?: string) => (name || '?').slice(0, 1).toUpperCase()
</script>

<template>
  <div v-if="article" class="post">
    <CommunityBreadcrumb
      :items="[
        { label: '社区首页', to: '/community' },
        ...(article.categoryName
          ? [{ label: article.categoryName, to: article.categoryId ? `/community?category=${article.categoryId}` : undefined }]
          : []),
        { label: article.title }
      ]"
    />

    <div class="layout">
      <article class="main-col">
        <header class="post-head">
          <h1 class="post-title">
            <span v-if="article.isTop" class="post-badge">置顶</span>{{ article.title }}
          </h1>
          <div class="post-meta">
            <NuxtLink v-if="article.authorId" :to="`/community/users/${article.authorId}`" class="post-author">
              {{ article.authorName || '匿名' }}
            </NuxtLink>
            <span v-else>{{ article.authorName || '匿名' }}</span>
            <span class="sep">·</span>
            <span class="meta-item">
              <Clock :size="13" />{{ formatRelativeTime(article.publishTime || article.createTime) }}
            </span>
            <span class="meta-item"><Eye :size="13" />{{ formatCount(article.viewCount) }} 浏览</span>
            <span class="meta-item">
              <MessageSquare :size="13" />{{ formatCount(article.commentCount) }} 评论
            </span>
          </div>

          <!-- 互动条：点赞 / 收藏 -->
          <div class="actions">
            <button
              type="button"
              class="action-btn"
              :class="{ active: reaction.like }"
              @click="toggleReaction('like')"
            >
              <Heart :size="15" />{{ reaction.like ? '已赞' : '点赞' }}
              <em>{{ formatCount(reaction.likeCount) }}</em>
            </button>
            <button
              type="button"
              class="action-btn"
              :class="{ active: reaction.favorite }"
              @click="toggleReaction('favorite')"
            >
              <Star :size="15" />{{ reaction.favorite ? '已收藏' : '收藏' }}
              <em>{{ formatCount(reaction.favoriteCount) }}</em>
            </button>
            <template v-if="isAuthor">
              <NuxtLink :to="`/community/edit/${id}`" class="action-btn">编辑</NuxtLink>
              <button type="button" class="action-btn" @click="removePost">删除</button>
            </template>
            <NuxtLink to="/community/notifications" class="action-link">我的消息</NuxtLink>
          </div>
        </header>

        <!-- 封面图 -->
        <figure v-if="hasImage(article.cover)" class="post-cover">
          <img :src="imageUrl(article.cover)" :alt="article.title" loading="lazy" />
        </figure>

        <!-- 正文 -->
        <div class="post-body markdown-body" v-html="htmlContent" />

        <!-- 评论区 -->
        <section class="comments">
          <div class="comments-head">
            <h2 class="comments-title">评论 <em>{{ commentPage?.total ?? 0 }}</em></h2>
            <div class="comments-sort">
              <button type="button" class="sort-btn" :class="{ active: csort === 'asc' }" @click="setSort('asc')">
                正序
              </button>
              <button type="button" class="sort-btn" :class="{ active: csort === 'desc' }" @click="setSort('desc')">
                倒序
              </button>
            </div>
          </div>

          <!-- 发表区 -->
          <div class="reply-box">
            <template v-if="!isLoggedIn">
              <p class="reply-hint">登录后即可参与讨论。</p>
              <button type="button" class="reply-login" @click="showLogin = true">登录 / 注册</button>
            </template>
            <template v-else>
              <p v-if="replyTo" class="reply-target">
                <CornerDownRight :size="14" />
                正在回复 <strong>{{ replyTo.authorName }}</strong>
                <button type="button" class="reply-cancel" @click="cancelReply">取消</button>
              </p>
              <textarea
                v-model="content"
                class="reply-input"
                rows="3"
                :maxlength="COMMENT_MAX"
                placeholder="理性讨论，友善发言；支持 @昵称 提及"
              />
              <div class="reply-actions">
                <span class="reply-count">{{ content.length }}/{{ COMMENT_MAX }}</span>
                <button type="button" class="reply-submit" :disabled="submitting" @click="submit">
                  {{ submitting ? '提交中…' : '发表评论' }}
                </button>
              </div>
            </template>
            <p v-if="errMsg" class="msg err">{{ errMsg }}</p>
            <p v-if="okMsg" class="msg ok">{{ okMsg }}</p>
          </div>

          <ul v-if="commentPage?.records?.length" class="floors">
            <li v-for="c in commentPage.records" :key="c.id" class="floor">
              <div class="floor-side">
                <img v-if="c.authorAvatar" :src="imageUrl(c.authorAvatar)" :alt="c.authorName" class="floor-avatar" />
                <span v-else class="floor-avatar floor-avatar-text">{{ initials(c.authorName) }}</span>
              </div>
              <div class="floor-main">
                <div class="floor-meta">
                  <NuxtLink :to="`/community/users/${c.authorId}`" class="floor-author">
                    {{ c.authorName }}
                  </NuxtLink>
                  <span class="floor-floor">#{{ c.floor }}</span>
                  <span class="sep">·</span>
                  <time>{{ formatRelativeTime(c.createTime) }}</time>
                  <button
                    type="button"
                    class="tool-btn like"
                    :class="{ active: commentLikes[c.id]?.active }"
                    @click="likeComment(c)"
                  >
                    <Heart :size="12" />{{ commentLikes[c.id]?.active ? '已赞' : '赞' }}
                    <em v-if="c.likeCount">{{ c.likeCount }}</em>
                  </button>
                </div>
                <!-- content_html 由服务端生成（转义 + 白名单标签），故可安全渲染 -->
                <p class="floor-content" v-html="c.contentHtml || c.content" />

                <div class="floor-tools">
                  <button type="button" class="tool-btn" @click="startReply(c)">回复</button>
                  <button type="button" class="tool-btn" @click="openReport(c)"><Flag :size="12" />举报</button>
                  <button v-if="canDelete(c)" type="button" class="tool-btn danger" @click="removeComment(c)">
                    <Trash2 :size="12" />撤回
                  </button>
                </div>

                <!-- 楼中楼 -->
                <ul v-if="c.replies?.length" class="subreplies">
                  <li v-for="r in c.replies" :key="r.id" class="subreply">
                    <NuxtLink :to="`/community/users/${r.authorId}`" class="subreply-author">
                      {{ r.authorName }}
                    </NuxtLink>
                    <template v-if="r.replyToName">
                      <span class="subreply-to">回复</span>
                      <span class="subreply-to-name">{{ r.replyToName }}</span>
                    </template>
                    <span class="sep">：</span>
                    <span class="subreply-content" v-html="r.contentHtml || r.content" />
                    <time class="subreply-time">{{ formatRelativeTime(r.createTime) }}</time>
                    <div class="subreply-tools">
                      <button
                        type="button"
                        class="tool-btn like"
                        :class="{ active: commentLikes[r.id]?.active }"
                        @click="likeComment(r)"
                      >
                        {{ commentLikes[r.id]?.active ? '已赞' : '赞' }}<em v-if="r.likeCount">{{ r.likeCount }}</em>
                      </button>
                      <button type="button" class="tool-btn" @click="startReply(r)">回复</button>
                      <button v-if="canDelete(r)" type="button" class="tool-btn danger" @click="removeComment(r)">
                        撤回
                      </button>
                    </div>
                  </li>
                </ul>
              </div>
            </li>
          </ul>
          <p v-else class="comments-empty">还没有评论，欢迎登录后参与讨论。</p>

          <CommunityPager
            :current="commentPage?.current ?? 1"
            :pages="commentPage?.pages ?? 0"
            :total="commentPage?.total"
            @change="changePage"
          />
        </section>
      </article>

      <CommunitySidebar class="side-col" />
    </div>

    <!-- 举报弹层 -->
    <div v-if="reportTarget" class="modal-mask" @click.self="reportTarget = null">
      <div class="modal">
        <h3 class="modal-title">举报评论</h3>
        <p class="modal-sub">{{ (reportTarget.content || '').slice(0, 60) }}</p>
        <label class="modal-label">原因</label>
        <select v-model="reportReason" class="modal-select">
          <option v-for="r in reportReasons" :key="r" :value="r">{{ r }}</option>
        </select>
        <label class="modal-label">补充说明（可选）</label>
        <textarea v-model="reportDetail" class="modal-input" rows="3" maxlength="500" />
        <div class="modal-actions">
          <button type="button" class="modal-cancel" @click="reportTarget = null">取消</button>
          <button type="button" class="modal-ok" @click="submitReport">提交举报</button>
        </div>
      </div>
    </div>

    <CommunityAuthModal v-model:visible="showLogin" @logged-in="showLogin = false" />
  </div>
</template>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 260px;
  gap: 20px;
  align-items: start;
}
.main-col {
  min-width: 0;
}
.post-head {
  padding-bottom: 14px;
  border-bottom: 1px solid var(--c-border);
}
.post-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  line-height: 1.4;
}
.post-badge {
  display: inline-block;
  margin-right: 8px;
  padding: 1px 6px;
  border-radius: 3px;
  background: var(--c-accent-soft);
  color: var(--c-accent);
  font-size: 13px;
  font-weight: 500;
  vertical-align: 3px;
}
.post-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 10px;
  color: var(--c-text-faint);
  font-size: 13px;
}
.post-author {
  color: var(--c-text-muted);
  font-weight: 500;
}
.post-author:hover {
  color: var(--c-accent);
}
.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.sep {
  color: var(--c-border-strong);
}

/* 互动条 */
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 30px;
  padding: 0 12px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text-muted);
  font-size: 13.5px;
  cursor: pointer;
}
.action-btn:hover {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.action-btn.active {
  border-color: var(--c-accent);
  background: var(--c-accent-soft);
  color: var(--c-accent);
}
.action-btn em {
  font-style: normal;
  color: var(--c-text-faint);
}
.action-link {
  margin-left: auto;
  color: var(--c-text-muted);
  font-size: 13px;
}
.action-link:hover {
  color: var(--c-accent);
}

.post-cover {
  margin: 16px 0 4px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  overflow: hidden;
  background: var(--c-surface-alt);
}
.post-cover img {
  display: block;
  width: 100%;
  max-height: 380px;
  object-fit: cover;
}

.post-body {
  padding: 18px 0;
  font-size: 15.5px;
  line-height: 1.8;
  overflow-wrap: break-word;
}
.post-body :deep(p) {
  margin: 0 0 14px;
}
.post-body :deep(img) {
  max-width: 100%;
  border-radius: var(--c-radius);
}
.post-body :deep(pre) {
  padding: 12px 14px;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: var(--c-radius);
  overflow-x: auto;
  font-size: 13.5px;
}
.post-body :deep(blockquote) {
  margin: 0 0 14px;
  padding: 6px 14px;
  border-left: 3px solid var(--c-border-strong);
  background: var(--c-surface-alt);
  color: var(--c-text-muted);
}

/* ===== 评论区 ===== */
.comments {
  margin-top: 8px;
  padding-top: 18px;
  border-top: 1px solid var(--c-border);
}
.comments-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.comments-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}
.comments-title em {
  margin-left: 4px;
  color: var(--c-text-faint);
  font-style: normal;
  font-size: 13px;
}
.comments-sort {
  display: flex;
  gap: 4px;
}
.sort-btn {
  padding: 3px 10px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text-muted);
  font-size: 12.5px;
  cursor: pointer;
}
.sort-btn.active {
  border-color: var(--c-accent);
  color: var(--c-accent);
}
.reply-box {
  margin-bottom: 16px;
  padding: 12px 14px;
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
}
.reply-hint {
  margin: 0 0 8px;
  color: var(--c-text-muted);
  font-size: 13.5px;
}
.reply-login {
  height: 30px;
  padding: 0 14px;
  border: 1px solid var(--c-accent);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-accent);
  font-size: 13.5px;
  cursor: pointer;
}
.reply-target {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 0 8px;
  color: var(--c-text-muted);
  font-size: 13px;
}
.reply-cancel {
  margin-left: 8px;
  border: 0;
  background: transparent;
  color: var(--c-accent);
  font-size: 12.5px;
  cursor: pointer;
}
.reply-input {
  width: 100%;
  padding: 8px 10px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text);
  font-size: 14px;
  font-family: inherit;
  line-height: 1.6;
  resize: vertical;
}
.reply-input:focus {
  outline: none;
  border-color: var(--c-accent);
}
.reply-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.reply-count {
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.reply-submit {
  height: 30px;
  padding: 0 16px;
  border: 1px solid var(--c-accent);
  border-radius: var(--c-radius);
  background: var(--c-accent);
  color: #fff;
  font-size: 13.5px;
  cursor: pointer;
}
.reply-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.msg {
  margin: 8px 0 0;
  font-size: 13px;
}
.msg.err {
  color: #d14343;
}
.msg.ok {
  color: #2f9e44;
}

.floors {
  list-style: none;
  margin: 0;
  padding: 0;
}
.floor {
  display: flex;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid var(--c-border);
}
.floor-side {
  flex-shrink: 0;
}
.floor-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
}
.floor-avatar-text {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--c-accent-soft);
  color: var(--c-accent);
  font-size: 14px;
  font-weight: 600;
}
.floor-main {
  min-width: 0;
  flex: 1;
}
.floor-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  color: var(--c-text-faint);
  font-size: 12.5px;
}
.floor-author {
  color: var(--c-text);
  font-weight: 500;
  font-size: 13.5px;
}
.floor-author:hover {
  color: var(--c-accent);
}
.floor-content {
  margin: 6px 0 0;
  color: var(--c-text);
  font-size: 14.5px;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: break-word;
}
.floor-content :deep(a) {
  color: var(--c-accent);
  word-break: break-all;
}
.floor-content :deep(.mention) {
  color: var(--c-accent);
}
.floor-tools {
  display: flex;
  gap: 10px;
  margin-top: 6px;
}
.tool-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--c-text-faint);
  font-size: 12.5px;
  cursor: pointer;
}
.tool-btn:hover {
  color: var(--c-accent);
}
.tool-btn.danger:hover {
  color: #d14343;
}
.tool-btn.like.active {
  color: var(--c-accent);
}
.tool-btn em {
  font-style: normal;
  margin-left: 2px;
  color: var(--c-text-faint);
}
.subreplies {
  list-style: none;
  margin: 10px 0 0;
  padding: 8px 10px;
  background: var(--c-surface-alt);
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
}
.subreply {
  padding: 6px 0;
  font-size: 13.5px;
  line-height: 1.65;
  color: var(--c-text-muted);
}
.subreply + .subreply {
  border-top: 1px dashed var(--c-border);
}
.subreply-author {
  color: var(--c-text);
  font-weight: 500;
}
.subreply-to {
  margin: 0 3px;
  color: var(--c-text-faint);
}
.subreply-content {
  color: var(--c-text);
}
.subreply-time {
  margin-left: 6px;
  color: var(--c-text-faint);
  font-size: 12px;
}
.subreply-tools {
  display: flex;
  gap: 10px;
  margin-top: 2px;
}
.comments-empty {
  padding: 24px 0;
  color: var(--c-text-faint);
  font-size: 13.5px;
}

/* ===== 举报弹层 ===== */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.4);
  padding: 20px;
}
.modal {
  width: 100%;
  max-width: 420px;
  padding: 18px;
  background: var(--c-surface);
  border-radius: var(--c-radius);
  border: 1px solid var(--c-border);
}
.modal-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 600;
}
.modal-sub {
  margin: 0 0 12px;
  color: var(--c-text-muted);
  font-size: 13px;
}
.modal-label {
  display: block;
  margin: 10px 0 4px;
  color: var(--c-text-muted);
  font-size: 13px;
}
.modal-select,
.modal-input {
  width: 100%;
  padding: 7px 10px;
  border: 1px solid var(--c-border);
  border-radius: var(--c-radius);
  background: var(--c-surface);
  color: var(--c-text);
  font-size: 14px;
  font-family: inherit;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
.modal-cancel,
.modal-ok {
  height: 32px;
  padding: 0 16px;
  border-radius: var(--c-radius);
  font-size: 13.5px;
  cursor: pointer;
}
.modal-cancel {
  border: 1px solid var(--c-border);
  background: var(--c-surface);
  color: var(--c-text-muted);
}
.modal-ok {
  border: 1px solid var(--c-accent);
  background: var(--c-accent);
  color: #fff;
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
