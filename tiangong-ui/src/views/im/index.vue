<template>
  <div class="im-page">
    <!-- 左：会话列表 -->
    <el-card class="im-left" shadow="never">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>消息</span>
          <el-button link type="primary" @click="newChatVisible = true">+ 发起会话</el-button>
        </div>
      </template>
      <div v-for="s in sessions" :key="s.id" class="im-session"
           :class="{ active: currentSessionKey === s.sessionKey }" @click="openSession(s)">
        <div class="im-session-name">{{ peerName(s) }}</div>
        <div class="im-session-last">{{ s.lastContent }}</div>
        <div class="im-session-time">{{ (s.lastTime || '').replace('T', ' ').slice(5, 16) }}</div>
      </div>
      <el-empty v-if="!sessions.length" description="暂无会话" :image-size="60" />
    </el-card>

    <!-- 右：聊天窗口 -->
    <el-card class="im-right" shadow="never">
      <template #header>
        <span>{{ currentPeerName || '选择左侧会话开始聊天' }}</span>
      </template>
      <div ref="msgBox" class="im-msgs">
        <div v-for="m in messages" :key="m.id" class="im-msg" :class="{ mine: m.senderId === myId }">
          <div class="im-msg-content">{{ m.content }}</div>
          <div class="im-msg-time">{{ (m.createTime || '').replace('T', ' ').slice(5, 16) }}</div>
        </div>
        <el-empty v-if="!messages.length && currentSessionKey" description="暂无消息" :image-size="60" />
      </div>
      <div class="im-input">
        <el-input v-model="draft" type="textarea" :rows="2" placeholder="回车发送，Shift+回车换行"
                  @keydown.enter.exact.prevent="send" />
        <el-button type="primary" :disabled="!currentPeerId || !draft.trim()" @click="send">发送</el-button>
      </div>
    </el-card>

    <!-- 发起会话 -->
    <el-dialog v-model="newChatVisible" title="发起会话" width="420px">
      <el-select v-model="newChatPeer" placeholder="选择同事" style="width: 100%" filterable>
        <el-option v-for="u in users" :key="u.id" :label="u.nickname || u.username" :value="u.id" />
      </el-select>
      <template #footer>
        <el-button @click="newChatVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!newChatPeer" @click="startChat">开始聊天</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { imApi, imWsUrl } from '@/api/im'
import { userApi } from '@/api/system'
import { getToken } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const sessions = ref<any[]>([])
const messages = ref<any[]>([])
const users = ref<any[]>([])
const myId = ref<string>('')
const currentSessionKey = ref('')
const currentPeerId = ref<string>('')
const currentPeerName = ref('')
const draft = ref('')
const newChatVisible = ref(false)
const newChatPeer = ref<string>('')
const msgBox = ref<HTMLElement>()
let ws: WebSocket | null = null
let heartbeat: any = null

const userNameMap = computed<Record<string, string>>(() => {
  const map: Record<string, string> = {}
  users.value.forEach((u: any) => (map[String(u.id)] = u.nickname || u.username))
  return map
})

function peerName(s: any) {
  const peerId = String(s.userA) === String(myId.value) ? s.userB : s.userA
  return userNameMap.value[String(peerId)] || '用户 ' + peerId
}

async function loadUsers() {
  const page = await userApi.page({ pageNo: 1, pageSize: 200 })
  users.value = page.list || []
}

async function loadSessions() {
  sessions.value = (await imApi.sessions()) || []
}

async function openSession(s: any) {
  currentSessionKey.value = s.sessionKey
  currentPeerId.value = String(s.userA) === String(myId.value) ? String(s.userB) : String(s.userA)
  currentPeerName.value = peerName(s)
  messages.value = (await imApi.history(s.sessionKey)) || []
  await scrollBottom()
}

async function startChat() {
  // 19 位 id 超出 Number 安全范围，用 BigInt 比较排序
  const me = BigInt(myId.value || '0')
  const peer = BigInt(newChatPeer.value)
  const [a, b] = me < peer ? [me, peer] : [peer, me]
  const key = a + '_' + b
  newChatVisible.value = false
  await openSession({ sessionKey: key, userA: a.toString(), userB: b.toString() } as any)
}

async function send() {
  const content = draft.value.trim()
  const receiver = currentPeerId.value
  if (!content || !receiver) return
  draft.value = ''
  // 优先 WebSocket；未连接则 REST 兜底（id 用字符串，19 位 id 超出 Number 安全范围）
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'chat', receiverId: receiver, content }))
  } else {
    await imApi.send(receiver, content)
  }
  // 本地回显 + 刷新会话
  setTimeout(refreshAfterSend, 300)
  async function refreshAfterSend() {
    await loadSessions()
    const fresh = sessions.value.find(s => s.sessionKey === currentSessionKey.value)
    if (fresh) await openSession(fresh)
    else {
      messages.value = (await imApi.history(currentSessionKey.value)) || []
      await scrollBottom()
    }
  }
}

async function scrollBottom() {
  await nextTick()
  if (msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight
}

function connectWs() {
  const token = getToken()
  if (!token) return
  try {
    ws = new WebSocket(imWsUrl())
  } catch {
    return
  }
  ws.onopen = () => {
    heartbeat = setInterval(() => ws && ws.readyState === WebSocket.OPEN && ws.send('ping'), 30000)
  }
  ws.onmessage = e => {
    if (e.data === 'pong') return
    try {
      const payload = JSON.parse(e.data)
      if (payload.type === 'open') {
        // 连接成功，解析自己 id 无法从报文获得，由 sessions 接口补
        return
      }
      if (payload.type === 'chat') {
        if (payload.sessionId === currentSessionKey.value) {
          messages.value.push({
            id: Date.now(),
            senderId: payload.senderId,
            content: payload.content,
            createTime: payload.createTime
          })
          scrollBottom()
        }
        loadSessions()
      }
    } catch {
      // 忽略非 JSON 帧
    }
  }
  ws.onclose = () => {
    if (heartbeat) clearInterval(heartbeat)
    setTimeout(connectWs, 5000)
  }
}

onMounted(async () => {
  await loadUsers()
  await loadSessions()
  // 自己的 id：来自登录态（get-permission-info 缓存于 user store）
  if (!userStore.fetched) {
    await userStore.fetchInfo()
  }
  myId.value = String(userStore.userId)
  connectWs()
})

onBeforeUnmount(() => {
  if (heartbeat) clearInterval(heartbeat)
  if (ws) {
    ws.onclose = null
    ws.close()
    ws = null
  }
})
</script>

<style scoped>
.im-page { display: flex; gap: 12px; height: calc(100vh - 140px); }
.im-left { width: 320px; flex-shrink: 0; overflow-y: auto; }
.im-right { flex: 1; display: flex; flex-direction: column; }
.im-session { padding: 10px 8px; border-bottom: 1px solid #f0f0f0; cursor: pointer; border-radius: 6px; }
.im-session:hover { background: #f5f7fa; }
.im-session.active { background: #ecf5ff; }
.im-session-name { font-weight: 600; font-size: 14px; }
.im-session-last { font-size: 12px; color: #909399; margin-top: 4px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.im-session-time { font-size: 11px; color: #c0c4cc; margin-top: 2px; }
.im-msgs { flex: 1; overflow-y: auto; padding: 8px; background: #fafafa; border-radius: 6px; }
.im-msg { margin: 8px 0; text-align: left; }
.im-msg.mine { text-align: right; }
.im-msg-content { display: inline-block; max-width: 70%; padding: 8px 12px; border-radius: 10px; background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,.06); font-size: 14px; text-align: left; white-space: pre-wrap; word-break: break-all; }
.im-msg.mine .im-msg-content { background: #409eff; color: #fff; }
.im-msg-time { font-size: 11px; color: #c0c4cc; margin-top: 3px; }
.im-input { display: flex; gap: 8px; margin-top: 10px; align-items: flex-end; }
</style>
