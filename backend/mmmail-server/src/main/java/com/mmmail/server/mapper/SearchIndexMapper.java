package com.mmmail.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mmmail.server.model.entity.SearchIndex;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

@Mapper
public interface SearchIndexMapper extends BaseMapper<SearchIndex> {
    @Delete("""
            <script>
            delete from search_index
            where module_type in
            <foreach collection="moduleTypes" item="moduleType" open="(" separator="," close=")">
                #{moduleType}
            </foreach>
            </script>
            """)
    int deleteByModuleTypes(@Param("moduleTypes") Collection<String> moduleTypes);
}
