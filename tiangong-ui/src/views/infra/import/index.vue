<template>
  <el-card shadow="never">
    <template #header>
      <span>数据导入（CSV，UTF-8 编码，英文逗号分隔，首行表头自动跳过）</span>
    </template>
    <el-tabs v-model="tab">
      <el-tab-pane label="商品导入" name="products">
        <div class="imp-tip">
          列格式：<b>编码,名称,规格,采购价,销售价,安全库存</b><br />
          按编码匹配：已存在则更新名称/规格/价格/安全库存，不存在则新增。
        </div>
      </el-tab-pane>
      <el-tab-pane label="往来单位导入" name="partners">
        <div class="imp-tip">
          列格式：<b>名称,类型(customer|supplier|both),联系人,电话,地址,备注</b><br />
          按名称匹配：已存在则更新联系人/电话/地址，不存在则新增。
        </div>
      </el-tab-pane>
      <el-tab-pane label="库存期初导入" name="stock-openings">
        <div class="imp-tip">
          列格式：<b>仓库编码,商品编码,数量,成本价</b><br />
          按仓库+商品定位库存：已存在则累加数量，不存在则新建期初批次（INIT）。
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-upload :show-file-list="false" :auto-upload="false" accept=".csv,.txt" :on-change="onPick">
      <el-button type="primary" :loading="uploading">选择 CSV 并导入</el-button>
    </el-upload>

    <el-alert v-if="result" :type="result.fail === 0 ? 'success' : 'warning'" :closable="false" style="margin-top:12px">
      <div>共 {{ result.total }} 行：成功 {{ result.success }}，失败 {{ result.fail }}</div>
      <div v-for="e in result.errors" :key="e" style="color:#e6a23c">{{ e }}</div>
    </el-alert>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { bulkImportApi } from '@/api/infra'

const tab = ref('products')
const uploading = ref(false)
const result = ref<any>(null)

async function onPick(file: any) {
  const raw: File = file.raw
  if (!raw) return
  uploading.value = true
  result.value = null
  try {
    const res: any = await bulkImportApi.upload(tab.value as any, raw)
    result.value = res
    if (res.fail === 0) ElMessage.success('导入成功：' + res.success + ' 条')
    else ElMessage.warning('成功 ' + res.success + ' 条，失败 ' + res.fail + ' 条')
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped>
.imp-tip { color: #909399; font-size: 13px; line-height: 1.9; margin-bottom: 12px; }
</style>
