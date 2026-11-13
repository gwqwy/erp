<template>
  <div class="page-card">
    <el-card shadow="never">
      <div class="table-toolbar">
        <span style="font-weight: 600">MRP 试算</span>
        <span style="color: #909399; font-size: 12px">
          输入成品需求 → 按 BOM 逐层展开（扣减现有库存）→ 得出原材料净需求（结果不落库）
        </span>
      </div>

      <el-form inline class="search-form" @submit.prevent>
        <el-form-item label="仓库">
          <el-select v-model="warehouseId" style="width: 180px">
            <el-option v-for="w in warehouses" :key="w.value" :label="w.label" :value="w.value" />
          </el-select>
        </el-form-item>
      </el-form>

      <el-table :data="demands" border size="small" style="max-width: 640px">
        <el-table-column label="需求产品" min-width="220">
          <template #default="{ row }">
            <el-select v-model="row.productId" filterable style="width: 100%">
              <el-option v-for="p in products" :key="p.value" :label="p.label" :value="p.value" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="需求量" width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0.01" :precision="2" :controls="false" style="width: 100%" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" :disabled="demands.length <= 1"
                       @click="demands.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin: 10px 0">
        <el-button :icon="Plus" size="small" @click="demands.push({ productId: '', qty: 1 })">添加需求行</el-button>
        <el-button type="primary" :loading="loading" @click="run">试算</el-button>
      </div>

      <template v-if="result">
        <el-divider />
        <div class="table-toolbar">
          <span style="font-weight: 600">净需求结果（叶子原材料）</span>
        </div>
        <el-table :data="result.lines" border size="small" style="max-width: 720px">
          <el-table-column prop="productName" label="原材料" min-width="180" />
          <el-table-column prop="stockQty" label="现有库存" width="120" align="right" />
          <el-table-column prop="netQty" label="净需求" width="120" align="right" />
          <el-table-column label="缺口提示" min-width="140">
            <template #default="{ row }">
              <el-tag v-if="Number(row.netQty) > 0" type="danger">需采购/生产 {{ row.netQty }}</el-tag>
              <el-tag v-else type="success">库存充足</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!result.lines.length" description="库存充足，无净需求" :image-size="60" />
      </template>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { mrpCalcApi } from '@/api/mrp'
import { productApi, warehouseApi } from '@/api/bas'

const loading = ref(false)
const warehouseId = ref('')
const warehouses = ref<Array<{ label: string; value: string }>>([])
const products = ref<Array<{ label: string; value: string }>>([])
const demands = ref<Array<{ productId: string; qty: number }>>([{ productId: '', qty: 1 }])
const result = ref<any>(null)

async function run() {
  if (!warehouseId.value) {
    ElMessage.warning('请选择仓库')
    return
  }
  const valid = demands.value.filter(d => d.productId && Number(d.qty) > 0)
  if (!valid.length) {
    ElMessage.warning('请至少填写一行有效需求')
    return
  }
  loading.value = true
  try {
    result.value = await mrpCalcApi.run({
      warehouseId: warehouseId.value,
      demands: valid.map(d => ({ productId: d.productId, qty: Number(d.qty) }))
    })
    if (!result.value?.lines?.length) {
      ElMessage.success('库存充足，无净需求')
    }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  warehouses.value = (await warehouseApi.list({ status: 0 })).map((w: any) => ({ label: w.name, value: w.id }))
  warehouseId.value = warehouses.value[0]?.value || ''
  const pp = await productApi.page({ pageNo: 1, pageSize: 200, status: 0 })
  products.value = (pp?.list ?? []).map((p: any) => ({
    label: `${p.name}${p.spec ? ' / ' + p.spec : ''}`,
    value: p.id
  }))
})
</script>
