package com.xfd.common.mapper;


import com.xfd.common.dao.UserCommonInfo;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserCommonInfoMapper {

    @Select("SELECT `name`,`birthday`,`job`,`height_and_weight` as heightWeight,`update_time` as updateTime " +
        " FROM `user_common_info` WHERE `user_id`=#{userId}")
    UserCommonInfo selectUserCommonInfoByUserId(@Param("userId") String userId);

    @Insert("INSERT INTO `user_common_info`(`user_id`,`update_time`) " +
        " VALUES (#{userId},#{updateTime})")
    boolean insertNewUserCommonInfo(@Param("userId") String userId, @Param("updateTime") Long updateTime);

    @Update("UPDATE `user_common_info` SET `update_time`=#{userCommonInfo.updateTime}" +
        ",`name`=#{userCommonInfo.name},`birthday`=#{userCommonInfo.birthday}" +
        ",`height_and_weight`=#{userCommonInfo.heightWeight},`job`=#{userCommonInfo.job} " +
        " WHERE `user_id`=#{userId}")
    boolean updateOtherUserCommonInfo(@Param("userId") String userId, UserCommonInfo userCommonInfo);


}
