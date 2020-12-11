package com.example.flowabledemo.listen;

import com.example.flowabledemo.config.ApplicationContextProvider;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

import java.util.Map;

public class BossTaskHandler implements TaskListener {

    @Override
    public void notify(DelegateTask delegateTask) {
        Map<String,Object> vars = delegateTask.getVariables();
        System.out.println(vars);
        String preUserId = (String) vars.get("taskUser");
        // 调用微服务接口 或 查询数据库
        // 指定单个人可操作
        delegateTask.setAssignee("老板");
        // 多个候选人
//        delegateTask.addCandidateUsers();
        // 分组
//        delegateTask.addCandidateGroups();
    }
}
