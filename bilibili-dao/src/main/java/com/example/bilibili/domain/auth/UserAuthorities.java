package com.example.bilibili.domain.auth;

import java.util.List;

public class UserAuthorities {

    List<AuthRoleElementOperation> roleElementOperationList; //在前端看到的你能做的操作权限，比如按钮能不能点

    List<AuthRoleMenu> roleMenuList; //你的菜单能看到的信息有什么，访问权限，你能不能看到一些事情

    public List<AuthRoleElementOperation> getRoleElementOperationList() {
        return roleElementOperationList;
    }

    public void setRoleElementOperationList(List<AuthRoleElementOperation> roleElementOperationList) {
        this.roleElementOperationList = roleElementOperationList;
    }

    public List<AuthRoleMenu> getRoleMenuList() {
        return roleMenuList;
    }

    public void setRoleMenuList(List<AuthRoleMenu> roleMenuList) {
        this.roleMenuList = roleMenuList;
    }
}
