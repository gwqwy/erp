<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline class="search-form" @submit.prevent>
        <el-form-item label="产品">
          <el-select v-model="productId" filterable clearable placeholder="全部产品" style="width: 220px">
            <el-option v-for="p in products" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <span style="font-weight: 600">即时库存</span>
      </div>
      <el-table :data="list" v-loading="loading" border stripe show-summary :summary-method="sumSummary">
        <el-table-column prop="productName" label="产品" min-width="160" />
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column label="批次" width="120">
          <template #default="{ row }">{{ row.batchNo || '—' }}</template>
        </el-table-column>
        <el-table-column prop="qty" label="结存数量" width="120" align="right" />
        <el-table-column prop="lockedQty" label="占用量" width="110" align="right" />
        <el-table-column prop="avgCost" label="加权成本" width="120" align="right" />
        <el-table-column prop="amount" label="库存金额" width="140" align="right" />
      </el-table>

      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 12px; justify-content: flex-end"
        @current-change="loadList"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { Refresh, Search } from '@element-plus/icons-vue'
import { stockApi } from '@/api/stk'
import { productApi } from '@/api/bas'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const productId = ref<string | undefined>(undefined)
const products = ref<Array<{ label: string; value: string }>>([])

async function loadList() {
  loading.value = true
  try {
    const res = await stockApi.quantPage({
      pageNo: pageNo.value, pageSize: pageSize.value, productId: productId.value
    })
    list.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

function reset() {
  productId.value = undefined
  pageNo.value = 1
  loadList()
}

function sumSummary({ columns }: { columns: any[] }) {
  return columns.map((col, i) => {
    if (i === 0) return '合计'
    if (col.property === 'qty') {
      return list.value.reduce((s, r) => s + Number(r.qty || 0), 0).toFixed(2)
    }
    if (col.property === 'amount') {
      return list.value.reduce((s, r) => s + Number(r.amount || 0), 0).toFixed(2)
    }
    return ''
  })
}

onMounted(async () => {
  loadList()
  const pp = await productApi.page({ pageNo: 1, pageSize: 200, status: 0 })
  products.value = (pp?.list ?? []).map((p: any) => ({
    label: `${p.name}${p.spec ? ' / ' + p.spec : ''}`,
    value: p.id
  }))
})
</script>
