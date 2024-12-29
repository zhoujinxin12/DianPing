package com.dianping.mapper;

import com.dianping.entity.VoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  Mapper 接口
 * </p>
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

//    @Select("select status from tb_voucher_order where id = #{id}")
//    Integer getStatusById(Long id);
//
//    // 需要status处于未支付状态
//    @Update("update tb_voucher_order set status=#{status}, ${timeType}=#{now} where id=#{id} and status=#{expectStatus}")
//    Boolean setStatusSuccess(Long id, Long status, Long expectStatus, String timeType, LocalDateTime now);

    @Select({
            "<script>",
            "select count(*) from tb_voucher_order tvo",
            "where voucher_id = #{voucherId}",
            "and user_id = #{userId}",
            "<if test='status != null and status.size > 0'>",
            "and status not in",
            "<foreach collection='status' item='item' open='(' separator=',' close=')'>",
            "#{item}",
            "</foreach>",
            "</if>",
            "</script>"
    })
    Integer getCountHaveVoucher(@Param("voucherId") Long voucherId,
                                @Param("userId") Long userId,
                                @Param("status") List<Integer> status);

}
