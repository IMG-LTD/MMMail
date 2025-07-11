package com.mmmail.admin.module.system.employee.service;

import com.mmmail.admin.module.system.employee.domain.entity.EmployeeEntity;
import com.mmmail.base.module.support.mail.service.MailCache;
import com.mmmail.base.module.support.mail.model.MailUser;
import com.mmmail.base.module.support.mail.config.MailServerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 员工业务逻辑服务
 * Employee Business Service
 * 
 * @author MMMAIL
 */
@Service
public class EmployeeBusinessService {
    
    private static final Logger log = LoggerFactory.getLogger(EmployeeBusinessService.class);
    
    @Autowired
    private MailCache mailCache;
    
    @Autowired
    private MailServerProperties mailServerProperties;
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    /**
     * 员工登录业务处理
     * Employee login business processing
     */
    public void handleEmployeeLogin(EmployeeEntity employee, String password) {
        if (employee == null || password == null) {
            log.warn("员工登录处理失败：员工信息或密码为空");
            return;
        }
        
        try {
            // 创建邮件用户对象
            MailUser mailUser = createMailUser(employee, password);
            
            // 缓存用户信息到Redis和内存
            mailCache.onUserLogin(mailUser);
            
            log.info("员工 {} 登录处理成功", employee.getLoginName());
        } catch (Exception e) {
            log.error("员工 {} 登录处理失败: {}", employee.getLoginName(), e.getMessage(), e);
            throw new RuntimeException("员工登录处理失败", e);
        }
    }
    
    /**
     * 员工登出业务处理
     * Employee logout business processing
     */
    public void handleEmployeeLogout(String loginName) {
        if (loginName == null || loginName.trim().isEmpty()) {
            log.warn("员工登出处理失败：登录名为空");
            return;
        }
        
        try {
            mailCache.removeUserInfo(loginName);
            log.info("员工 {} 登出处理成功", loginName);
        } catch (Exception e) {
            log.error("员工 {} 登出处理失败: {}", loginName, e.getMessage(), e);
            throw new RuntimeException("员工登出处理失败", e);
        }
    }
    
    /**
     * 员工新增业务处理
     * Employee add business processing
     */
    public void handleEmployeeAdd(EmployeeEntity employee, String password) {
        if (employee == null || password == null) {
            log.warn("员工新增处理失败：员工信息或密码为空");
            return;
        }
        
        try {
            // 创建邮件用户对象
            MailUser mailUser = createMailUser(employee, password);
            
            // 缓存用户信息
            mailCache.cacheUser(employee.getLoginName(), mailUser);
            mailCache.cacheUserInfo(employee.getLoginName(), employee.getEmail(), password);
            
            log.info("员工 {} 新增处理成功", employee.getLoginName());
        } catch (Exception e) {
            log.error("员工 {} 新增处理失败: {}", employee.getLoginName(), e.getMessage(), e);
            throw new RuntimeException("员工新增处理失败", e);
        }
    }
    
    /**
     * 员工更新业务处理
     * Employee update business processing
     */
    public void handleEmployeeUpdate(EmployeeEntity oldEmployee, EmployeeEntity newEmployee) {
        if (oldEmployee == null || newEmployee == null) {
            log.warn("员工更新处理失败：员工信息为空");
            return;
        }
        
        try {
            // 检查是否为有效员工
            if (Boolean.TRUE.equals(newEmployee.getDeletedFlag()) || Boolean.TRUE.equals(newEmployee.getDisabledFlag())) {
                // 删除或禁用员工，移除缓存
                mailCache.removeUserInfo(oldEmployee.getLoginName());
                mailCache.removeUser(oldEmployee.getLoginName());
                log.info("员工 {} 已被删除或禁用，缓存已清理", oldEmployee.getLoginName());
                return;
            }
            
            // 检查登录名或邮箱是否变更
            boolean loginNameChanged = !oldEmployee.getLoginName().equals(newEmployee.getLoginName());
            boolean emailChanged = !oldEmployee.getEmail().equals(newEmployee.getEmail());
            
            if (loginNameChanged || emailChanged) {
                // 移除旧缓存
                mailCache.removeUserInfo(oldEmployee.getLoginName());
                mailCache.removeUser(oldEmployee.getLoginName());
                
                // 获取密码并重新缓存
                String password = redisTemplate.opsForValue().get(MailCache.MAIL_PWD_KEY_PREFIX + oldEmployee.getLoginName());
                if (password != null) {
                    // 创建新的邮件用户对象
                    MailUser mailUser = createMailUser(newEmployee, password);
                    
                    // 缓存新的用户信息
                    mailCache.cacheUserInfo(newEmployee.getLoginName(), newEmployee.getEmail(), password);
                    mailCache.cacheUser(newEmployee.getLoginName(), mailUser);
                }
            }
            
            log.info("员工 {} 更新处理成功", newEmployee.getLoginName());
        } catch (Exception e) {
            log.error("员工 {} 更新处理失败: {}", oldEmployee.getLoginName(), e.getMessage(), e);
            throw new RuntimeException("员工更新处理失败", e);
        }
    }
    
    /**
     * 员工密码更新业务处理
     * Employee password update business processing
     */
    public void handleEmployeePasswordUpdate(EmployeeEntity employee, String newPassword) {
        if (employee == null || newPassword == null) {
            log.warn("员工密码更新处理失败：员工信息或新密码为空");
            return;
        }
        
        try {
            // 检查是否为有效员工
            if (Boolean.TRUE.equals(employee.getDeletedFlag()) || Boolean.TRUE.equals(employee.getDisabledFlag())) {
                // 删除或禁用员工，移除缓存
                mailCache.removeUserInfo(employee.getLoginName());
                mailCache.removeUser(employee.getLoginName());
                log.info("员工 {} 已被删除或禁用，缓存已清理", employee.getLoginName());
                return;
            }
            
            // 更新密码缓存
            mailCache.cacheUserInfo(employee.getLoginName(), employee.getEmail(), newPassword);
            
            // 创建新的邮件用户对象
            MailUser mailUser = createMailUser(employee, newPassword);
            mailCache.cacheUser(employee.getLoginName(), mailUser);
            
            log.info("员工 {} 密码更新处理成功", employee.getLoginName());
        } catch (Exception e) {
            log.error("员工 {} 密码更新处理失败: {}", employee.getLoginName(), e.getMessage(), e);
            throw new RuntimeException("员工密码更新处理失败", e);
        }
    }
    
    /**
     * 员工密码重置业务处理
     * Employee password reset business processing
     */
    public void handleEmployeePasswordReset(EmployeeEntity employee, String newPassword) {
        if (employee == null || newPassword == null) {
            log.warn("员工密码重置处理失败：员工信息或新密码为空");
            return;
        }
        
        try {
            // 检查是否为有效员工
            if (Boolean.TRUE.equals(employee.getDeletedFlag()) || Boolean.TRUE.equals(employee.getDisabledFlag())) {
                log.info("员工 {} 已被删除或禁用，跳过密码重置处理", employee.getLoginName());
                return;
            }
            
            // 重置密码缓存
            mailCache.cacheUserInfo(employee.getLoginName(), employee.getEmail(), newPassword);
            
            // 创建新的邮件用户对象
            MailUser mailUser = createMailUser(employee, newPassword);
            mailCache.cacheUser(employee.getLoginName(), mailUser);
            
            log.info("员工 {} 密码重置处理成功", employee.getLoginName());
        } catch (Exception e) {
            log.error("员工 {} 密码重置处理失败: {}", employee.getLoginName(), e.getMessage(), e);
            throw new RuntimeException("员工密码重置处理失败", e);
        }
    }
    
    /**
     * 员工禁用业务处理
     * Employee disable business processing
     */
    public void handleEmployeeDisable(EmployeeEntity employee) {
        if (employee == null) {
            log.warn("员工禁用处理失败：员工信息为空");
            return;
        }
        
        try {
            mailCache.onUserDisable(employee.getLoginName());
            log.info("员工 {} 禁用处理成功", employee.getLoginName());
        } catch (Exception e) {
            log.error("员工 {} 禁用处理失败: {}", employee.getLoginName(), e.getMessage(), e);
            throw new RuntimeException("员工禁用处理失败", e);
        }
    }
    
    /**
     * 员工删除业务处理
     * Employee delete business processing
     */
    public void handleEmployeeDelete(EmployeeEntity employee) {
        if (employee == null) {
            log.warn("员工删除处理失败：员工信息为空");
            return;
        }
        
        try {
            mailCache.onUserDelete(employee.getLoginName());
            log.info("员工 {} 删除处理成功", employee.getLoginName());
        } catch (Exception e) {
            log.error("员工 {} 删除处理失败: {}", employee.getLoginName(), e.getMessage(), e);
            throw new RuntimeException("员工删除处理失败", e);
        }
    }
    
    /**
     * 创建邮件用户对象
     * Create mail user object
     */
    private MailUser createMailUser(EmployeeEntity employee, String password) {
        MailUser mailUser = new MailUser();
        mailUser.setUsername(employee.getLoginName());
        mailUser.setEmail(employee.getEmail());
        mailUser.setPassword(password);
        mailUser.setMailHost(mailServerProperties.getHost());
        mailUser.setMailPort(mailServerProperties.getPort());
        return mailUser;
    }
    
    /**
     * 验证员工信息
     * Validate employee info
     */
    public boolean validateEmployeeInfo(EmployeeEntity employee) {
        if (employee == null) {
            log.warn("员工信息验证失败：员工为空");
            return false;
        }
        
        if (employee.getLoginName() == null || employee.getLoginName().trim().isEmpty()) {
            log.warn("员工信息验证失败：登录名为空");
            return false;
        }
        
        if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
            log.warn("员工信息验证失败：邮箱为空");
            return false;
        }
        
        return true;
    }
    
    /**
     * 检查员工是否存在于缓存中
     * Check if employee exists in cache
     */
    public boolean isEmployeeExistsInCache(String loginName) {
        if (loginName == null || loginName.trim().isEmpty()) {
            return false;
        }
        
        try {
            return mailCache.userExistsInRedis(loginName);
        } catch (Exception e) {
            log.error("检查员工 {} 是否存在于缓存中失败: {}", loginName, e.getMessage(), e);
            return false;
        }
    }
}
