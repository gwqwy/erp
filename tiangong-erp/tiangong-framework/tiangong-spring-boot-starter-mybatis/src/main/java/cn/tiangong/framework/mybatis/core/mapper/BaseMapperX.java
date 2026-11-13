package cn.tiangong.framework.mybatis.core.mapper;

import cn.tiangong.framework.web.core.pojo.PageParam;
import cn.tiangong.framework.web.core.pojo.PageResult;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * BaseMapper 扩展：单字段查询与标准分页的便捷方法，替代手写样板代码。
 */
@Mapper
public interface BaseMapperX<T> extends BaseMapper<T> {

    default T selectOne(SFunction<T, ?> field, Object value) {
        return selectOne(new LambdaQueryWrapper<T>().eq(field, value).last("LIMIT 1"));
    }

    default List<T> selectList(SFunction<T, ?> field, Object value) {
        return selectList(new LambdaQueryWrapper<T>().eq(field, value));
    }

    default Long selectCount(SFunction<T, ?> field, Object value) {
        return selectCount(new LambdaQueryWrapper<T>().eq(field, value));
    }

    /** 标准分页：入参 PageParam，出参 PageResult */
    default PageResult<T> selectPage(PageParam pageParam, Wrapper<T> wrapper) {
        Page<T> page = new Page<>(pageParam.getPageNo(), pageParam.getPageSize());
        IPage<T> result = BaseMapper.super.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), result.getTotal());
    }
}
