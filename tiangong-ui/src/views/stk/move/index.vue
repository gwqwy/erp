<template>
  <div class="page-card">
    <el-card shadow="never">
      <el-form inline class="search-form" @submit.prevent>
        <el-form-item label="来源单号">
          <el-input v-model="bizNo" placeholder="如 PO2026110001" clearable style="width: 180px" />
        </el-form-item>
        <el-form-item label="方向">
          <el-select v-model="direction" clearable placeholder="全部" style="width: 110px">
            <el-option label="入库" :value="1" />
            <el-option label="出库" :value="-1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="loadList">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <span style="font-weight: 600">库存流水</span>
      </div>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="moveNo" label="流水号" width="160" />
        <el-table-column prop="moveTime" label="过账时间" width="170">
          <template #default="{ row }">{{ (row.moveTime || '').replace('T', ' ').slice(0, 19) }}</template>
        </el-table-column>
        <el-table-column prop="bizType" label="业务" width="110">
          <template #default="{ row }">
            <el-tag :type="row.direction === 1 ? 'success' : 'danger'" effect="plain">
              {{ bizTypeLabel(row.bizType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bizNo" label="来源单号" width="150" />
        <el-table-column prop="productName" label="产品" min-width="150" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column label="数量" width="110" align="right">
          <template #default="{ row }">
            <span :style="{ color: row.direction === 1 ? '#67c23a' : '#f56c6c' }">
              {{ row.direction === 1 ? '+' : '' }}{{ row.qty }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="成本单价" width="110" align="right" />
        <el-table-column prop="amount" label="成本金额" width="120" align="right" />
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

const BIZ_TYPES: Record<string, string> = {
  purchase_in: '采购入库',
  sale_out: '销售出库',
  produce_in: '完工入库',
  material_out: '生产领料',
  other_in: '其他入库',
  other_out: '其他出库',
  check_in: '盘盈入库',
  check_out: '盘亏出库'
}

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const pageNo = ref(1)
const pageSize = ref(20)
const bizNo = ref('')
const direction = ref<number | undefined>(undefined)

function bizTypeLabel(t: string) {
  return BIZ_TYPES[t] ?? t
}

async function loadList() {
  loading.value = true
  try {
    const res = await stockApi.movePage({
      pageNo: pageNo.value, pageSize: pageSize.value,
      bizNo: bizNo.value || undefined, direction: direction.value
    })
    list.value = res?.list ?? []
    total.value = Number(res?.total ?? 0)
  } finally {
    loading.value = false
  }
}

function reset() {
  bizNo.value = ''
  direction.value = undefined
  pageNo.value = 1
  loadList()
}

onMounted(loadList)
</script>
