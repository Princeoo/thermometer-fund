package com.tempinvest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tempinvest.entity.FundInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface FundInfoMapper extends BaseMapper<FundInfo> {

    @Select("SELECT * FROM fund_info WHERE fund_code = #{fundCode}")
    FundInfo selectByCode(@Param("fundCode") String fundCode);

    @Select("SELECT * FROM fund_info")
    List<FundInfo> selectAll();
}
