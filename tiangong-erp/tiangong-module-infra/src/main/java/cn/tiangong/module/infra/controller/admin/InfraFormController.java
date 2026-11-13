package cn.tiangong.module.infra.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.infra.dal.dataobject.InfraFormDataDO;
import cn.tiangong.module.infra.dal.dataobject.InfraFormDefDO;
import cn.tiangong.module.infra.dal.mysql.InfraFormDataMapper;
import cn.tiangong.module.infra.dal.mysql.InfraFormDefMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 低代码动态表单：表单定义（JSON 字段 schema）+ 数据填报。
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/infra/form")
public class InfraFormController {

    private final InfraFormDefMapper formDefMapper;
    private final InfraFormDataMapper formDataMapper;

    // ========== 表单定义 ==========

    @GetMapping("/def/list")
    public CommonResult<List<InfraFormDefDO>> getFormDefList() {
        return CommonResult.success(formDefMapper.selectList(
                new LambdaQueryWrapper<InfraFormDefDO>().orderByDesc(InfraFormDefDO::getId)));
    }

    @GetMapping("/def/get")
    public CommonResult<InfraFormDefDO> getFormDef(@RequestParam("id") Long id) {
        return CommonResult.success(formDefMapper.selectById(id));
    }

    @PostMapping("/def/create")
    @PreAuthorize("@ss.hasPermission('infra:form:create')")
    public CommonResult<Long> createFormDef(@RequestParam("name") @NotBlank String name,
                                            @RequestParam("fieldsJson") @NotBlank String fieldsJson) {
        InfraFormDefDO def = new InfraFormDefDO();
        def.setName(name);
        def.setFieldsJson(fieldsJson);
        def.setStatus(0);
        formDefMapper.insert(def);
        return CommonResult.success(def.getId());
    }

    @PutMapping("/def/update")
    @PreAuthorize("@ss.hasPermission('infra:form:create')")
    public CommonResult<Boolean> updateFormDef(@RequestParam("id") Long id,
                                               @RequestParam(value = "name", required = false) String name,
                                               @RequestParam(value = "fieldsJson", required = false) String fieldsJson,
                                               @RequestParam(value = "status", required = false) Integer status) {
        InfraFormDefDO def = formDefMapper.selectById(id);
        if (def == null) {
            return CommonResult.error(404, "表单定义不存在");
        }
        if (name != null) def.setName(name);
        if (fieldsJson != null) def.setFieldsJson(fieldsJson);
        if (status != null) def.setStatus(status);
        formDefMapper.updateById(def);
        return CommonResult.success(true);
    }

    @DeleteMapping("/def/delete")
    @PreAuthorize("@ss.hasPermission('infra:form:create')")
    public CommonResult<Boolean> deleteFormDef(@RequestParam("id") Long id) {
        Long dataCount = formDataMapper.selectCount(InfraFormDataDO::getFormDefId, id);
        if (dataCount > 0) {
            return CommonResult.error(400, "该表单已存在填报数据，不可删除");
        }
        formDefMapper.deleteById(id);
        return CommonResult.success(true);
    }

    // ========== 表单数据 ==========

    @GetMapping("/data/page")
    public CommonResult<PageResult<Map<String, Object>>> getFormDataPage(PageParam pageParam,
                                                                         @RequestParam("formDefId") Long formDefId) {
        PageResult<InfraFormDataDO> page = formDataMapper.selectPage(pageParam,
                new LambdaQueryWrapper<InfraFormDataDO>()
                        .eq(InfraFormDataDO::getFormDefId, formDefId)
                        .orderByDesc(InfraFormDataDO::getId));
        List<Map<String, Object>> rows = page.getList().stream()
                .map(d -> Map.<String, Object>of(
                        "id", String.valueOf(d.getId()),
                        "data", parseJson(d.getDataJson()),
                        "createTime", String.valueOf(d.getCreateTime())))
                .toList();
        return CommonResult.success(PageResult.of(rows, page.getTotal()));
    }

    @PostMapping("/data/create")
    public CommonResult<Long> createFormData(@RequestParam("formDefId") @NotNull Long formDefId,
                                             @RequestParam("dataJson") @NotBlank String dataJson) {
        InfraFormDataDO data = new InfraFormDataDO();
        data.setFormDefId(formDefId);
        data.setDataJson(dataJson);
        formDataMapper.insert(data);
        return CommonResult.success(data.getId());
    }

    @DeleteMapping("/data/delete")
    @PreAuthorize("@ss.hasPermission('infra:form:create')")
    public CommonResult<Boolean> deleteFormData(@RequestParam("id") Long id) {
        formDataMapper.deleteById(id);
        return CommonResult.success(true);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    json == null ? "{}" : json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }
}
