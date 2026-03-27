package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.SheetsWorkbookShare;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SheetsWorkbookShareMapper extends BaseMapper<SheetsWorkbookShare> {

    @Delete("delete from sheets_workbook_share where workbook_id = #{workbookId} and collaborator_user_id = #{collaboratorUserId}")
    int purgeByWorkbookAndCollaborator(@Param("workbookId") Long workbookId, @Param("collaboratorUserId") Long collaboratorUserId);

    @Delete("delete from sheets_workbook_share where owner_id = #{ownerId} and workbook_id = #{workbookId} and id = #{shareId}")
    int purgeByOwnerWorkbookAndId(@Param("ownerId") Long ownerId, @Param("workbookId") Long workbookId, @Param("shareId") Long shareId);

    @Delete("delete from sheets_workbook_share where workbook_id = #{workbookId}")
    int purgeByWorkbookId(@Param("workbookId") Long workbookId);
}
