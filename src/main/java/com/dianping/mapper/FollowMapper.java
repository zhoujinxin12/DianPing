package com.dianping.mapper;

import com.dianping.entity.Follow;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface FollowMapper extends BaseMapper<Follow> {
    @Delete("delete from tb_follow where user_id = #{userId} and follow_user_id = #{followUserId}")
    int delete(@Param("userId") Long userId, @Param("followUserId") Long followUserId);
}
