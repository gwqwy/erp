package cn.tiangong.module.hr.controller.admin;

import cn.tiangong.framework.web.core.pojo.CommonResult;
import cn.tiangong.module.hr.dal.dataobject.HrPerformanceDO;
import cn.tiangong.module.hr.dal.dataobject.HrRecruitDO;
import cn.tiangong.module.hr.dal.dataobject.HrTrainingDO;
import cn.tiangong.module.hr.dal.mysql.HrPerformanceMapper;
import cn.tiangong.module.hr.dal.mysql.HrRecruitMapper;
import cn.tiangong.module.hr.dal.mysql.HrTrainingMapper;
import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** HR 扩展：招聘 / 培训 / 绩效。 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin-api/hr")
public class HrExtendController {

    private final HrRecruitMapper recruitMapper;
    private final HrTrainingMapper trainingMapper;
    private final HrPerformanceMapper performanceMapper;

    // ========== 招聘 ==========

    @GetMapping("/recruit/list")
    @PreAuthorize("@ss.hasPermission('hr:recruit:query')")
    public CommonResult<List<HrRecruitDO>> getRecruitList() {
        return CommonResult.success(recruitMapper.selectList(
                new LambdaQueryWrapper<HrRecruitDO>().orderByDesc(HrRecruitDO::getId)));
    }

    @PostMapping("/recruit/create")
    @PreAuthorize("@ss.hasPermission('hr:recruit:create')")
    public CommonResult<Long> createRecruit(@RequestParam("position") String position,
                                            @RequestParam(value = "headcount", defaultValue = "1") Integer headcount,
                                            @RequestParam(value = "department", required = false) String department,
                                            @RequestParam(value = "remark", required = false) String remark) {
        HrRecruitDO recruit = new HrRecruitDO();
        recruit.setNo("ZP" + System.currentTimeMillis() % 10000000000L);
        recruit.setPosition(position);
        recruit.setHeadcount(headcount);
        recruit.setDepartment(department);
        recruit.setStatus(0);
        recruit.setRemark(remark);
        recruitMapper.insert(recruit);
        return CommonResult.success(recruit.getId());
    }

    @DeleteMapping("/recruit/delete")
    @PreAuthorize("@ss.hasPermission('hr:recruit:delete')")
    public CommonResult<Boolean> deleteRecruit(@RequestParam("id") Long id) {
        recruitMapper.deleteById(id);
        return CommonResult.success(true);
    }

    // ========== 培训 ==========

    @GetMapping("/training/list")
    @PreAuthorize("@ss.hasPermission('hr:training:query')")
    public CommonResult<List<HrTrainingDO>> getTrainingList() {
        return CommonResult.success(trainingMapper.selectList(
                new LambdaQueryWrapper<HrTrainingDO>().orderByDesc(HrTrainingDO::getId)));
    }

    @PostMapping("/training/create")
    @PreAuthorize("@ss.hasPermission('hr:training:create')")
    public CommonResult<Long> createTraining(@RequestParam("title") String title,
                                             @RequestParam(value = "trainer", required = false) String trainer,
                                             @RequestParam(value = "trainDate", required = false) LocalDate trainDate,
                                             @RequestParam(value = "employees", required = false) String employees,
                                             @RequestParam(value = "remark", required = false) String remark) {
        HrTrainingDO training = new HrTrainingDO();
        training.setNo("PX" + System.currentTimeMillis() % 10000000000L);
        training.setTitle(title);
        training.setTrainer(trainer);
        training.setTrainDate(trainDate);
        training.setEmployees(employees);
        training.setRemark(remark);
        trainingMapper.insert(training);
        return CommonResult.success(training.getId());
    }

    @DeleteMapping("/training/delete")
    @PreAuthorize("@ss.hasPermission('hr:training:delete')")
    public CommonResult<Boolean> deleteTraining(@RequestParam("id") Long id) {
        trainingMapper.deleteById(id);
        return CommonResult.success(true);
    }

    // ========== 绩效 ==========

    @GetMapping("/performance/list")
    @PreAuthorize("@ss.hasPermission('hr:performance:query')")
    public CommonResult<List<HrPerformanceDO>> getPerformanceList(
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String period) {
        return CommonResult.success(performanceMapper.selectList(new LambdaQueryWrapper<HrPerformanceDO>()
                .eq(employeeId != null, HrPerformanceDO::getEmployeeId, employeeId)
                .eq(period != null && !period.isBlank(), HrPerformanceDO::getPeriod, period)
                .orderByDesc(HrPerformanceDO::getId)));
    }

    @PostMapping("/performance/create")
    @PreAuthorize("@ss.hasPermission('hr:performance:create')")
    public CommonResult<Long> createPerformance(@RequestParam("employeeId") Long employeeId,
                                                @RequestParam("period") String period,
                                                @RequestParam("score") BigDecimal score,
                                                @RequestParam(value = "grade", required = false) String grade,
                                                @RequestParam(value = "remark", required = false) String remark) {
        HrPerformanceDO perf = new HrPerformanceDO();
        perf.setNo("JX" + System.currentTimeMillis() % 10000000000L);
        perf.setEmployeeId(employeeId);
        perf.setPeriod(period);
        perf.setScore(score);
        perf.setGrade(grade);
        perf.setRemark(remark);
        performanceMapper.insert(perf);
        return CommonResult.success(perf.getId());
    }

    @DeleteMapping("/performance/delete")
    @PreAuthorize("@ss.hasPermission('hr:performance:delete')")
    public CommonResult<Boolean> deletePerformance(@RequestParam("id") Long id) {
        performanceMapper.deleteById(id);
        return CommonResult.success(true);
    }
}
