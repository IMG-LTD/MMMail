package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.DriveCollaboratorShare;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DriveCollaboratorShareMapper extends BaseMapper<DriveCollaboratorShare> {

    @Delete("delete from drive_collaborator_share where item_id = #{itemId} and collaborator_user_id = #{collaboratorUserId}")
    int purgeByItemAndCollaborator(@Param("itemId") Long itemId, @Param("collaboratorUserId") Long collaboratorUserId);

    @Delete("delete from drive_collaborator_share where owner_id = #{ownerId} and item_id = #{itemId} and id = #{shareId}")
    int purgeByOwnerItemAndId(@Param("ownerId") Long ownerId, @Param("itemId") Long itemId, @Param("shareId") Long shareId);

    @Delete("delete from drive_collaborator_share where item_id = #{itemId}")
    int purgeByItemId(@Param("itemId") Long itemId);
}
