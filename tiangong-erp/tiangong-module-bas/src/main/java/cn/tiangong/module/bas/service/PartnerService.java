package cn.tiangong.module.bas.service;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import cn.tiangong.module.bas.dal.dataobject.ErpPartnerDO;

public interface PartnerService {

    PageResult<ErpPartnerDO> getPartnerPage(PageParam pageParam, String name, Boolean isCustomer,
                                            Boolean isSupplier, Integer status);

    ErpPartnerDO getPartner(Long id);

    /** 创建往来单位，no 为空时按规则自动生成（规则 bizType = erp_partner） */
    Long createPartner(ErpPartnerDO partner);

    void updatePartner(ErpPartnerDO partner);

    void deletePartner(Long id);

    void updatePartnerStatus(Long id, Integer status);
}
