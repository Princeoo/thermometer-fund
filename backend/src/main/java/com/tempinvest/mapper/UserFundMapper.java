package com.tempinvest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tempinvest.entity.UserFund;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserFundMapper extends BaseMapper<UserFund> {

    @Select("SELECT * FROM user_fund WHERE user_id = #{userId} AND fund_code = #{fundCode}")
    UserFund selectByUserIdAndCode(@Param("userId") Long userId, @Param("fundCode") String fundCode);

    @Select("SELECT * FROM user_fund WHERE user_id = #{userId}")
    List<UserFund> selectByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM user_fund WHERE current_holding > 0")
    List<UserFund> selectAllActive();
}
