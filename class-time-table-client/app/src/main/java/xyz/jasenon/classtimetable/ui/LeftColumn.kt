package xyz.jasenon.classtimetable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * 左侧列组件
 *
 * 实验室信息展示区，包含三个垂直排列的信息面板：
 * 1. [LabInfoCard] 实验室信息面板：显示实验室号、名称、安全等级、责任人员、联系方式
 * 2. [LabIntroductionCard] 实验室简介面板：显示实验室的详细介绍
 * 3. [LabRegulationsCard] 实验室规章制度面板：显示实验室的规章制度内容
 *
 * ## 布局特点
 *
 * - 使用 [Column] 垂直排列三个面板
 * - 每个面板使用 [Modifier.weight(1f)] 均分可用高度
 * - 面板之间使用 [Arrangement.spacedBy] 设置间距
 * - 内容区域使用 [Card] 卡片样式，带阴影和背景色
 *
 * ## 数据来源
 *
 * 当前为静态 Mock 数据，后续可从 [RemoteDataObservable] 获取：
 * - 实验室信息：从服务器获取实验室详情
 * - 简介和规章制度：从服务器获取富文本内容
 *
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 *
 * @see LabInfoCard
 * @see LabIntroductionCard
 * @see LabRegulationsCard
 */
@Composable
fun LeftColumn(
    modifier: Modifier = Modifier
) {
    /**
     * 垂直布局容器，三个面板均分高度
     */
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 实验室信息面板 - 均分高度
        LabInfoCard(
            modifier = Modifier.weight(1f)
        )

        // 实验室简介面板 - 均分高度
        LabIntroductionCard(
            modifier = Modifier.weight(1f)
        )

        // 实验室规章制度面板 - 均分高度
        LabRegulationsCard(
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * 实验室信息卡片
 *
 * 显示实验室的基本信息，使用垂直列表形式展示：
 * - 实验室号
 * - 实验室名
 * - 安全等级
 * - 责任人员
 * - 联系方式
 *
 * ## UI 设计
 *
 * - [Card] 组件提供卡片容器，带阴影效果
 * - [Column] 垂直排列信息行
 * - [InfoRow] 组件统一展示标签和值
 * - 背景色使用 [MaterialTheme.colorScheme.surfaceVariant]
 *
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 */
@Composable
fun LabInfoCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 实验室号
            InfoRow(label = "实验室号", value = "16-207")

            // 实验室名
            InfoRow(label = "实验室名", value = "计算机基础实验室(三)")

            // 安全等级
            InfoRow(label = "安全等级", value = "四级")

            // 责任人员
            InfoRow(label = "责任人员", value = "石英")

            // 联系方式
            InfoRow(label = "联系方式", value = "12547745874")
        }
    }
}

/**
 * 实验室简介卡片
 *
 * 显示实验室的详细介绍信息。
 *
 * ## UI 设计
 *
 * - 标题居中显示"实验室简介"
 * - 内容区域预留，当前显示"暂无简介内容"
 * - 后续可替换为 [TextField] 或富文本组件
 *
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 */
@Composable
fun LabIntroductionCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "实验室简介",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 简介内容（实际应用中应从数据源获取）
            Text(
                text = "暂无简介内容",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 实验室规章制度卡片
 *
 * 显示实验室的规章制度内容。
 *
 * ## UI 设计
 *
 * - 标题居中显示"实验室规章制度"
 * - 内容区域预留，当前显示"暂无规章制度内容"
 * - 后续可替换为列表或富文本组件
 *
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 */
@Composable
fun LabRegulationsCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "实验室规章制度",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 规章制度内容（实际应用中应从数据源获取）
            Text(
                text = "暂无规章制度内容",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * 信息行组件
 *
 * 用于显示标签和值的组合，格式为"标签: 值"。
 *
 * ## UI 设计
 *
 * - [Row] 水平布局，标签居左，值居右
 * - 使用 [Arrangement.SpaceBetween] 实现两端对齐
 * - 标签使用半透明色，值使用主色调
 *
 * @param label 标签文本，如"实验室号"
 * @param value 值文本，如"16-207"
 * @param modifier Compose 修饰符，用于外部传入的布局调整
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = MaterialTheme.typography.bodyMedium.fontWeight
        )
    }
}

// ============== 预览 ==============

/**
 * LeftColumn 预览
 */
@Preview(showBackground = true, widthDp = 300, heightDp = 800)
@Composable
fun LeftColumnPreview() {
    MaterialTheme {
        LeftColumn(
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * LabInfoCard 预览
 */
@Preview(showBackground = true, widthDp = 300, heightDp = 200)
@Composable
fun LabInfoCardPreview() {
    MaterialTheme {
        LabInfoCard()
    }
}

/**
 * LabIntroductionCard 预览
 */
@Preview(showBackground = true, widthDp = 300, heightDp = 200)
@Composable
fun LabIntroductionCardPreview() {
    MaterialTheme {
        LabIntroductionCard()
    }
}

/**
 * LabRegulationsCard 预览
 */
@Preview(showBackground = true, widthDp = 300, heightDp = 200)
@Composable
fun LabRegulationsCardPreview() {
    MaterialTheme {
        LabRegulationsCard()
    }
}
