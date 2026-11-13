<template>
  <div class="print-page">
    <div class="print-toolbar no-print">
      <el-button type="primary" @click="doPrint">打 印</el-button>
      <el-button @click="close">关 闭</el-button>
    </div>

    <div v-if="head" class="print-doc">
      <h2 class="print-title">{{ title }}</h2>
      <div class="print-meta">
        <span>单号：<b>{{ head.no }}</b></span>
        <span>日期：{{ head.orderDate || head.outboundDate || head.inboundDate || '' }}</span>
        <span>状态：{{ statusText }}</span>
      </div>
      <div class="print-meta">
        <span>{{ partnerLabel }}：<b>{{ partnerName }}</b></span>
      </div>
      <table class="print-table">
        <thead>
          <tr>
            <th style="width:50px">#</th>
            <th>商品</th>
            <th style="width:110px">数量</th>
            <th style="width:120px">单价</th>
            <th style="width:130px">金额</th>
            <th>备注</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(it, i) in items" :key="i">
            <td>{{ i + 1 }}</td>
            <td>{{ productName(it.productId) }}</td>
            <td>{{ it.qty }}</td>
            <td>{{ it.price }}</td>
            <td>{{ it.amount ?? (Number(it.qty || 0) * Number(it.price || 0)).toFixed(2) }}</td>
            <td>{{ it.remark || '' }}</td>
          </tr>
        </tbody>
        <tfoot>
          <tr>
            <td colspan="2">合计</td>
            <td>{{ totalQty }}</td>
            <td></td>
            <td>{{ totalAmount }}</td>
            <td></td>
          </tr>
        </tfoot>
      </table>
      <div class="print-remark" v-if="head.remark">备注：{{ head.remark }}</div>
      <div class="print-sign">
        <span>制单人：__________</span>
        <span>审核人：__________</span>
        <span>客户确认：__________</span>
      </div>
    </div>
    <el-empty v-else :description="loadError || '单据不存在'" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { saleOrderApi, saleOutboundApi } from '@/api/sal'
import { purchaseOrderApi, purchaseInboundApi } from '@/api/pur'
import { partnerApi, productApi } from '@/api/bas'

const route = useRoute()
const head = ref<any>(null)
const items = ref<any[]>([])
const partners = ref<any[]>([])
const products = ref<any[]>([])
const loadError = ref('')

const TYPE_MAP: Record<string, { api: () => any; headKey: string; title: string; partnerProp: string; partnerType: 'customer' | 'supplier' }> = {
  sal_order: { api: () => saleOrderApi, headKey: 'order', title: '销售订单', partnerProp: 'customerId', partnerType: 'customer' },
  pur_order: { api: () => purchaseOrderApi, headKey: 'order', title: '采购订单', partnerProp: 'supplierId', partnerType: 'supplier' },
  sal_outbound: { api: () => saleOutboundApi, headKey: 'outbound', title: '销售出库单', partnerProp: 'customerId', partnerType: 'customer' },
  pur_inbound: { api: () => purchaseInboundApi, headKey: 'inbound', title: '采购入库单', partnerProp: 'supplierId', partnerType: 'supplier' }
}

const conf = computed(() => TYPE_MAP[String(route.query.type || '')])
const title = computed(() => conf.value?.title || '单据')
const partnerLabel = computed(() => conf.value?.partnerType === 'supplier' ? '供应商' : '客户')
const partnerName = computed(() => {
  const id = head.value?.[conf.value?.partnerProp || '']
  return partners.value.find(p => String(p.id) === String(id))?.name ?? id ?? ''
})
const statusText = computed(() => ({ 10: '草稿', 20: '审批中', 30: '已审核', 40: '已完成', 50: '已作废', 60: '已冲销' }[Number(head.value?.status)] || head.value?.status))
const totalQty = computed(() => items.value.reduce((s, i) => s + Number(i.qty || 0), 0))
const totalAmount = computed(() => items.value.reduce((s, i) => s + Number(i.amount ?? Number(i.qty || 0) * Number(i.price || 0)), 0).toFixed(2))

function productName(id: any) {
  return products.value.find(p => String(p.id) === String(id))?.name ?? id
}

function close() { window.close() }

function doPrint() { window.print() }

onMounted(async () => {
  const type = String(route.query.type || '')
  const id = String(route.query.id || '')
  const conf = TYPE_MAP[type]
  if (!conf || !id) {
    loadError.value = !conf ? '未知单据类型: ' + type : '缺少单据 id'
    return
  }
  try {
    const [detail, partnerPage, productPage] = await Promise.all([
      conf.api().detail(id),
      partnerApi.page({ pageNo: 1, pageSize: 200 }),
      productApi.page({ pageNo: 1, pageSize: 500 })
    ])
    head.value = detail[conf.headKey]
    items.value = detail.items || []
    partners.value = partnerPage.list || []
    products.value = productPage.list || []
    if (!head.value) loadError.value = '单据数据为空（可能已删除或无权限）'
  } catch (e: any) {
    loadError.value = '加载失败: ' + (e?.message || e)
  }
})
</script>

<style scoped>
.print-page { padding: 24px; background: #f0f2f5; min-height: 100vh; }
.print-toolbar { margin-bottom: 16px; }
.print-doc { background: #fff; margin: 0 auto; padding: 40px 48px; max-width: 860px; box-shadow: 0 2px 8px rgba(0,0,0,.08); }
.print-title { text-align: center; margin: 0 0 18px; letter-spacing: 6px; }
.print-meta { display: flex; gap: 32px; margin-bottom: 8px; font-size: 13px; color: #333; }
.print-table { width: 100%; border-collapse: collapse; margin-top: 14px; font-size: 13px; }
.print-table th, .print-table td { border: 1px solid #333; padding: 7px 9px; text-align: left; }
.print-table th { background: #f5f5f5; text-align: center; }
.print-table tfoot td { font-weight: 700; background: #fafafa; }
.print-remark { margin-top: 12px; font-size: 13px; color: #333; }
.print-sign { display: flex; justify-content: space-between; margin-top: 56px; font-size: 13px; }

@media print {
  .print-page { background: #fff; padding: 0; }
  .no-print { display: none !important; }
  .print-doc { box-shadow: none; max-width: none; padding: 0; }
}
</style>
