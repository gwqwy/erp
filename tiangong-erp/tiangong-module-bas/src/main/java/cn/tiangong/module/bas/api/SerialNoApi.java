package cn.tiangong.module.bas.api;

/**
 * 基础数据 - 单号规则对外 API。
 */
public interface SerialNoApi {

    /** 取下一个单号（消耗序列），规则不存在时抛业务异常 */
    String nextNo(String bizType);

    /** 预览下一个单号（不消耗序列） */
    String previewNo(String bizType);
}
