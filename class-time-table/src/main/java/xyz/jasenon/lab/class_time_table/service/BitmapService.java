package xyz.jasenon.lab.class_time_table.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.jasenon.lab.class_time_table.vo.BitmapPushResultVo;
import xyz.jasenon.lab.common.utils.R;
import xyz.jasenon.lab.core.ImChannelContext;
import xyz.jasenon.lab.core.ImConst;
import xyz.jasenon.lab.core.ImPacket;
import xyz.jasenon.lab.core.ImStatus;
import xyz.jasenon.lab.core.config.ImConfig;
import xyz.jasenon.lab.core.exception.ImException;
import xyz.jasenon.lab.core.message.MessageHelper;
import xyz.jasenon.lab.core.packets.*;
import xyz.jasenon.lab.core.utils.JsonKit;
import xyz.jasenon.lab.server.JimServerAPI;
import xyz.jasenon.lab.server.config.ImServerConfig;
import xyz.jasenon.lab.server.protocol.ProtocolManager;

import java.io.IOException;
import java.util.*;

@Service
public class BitmapService {

    private final ImServerConfig config = ImConfig.Global.get();
    private final MessageHelper helper = config.getMessageHelper();

    /**
     * 下发人脸到班牌设备
     */
    public R<BitmapPushResultVo> pushBitmapToClassTimeTable(MultipartFile file, String faceName,
                                                             List<String> uuids, List<String> groupIds) {
        try {
            byte[] bytes = file.getBytes();
            BitmapBody bitmapBody = new BitmapBody(bytes, faceName);
            RespBody respBody = new RespBody(Command.COMMAND_BITMAP_PUSH_REQ).setData(bitmapBody);

            List<String> targetUuids = collectTargetUuids(uuids, groupIds);
            if (targetUuids.isEmpty()) {
                return R.fail("没有可用的目标设备");
            }

            List<BitmapRespBody> results = batchSynSend(targetUuids, respBody);
            BitmapPushResultVo vo = buildResultVo(results);
            return R.success(vo, "人脸注册结果");

        } catch (IOException e) {
            throw new RuntimeException("读取人脸文件失败", e);
        }
    }

    private List<String> collectTargetUuids(List<String> uuids, List<String> groupIds) {
        if (groupIds != null && !groupIds.isEmpty()) {
            Set<String> uuidSet = new HashSet<>();
            for (String groupId : groupIds) {
                Group group = helper.getGroupClassTimeTables(groupId, ImConst.ONLINE);
                if (group != null && group.getClassTimeTables() != null) {
                    for (ClassTimeTable device : group.getClassTimeTables()) {
                        uuidSet.add(device.getUuid());
                    }
                }
            }
            return new ArrayList<>(uuidSet);
        }

        if (uuids != null && !uuids.isEmpty()) {
            return new ArrayList<>(new LinkedHashSet<>(uuids));
        }

        return Collections.emptyList();
    }

    private BitmapRespBody synSend(String uuid, RespBody respBody) {
        List<ImChannelContext> contexts = JimServerAPI.getByUserId(uuid);
        if (contexts == null || contexts.isEmpty()) {
            return BitmapRespBody.fail(ImStatus.C10001, uuid, null);
        }

        ImChannelContext channelContext = contexts.get(0);
        try {
            ImPacket req = ProtocolManager.Converter.respPacket(respBody, channelContext);
            ImPacket resp = JimServerAPI.synSend(channelContext, req);

            if (resp == null || resp.getBody() == null) {
                return BitmapRespBody.fail(ImStatus.C10002, uuid, "设备无响应");
            }

            return JsonKit.toBean(resp.getBody(), BitmapRespBody.class);
        } catch (ImException e) {
            throw new RuntimeException(e);
        }
    }

    private List<BitmapRespBody> batchSynSend(List<String> uuids, RespBody respBody) {
        List<BitmapRespBody> results = new ArrayList<>();
        for (String uuid : uuids) {
            BitmapRespBody one = synSend(uuid, respBody);
            results.add(one);
            // 人脸注册失败直接break
            if (one.getCode() != null && one.getCode() == ImStatus.C10027.getCode()) {
                break;
            }
        }
        return results;
    }

    private BitmapPushResultVo buildResultVo(List<BitmapRespBody> results) {
        int success = 0, offline = 0, fail = 0;
        for (BitmapRespBody result : results) {
            Integer code = result.getCode();
            if (code == null) {
                fail++;
            } else if (code == ImStatus.C10026.getCode()) {
                success++;
            } else if (code == ImStatus.C10001.getCode()) {
                offline++;
            } else {
                fail++;
            }
        }

        BitmapPushResultVo vo = new BitmapPushResultVo();
        vo.setTotal(results.size());
        vo.setSuccess(success);
        vo.setOffline(offline);
        vo.setFail(fail);
        vo.setDetails(results);
        return vo;
    }
}
