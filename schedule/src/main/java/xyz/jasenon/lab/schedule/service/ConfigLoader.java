package xyz.jasenon.lab.schedule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.schedule.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConfigLoader {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    private ConfigRoot cached;

    @PostConstruct
    public void init() {
        this.cached = loadByGroupId(1L);
    }

    public synchronized ConfigRoot loadByGroupId(Long groupId) {
        TaskGroupConfig group = queryTaskGroup(groupId);
        if (group == null || !group.isEnable()) {
            log.warn("Task group {} not found or disabled", groupId);
            return null;
        }
        TimeRule timeRule = queryTimeRule(groupId);
        group.setTimeRule(timeRule);

        ConditionGroup conditionGroup = queryConditionGroup(groupId);
        List<ConditionItem> conds = queryConditions(conditionGroup.getId());
        conditionGroup.setConditions(conds);

        List<DataGroup> dataGroups = queryDataGroupsUsed(conds);
        List<Action> actions = queryActions(groupId);
        AlertConfig alerts = queryAlerts(groupId);

        ConfigRoot root = new ConfigRoot();
        root.setTaskGroup(group);
        root.setConditionGroup(conditionGroup);
        root.setDataGroups(dataGroups);
        root.setActions(actions);
        root.setAlerts(alerts);

        this.cached = root;
        return root;
    }

    public ConfigRoot current() {
        return cached;
    }

    private TaskGroupConfig queryTaskGroup(Long groupId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select id,name,cron_expr,enable,misfire_policy,desc_text from task_group where id=?",
                    (rs, i) -> {
                        TaskGroupConfig g = new TaskGroupConfig();
                        g.setId(rs.getLong("id"));
                        g.setName(rs.getString("name"));
                        g.setCron(rs.getString("cron_expr"));
                        g.setEnable(rs.getBoolean("enable"));
                        g.setMisfirePolicy(rs.getString("misfire_policy"));
                        g.setDescText(rs.getString("desc_text"));
                        return g;
                    }, groupId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private TimeRule queryTimeRule(Long groupId) {
        try {
            return jdbcTemplate.queryForObject(
                    "select business_time,weekdays,week_parity,date_range,timezone from time_rule where group_id=? limit 1",
                    (rs, i) -> {
                        TimeRule r = new TimeRule();
                        r.setGroupId(groupId);
                        r.setBusinessTime(splitComma(rs.getString("business_time")));
                        r.setWeekdays(parseIntList(rs.getString("weekdays")));
                        r.setWeekParity(rs.getString("week_parity"));
                        r.setDateRange(splitRange(rs.getString("date_range")));
                        r.setTimezone(Optional.ofNullable(rs.getString("timezone")).orElse(ZoneId.systemDefault().getId()));
                        return r;
                    }, groupId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    private ConditionGroup queryConditionGroup(Long groupId) {
        return jdbcTemplate.queryForObject(
                "select id,logic,enable from condition_group where group_id=? limit 1",
                (rs, i) -> {
                    ConditionGroup g = new ConditionGroup();
                    g.setId(rs.getLong("id"));
                    g.setGroupId(groupId);
                    g.setLogic(rs.getString("logic"));
                    g.setEnable(rs.getBoolean("enable"));
                    g.setConditions(List.of());
                    return g;
                }, groupId);
    }

    private List<ConditionItem> queryConditions(Long conditionGroupId) {
        return jdbcTemplate.query(
                "select id,expr,desc_text,data_group_id from cond where condition_group_id=?",
                (rs, i) -> {
                    ConditionItem c = new ConditionItem();
                    c.setId(rs.getLong("id"));
                    c.setExpr(rs.getString("expr"));
                    c.setDesc(rs.getString("desc_text"));
                    c.setDataGroupId(rs.getLong("data_group_id"));
                    return c;
                }, conditionGroupId);
    }

    private List<DataGroup> queryDataGroupsUsed(List<ConditionItem> conds) {
        // collect ids from expressions like #data[100]
        Set<Long> ids = conds.stream()
                .map(c -> extractIds(c.getExpr()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return List.of();
        String inSql = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
        return jdbcTemplate.query(
                "select id,name,type,fetch_config,agg,mock_value,enable from data_group where id in (" + inSql + ")",
                new DataGroupRowMapper());
    }

    private List<Action> queryActions(Long groupId) {
        return jdbcTemplate.query(
                "select id,group_id,name,type,payload,order_no,retry_times,retry_backoff_ms,timeout_ms,parallel_tag,enable from action where group_id=? and enable=1 order by order_no",
                (rs, i) -> {
                    Action a = new Action();
                    a.setId(rs.getLong("id"));
                    a.setGroupId(rs.getLong("group_id"));
                    a.setName(rs.getString("name"));
                    a.setType(rs.getString("type"));
                    a.setOrder(rs.getInt("order_no"));
                    a.setRetryTimes(rs.getInt("retry_times"));
                    a.setRetryBackoffMs(rs.getInt("retry_backoff_ms"));
                    a.setTimeoutMs(rs.getInt("timeout_ms"));
                    a.setParallelTag(rs.getString("parallel_tag"));
                    a.setEnable(rs.getBoolean("enable"));
                    a.setPayload(parseJson(rs.getString("payload")));
                    return a;
                }, groupId);
    }

    private AlertConfig queryAlerts(Long groupId) {
        AlertConfig cfg = new AlertConfig();
        try {
            AlertStrategy s = jdbcTemplate.queryForObject(
                    "select enable, throttle_ms, continuous_fail_threshold, bind_channels from task_group_alert where group_id=? limit 1",
                    (rs, i) -> {
                        AlertStrategy st = new AlertStrategy();
                        st.setEnable(rs.getBoolean("enable"));
                        st.setThrottleMs(rs.getLong("throttle_ms"));
                        st.setContinuousFailThreshold(rs.getInt("continuous_fail_threshold"));
                        st.setBindChannels(parseLongList(rs.getString("bind_channels")));
                        return st;
                    }, groupId);
            cfg.setStrategy(s);
        } catch (EmptyResultDataAccessException e) {
            cfg.setStrategy(null);
        }
        List<AlertChannel> channels = jdbcTemplate.query(
                "select id,type,config from alert_channel where enable=1",
                (rs, i) -> {
                    AlertChannel c = new AlertChannel();
                    c.setId(rs.getLong("id"));
                    c.setType(rs.getString("type"));
                    c.setConfig(parseJson(rs.getString("config")));
                    return c;
                });
        cfg.setChannels(channels);
        return cfg;
    }

    private List<String> splitComma(String s) {
        if (s == null || s.isEmpty()) return List.of();
        return Arrays.stream(s.split(",")).map(String::trim).filter(t -> !t.isEmpty()).toList();
    }

    private List<String> splitRange(String s) {
        if (s == null || !s.contains("~")) return List.of();
        String[] arr = s.split("~");
        return Arrays.asList(arr[0], arr.length > 1 ? arr[1] : "");
    }

    private List<Integer> parseIntList(String s) {
        return splitComma(s).stream().map(Integer::valueOf).toList();
    }

    private List<Long> parseLongList(String s) {
        return splitComma(s).stream().map(Long::valueOf).toList();
    }

    private Set<Long> extractIds(String expr) {
        Set<Long> set = new HashSet<>();
        // find patterns like #data[123]
        int idx = expr.indexOf("#data[");
        while (idx >= 0) {
            int start = expr.indexOf('[', idx);
            int end = expr.indexOf(']', start);
            if (start > 0 && end > start) {
                try {
                    long id = Long.parseLong(expr.substring(start + 1, end));
                    set.add(id);
                } catch (NumberFormatException ignored) {
                }
                idx = expr.indexOf("#data[", end);
            } else break;
        }
        return set;
    }

    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) return Map.of();
        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    static class DataGroupRowMapper implements RowMapper<DataGroup> {
        @Override
        public DataGroup mapRow(ResultSet rs, int rowNum) throws SQLException {
            DataGroup d = new DataGroup();
            d.setId(rs.getLong("id"));
            d.setName(rs.getString("name"));
            d.setType(rs.getString("type"));
            d.setAgg(rs.getString("agg"));
            d.setMockValue(rs.getDouble("mock_value"));
            d.setEnable(rs.getBoolean("enable"));
            String cfg = rs.getString("fetch_config");
            if (cfg != null) {
                try {
                    d.setFetchConfig(new ObjectMapper().readValue(cfg, Map.class));
                } catch (Exception ignored) {
                }
            }
            return d;
        }
    }
}

