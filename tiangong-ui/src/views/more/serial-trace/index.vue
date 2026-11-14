<template>
  <el-card shadow="never">
    <template #header>
      <div style="display:flex;gap:10px;align-items:center">
        <span>序列号追溯</span>
        <el-input v-model="serialNo" placeholder="输入序列号，如 SN20261114001" clearable style="width:280px"
                  @keyup.enter="trace" />
        <el-button type="primary" :disabled="!serialNo.trim()" @click="trace">追溯</el-button>
      </div>
    </template>

    <div v-if="rows.length">
      <el-descriptions :column="2" border style="margin-bottom:16px">
        <el-descriptions-item label="商品">{{ rows[0].productNo }} · {{ rows[0].productName }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <el-tag :type="Number(rows[rows.length - 1].status) === 1 ? 'success' : 'info'">
            {{ Number(rows[rows.length - 1].status) === 1 ? '在库' : '已出库' }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-steps direction="vertical" :active="rows.length">
        <el-step v-for="r in rows" :key="r.id"
                 :title="Number(r.status) === 1 ? '入库' : '出库'"
                 :description="(Number(r.status) === 1 ? r.inboundNo : r.outboundNo) + ' · ' +
                   fmtTime(Number(r.status) === 1 ? r.inboundTime : r.outboundTime)" />
      </el-steps>
    </div>
    <el-empty v-else-if="searched" description="未找到该序列号（入库/出库单明细备注中以逗号分隔写入序列号即可自动登记）" :image-size="80" />
    <el-empty v-else description="输入序列号开始追溯" :image-size="80" />

    <el-divider>按商品查询在库序列号</el-divider>
    <div style="display:flex;gap:10px;align-items:center;margin-bottom:12px">
      <el-select v-model="productId" filterable placeholder="选择商品" style="width:280px">
        <el-option v-for="p in products" :key="p.value" :label="p.label" :value="p.value" />
      </el-select>
      <el-button :disabled="!productId" @click="loadStock">查询在库</el-button>
      <el-button :disabled="!productId" @click="manualRegister">手工登记</el-button>
    </div>
    <el-table :data="stockRows" border size="small">
      <el-table-column prop="serialNo" label="序列号" min-width="180" />
      <el-table-column label="入库单号" prop="inboundNo" width="180" />
      <el-table-column label="入库时间" width="170">
        <template #default="{ row }">{{ fmtTime(row.inboundTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90" class-name="op-cell">
        <template #default="{ row }">
          <div class="op-group">
            <el-button link type="warning" @click="manualConsume(row)">核销</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { serialApi } from '@/api/stock'
import { productApi } from '@/api/bas'

const serialNo = ref('')
const rows = ref<any[]>([])
const searched = ref(false)
const products = ref<any[]>([])
const productId = ref('')
const stockRows = ref<any[]>([])

onMounted(async () => {
  const page = await productApi.page({ pageNo: 1, pageSize: 300 })
  products.value = (page.list || []).map((p: any) => ({ label: p.no + ' ' + p.name, value: p.id }))
})

async function trace() {
  rows.value = (await serialApi.trace(serialNo.value.trim())) || []
  searched.value = true
}

async function loadStock() {
  stockRows.value = (await serialApi.inStock(productId.value)) || []
}

async function manualRegister() {
  const { value } = await ElMessageBox.prompt('输入序列号（多个用逗号分隔）', '手工登记', { type: 'info' })
  const sns = value.split(/[,，s]+/).filter(Boolean)
  for (const sn of sns) {
    await serialApi.register({ serialNo: sn, productId: productId.value, bizNo: 'MANUAL' })
  }
  ElMessage.success('已登记 ' + sns.length + ' 个序列号')
  await loadStock()
}

async function manualConsume(row: any) {
  await ElMessageBox.confirm('确认核销序列号 ' + row.serialNo + '？', '提示', { type: 'warning' })
  await serialApi.consume({ serialNo: row.serialNo, productId: productId.value, bizNo: 'MANUAL' })
  ElMessage.success('已核销')
  await loadStock()
}

function fmtTime(t: string) {
  return t ? String(t).replace('T', ' ').slice(0, 19) : ''
}
</script>
