package cn.tiangong.module.file.dal.mysql;

import cn.tiangong.framework.mybatis.core.mapper.BaseMapperX;
import cn.tiangong.module.file.dal.dataobject.ErpFileDO;
import org.apache.ibatis.annotations.Select;

public interface ErpFileMapper extends BaseMapperX<ErpFileDO> {

    @Select("SELECT * FROM erp_file WHERE md5 = #{md5} AND deleted = 0 LIMIT 1")
    ErpFileDO selectByMd5(String md5);
}
