package com.example.bilibili.api.aspect;

import com.example.bilibili.api.support.UserSupport;
import com.example.bilibili.domain.annotation.ApiLimitedRole;
import com.example.bilibili.domain.auth.UserRole;
import com.example.bilibili.domain.exception.ConditionException;
import com.example.bilibili.service.UserRoleService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Order(1) //优先级为1，比较高
@Component //@COMPONENT本质上和@RESTCONTROLLER和@SERVICE相似，只是@RESTCONTROLLER和@SERVICE分别被用在控制层和服务层的，@COMPONENT主要用在工具类等项目上的
@Aspect
public class ApiLimitedRoleAspect {

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserRoleService userRoleService;

    //告诉SPRINGBOOT什么地方进行切入
    @Pointcut("@annotation(com.example.bilibili.domain.annotation.ApiLimitedRole)")
    public void check(){
    }

    @Before("check() && @annotation(apiLimitedRole)")
    public void doBefore(JoinPoint joinPoint, ApiLimitedRole apiLimitedRole){
        Long userId = userSupport.getCurrentUserId();
        //获取用户的ROLE
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        //通过apiLimitedRole来获取当前所有被限制的ROLE的列表
        String[] limitedRoleCodeList = apiLimitedRole.limitedRoleCodeList();
        //SET里的数据是唯一的所以可以过滤去重，然后把上面两个LIST放到分别放到下面两个SET当中进行合并
        Set<String> limitedRoleCodeSet = Arrays.stream(limitedRoleCodeList).collect(Collectors.toSet());
        Set<String> roleCodeSet = userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());

        //提取两个SET的交集即roleCodeSet里的ELEMENT>0，如果出现交集那就代表着当前用户是没有使用权限的
        roleCodeSet.retainAll(limitedRoleCodeSet);

        //取交集之后不为空，那么就是被限制的ROLE
        if (roleCodeSet.size() > 0) {
            throw new ConditionException("Insufficient permissions!");
        }

        //如果为空，那么权限被允许
    }
}