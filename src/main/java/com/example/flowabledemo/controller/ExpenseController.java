package com.example.flowabledemo.controller;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.history.HistoricProcessInstanceQuery;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping(value = "expense")
public class ExpenseController {

    @Autowired
    private RuntimeService runtimeService;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RepositoryService repositoryService;
    @Autowired
    private HistoryService historyService;
    @Autowired
    private ProcessEngine processEngine;

    /**
     * 添加报销
     *
     * @param userId    用户Id
     * @param money     报销金额
     * @param descption 描述
     */
    @RequestMapping(value = "add")
    @ResponseBody
    public String addExpense(String userId, Integer money, String descption) {
        //启动流程
        HashMap<String, Object> map = new HashMap<>();
        map.put("taskUser", userId);
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("Expense", map);
        // 发起流程时就发布任务
        Task task = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        apply(task.getId(),String.valueOf(money));
        return "提交成功.流程Id为：" + processInstance.getId();
    }

    /**
     * 获取我的任务列表
     * 领取任务 taskService.claim(task.getId(), userid); 该任务将从同组其他成员的每个任务列表中消失
     */
    @RequestMapping(value = "/list")
    @ResponseBody
    public Object list(String userId) {
        List<Task> tasks = taskService.createTaskQuery().taskCandidateGroup(userId).orderByTaskCreateTime().desc().list();
//        List<Task> tasks = taskService.createTaskQuery().taskAssignee(userId).orderByTaskCreateTime().desc().list();
        for (Task task : tasks) {
            System.out.println(taskService.getTaskAttachments(task.getId()));
        }
        System.out.println(tasks.stream().map(t-> t.getProcessDefinitionId()).collect(Collectors.joining()));
        System.out.println(tasks.toString());
        return tasks.toString();
    }

    /**
     * 待我处理
     * @param userId
     * @param group
     * @return
     */
    @GetMapping("groupList")
    @ResponseBody
    public Object taskList(String userId, String group) {
        TaskQuery query = taskService.createTaskQuery();
        if(!StringUtils.isEmpty(group)){
            query.taskCandidateGroup(group);
        }else if(!StringUtils.isEmpty(userId)){
            query.taskCandidateOrAssigned(userId);
        }
        return query.orderByTaskCreateTime().desc().list();
    }

    /**
     * 批准
     *
     * @param taskId 任务ID
     */
    @RequestMapping(value = "apply")
    @ResponseBody
    public String apply(String taskId,String mapStr) {
//         List<Task> t = taskService.createTaskQuery().list();
         Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("流程不存在");
        }
        //通过审核
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", mapStr);
        map.put("money", mapStr);
        taskService.complete(taskId, map);
        return "processed ok!";
    }

    /**
     * 拒绝
     */
    @ResponseBody
    @RequestMapping(value = "reject")
    public String reject(String taskId) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("outcome", "驳回");
        taskService.complete(taskId, map);
        return "reject";
    }


    /**
     * 已完成的任务列表
     * // 或者 按进程query.processInstanceId(procId).singleResult()
     * @param userId
     * @return
     */
    @GetMapping("hist")
    @ResponseBody
    public Object historicTaskList(String userId) {
        HistoricProcessInstanceQuery query = historyService.createHistoricProcessInstanceQuery();
        if(!StringUtils.isEmpty(userId)){
            query.involvedUser(userId);
        }
        query.finished();
        List<HistoricProcessInstance> hpis = query.orderByProcessInstanceEndTime().desc().list();
        return hpis;
    }
    /**
     * 生成流程图
     *
     * @param processId 任务ID
     */
    @RequestMapping(value = "processDiagram")
    public void genProcessDiagram(HttpServletResponse httpServletResponse, String processId) throws Exception {
        List<ProcessInstance> t = runtimeService.createProcessInstanceQuery().list();
        ProcessInstance pi = runtimeService.createProcessInstanceQuery().processInstanceId(processId).singleResult();


        //流程走完的不显示图
        if (pi == null) {
            return;
        }
        Task task = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
        //使用流程实例ID，查询正在执行的执行对象表，返回流程实例对象
        String InstanceId = task.getProcessInstanceId();
        List<Execution> executions = runtimeService
                .createExecutionQuery()
                .processInstanceId(InstanceId)
                .list();

        //得到正在执行的Activity的Id
        List<String> activityIds = new ArrayList<>();
        List<String> flows = new ArrayList<>();
        for (Execution exe : executions) {
            List<String> ids = runtimeService.getActiveActivityIds(exe.getId());
            activityIds.addAll(ids);
        }

        //获取流程图
        BpmnModel bpmnModel = repositoryService.getBpmnModel(pi.getProcessDefinitionId());
        ProcessEngineConfiguration engconf = processEngine.getProcessEngineConfiguration();
        ProcessDiagramGenerator diagramGenerator = engconf.getProcessDiagramGenerator();
//        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, flows, engconf.getActivityFontName(), engconf.getLabelFontName(), engconf.getAnnotationFontName(), engconf.getClassLoader(), 1.0);
        InputStream in = diagramGenerator.generateDiagram(bpmnModel, "png", activityIds, Collections.emptyList(),engconf.getActivityFontName(),engconf.getLabelFontName(),engconf.getAnnotationFontName(),null,1.0, false);
        OutputStream out = null;
        byte[] buf = new byte[1024];
        int legth = 0;
        try {
            out = httpServletResponse.getOutputStream();
            while ((legth = in.read(buf)) != -1) {
                out.write(buf, 0, legth);
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

}
