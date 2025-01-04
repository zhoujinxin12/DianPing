package com.dianping.dao;

import com.dianping.entity.SysLog;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

//@Component
@Mapper
public interface SysLogDao {

    @Insert("insert into tb_syslog(username,operation,time,method,params,ip,created_time) " +
            "values(#{username},#{operation},#{time},#{method},#{params},#{ip},#{createTime})")
    int add(SysLog sysLog);

    @Select("select * from tb_syslog where username=#{username}")
    @Results(id = "sysLog",value= {
            @Result(property = "username", column = "username", javaType = String.class),
            @Result(property = "operation", column = "operation", javaType = String.class),
            @Result(property = "time", column = "time", javaType = Integer.class),
            @Result(property = "method", column = "method", javaType = String.class),
            @Result(property = "params", column = "params", javaType = String.class),
            @Result(property = "ip", column = "ip", javaType = String.class),
            @Result(property = "createTime", column = "created_time", javaType = LocalDateTime.class)
    })
    List<SysLog> querySyslogByUsername(String username);
}