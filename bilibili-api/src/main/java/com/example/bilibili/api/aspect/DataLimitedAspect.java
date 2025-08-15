package com.example.bilibili.api.aspect;

import com.example.bilibili.api.support.UserSupport;
import com.example.bilibili.domain.UserMoment;
import com.example.bilibili.domain.annotation.ApiLimitedRole;
import com.example.bilibili.domain.auth.UserRole;
import com.example.bilibili.domain.constant.AuthRoleConstant;
import com.example.bilibili.domain.exception.ConditionException;
import com.example.bilibili.service.UserRoleService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Order(1)
@Component
@Aspect
public class DataLimitedAspect {

    @Autowired
    private UserSupport userSupport;

    @Autowired
    private UserRoleService userRoleService;

    @Pointcut("@annotation(com.example.bilibili.domain.annotation.DataLimited)")
    public void check(){
    }

    //数据的限制，我们对发出去的数据进行限制，比如我们可以限制UserMoment里用户发布的Type
    @Before("check()")
    public void doBefore(JoinPoint joinPoint){
        Long userId = userSupport.getCurrentUserId();
        //查询角色所属于的ROLE的LIST
        List<UserRole> userRoleList = userRoleService.getUserRoleByUserId(userId);
        Set<String> roleCodeSet = userRoleList.stream().map(UserRole::getRoleCode).collect(Collectors.toSet());
        //getArgs()会返回这个切口本身所有传的参数，比如对应UserMoment里的id,userId,type, etc
        Object[] args = joinPoint.getArgs();
        for (Object arg : args) {
            if (arg instanceof UserMoment) {
                UserMoment userMoment = (UserMoment)arg;
                //找到他的TYPE
                String type = userMoment.getType();
                //如果当前角色属于LVL0并且发送了一个TYPE不为0的动态，那他将报错，也就是说他只能发视频(0)
                if (roleCodeSet.contains(AuthRoleConstant.ROLE_LV1) && !"0".equals(type)){
                    throw new ConditionException("Parameter exception");
                }
            }
        }
    }
}