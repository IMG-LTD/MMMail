package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.SheetsWorkbookVersion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SheetsWorkbookVersionMapper extends BaseMapper<SheetsWorkbookVersion> {

    @Delete("delete from sheets_workbook_version where workbook_id = #{workbookId}")
    int purgeByWorkbookId(@Param("workbookId") Long workbookId);
}
