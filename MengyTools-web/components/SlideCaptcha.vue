<script setup lang="ts">
import { Loader2, RefreshCw, Check, AlertTriangle } from 'lucide-vue-next'

const emit = defineEmits<{
  (e: 'verified', token: string): void
  (e: 'fail', msg: string): void
}>()

const api = useApi()

const WIDTH = 300
const HEIGHT = 150

const loading = ref(false)
const verifying = ref(false)
const captchaId = ref('')
const bgSrc = ref('')
const pieceSrc = ref('')
const pieceY = ref(0)
const pieceSize = ref(46)

// 拖动状态
const dragging = ref(false)
const dragX = ref(0) // 拼图块当前 left
const maxX = computed(() => WIDTH - pieceSize.value)

// 校验结果
const status = ref<'idle' | 'success' | 'fail'>('idle')
const tip = ref('拖动下方滑块完成拼图')

const reset = async () => {
  status.value = 'idle'
  tip.value = '拖动下方滑块完成拼图'
  dragX.value = 0
  await fetchCaptcha()
}

const fetchCaptcha = async () => {
  loading.value = true
  try {
    const res = await api.get<{
      captchaId: string
      background: string
      piece: string
      y: number
      pieceSize: number
      width: number
      height: number
    }>('/v1/portal/auth/captcha/generate')
    captchaId.value = res.captchaId
    bgSrc.value = `data:image/png;base64,${res.background}`
    pieceSrc.value = `data:image/png;base64,${res.piece}`
    pieceY.value = res.y
    pieceSize.value = res.pieceSize || 46
  } catch (e: any) {
    tip.value = e?.message || '验证码加载失败'
    status.value = 'fail'
  } finally {
    loading.value = false
  }
}

// 指针拖动滑块
const onPointerDown = (e: PointerEvent) => {
  if (status.value === 'success' || verifying.value) return
  dragging.value = true
  ;(e.target as HTMLElement).setPointerCapture(e.pointerId)
  moveAt(e.clientX)
}
const onPointerMove = (e: PointerEvent) => {
  if (!dragging.value) return
  moveAt(e.clientX)
}
const onPointerUp = async () => {
  if (!dragging.value) return
  dragging.value = false
  await verify()
}

// 轨道参考元素，将客户端 X 转为相对偏移
const trackRef = ref<HTMLElement | null>(null)
const startXRef = ref(0)
const moveAt = (clientX: number) => {
  if (!trackRef.value) return
  const rect = trackRef.value.getBoundingClientRect()
  const handleW = 40
  let x = clientX - rect.left - handleW / 2
  x = Math.max(0, Math.min(x, maxX.value))
  dragX.value = x
}

const verify = async () => {
  if (!captchaId.value) return
  verifying.value = true
  try {
    const res = await api.post<{ captchaToken: string }>(
      '/v1/portal/auth/captcha/verify',
      { },
      { params: { captchaId: captchaId.value, x: Math.round(dragX.value) } }
    )
    status.value = 'success'
    tip.value = '验证通过'
    emit('verified', res.captchaToken)
  } catch (e: any) {
    status.value = 'fail'
    tip.value = e?.message || '未对齐缺口，请重试'
    emit('fail', tip.value)
    // 失败后自动刷新
    setTimeout(reset, 600)
  } finally {
    verifying.value = false
  }
}

onMounted(fetchCaptcha)
</script>

<template>
  <div class="captcha">
    <div class="captcha-canvas" :style="{ width: WIDTH + 'px', height: HEIGHT + 'px' }">
      <img v-if="bgSrc" :src="bgSrc" class="bg" alt="captcha" draggable="false" />
      <img
        v-if="pieceSrc"
        :src="pieceSrc"
        class="piece"
        :style="{ top: pieceY + 'px', left: dragX + 'px', width: pieceSize + 'px', height: pieceSize + 'px' }"
        alt="piece"
        draggable="false"
      />
      <div v-if="loading" class="mask"><Loader2 :size="22" class="spin" /></div>
    </div>

    <div class="track" ref="trackRef">
      <div class="track-fill" :style="{ width: dragX + 40 + 'px' }" :class="status" />
      <button
        class="handle"
        :class="status"
        :style="{ transform: `translateX(${dragX}px)` }"
        :disabled="status === 'success' || verifying"
        @pointerdown.prevent="onPointerDown"
        @pointermove="onPointerMove"
        @pointerup="onPointerUp"
        @pointercancel="onPointerUp"
      >
        <Check v-if="status === 'success'" :size="18" />
        <AlertTriangle v-else-if="status === 'fail'" :size="18" />
        <template v-else>⇆</template>
      </button>
      <span v-if="!dragging && dragX === 0" class="track-hint">{{ tip }}</span>
    </div>

    <div class="tip" :class="status">
      {{ tip }}
      <button v-if="status !== 'success'" class="refresh" @click="reset" title="刷新">
        <RefreshCw :size="13" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.captcha {
  user-select: none;
}
.captcha-canvas {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  background: var(--bg-hover);
  margin: 0 auto 10px;
}
.bg {
  display: block;
  width: 100%;
  height: 100%;
}
.piece {
  position: absolute;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.35));
  pointer-events: none;
}
.mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.6);
}
.spin {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
.track {
  position: relative;
  height: 40px;
  border-radius: 20px;
  background: var(--bg-hover);
  border: 1px solid var(--border-color);
  overflow: hidden;
}
.track-fill {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: rgba(99, 102, 241, 0.15);
  border-radius: 20px 0 0 20px;
  transition: background 0.2s;
}
.track-fill.success {
  background: rgba(16, 185, 129, 0.2);
}
.track-fill.fail {
  background: rgba(239, 68, 68, 0.18);
}
.handle {
  position: absolute;
  top: 0;
  left: 0;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  border: 1px solid var(--border-color);
  background: var(--bg-color);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: grab;
  font-size: 16px;
  touch-action: none;
  box-shadow: var(--shadow);
}
.handle:active {
  cursor: grabbing;
}
.handle.success {
  background: #10b981;
  color: #fff;
  border-color: #10b981;
}
.handle.fail {
  background: #ef4444;
  color: #fff;
  border-color: #ef4444;
}
.track-hint {
  position: absolute;
  left: 0;
  right: 0;
  text-align: center;
  line-height: 40px;
  font-size: 12px;
  color: var(--text-tertiary);
  pointer-events: none;
}
.tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--text-tertiary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.tip.success {
  color: #10b981;
}
.tip.fail {
  color: #ef4444;
}
.refresh {
  display: inline-flex;
  border: none;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  padding: 0;
}
.refresh:hover {
  color: var(--primary);
}
</style>
