import fs from 'node:fs';

// ================= xlsx 导入：BulkImportController 支持 Excel =================

const p = 'tiangong-erp/tiangong-module-infra/src/main/java/cn/tiangong/module/infra/controller/admin/BulkImportController.java';
let s = fs.readFileSync(p, 'utf8');
if (!s.includes('XSSFWorkbook')) {
  // 1) imports
  s = s.replace('import java.io.IOException;',
    `import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;`);
  // 2) readCsv 改名 readRows：按文件类型分流
  s = s.replace(/private List<String\[\]> readCsv\(MultipartFile file\) throws IOException \{[\s\S]*?\n    \}/,
    `private List<String[]> readRows(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return readExcel(file);
        }
        return readCsv(file);
    }

    /** Excel 读取：所有单元格按文本取值，空行跳过，首行表头跳过 */
    private List<String[]> readExcel(MultipartFile file) throws IOException {
        List<String[]> rows = new ArrayList<>();
        DataFormatter fmt = new DataFormatter();
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                int last = row.getLastCellNum();
                if (last <= 0) continue;
                String[] cells = new String[last];
                boolean allBlank = true;
                for (int i = 0; i < last; i++) {
                    Cell cell = row.getCell(i);
                    String v = cell == null ? "" : fmt.formatCellValue(cell).trim();
                    cells[i] = v;
                    if (!v.isEmpty()) allBlank = false;
                }
                if (allBlank) continue;
                if (rows.isEmpty() && (cells[0].contains("编码") || cells[0].contains("名称"))) continue;
                rows.add(cells);
            }
        }
        return rows;
    }

    private List<String[]> readCsv(MultipartFile file) throws IOException {`);
  // 3) 三个 import 方法里 readCsv(file) 调用改 readRows(file)
  s = s.split('List<String[]> rows = readCsv(file);').join('List<String[]> rows = readRows(file);');
  fs.writeFileSync(p, s, 'utf8');
}
console.log('import xlsx:', fs.readFileSync(p, 'utf8').includes('readRows'));

// ================= xlsx 导出：ExportController 新增 /xlsx =================

const ep = 'tiangong-erp/tiangong-module-infra/src/main/java/cn/tiangong/module/infra/controller/admin/ExportController.java';
let e = fs.readFileSync(ep, 'utf8');
if (!e.includes('XSSFWorkbook')) {
  e = e.replace('import java.nio.charset.StandardCharsets;',
    `import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;`);
  // 追加 /xlsx 端点（类尾）
  const tail = e.lastIndexOf('}');
  const method = `
    /** 通用 Excel 导出：同一白名单，xlsx 格式 */
    @GetMapping("/xlsx")
    @PreAuthorize("@ss.hasPermission('infra:export:csv')")
    public void exportXlsx(@RequestParam("table") String table, HttpServletResponse response) throws Exception {
        if (!WHITELIST.contains(table)) {
            response.setStatus(400);
            response.getWriter().write("table not allowed");
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + table + " WHERE deleted = 0 ORDER BY id LIMIT 100000");

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet(table);
            java.util.List<String> columns = new java.util.ArrayList<>();
            if (!rows.isEmpty()) {
                columns.addAll(rows.get(0).keySet());
            }
            Row head = sheet.createRow(0);
            for (int i = 0; i < columns.size(); i++) {
                Cell c = head.createCell(i);
                c.setCellValue(columns.get(i));
            }
            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                for (int i = 0; i < columns.size(); i++) {
                    Object v = rows.get(r).get(columns.get(i));
                    if (v == null) continue;
                    if (v instanceof Number n) row.createCell(i).setCellValue(n.doubleValue());
                    else if (v instanceof java.time.LocalDateTime dt) row.createCell(i).setCellValue(dt.toString());
                    else if (v instanceof java.time.LocalDate d) row.createCell(i).setCellValue(d.toString());
                    else if (v instanceof Boolean b) row.createCell(i).setCellValue(b);
                    else row.createCell(i).setCellValue(String.valueOf(v));
                }
            }
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + table + ".xlsx");
            wb.write(response.getOutputStream());
        }
    }
}`;
  e = e.slice(0, tail) + method + e.slice(tail);
  fs.writeFileSync(ep, e, 'utf8');
}
console.log('export xlsx:', fs.readFileSync(ep, 'utf8').includes('exportXlsx'));

// ================= POI 白名单权限复用 infra:export:csv（已存在） =================

console.log('gen-v17-backend3 done');
