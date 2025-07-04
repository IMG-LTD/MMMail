package com.mmmail.base.module.support.securityprotect.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mmmail.base.module.support.securityprotect.domain.LoginFailEntity;
import com.mmmail.base.module.support.securityprotect.domain.LoginFailQueryForm;
import com.mmmail.base.module.support.securityprotect.domain.LoginFailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 登录失败
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022/07/22 19:46:23
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright  <a href="https://1024lab.net">1024创新实验室</a>
 */
@Mapper
public interface LoginFailDao extends BaseMapper<LoginFailEntity> {

    /**
     * 根据用户id和类型查询
     *
     * @param userId
     * @param userType
     * @return
     */
    LoginFailEntity selectByUserIdAndUserType(@Param("userId") Long userId, @Param("userType") Integer userType);

    /**
     * 根据用户id和类型查询 进行删除
     *
     * @param userId
     * @param userType
     * @return
     */
    void deleteByUserIdAndUserType(@Param("userId") Long userId, @Param("userType") Integer userType);

    /**
     * 分页 查询
     *
     * @param page
     * @param queryForm
     * @return
     */
    List<LoginFailVO> queryPage(Page page, @Param("queryForm") LoginFailQueryForm queryForm);
}
