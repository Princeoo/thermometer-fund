package com.tempinvest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tempinvest.entity.InvestRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InvestRecordMapper extends BaseMapper<InvestRecord> {

    @Select("SELECT * FROM invest_record WHERE user_id = #{userId} AND fund_code = #{fundCode} ORDER BY date DESC")
    List<InvestRecord> selectByUserIdAndCode(@Param("userId") Long userId, @Param("fundCode") String fundCode);

    @Select("SELECT * FROM invest_record WHERE user_id = #{userId} AND fund_code = #{fundCode} AND actual_amount < 0 ORDER BY date DESC LIMIT 1")
    InvestRecord selectLastSell(@Param("userId") Long userId, @Param("fundCode") String fundCode);
}
