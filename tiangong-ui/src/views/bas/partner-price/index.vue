<template>
  <CrudPage :config="config" />
</template>

<script setup lang="ts">
import CrudPage, { type CrudConfig } from '@/components/CrudPage.vue'
import { partnerPriceApi, partnerApi } from '@/api/bas'
import { productApi } from '@/api/bas'

const config: CrudConfig = {
  title: '价格策略',
  permission: 'bas:price',
  bodyAsQuery: true,
  api: { page: pagedList, create: partnerPriceApi.create, delete: partnerPriceApi.delete },
  fields: [
    { prop: 'partnerId', label: '往来单位', type: 'select', required: true, inSearch: true, width: 160,
      options: async () => (await partnerApi.page({ pageNo: 1, pageSize: 200 })).list
        .map((p: any) => ({ label: p.name, value: p.id })) },
    { prop: 'productId', label: '商品', type: 'select', required: true, inSearch: true, width: 160,
      options: async () => (await productApi.page({ pageNo: 1, pageSize: 200 })).list
        .map((p: any) => ({ label: p.name, value: p.id })) },
    { prop: 'price', label: '专享价', type: 'number', required: true, width: 120 },
    { prop: 'remark', label: '备注', type: 'textarea', span: 24, inTable: false }
  ]
}

async function pagedList(params: any) {
  const list = await partnerPriceApi.list({ partnerId: params.partnerId, productId: params.productId })
  return { list, total: list.length }
}
</script>
