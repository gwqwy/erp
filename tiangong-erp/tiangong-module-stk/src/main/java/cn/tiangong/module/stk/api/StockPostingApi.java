package cn.tiangong.module.stk.api;

import cn.tiangong.module.stk.api.dto.MoveCommand;

import java.util.List;

/**
 * 库存过账唯一收口（《02-总体架构》§4.2）：
 * 任何业务模块禁止直接写 erp_stock_move / erp_stock_quant，必须翻译成 MoveCommand 提交。
 */
public interface StockPostingApi {

    /**
     * 逐条过账（同一事务内）。任一失败整体回滚。
     *
     * @return 每条指令对应的过账成本金额（出库=数量×当时加权平均成本，入库=数量×入库价）
     */
    java.util.List<java.math.BigDecimal> submit(List<MoveCommand> commands);
}
