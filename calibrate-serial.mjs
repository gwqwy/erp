// 校准 Redis 单号序列：对每个 key，按 DB 当月最大单号 +100 推高
import { execSync } from 'node:child_process';

const MAP = [
  ['tiangong:serial:purchase_order:202611',   "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_purchase_order WHERE no LIKE 'PO202611%'", 'PO'],
  ['tiangong:serial:purchase_inbound:202611', "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_purchase_inbound WHERE no LIKE 'PI202611%'", 'PI'],
  ['tiangong:serial:sale_order:202611',       "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_sale_order WHERE no LIKE 'SO202611%'", 'SO'],
  ['tiangong:serial:sale_outbound:202611',    "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_sale_outbound WHERE no LIKE 'SD202611%'", 'SD'],
  ['tiangong:serial:finance_receipt:202611',  "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_finance_receipt WHERE no LIKE 'SK202611%'", 'SK'],
  ['tiangong:serial:finance_payment:202611',  "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_finance_payment WHERE no LIKE 'FK202611%'", 'FK'],
  ['tiangong:serial:finance_voucher:202611',  "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_finance_voucher WHERE no LIKE 'PZ202611%'", 'PZ'],
  ['tiangong:serial:stock_move:202611',       "SELECT COALESCE(MAX(CAST(RIGHT(move_no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_stock_move WHERE move_no LIKE 'MV202611%'", 'MV'],
  ['tiangong:serial:work_order:202611',       "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_mrp_work_order WHERE no LIKE 'MO202611%'", 'MO'],
  ['tiangong:serial:material_req:202611',     "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_mrp_material_req WHERE no LIKE 'MR202611%'", 'MR'],
  ['tiangong:serial:mrp_plan:202611',         "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_mrp_plan WHERE no LIKE 'PL202611%'", 'PL'],
  ['tiangong:serial:mrp_bom:202611',          "SELECT COALESCE(MAX(CAST(RIGHT(no,4) AS UNSIGNED)),0) FROM tiangong_erp.erp_mrp_bom WHERE no LIKE 'BOM202611%'", 'BOM'],
];

for (const [key, sql] of MAP) {
  const raw = execSync(
    `docker exec tiangong-mysql mysql -uroot -ptiangong123 -N -e "${sql}" 2>nul`,
    { shell: 'cmd.exe' }
  ).toString().trim();
  const max = parseInt(raw || '0', 10) || 0;
  const newVal = max + 100;
  execSync(`docker exec tiangong-redis redis-cli SET "${key}" "${newVal}"`, { shell: 'cmd.exe' });
  console.log(`${key} -> ${newVal} (db max ${max})`);
}
console.log('done');
