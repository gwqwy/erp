package cn.tiangong.module.system.service.dept;

import cn.tiangong.module.system.dal.dataobject.SystemDeptDO;
import cn.tiangong.module.system.dal.mysql.SystemDeptMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static cn.tiangong.framework.web.core.exception.ServiceException.of;

@RequiredArgsConstructor
@Service
public class DeptServiceImpl implements DeptService {

    private final SystemDeptMapper deptMapper;

    @Override
    public List<SystemDeptDO> getDeptList() {
        return deptMapper.selectList(new LambdaQueryWrapper<SystemDeptDO>()
                .orderByAsc(SystemDeptDO::getSort));
    }

    @Override
    public SystemDeptDO getDept(Long id) {
        SystemDeptDO dept = deptMapper.selectById(id);
        if (dept == null) {
            throw of(1_006, 1, "部门不存在");
        }
        return dept;
    }

    @Override
    public Long createDept(String name, Long parentId, Long leaderUserId, Integer sort) {
        SystemDeptDO dept = new SystemDeptDO();
        dept.setName(name);
        dept.setParentId(parentId != null ? parentId : 0L);
        dept.setLeaderUserId(leaderUserId);
        dept.setSort(sort != null ? sort : 0);
        dept.setStatus(0);
        deptMapper.insert(dept);
        return dept.getId();
    }

    @Override
    public void updateDept(Long id, String name, Long parentId, Long leaderUserId, Integer sort, Integer status) {
        SystemDeptDO dept = getDept(id);
        dept.setName(name != null ? name : dept.getName());
        dept.setParentId(parentId != null ? parentId : dept.getParentId());
        dept.setLeaderUserId(leaderUserId != null ? leaderUserId : dept.getLeaderUserId());
        dept.setSort(sort != null ? sort : dept.getSort());
        dept.setStatus(status != null ? status : dept.getStatus());
        deptMapper.updateById(dept);
    }

    @Override
    public void deleteDept(Long id) {
        getDept(id);
        if (deptMapper.selectCount(SystemDeptDO::getParentId, id) > 0) {
            throw of(1_006, 2, "存在子部门，无法删除");
        }
        deptMapper.deleteById(id);
    }
}
