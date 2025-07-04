package com.mmmail.admin.module.system.datascope.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import com.mmmail.admin.AdminApplication;
import com.mmmail.admin.module.system.datascope.DataScope;
import com.mmmail.admin.module.system.datascope.constant.DataScopeTypeEnum;
import com.mmmail.admin.module.system.datascope.constant.DataScopeViewTypeEnum;
import com.mmmail.admin.module.system.datascope.constant.DataScopeWhereInTypeEnum;
import com.mmmail.admin.module.system.datascope.domain.DataScopeSqlConfig;
import com.mmmail.admin.module.system.datascope.strategy.AbstractDataScopeStrategy;
import com.mmmail.base.common.util.SmartRequestUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * sql配置
 *
 * @Author 1024创新实验室: 罗伊
 * @Date 2020/11/28  20:59:17
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Slf4j
@Service
public class DataScopeSqlConfigService {

    /**
     * 注解joinsql 参数
     */
    private static final String EMPLOYEE_PARAM = "#employeeIds";

    private static final String DEPARTMENT_PARAM = "#departmentIds";

    /**
     * 用于拼接查看本人数据范围的 SQL
     */
    private static final String CREATE_USER_ID_EQUALS = "create_user_id = ";

    private final ConcurrentHashMap<String, DataScopeSqlConfig> dataScopeMethodMap = new ConcurrentHashMap<>();

    @Resource
    private DataScopeViewService dataScopeViewService;

    @Resource
    private ApplicationContext applicationContext;


    @PostConstruct
    private void initDataScopeMethodMap() {
        this.refreshDataScopeMethodMap();
    }

    /**
     * 刷新 所有添加数据范围注解的接口方法配置<class.method,DataScopeSqlConfigDTO></>
     */
    private Map<String, DataScopeSqlConfig> refreshDataScopeMethodMap() {
        Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(ClasspathHelper.forPackage(AdminApplication.COMPONENT_SCAN)).setScanners(new MethodAnnotationsScanner()));
        Set<Method> methods = reflections.getMethodsAnnotatedWith(DataScope.class);
        for (Method method : methods) {
            DataScope dataScopeAnnotation = method.getAnnotation(DataScope.class);
            if (dataScopeAnnotation != null) {
                DataScopeSqlConfig configDTO = new DataScopeSqlConfig();
                configDTO.setDataScopeType(dataScopeAnnotation.dataScopeType());
                configDTO.setJoinSql(dataScopeAnnotation.joinSql());
                configDTO.setWhereIndex(dataScopeAnnotation.whereIndex());
                configDTO.setDataScopeWhereInType(dataScopeAnnotation.whereInType());
                configDTO.setParamName(dataScopeAnnotation.paramName());
                configDTO.setJoinSqlImplClazz(dataScopeAnnotation.joinSqlImplClazz());
                dataScopeMethodMap.put(method.getDeclaringClass().getSimpleName() + "." + method.getName(), configDTO);
            }
        }
        return dataScopeMethodMap;
    }

    /**
     * 根据调用的方法获取，此方法的配置信息
     */
    public DataScopeSqlConfig getSqlConfig(String method) {
        return this.dataScopeMethodMap.get(method);
    }

    /**
     * 组装需要拼接的sql
     */
    public String getJoinSql(Map<String, Object> paramMap, DataScopeSqlConfig sqlConfigDTO) {
        Long employeeId = SmartRequestUtil.getRequestUserId();
        if (employeeId == null) {
            return "";
        }

        DataScopeTypeEnum dataScopeTypeEnum = sqlConfigDTO.getDataScopeType();
        DataScopeViewTypeEnum viewTypeEnum = dataScopeViewService.getEmployeeDataScopeViewType(dataScopeTypeEnum, employeeId);

        // 数据权限设置为仅本人可见时 直接返回 create_user_id = employeeId
        if (DataScopeViewTypeEnum.ME == viewTypeEnum) {
            return CREATE_USER_ID_EQUALS + employeeId;
        }

        String joinSql = sqlConfigDTO.getJoinSql();

        if (DataScopeWhereInTypeEnum.CUSTOM_STRATEGY == sqlConfigDTO.getDataScopeWhereInType()) {
            Class<?> strategyClass = sqlConfigDTO.getJoinSqlImplClazz();
            if (strategyClass == null) {
                log.warn("data scope custom strategy class is null");
                return "";
            }
            AbstractDataScopeStrategy powerStrategy = (AbstractDataScopeStrategy) applicationContext.getBean(sqlConfigDTO.getJoinSqlImplClazz());
            if (powerStrategy == null) {
                log.warn("data scope custom strategy class：{} ,bean is null", sqlConfigDTO.getJoinSqlImplClazz());
                return "";
            }
            return powerStrategy.getCondition(viewTypeEnum, paramMap, sqlConfigDTO);
        }
        if (DataScopeWhereInTypeEnum.EMPLOYEE == sqlConfigDTO.getDataScopeWhereInType()) {
            List<Long> canViewEmployeeIds = dataScopeViewService.getCanViewEmployeeId(viewTypeEnum, employeeId);
            if (CollectionUtils.isEmpty(canViewEmployeeIds)) {
                return "";
            }
            String employeeIds = StringUtils.join(canViewEmployeeIds, ",");
            String sql = joinSql.replaceAll(EMPLOYEE_PARAM, employeeIds);
            return sql;
        }
        if (DataScopeWhereInTypeEnum.DEPARTMENT == sqlConfigDTO.getDataScopeWhereInType()) {
            List<Long> canViewDepartmentIds = dataScopeViewService.getCanViewDepartmentId(viewTypeEnum, employeeId);
            if (CollectionUtils.isEmpty(canViewDepartmentIds)) {
                return "";
            }
            String departmentIds = StringUtils.join(canViewDepartmentIds, ",");
            String sql = joinSql.replaceAll(DEPARTMENT_PARAM, departmentIds);
            return sql;
        }
        return "";
    }
}
