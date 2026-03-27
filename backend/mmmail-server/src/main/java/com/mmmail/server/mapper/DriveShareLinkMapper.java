package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.DriveShareLink;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DriveShareLinkMapper extends BaseMapper<DriveShareLink> {

    @Delete("delete from drive_share_link where item_id = #{itemId}")
    int purgeByItemId(@Param("itemId") Long itemId);
}
