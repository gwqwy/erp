package cn.tiangong.framework.web.core.pojo;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页返回结果。
 */
@Data
public class PageResult<T> implements Serializable {

    private List<T> list;

    private Long total;

    public static <T> PageResult<T> of(List<T> list, Long total) {
        PageResult<T> result = new PageResult<>();
        result.list = list;
        result.total = total;
        return result;
    }

    public static <T> PageResult<T> empty() {
        return of(List.of(), 0L);
    }

    /** 分页记录 VO 转换 */
    public static <S, T> PageResult<T> of(PageResult<S> page, Function<S, T> mapper) {
        return of(page.getList().stream().map(mapper).collect(Collectors.toList()), page.getTotal());
    }
}
