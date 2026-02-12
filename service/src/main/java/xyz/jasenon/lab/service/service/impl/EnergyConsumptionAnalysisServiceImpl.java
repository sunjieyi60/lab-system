package xyz.jasenon.lab.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.jasenon.lab.common.entity.base.Dept;
import xyz.jasenon.lab.common.entity.base.Laboratory;
import xyz.jasenon.lab.common.entity.device.CircuitBreak;
import xyz.jasenon.lab.common.entity.record.CircuitBreakRecord;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.service.dto.analysis.EnergyConsumptionQueryDto;
import xyz.jasenon.lab.service.mapper.DeptMapper;
import xyz.jasenon.lab.service.mapper.LaboratoryMapper;
import xyz.jasenon.lab.service.mapper.record.CircuitBreakRecordMapper;
import xyz.jasenon.lab.service.service.IEnergyConsumptionAnalysisService;
import xyz.jasenon.lab.service.strategy.device.ex.CircuitBreakQ;
import xyz.jasenon.lab.service.vo.analysis.EnergyConsumptionChartSegmentVo;
import xyz.jasenon.lab.service.vo.analysis.EnergyConsumptionResultVo;
import xyz.jasenon.lab.service.vo.analysis.EnergyConsumptionRowVo;
import xyz.jasenon.lab.service.vo.analysis.EnergyConsumptionSummaryVo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 能耗统计：按 device_id 关联设备与记录，首尾 energy 相减得 Kwh
 */
@Slf4j
@Service
public class EnergyConsumptionAnalysisServiceImpl implements IEnergyConsumptionAnalysisService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd-H:mm");

    @Autowired
    private CircuitBreakRecordMapper circuitBreakRecordMapper;
    @Autowired
    private LaboratoryMapper laboratoryMapper;
    @Autowired
    private DeptMapper deptMapper;
    @Autowired
    private CircuitBreakQ circuitBreakQ;

    @Override
    public R<EnergyConsumptionResultVo> getEnergyConsumption(EnergyConsumptionQueryDto query) {
        if (query.getStartTime() == null || query.getEndTime() == null || !query.getStartTime().isBefore(query.getEndTime())) {
            return R.fail("请填写有效的开始、结束时间");
        }
        List<Long> labIds = resolveLabIds(query);
        List<CircuitBreak> devices = listDevices(labIds, query.getDeviceIds());
        if (devices.isEmpty()) {
            return R.success(buildEmptyResult(), "无符合条件的智能空开");
        }

        Map<Long, Laboratory> labMap = loadLabMap(devices);
        Map<Long, String> deptNameMap = loadDeptNameMap(labMap);
        String timeRangeStr = formatTimeRange(query.getStartTime(), query.getEndTime());
        LocalDateTime queryStart = query.getStartTime();
        LocalDateTime queryEnd = query.getEndTime();

        List<EnergyConsumptionRowVo> list = new ArrayList<>();
        for (CircuitBreak device : devices) {
            BigDecimal kwh = computeEnergyKwh(device, queryStart, queryEnd);
            if (kwh == null || kwh.compareTo(BigDecimal.ZERO) <= 0) continue;
            Laboratory lab = labMap.get(device.getBelongToLaboratoryId());
            String deptName = lab != null && lab.getBelongToDepts() != null && !lab.getBelongToDepts().isEmpty()
                    ? deptNameMap.getOrDefault(lab.getBelongToDepts().get(0), "")
                    : "";
            list.add(new EnergyConsumptionRowVo()
                    .setTimeRange(timeRangeStr)
                    .setLaboratoryNo(lab != null ? lab.getLaboratoryId() : "")
                    .setDeptName(deptName)
                    .setSwitchName(device.getDeviceName() != null ? device.getDeviceName() : ("空开" + device.getId()))
                    .setEnergyKwh(kwh));
        }

        BigDecimal totalKwh = list.stream().map(EnergyConsumptionRowVo::getEnergyKwh).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalKwh.compareTo(BigDecimal.ZERO) == 0) {
            return R.success(buildEmptyResult(), "该时间范围内无能耗数据");
        }
        for (EnergyConsumptionRowVo row : list) {
            BigDecimal p = row.getEnergyKwh().divide(totalKwh, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            row.setProportion(p.setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + "%");
        }
        List<EnergyConsumptionChartSegmentVo> chartSegments = list.stream()
                .map(r -> new EnergyConsumptionChartSegmentVo()
                        .setName(r.getSwitchName())
                        .setKwh(r.getEnergyKwh())
                        .setProportion(r.getEnergyKwh().divide(totalKwh, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(1, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
        EnergyConsumptionSummaryVo summaryRow = buildSummaryRow(timeRangeStr, list, totalKwh);

        EnergyConsumptionResultVo vo = new EnergyConsumptionResultVo()
                .setTotalKwh(totalKwh)
                .setList(list)
                .setChartSegments(chartSegments)
                .setSummaryRow(summaryRow);
        return R.success(vo);
    }

    /**
     * 首尾相减：取 T1 前最近一条与 T2 前最近一条的 energy，能耗 = 尾 - 首。
     */
    private BigDecimal computeEnergyKwh(CircuitBreak device, LocalDateTime queryStart, LocalDateTime queryEnd) {
        CircuitBreakRecord startRecord = circuitBreakRecordMapper.selectOne(
                new LambdaQueryWrapper<CircuitBreakRecord>()
                        .eq(CircuitBreakRecord::getDeviceId, device.getId())
                        .le(CircuitBreakRecord::getCreateTime, queryStart)
                        .orderByDesc(CircuitBreakRecord::getCreateTime)
                        .last("LIMIT 1")
        );
        CircuitBreakRecord endRecord = circuitBreakRecordMapper.selectOne(
                new LambdaQueryWrapper<CircuitBreakRecord>()
                        .eq(CircuitBreakRecord::getDeviceId, device.getId())
                        .le(CircuitBreakRecord::getCreateTime, queryEnd)
                        .orderByDesc(CircuitBreakRecord::getCreateTime)
                        .last("LIMIT 1")
        );
        float startEnergy = startRecord != null && startRecord.getEnergy() != null ? startRecord.getEnergy() : 0f;
        float endEnergy = endRecord != null && endRecord.getEnergy() != null ? endRecord.getEnergy() : 0f;
        float diff = endEnergy - startEnergy;
        if (diff < 0) diff = 0;
        return BigDecimal.valueOf(diff).setScale(2, RoundingMode.HALF_UP);
    }

    private List<Long> resolveLabIds(EnergyConsumptionQueryDto query) {
        List<Laboratory> labs;
        if (query.getBuildingId() != null) {
            labs = laboratoryMapper.selectList(new LambdaQueryWrapper<Laboratory>().eq(Laboratory::getBelongToBuilding, query.getBuildingId()));
        } else {
            labs = laboratoryMapper.selectList(null);
        }
        if (query.getDeptId() != null && !labs.isEmpty()) {
            labs = labs.stream().filter(l -> l.getBelongToDepts() != null && l.getBelongToDepts().contains(query.getDeptId())).toList();
        }
        if (query.getLaboratoryIds() != null && !query.getLaboratoryIds().isEmpty()) {
            Set<Long> set = new HashSet<>(query.getLaboratoryIds());
            labs = labs.stream().filter(l -> set.contains(l.getId())).toList();
        }
        return labs.stream().map(Laboratory::getId).toList();
    }

    private List<CircuitBreak> listDevices(List<Long> labIds, List<Long> deviceIds) {
        if (labIds.isEmpty()) return List.of();
        List<CircuitBreak> list = circuitBreakQ.list(labIds);
        if (deviceIds != null && !deviceIds.isEmpty()) {
            Set<Long> set = new HashSet<>(deviceIds);
            list = list.stream().filter(d -> set.contains(d.getId())).toList();
        }
        return list;
    }

    private Map<Long, Laboratory> loadLabMap(List<CircuitBreak> devices) {
        Set<Long> labIds = devices.stream().map(CircuitBreak::getBelongToLaboratoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (labIds.isEmpty()) return Map.of();
        List<Laboratory> labs = laboratoryMapper.selectBatchIds(labIds);
        return labs.stream().collect(Collectors.toMap(Laboratory::getId, l -> l, (a, b) -> a));
    }

    private Map<Long, String> loadDeptNameMap(Map<Long, Laboratory> labMap) {
        Set<Long> deptIds = new HashSet<>();
        for (Laboratory lab : labMap.values()) {
            if (lab.getBelongToDepts() != null) deptIds.addAll(lab.getBelongToDepts());
        }
        if (deptIds.isEmpty()) return Map.of();
        List<Dept> depts = deptMapper.selectBatchIds(deptIds);
        return depts.stream().collect(Collectors.toMap(Dept::getId, Dept::getDeptName, (a, b) -> a));
    }

    private String formatTimeRange(LocalDateTime start, LocalDateTime end) {
        return start.format(FMT) + " 至 " + end.format(FMT);
    }

    private EnergyConsumptionSummaryVo buildSummaryRow(String timeRangeStr, List<EnergyConsumptionRowVo> list, BigDecimal totalKwh) {
        String laboratoryNos = list.stream()
                .map(EnergyConsumptionRowVo::getLaboratoryNo)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .collect(Collectors.joining("、"));
        String deptName = list.isEmpty() ? "" : (list.get(0).getDeptName() != null ? list.get(0).getDeptName() : "");
        String switchSummary = list.isEmpty() ? "全部"
                : list.stream().map(EnergyConsumptionRowVo::getSwitchName).filter(Objects::nonNull).min(String::compareTo)
                        .map(s -> s + "...").orElse("全部");
        return new EnergyConsumptionSummaryVo()
                .setTimeRange(timeRangeStr)
                .setLaboratoryNos(laboratoryNos.isEmpty() ? "" : laboratoryNos)
                .setDeptName(deptName)
                .setSwitchSummary(switchSummary)
                .setTotalKwh(totalKwh)
                .setProportion("100%");
    }

    private EnergyConsumptionResultVo buildEmptyResult() {
        return new EnergyConsumptionResultVo()
                .setTotalKwh(BigDecimal.ZERO)
                .setList(List.of())
                .setChartSegments(List.of())
                .setSummaryRow(null);
    }
}
