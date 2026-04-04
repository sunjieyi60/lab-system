package xyz.jasenon.classtimetable.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.ChannelContext;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 分片重组器
 * 用于重组被分片传输的大数据包（如人脸图片、OTA升级包等）
 *
 * @author Jasenon_ce
 * @date 2026/1/31
 */
public class FragmentReassembler {

    private final static Logger log = LoggerFactory.getLogger(FragmentReassembler.class);
    /** 分片数据缓存，key为channelId:cmdType:baseSeqId */
    private final ConcurrentHashMap<String, FragmentGroup> fragmentGroups = new ConcurrentHashMap<>();

    /** 分片组超时时间（毫秒），超过此时间未完成的分片组将被清理 */
    private static final long FRAGMENT_TIMEOUT = 60000;

    /**
     * 分片组
     */
    private static class FragmentGroup {
        @SuppressWarnings("unused")
        final String key; // 用于日志和调试
        @SuppressWarnings("unused")
        final Byte cmdType; // 用于验证分片类型一致性
        @SuppressWarnings("unused")
        final ChannelContext channelContext; // 用于未来扩展：通道状态检查
        final List<FragmentData> fragments = new ArrayList<>();
        final long createTime;
        @SuppressWarnings("unused")
        int expectedTotalSize = -1; // 用于未来扩展：预分配缓冲区大小

        FragmentGroup(String key, Byte cmdType, ChannelContext channelContext) {
            this.key = key;
            this.cmdType = cmdType;
            this.channelContext = channelContext;
            this.createTime = System.currentTimeMillis();
        }

        /**
         * 检查是否超时
         */
        boolean isTimeout() {
            return System.currentTimeMillis() - createTime > FRAGMENT_TIMEOUT;
        }

        /**
         * 检查是否完整
         */
        boolean isComplete() {
            if (fragments.isEmpty()) {
                return false;
            }

            // 检查是否有START和END标志
            boolean hasStart = false;
            boolean hasEnd = false;
            for (FragmentData fragment : fragments) {
                if (fragment.packet.hasFlag(PacketFlags.START)) {
                    hasStart = true;
                }
                if (fragment.packet.hasFlag(PacketFlags.END)) {
                    hasEnd = true;
                }
            }

            return hasStart && hasEnd;
        }

        /**
         * 重组完整数据
         */
        byte[] reassemble() {
            // 按序列号排序
            fragments.sort((a, b) -> Short.compare(a.packet.getSeqId(), b.packet.getSeqId()));

            // 计算总长度
            int totalLength = 0;
            for (FragmentData fragment : fragments) {
                totalLength += fragment.packet.getPayload() != null ? fragment.packet.getPayload().length : 0;
            }

            // 重组数据
            byte[] result = new byte[totalLength];
            int offset = 0;
            for (FragmentData fragment : fragments) {
                if (fragment.packet.getPayload() != null) {
                    System.arraycopy(fragment.packet.getPayload(), 0, result, offset, fragment.packet.getPayload().length);
                    offset += fragment.packet.getPayload().length;
                }
            }

            return result;
        }
    }

    /**
     * 分片数据
     */
    private static class FragmentData {
        final SmartBoardPacket packet;
        @SuppressWarnings("unused")
        final long receiveTime; // 用于未来扩展：分片接收时间统计

        FragmentData(SmartBoardPacket packet) {
            this.packet = packet;
            this.receiveTime = System.currentTimeMillis();
        }
    }

    /**
     * 添加分片数据包
     *
     * @param packet 分片数据包
     * @param channelContext 通道上下文
     * @return 如果分片组已完整，返回重组后的完整数据包；否则返回null
     */
    public SmartBoardPacket addFragment(SmartBoardPacket packet, ChannelContext channelContext) {
        // 如果不是分片包，直接返回
        if (!packet.isFragment()) {
            return packet;
        }

        String key = getFragmentKey(channelContext, packet);
        FragmentGroup group = fragmentGroups.computeIfAbsent(
                key,
                k -> new FragmentGroup(k, packet.getCmdType(), channelContext)
        );

        // 检查是否超时
        if (group.isTimeout()) {
            log.warn("分片组超时，清理: key={}", key);
            fragmentGroups.remove(key);
            return null;
        }

        // 添加分片
        group.fragments.add(new FragmentData(packet));
        log.debug("添加分片: key={}, seqId={}, flags={}, currentFragments={}",
                key, packet.getSeqId(), packet.getFlags(), group.fragments.size());

        // 检查是否完整
        if (group.isComplete()) {
            // 重组数据
            byte[] reassembledData = group.reassemble();

            // 创建完整的数据包
            SmartBoardPacket completePacket = new SmartBoardPacket();
            completePacket.setVersion(packet.getVersion());
            completePacket.setCmdType(packet.getCmdType());
            completePacket.setSeqId(group.fragments.get(0).packet.getSeqId()); // 使用第一个分片的序列号
            completePacket.setQos(packet.getQos());
            completePacket.setFlags(PacketFlags.NONE); // 完整包无分片标志
            completePacket.setReserved(packet.getReserved());
            completePacket.setPayload(reassembledData);
            completePacket.setLength(reassembledData.length);
            completePacket.calculateCheckSum();

            // 清理分片组
            fragmentGroups.remove(key);

            log.info("分片重组完成: key={}, totalSize={}", key, reassembledData.length);
            return completePacket;
        }

        return null; // 分片未完整
    }

    /**
     * 生成分片组key
     *
     * @param channelContext 通道上下文
     * @param packet 数据包
     * @return 分片组key
     */
    private String getFragmentKey(ChannelContext channelContext, SmartBoardPacket packet) {
        // 使用通道ID、指令类型和基础序列号（第一个分片的序列号）作为key
        // 这里简化处理，使用当前序列号作为基础（实际应该从START分片获取）
        return channelContext.getId() + ":" + packet.getCmdType() + ":" + packet.getSeqId();
    }

    /**
     * 清理指定通道的所有分片组（通道断开时调用）
     *
     * @param channelContext 通道上下文
     */
    public void clearChannel(ChannelContext channelContext) {
        String channelId = channelContext.getId();
        fragmentGroups.entrySet().removeIf(entry -> entry.getKey().startsWith(channelId + ":"));
        log.debug("清理通道的分片数据: channel={}", channelId);
    }

    /**
     * 清理超时的分片组
     */
    public void cleanupTimeoutFragments() {
        fragmentGroups.entrySet().removeIf(entry -> {
            if (entry.getValue().isTimeout()) {
                log.warn("清理超时分片组: key={}", entry.getKey());
                return true;
            }
            return false;
        });
    }
}

