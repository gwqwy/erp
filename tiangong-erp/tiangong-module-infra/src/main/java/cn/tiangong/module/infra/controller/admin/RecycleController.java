package cn.tiangong.module.infra.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 回收站：白名单表的逻辑删除数据查询与恢复。
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/infra/recycle")
public class RecycleController {

    private final JdbcTemplate jdbcTemplate;

    /** 白名单：表 -> 展示列 */
    private static final Set<String> WHITELIST = Set.of(
            "erp_product", "erp_partner", "erp_warehouse", "crm_lead",
            "hr_employee", "bas_serial_rule", "erp_location"
    );

    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermission('infra:recycle:query')")
    public CommonResult<List<Map<String, Object>>> getDeletedList(@RequestParam("table") String table) {
        if (!WHITELIST.contains(table)) {
            return CommonResult.error(400, "该表不在回收站白名单内");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, name, create_time FROM " + table + " WHERE deleted = 1 ORDER BY id DESC LIMIT 200");
        return CommonResult.success(rows);
    }

    /** 恢复：把 deleted 置回 0 */
    @PostMapping("/restore")
    @PreAuthorize("@ss.hasPermission('infra:recycle:restore')")
    public CommonResult<Boolean> restore(@RequestParam("table") String table,
                                         @RequestParam("id") Long id) {
        if (!WHITELIST.contains(table)) {
            return CommonResult.error(400, "该表不在回收站白名单内");
        }
        jdbcTemplate.update("UPDATE " + table + " SET deleted = 0 WHERE id = ? AND deleted = 1", id);
        return CommonResult.success(true);
    }
}
