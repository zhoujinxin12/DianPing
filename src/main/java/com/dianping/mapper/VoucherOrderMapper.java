package com.dianping.mapper;

import com.dianping.entity.VoucherOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * <p>
 *  Mapper 接口
 * </p>
 */
public interface VoucherOrderMapper extends BaseMapper<VoucherOrder> {

    @Select("select status from tb_voucher_order where id = #{id}")
    Integer getStatusById(Long id);

    // 需要status处于未支付状态
    @Update("update tb_voucher_order set status=#{status}, ${timeType}=#{now} where id=#{id} and status=#{expectStatus}")
    Boolean setStatusSuccess(Long id, Long status, Long expectStatus, String timeType, LocalDateTime now);
}
