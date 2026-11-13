<template>
  <div class="biz-chatter">
    <div class="chatter-title">动态</div>
    <el-timeline v-if="records.length" style="padding-left: 4px">
      <el-timeline-item
        v-for="r in records"
        :key="r.id"
        :timestamp="formatTime(r.createTime)"
        :type="r.msgType === 'system_trace' ? 'primary' : 'success'"
      >
        <div class="chatter-content">
          <span class="chatter-kind">{{ r.msgType === 'system_trace' ? '轨迹' : '评论' }}</span>
          {{ r.content }}
        </div>
      </el-timeline-item>
    </el-timeline>
    <el-empty v-else description="暂无动态" :image-size="48" />

    <div class="chatter-input">
      <el-input v-model="comment" placeholder="写条评论…" @keyup.enter="send" />
      <el-button type="primary" :loading="sending" @click="send">发送</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { chatterApi } from '@/api/common'

const props = defineProps<{ bizType: string; bizId: string }>()

const records = ref<any[]>([])
const comment = ref('')
const sending = ref(false)

async function load() {
  if (!props.bizId) return
  records.value = await chatterApi.list(props.bizType, props.bizId)
}

async function send() {
  if (!comment.value.trim()) return
  sending.value = true
  try {
    await chatterApi.comment({ bizType: props.bizType, bizId: props.bizId, content: comment.value.trim() })
    comment.value = ''
    await load()
  } catch {
    ElMessage.error('评论发送失败')
  } finally {
    sending.value = false
  }
}

function formatTime(t: string) {
  return t ? t.replace('T', ' ').slice(0, 19) : ''
}

onMounted(load)
watch(() => props.bizId, load)
</script>

<style scoped>
.biz-chatter { border-top: 1px solid #e4e7ed; margin-top: 16px; padding-top: 12px; }
.chatter-title { font-weight: 600; margin-bottom: 12px; }
.chatter-content { font-size: 13px; color: #303133; }
.chatter-kind {
  display: inline-block;
  font-size: 11px;
  color: #909399;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  padding: 0 4px;
  margin-right: 6px;
}
.chatter-input { display: flex; gap: 8px; margin-top: 8px; }
</style>
