package cn.tiangong.framework.web.core.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求参数。pageSize 上限 100，防止恶意大分页。
 */
@Data
public class PageParam implements Serializable {

    private static final Integer PAGE_NO = 1;
    private static final Integer PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;

    /** 页码，从 1 开始 */
    @Min(value = 1, message = "页码最小值为 1")
    private Integer pageNo = PAGE_NO;

    /** 每页条数，最大 100 */
    @Min(value = 1, message = "每页条数最小值为 1")
    @Max(value = MAX_PAGE_SIZE, message = "每页条数最大值为 " + MAX_PAGE_SIZE)
    private Integer pageSize = PAGE_SIZE;
}
