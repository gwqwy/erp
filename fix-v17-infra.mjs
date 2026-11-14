import fs from 'node:fs';

// ===== BulkImportController：恢复 readCsv 方法体 =====
{
  const p = 'tiangong-erp/tiangong-module-infra/src/main/java/cn/tiangong/module/infra/controller/admin/BulkImportController.java';
  let s = fs.readFileSync(p, 'utf8');
  const broken = '    private List<String[]> readCsv(MultipartFile file) throws IOException {\n\n    private String cell(';
  const restored = `    private List<String[]> readCsv(MultipartFile file) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        if (content.startsWith("\\uFEFF")) content = content.substring(1);
        List<String[]> rows = new ArrayList<>();
        String[] lines = content.split("\\\\r?\\\\n");
        for (String line : lines) {
            if (line.isBlank()) continue;
            String[] cells = line.split(",", -1);
            for (int i = 0; i < cells.length; i++) cells[i] = cells[i].trim();
            // 表头行：首列含中文表头关键字则跳过
            if (rows.isEmpty() && (cells[0].contains("编码") || cells[0].contains("名称"))) continue;
            rows.add(cells);
        }
        return rows;
    }

    private String cell(`;
  if (s.includes(broken)) {
    s = s.replace(broken, restored);
    fs.writeFileSync(p, s, 'utf8');
    console.log('readCsv restored');
  } else {
    console.log('readCsv pattern miss');
  }
}

// ===== ExportController：去掉多余的 }} =====
{
  const p = 'tiangong-erp/tiangong-module-infra/src/main/java/cn/tiangong/module/infra/controller/admin/ExportController.java';
  let s = fs.readFileSync(p, 'utf8');
  if (s.includes('}}\n')) {
    s = s.replace(/}\s*}\s*$/, '}\n');
    fs.writeFileSync(p, s, 'utf8');
    console.log('export tail fixed');
  } else {
    console.log('export tail ok');
  }
}
