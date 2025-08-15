package com.example.bilibili.domain.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME) //这个注解是在运行RUNTIME当中实现的
@Target({ElementType.METHOD}) //目标，这个注解可以作用于方法METHOD
@Documented
@Component
public @interface ApiLimitedRole {

    String[] limitedRoleCodeList() default {}; //这个字符串，你想要提供限制的CODE列表，默认为空，将来如果我们用到ApiLimitedRole我们就可传参，比如让LEVEL1用户不能使用特定的方法，进而控制谁会收到限制
}