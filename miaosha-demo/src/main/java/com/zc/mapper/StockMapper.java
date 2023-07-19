package com.zc.mapper;

import com.zc.entity.Stock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author admin
 * @since 2023-07-14
 */
public interface StockMapper extends BaseMapper<Stock> {
    int updateSaleCnt(@Param("sid") int sid);
    int updateByOptimistic(Stock stock);
}
