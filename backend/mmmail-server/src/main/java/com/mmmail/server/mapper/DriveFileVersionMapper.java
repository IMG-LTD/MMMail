package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.DriveFileVersion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DriveFileVersionMapper extends BaseMapper<DriveFileVersion> {

    @Delete("delete from drive_file_version where item_id = #{itemId}")
    int purgeByItemId(@Param("itemId") Long itemId);
}
