package com.example.flowabledemo.config;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.idm.api.Group;
import org.flowable.idm.api.GroupQuery;
import org.flowable.idm.api.User;
import org.flowable.idm.api.UserQuery;
import org.flowable.idm.engine.impl.IdmIdentityServiceImpl;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IDMCustomServiceImpl extends IdmIdentityServiceImpl {
    public UserQuery createUserQuery() {
//        return new LDAPUserQueryImpl(this.ldapConfigurator);
        System.out.println("查询用户");
        return null;
    }

    public GroupQuery createGroupQuery() {
//        return new LDAPGroupQueryImpl(this.ldapConfigurator, this.ldapGroupCache);
        System.out.println("查询用户 ------ 组");
        return null;
    }


    @Override
    public List<User> getUsersWithPrivilege(String name) {
        System.out.println("get查询用户");
        throw new FlowableException("LDAP identity service doesn't support creating a new user");
    }

    @Override
    public List<Group> getGroupsWithPrivilege(String name) {
        System.out.println("get查询用户 ------ 组");
        throw new FlowableException("LDAP identity service doesn't support creating a new user");
    }

    @Override
    public boolean checkPassword(String userId, String password) {
        return true;
    }
}
