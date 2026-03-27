package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.DriveItem;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface DriveItemMapper extends BaseMapper<DriveItem> {

    @Select("""
            SELECT COALESCE(SUM(size_bytes), 0)
            FROM drive_item
            WHERE owner_id = #{ownerId}
              AND item_type = 'FILE'
              AND deleted = 0
            """)
    Long selectStorageBytesByOwner(@Param("ownerId") Long ownerId);

    @Select("""
            SELECT COALESCE(SUM(size_bytes), 0)
            FROM drive_item
            WHERE team_space_id = #{teamSpaceId}
              AND item_type = 'FILE'
              AND deleted = 0
            """)
    Long selectStorageBytesByTeamSpace(@Param("teamSpaceId") Long teamSpaceId);

    @Select("""
            SELECT COUNT(1)
            FROM drive_item
            WHERE team_space_id = #{teamSpaceId}
              AND deleted = 0
            """)
    Long countItemsByTeamSpace(@Param("teamSpaceId") Long teamSpaceId);

    @Select("""
            SELECT COUNT(1)
            FROM drive_item
            WHERE owner_id = #{ownerId}
              AND parent_id = #{parentId}
              AND deleted = 0
            """)
    long countChildren(@Param("ownerId") Long ownerId, @Param("parentId") Long parentId);

    @Select("""
            SELECT COUNT(1)
            FROM drive_item
            WHERE team_space_id = #{teamSpaceId}
              AND parent_id = #{parentId}
              AND deleted = 0
            """)
    long countChildrenByTeamSpace(@Param("teamSpaceId") Long teamSpaceId, @Param("parentId") Long parentId);

    @Select("""
            SELECT *
            FROM drive_item
            WHERE owner_id = #{ownerId}
              AND deleted = 1
            ORDER BY trashed_at DESC, updated_at DESC
            LIMIT #{limit}
            """)
    List<DriveItem> selectTrashedItems(@Param("ownerId") Long ownerId, @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM drive_item
            WHERE team_space_id = #{teamSpaceId}
              AND deleted = 1
            ORDER BY trashed_at DESC, updated_at DESC
            LIMIT #{limit}
            """)
    List<DriveItem> selectTrashedItemsByTeamSpace(@Param("teamSpaceId") Long teamSpaceId, @Param("limit") int limit);

    @Select("""
            SELECT *
            FROM drive_item
            WHERE id = #{itemId}
              AND owner_id = #{ownerId}
              AND deleted = 1
            LIMIT 1
            """)
    DriveItem selectTrashedById(@Param("ownerId") Long ownerId, @Param("itemId") Long itemId);

    @Select("""
            SELECT *
            FROM drive_item
            WHERE id = #{itemId}
              AND team_space_id = #{teamSpaceId}
              AND deleted = 1
            LIMIT 1
            """)
    DriveItem selectTrashedItemByTeamSpace(@Param("teamSpaceId") Long teamSpaceId, @Param("itemId") Long itemId);

    @Update("""
            UPDATE drive_item
            SET parent_id = #{parentId},
                deleted = 0,
                trashed_at = NULL,
                purge_after_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{itemId}
              AND owner_id = #{ownerId}
              AND deleted = 1
            """)
    int restoreTrashedItem(
            @Param("ownerId") Long ownerId,
            @Param("itemId") Long itemId,
            @Param("parentId") Long parentId
    );

    @Update("""
            UPDATE drive_item
            SET parent_id = #{parentId},
                deleted = 0,
                trashed_at = NULL,
                purge_after_at = NULL,
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{itemId}
              AND team_space_id = #{teamSpaceId}
              AND deleted = 1
            """)
    int restoreTrashedItemByTeamSpace(
            @Param("teamSpaceId") Long teamSpaceId,
            @Param("itemId") Long itemId,
            @Param("parentId") Long parentId
    );

    @Delete("""
            DELETE FROM drive_item
            WHERE id = #{itemId}
              AND owner_id = #{ownerId}
              AND deleted = 1
            """)
    int deleteTrashedItemPermanently(@Param("ownerId") Long ownerId, @Param("itemId") Long itemId);

    @Delete("""
            DELETE FROM drive_item
            WHERE id = #{itemId}
              AND team_space_id = #{teamSpaceId}
              AND deleted = 1
            """)
    int deleteTrashedItemPermanentlyByTeamSpace(@Param("teamSpaceId") Long teamSpaceId, @Param("itemId") Long itemId);
}
