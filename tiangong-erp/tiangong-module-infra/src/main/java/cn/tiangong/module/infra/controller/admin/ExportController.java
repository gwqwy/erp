package cn.tiangong.module.infra.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通用 CSV 导出：白名单表一键导出（所有行，deleted=0）。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/infra/export")
public class ExportController {

    private final JdbcTemplate jdbcTemplate;

    /** 白名单：表 -> 导出列 */
    private static final Set<String> WHITELIST = Set.of(
            "erp_purchase_order", "erp_purchase_inbound", "erp_sale_order", "erp_sale_outbound",
            "erp_product", "erp_partner", "erp_warehouse", "erp_stock_quant",
            "erp_stock_move", "erp_finance_receivable", "erp_finance_payable",
            "hr_employee", "hr_salary"
    );

    @GetMapping("/csv")
    @PreAuthorize("@ss.hasPermission('infra:export:csv')")
    public void exportCsv(@RequestParam("table") String table, HttpServletResponse response) throws Exception {
        if (!WHITELIST.contains(table)) {
            response.setStatus(400);
            response.getWriter().write("table not allowed");
            return;
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM " + table + " WHERE deleted = 0 ORDER BY id LIMIT 100000");

        StringBuilder csv = new StringBuilder();
        if (!rows.isEmpty()) {
            var headers = rows.get(0).keySet();
            csv.append(String.join(",", headers)).append("\r\n");
            for (Map<String, Object> row : rows) {
                StringBuilder line = new StringBuilder();
                for (var h : headers) {
                    Object v = row.get(h);
                    String cell = v == null ? "" : String.valueOf(v);
                    if (cell.contains(",") || cell.contains("\"") || cell.contains("\n")) {
                        cell = "\"" + cell.replace("\"", "\"\"") + "\"";
                    }
                    line.append(cell).append(',');
                }
                csv.append(line.substring(0, line.length() - 1)).append("\r\n");
            }
        }
        // UTF-8 BOM 让 Excel 正确识别中文
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=" + table + ".csv");
        response.getOutputStream().write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        response.getOutputStream().write(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

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
}
