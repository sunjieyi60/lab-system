## Quartz 定时任务方案（任务组 + 条件组 + 时间条件）

### 1. 目标
- 支持任务组：一组 Task 由 Quartz 调度（cron/interval），支持启停、立即触发、错过补跑策略。
- 支持条件组：多个条件（布尔表达式）组合，关联数据来源组；未满足则跳过任务执行。
- 支持时间条件：业务时段、工作日/节假日策略、日期区间。
- 可观测：执行日志、条件评估日志、任务详情、指标和告警。
- 可配置：后台增删改查任务组/Task/条件/数据源，动态生效。

### 2. 数据模型（建议表）
- `task_group`：id, name, cron_expr/interval, enable, misfire_policy, desc.
- `task`：id, group_id, name, type(enum: HTTP/SQL/MQ/CUSTOM), payload(json), order_no, parallel_tag, enable, retry_times, retry_backoff_ms, timeout_ms, idempotent_key, desc.
- `condition_group`：id, group_id, logic(enum: ALL/ANY), time_window(json: biz_hours/weekday/holiday/date_range), enable.
- `condition`：id, condition_group_id, expr(spel/mvel/sql_bool)，data_source_id, desc.
- `data_source`：id, type(enum: HTTP/SQL/KV/CUSTOM), config(json: url/sql/headers/mapper/cache)，enable.
- `task_run_log`：id, group_id, fire_time, status, duration_ms, error, retry_times, trace_id.
- `condition_eval_log`：id, run_log_id, condition_id, result, detail(json).
- `task_run_detail`：id, run_log_id, task_id, status, duration_ms, error, output(json)。

### 3. 核心组件
- `QuartzConfig`：Scheduler、线程池、持久化、misfire 配置。
- `TaskGroupJob`：QuartzJobBean，实现一次触发的完整流程。
- `ConditionEvaluator`：评估条件组；拉取数据源，执行表达式。
- `DataSourceExecutor`：按类型执行 HTTP/SQL/KV/CUSTOM，支持缓存和超时。
- `TaskExecutor`：按 Task 类型执行，支持超时、重试、幂等、串行/并行。
- `TimeWindowChecker`：业务时段/工作日/节假日校验。
- `RunLogger`：写入 run_log / condition_log / task_detail，生成 trace_id。
- `TaskAdminService`：增删改任务组/Task/条件/数据源，动态注册/暂停/恢复触发器。

### 4. 时序（单次触发）
1) Quartz 触发 `TaskGroupJob`，传入 group_id。
2) 加载配置：group → tasks（含顺序/并行分组）→ condition_group → conditions → data_source。
3) 时间条件校验：通过才继续。
4) 条件评估：拉取数据源，表达式求值，按逻辑汇总；不满足则 run_log=SKIPPED。
5) 执行任务：串行或并行（按 order_no / parallel_tag），超时、重试、幂等可配置。
6) 汇总日志：run_log + detail + condition_log，触发告警/通知。

### 5. Spring / Quartz 配置示例
```java
@Configuration
public class QuartzConfig {
    @Bean
    public SchedulerFactoryBean schedulerFactory(DataSource ds, ApplicationContext ctx) {
        Properties props = new Properties();
        props.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        props.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.StdJDBCDelegate");
        props.put("org.quartz.jobStore.useProperties", "false");
        props.put("org.quartz.threadPool.threadCount", "10");
        props.put("org.quartz.jobStore.misfireThreshold", "60000");
        SchedulerFactoryBean bean = new SchedulerFactoryBean();
        bean.setDataSource(ds);
        bean.setQuartzProperties(props);
        bean.setApplicationContextSchedulerContextKey("applicationContext");
        bean.setOverwriteExistingJobs(true);
        return bean;
    }
}
```

### 6. 核心实现示例
#### 6.1 TaskGroupJob
```java
@Component
public class TaskGroupJob extends QuartzJobBean {
    @Autowired private TaskRuntimeService runtimeService;
    @Override
    protected void executeInternal(JobExecutionContext ctx) {
        Long groupId = ctx.getMergedJobDataMap().getLong("groupId");
        runtimeService.execute(groupId);
    }
}
```

#### 6.2 TaskRuntimeService（流程编排）
```java
@Service
public class TaskRuntimeService {
    @Autowired private ConfigLoader configLoader;
    @Autowired private TimeWindowChecker timeChecker;
    @Autowired private ConditionEvaluator conditionEvaluator;
    @Autowired private TaskExecutor taskExecutor;
    @Autowired private RunLogger runLogger;

    public void execute(Long groupId) {
        String traceId = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        TaskGroupConfig cfg = configLoader.load(groupId);
        if (cfg == null || !cfg.enabled()) return;

        if (!timeChecker.pass(cfg.getConditionGroup())) {
            runLogger.skipped(groupId, traceId, "时间条件不满足");
            return;
        }
        ConditionResult cond = conditionEvaluator.evaluate(cfg.getConditionGroup(), traceId);
        if (!cond.passed()) {
            runLogger.skipped(groupId, traceId, "条件未满足", cond);
            return;
        }

        RunContext rc = new RunContext(traceId, groupId, cond);
        TaskResult result = taskExecutor.runTasks(cfg.getTasks(), rc);
        runLogger.finish(groupId, traceId, start, result);
    }
}
```

#### 6.3 ConditionEvaluator
```java
@Service
public class ConditionEvaluator {
    @Autowired private DataSourceExecutor dataSourceExecutor;
    @Autowired private ExpressionEvaluator evaluator;

    public ConditionResult evaluate(ConditionGroup cfg, String traceId) {
        List<ConditionEvalDetail> details = new ArrayList<>();
        boolean passed = cfg.getLogic() == Logic.ALL;
        for (Condition c : cfg.getConditions()) {
            Object data = dataSourceExecutor.fetch(c.getDataSourceId(), c.getCacheTtlMs());
            EvaluationContext ctx = evaluator.createEvaluationContext(data, data.getClass(), null, null);
            Boolean ok = evaluator.condition(c.getExpr(), new AnnotatedElementKey(Object.class, Object.class), ctx, Boolean.class);
            details.add(new ConditionEvalDetail(c.getId(), ok, data));
            passed = cfg.getLogic() == Logic.ALL ? passed && Boolean.TRUE.equals(ok)
                                                 : passed || Boolean.TRUE.equals(ok);
        }
        return new ConditionResult(passed, details);
    }
}
```

#### 6.4 DataSourceExecutor（示意）
```java
@Service
public class DataSourceExecutor {
    @Autowired private DataSourceRepository repo;
    @Autowired private HttpClient http;
    @Autowired private JdbcTemplate jdbc;
    private final Cache<Long, Object> cache = Caffeine.newBuilder().maximumSize(200).build();

    public Object fetch(Long id, Long ttlMs) {
        if (ttlMs != null && ttlMs > 0) {
            Object cached = cache.getIfPresent(id);
            if (cached != null) return cached;
        }
        DataSourceConfig cfg = repo.find(id);
        Object data = switch (cfg.type()) {
            case HTTP -> httpCall(cfg);
            case SQL  -> sqlCall(cfg);
            case KV   -> kvCall(cfg);
            case CUSTOM -> customCall(cfg);
        };
        if (ttlMs != null && ttlMs > 0) cache.put(id, data);
        return data;
    }
    // httpCall/sqlCall/kvCall/customCall 需实现超时、白名单等安全控制
}
```

#### 6.5 TaskExecutor（串行/并行 + 重试/超时）
```java
@Service
public class TaskExecutor {
    private final ExecutorService pool = Executors.newFixedThreadPool(16);

    public TaskResult runTasks(List<TaskConfig> tasks, RunContext rc) {
        Map<Long, TaskExecDetail> details = new LinkedHashMap<>();
        // 按 order_no 分组并行，同组并行、组间串行
        tasks.stream().collect(groupingBy(TaskConfig::orderNo, TreeMap::new, toList()))
             .forEach((order, group) -> runParallel(group, rc, details));
        boolean ok = details.values().stream().allMatch(TaskExecDetail::isSuccess);
        return new TaskResult(ok, details);
    }

    private void runParallel(List<TaskConfig> group, RunContext rc, Map<Long, TaskExecDetail> details) {
        List<Future<TaskExecDetail>> futures = group.stream()
            .map(t -> pool.submit(() -> execWithRetry(t, rc)))
            .toList();
        futures.forEach(f -> {
            try { TaskExecDetail d = f.get(); details.put(d.getTaskId(), d); }
            catch (Exception e) { /* 记录异常 */ }
        });
    }

    private TaskExecDetail execWithRetry(TaskConfig t, RunContext rc) {
        int attempts = 0;
        Exception last = null;
        while (attempts <= t.getRetryTimes()) {
            attempts++;
            try {
                return doExec(t, rc, attempts);
            } catch (Exception e) {
                last = e;
                sleep(t.getRetryBackoffMs());
            }
        }
        return TaskExecDetail.fail(t.getId(), last);
    }

    private TaskExecDetail doExec(TaskConfig t, RunContext rc, int attempt) throws Exception {
        // 按类型分派：HTTP/SQL/MQ/CUSTOM，带超时/幂等 token
        return TaskExecDetail.success(t.getId(), "ok");
    }
}
```

#### 6.6 TimeWindowChecker
```java
@Service
public class TimeWindowChecker {
    public boolean pass(ConditionGroup group) {
        TimeWindow w = group.getTimeWindow();
        if (w == null) return true;
        LocalDateTime now = LocalDateTime.now(w.getZoneId());
        if (!w.weekdays().contains(now.getDayOfWeek())) return false;
        if (!withinBizHours(now.toLocalTime(), w.getBizHours())) return false;
        if (w.isExcludeHoliday() && isHoliday(now.toLocalDate())) return false;
        if (!withinDateRange(now.toLocalDate(), w.getDateRange())) return false;
        return true;
    }
}
```

#### 6.7 RunLogger（示意）
```java
@Service
public class RunLogger {
    @Autowired private RunLogMapper runLogMapper;
    @Autowired private ConditionLogMapper conditionLogMapper;
    @Autowired private TaskDetailMapper taskDetailMapper;

    public void skipped(Long groupId, String traceId, String reason) { /* insert run_log=SKIPPED */ }
    public void skipped(Long groupId, String traceId, String reason, ConditionResult cond) { /* insert detail */ }
    public void finish(Long groupId, String traceId, long start, TaskResult result) { /* insert run_log/task_detail */ }
}
```

### 7. 管理接口（示意）
- 任务组：`POST /task-groups`，`PUT /task-groups/{id}`，`PATCH /task-groups/{id}/enable`，`POST /task-groups/{id}/trigger-now`。
- Task：`POST /task-groups/{id}/tasks`，`PUT /tasks/{id}`，`PATCH /tasks/{id}/enable`。
- 条件/条件组：`POST /task-groups/{id}/condition-group`，`POST /condition-group/{id}/conditions`，表达式预校验接口。
- 数据源：`POST /data-sources`，`PUT /data-sources/{id}`，`POST /data-sources/{id}/test`。
- 日志：`GET /runs`（支持 traceId/时间过滤），`GET /runs/{id}/details`。

### 8. 安全与治理
- 表达式白名单：限制 SpEL 访问范围（仅数据对象和少量辅助函数）。
- 数据源白名单：HTTP 仅允许配置域名；SQL 仅允许指定库/只读；KV 前缀限制。
- 超时与隔离：数据源调用和任务执行均需超时；独立线程池；必要时熔断。
- 配置审计：记录操作人和变更 diff，支持回滚。
- 幂等与锁：同一 group fire 在集群可加分布式锁（Quartz JDBC + instanceId 或 Redis 锁）。

### 9. 告警设计（SMS / SMTP 可配置）
- 触发场景：任务执行失败、条件未满足（可选）、重试用尽、数据源拉取失败、超时、连续 N 次失败。
- 配置模型：
  - `alert_channel`：id, type(enum: SMS/SMTP), config(json: sms_provider/api_key/sign/templateId | smtp_host/port/username/password/ssl/from/to/cc)。
  - `task_group_alert`：group_id, enable, level(enum: INFO/WARN/ERROR/CRITICAL), throttle_ms(节流), continuous_fail_threshold, bind_channels (list)。
- 执行逻辑：
  1) 在 `RunLogger.finish/failed/skipped` 后，根据任务结果和配置判断是否告警。
  2) 节流：同一 group 同一错误摘要在 throttle_ms 内只发一次。
  3) 连续失败计数：落库或缓存计数，超过阈值再告警。
  4) 告警内容：group 名、traceId、触发时间、错误摘要、重试次数、最近 N 次状态。
  5) 模板：支持简单占位符 `{group}`, `{traceId}`, `{error}`, `{time}`。
- 发送实现：
  - SMS：抽象 `SmsClient`，适配不同供应商；使用签名/模板 ID；失败重试 1-2 次。
  - SMTP：使用 JavaMailSender；支持 SSL/TLS；支持多收件人/抄送。
- 幂等与失败策略：发送失败记录告警日志，必要时补发（可选补偿任务）。

#### 9.1 代码示意
```java
public enum AlertChannelType { SMS, SMTP }

public record AlertChannel(Long id, AlertChannelType type, Map<String,Object> config) {}

public record TaskGroupAlert(Long groupId, boolean enable, String level,
                             long throttleMs, int continuousFailThreshold,
                             List<Long> channelIds) {}

@Service
public class AlertService {
    @Autowired private AlertChannelRepository repo;
    @Autowired private SmsClient smsClient;
    @Autowired private MailSender mailSender;
    private final Cache<String, Long> throttle = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10)).build(); // key: groupId+error

    public void notify(TaskGroupAlert cfg, RunSummary summary) {
        if (!cfg.enable()) return;
        String key = cfg.groupId() + ":" + summary.errorSign();
        Long last = throttle.getIfPresent(key);
        if (last != null && System.currentTimeMillis() - last < cfg.throttleMs()) return;
        if (summary.continuousFail() < cfg.continuousFailThreshold()) return;
        throttle.put(key, System.currentTimeMillis());

        for (Long chId : cfg.channelIds()) {
            AlertChannel ch = repo.find(chId);
            switch (ch.type()) {
                case SMS  -> smsClient.send(ch.config(), buildText(summary));
                case SMTP -> sendMail(ch.config(), buildText(summary));
            }
        }
    }

    private void sendMail(Map<String,Object> cfg, String text) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setFrom((String) cfg.get("from"));
        msg.setTo(((List<String>) cfg.get("to")).toArray(new String[0]));
        msg.setSubject("任务告警");
        msg.setText(text);
        mailSender.send(msg);
    }

    private String buildText(RunSummary s) {
        return "任务组:" + s.groupName() + "\ntraceId:" + s.traceId()
                + "\n时间:" + s.time() + "\n错误:" + s.errorSign()
                + "\n重试:" + s.retryTimes() + "\n最近状态:" + s.recentStatus();
    }
}
```

#### 9.2 RunLogger 触发告警示意
```java
@Service
public class RunLogger {
    @Autowired private TaskGroupAlertRepository alertRepo;
    @Autowired private AlertService alertService;
    @Autowired private RunLogMapper runLogMapper;
    // ...

    public void finish(Long groupId, String traceId, long start, TaskResult result) {
        RunSummary summary = persistAndBuildSummary(groupId, traceId, start, result);
        TaskGroupAlert alertCfg = alertRepo.findByGroupId(groupId);
        if (result.failed()) {
            alertService.notify(alertCfg, summary);
        }
    }
}
```

#### 9.3 前端/管理配置
- 频道管理：新增 SMS/SMTP 渠道，录入认证/模板/收件人。
- 任务告警策略：勾选启用、选择渠道、设置节流、连续失败阈值、告警级别。
- 手动测试：提供“测试发送”按钮。

### 9. 实施步骤
1) 引入 quartz 依赖，初始化 quartz 表。
2) 建表（见模型）并生成 MyBatis/ORM 映射。
3) 实现核心组件：TaskGroupJob、TaskRuntimeService、ConditionEvaluator、DataSourceExecutor、TaskExecutor、TimeWindowChecker、RunLogger。
4) 完成管理接口与前端表单（任务组/任务/条件/数据源/日志）。
5) 接入告警与监控（日志、metrics、通知）。
6) 预发布演练：构造示例任务组，验证条件评估、重试、超时、告警链路。
7) 上线后巡检，根据运行数据调优线程池、超时、缓存策略。

