package cn.tiangong.module.system.service.dept;

import cn.tiangong.module.system.dal.dataobject.SystemDeptDO;

import java.util.List;

public interface DeptService {

    List<SystemDeptDO> getDeptList();

    SystemDeptDO getDept(Long id);

    Long createDept(String name, Long parentId, Long leaderUserId, Integer sort);

    void updateDept(Long id, String name, Long parentId, Long leaderUserId, Integer sort, Integer status);

    void deleteDept(Long id);
}
