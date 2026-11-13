/**
 * 显式页面组件映射：key 与后端 system_menu.component 字段一致。
 * 不用 import.meta.glob——避免 dev 下新增文件后 glob 清单不刷新导致路由注册失败。
 */
import login from '@/views/login/index.vue'
import dashboard from '@/views/dashboard/index.vue'

import systemUser from '@/views/system/user/index.vue'
import systemRole from '@/views/system/role/index.vue'
import systemMenu from '@/views/system/menu/index.vue'
import systemDept from '@/views/system/dept/index.vue'
import systemDict from '@/views/system/dict/index.vue'

import basProduct from '@/views/bas/product/index.vue'
import basProductCategory from '@/views/bas/product-category/index.vue'
import basUnit from '@/views/bas/unit/index.vue'
import basPartner from '@/views/bas/partner/index.vue'
import basWarehouse from '@/views/bas/warehouse/index.vue'
import basSerialRule from '@/views/bas/serial-rule/index.vue'

import purOrder from '@/views/pur/order/index.vue'
import purInbound from '@/views/pur/inbound/index.vue'
import salOrder from '@/views/sal/order/index.vue'
import salOutbound from '@/views/sal/outbound/index.vue'
import stkQuant from '@/views/stk/quant/index.vue'
import stkMove from '@/views/stk/move/index.vue'

import finReceivable from '@/views/fin/receivable/index.vue'
import finPayable from '@/views/fin/payable/index.vue'
import finReceipt from '@/views/fin/receipt/index.vue'
import finPayment from '@/views/fin/payment/index.vue'
import finVoucher from '@/views/fin/voucher/index.vue'
import finVoucherTemplate from '@/views/fin/voucher-template/index.vue'

import mrpBom from '@/views/mrp/bom/index.vue'
import mrpWorkOrder from '@/views/mrp/work-order/index.vue'
import mrpMaterialReq from '@/views/mrp/material-req/index.vue'
import mrpCalc from '@/views/mrp/calc/index.vue'
import mrpPlan from '@/views/mrp/plan/index.vue'

import crmLead from '@/views/crm/lead/index.vue'
import crmBusiness from '@/views/crm/business/index.vue'

import hrEmployee from '@/views/hr/employee/index.vue'
import hrLeave from '@/views/hr/leave/index.vue'
import hrSalary from '@/views/hr/salary/index.vue'
import hrAttendance from '@/views/hr/attendance/index.vue'
import hrAttendanceSummary from '@/views/hr/attendance-summary/index.vue'
import hrOvertime from '@/views/hr/overtime/index.vue'
import hrTrip from '@/views/hr/trip/index.vue'
import hrResignation from '@/views/hr/resignation/index.vue'
import hrRewardPenalty from '@/views/hr/reward-penalty/index.vue'
import hrContract from '@/views/hr/contract/index.vue'
import hrPayroll from '@/views/hr/payroll/index.vue'

import bpmMyTasks from '@/views/bpm/my-tasks/index.vue'
import bpmConfig from '@/views/bpm/config/index.vue'
import reportBalance from '@/views/report/balance/index.vue'
import reportProfit from '@/views/report/profit/index.vue'
import moreBatchTrace from '@/views/more/batch-trace/index.vue'
import moreQualityCheck from '@/views/more/quality-check/index.vue'
import moreDynamicForm from '@/views/more/dynamic-form/index.vue'


import stkTransfer from '@/views/stk/transfer/index.vue'
import stkCheck from '@/views/stk/check/index.vue'
import stkAlert from '@/views/stk/alert/index.vue'
import stkOtherStock from '@/views/stk/other-stock/index.vue'
import salReturn from '@/views/sal/return/index.vue'
import purReturn from '@/views/pur/return/index.vue'
import finExpense from '@/views/fin/expense/index.vue'
import finAging from '@/views/fin/aging/index.vue'
import moreNotice from '@/views/more/notice/index.vue'
import systemOperLog from '@/views/system/oper-log/index.vue'

import basLocation from '@/views/bas/location/index.vue'
import basPartnerPrice from '@/views/bas/partner-price/index.vue'
import finInvoice from '@/views/fin/invoice/index.vue'
import finReceivablePlan from '@/views/fin/receivable-plan/index.vue'
import finPeriodClose from '@/views/fin/period-close/index.vue'
import hrRecruit from '@/views/hr/recruit/index.vue'
import hrTraining from '@/views/hr/training/index.vue'
import hrPerformance from '@/views/hr/performance/index.vue'
import moreFormData from '@/views/more/form-data/index.vue'
import moreFileCenter from '@/views/more/file-center/index.vue'
import imIndex from '@/views/im/index.vue'

import reportRankSale from '@/views/report/rank-sale/index.vue'
import reportRankPurchase from '@/views/report/rank-purchase/index.vue'
import reportStockAbc from '@/views/report/stock-abc/index.vue'
import reportCustomerStatement from '@/views/report/customer-statement/index.vue'
import infraImport from '@/views/infra/import/index.vue'

export const viewModules: Record<string, any> = {
  'login/index': login,
  'dashboard/index': dashboard,
  'system/user/index': systemUser,
  'system/role/index': systemRole,
  'system/menu/index': systemMenu,
  'system/dept/index': systemDept,
  'system/dict/index': systemDict,
  'bas/product/index': basProduct,
  'bas/product-category/index': basProductCategory,
  'bas/unit/index': basUnit,
  'bas/partner/index': basPartner,
  'bas/warehouse/index': basWarehouse,
  'bas/serial-rule/index': basSerialRule,
  'pur/order/index': purOrder,
  'pur/inbound/index': purInbound,
  'sal/order/index': salOrder,
  'sal/outbound/index': salOutbound,
  'stk/quant/index': stkQuant,
  'stk/move/index': stkMove,
  'fin/receivable/index': finReceivable,
  'fin/payable/index': finPayable,
  'fin/receipt/index': finReceipt,
  'fin/payment/index': finPayment,
  'fin/voucher/index': finVoucher,
  'fin/voucher-template/index': finVoucherTemplate,
  'mrp/bom/index': mrpBom,
  'mrp/work-order/index': mrpWorkOrder,
  'mrp/material-req/index': mrpMaterialReq,
  'mrp/calc/index': mrpCalc,
  'mrp/plan/index': mrpPlan,
  'crm/lead/index': crmLead,
  'crm/business/index': crmBusiness,
  'hr/employee/index': hrEmployee,
  'hr/leave/index': hrLeave,
  'hr/salary/index': hrSalary,
  'hr/attendance/index': hrAttendance,
  'hr/attendance-summary/index': hrAttendanceSummary,
  'hr/overtime/index': hrOvertime,
  'hr/trip/index': hrTrip,
  'hr/resignation/index': hrResignation,
  'hr/reward-penalty/index': hrRewardPenalty,
  'hr/contract/index': hrContract,
  'hr/payroll/index': hrPayroll,
  'bpm/my-tasks/index': bpmMyTasks,
  'bpm/config/index': bpmConfig,
  'report/balance/index': reportBalance,
  'report/profit/index': reportProfit,
  'more/batch-trace/index': moreBatchTrace,
  'more/quality-check/index': moreQualityCheck,
  'more/dynamic-form/index': moreDynamicForm,
  'stk/transfer/index': stkTransfer,
  'stk/check/index': stkCheck,
  'stk/alert/index': stkAlert,
  'stk/other-stock/index': stkOtherStock,
  'sal/return/index': salReturn,
  'pur/return/index': purReturn,
  'fin/expense/index': finExpense,
  'fin/aging/index': finAging,
  'more/notice/index': moreNotice,
  'system/oper-log/index': systemOperLog,

  'bas/location/index': basLocation,
  'bas/partner-price/index': basPartnerPrice,
  'fin/invoice/index': finInvoice,
  'fin/receivable-plan/index': finReceivablePlan,
  'fin/period-close/index': finPeriodClose,
  'hr/recruit/index': hrRecruit,
  'hr/training/index': hrTraining,
  'hr/performance/index': hrPerformance,
  'more/form-data/index': moreFormData,
  'more/file-center/index': moreFileCenter,
  'im/index': imIndex,
  'report/rank-sale/index': reportRankSale,
  'report/rank-purchase/index': reportRankPurchase,
  'report/stock-abc/index': reportStockAbc,
  'report/customer-statement/index': reportCustomerStatement,
  'infra/import/index': infraImport,
}
