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
 * 功能区域说明：
 * 包含三个垂直排列的信息面板：
 * 1. 实验室信息面板：显示实验室号、名称、安全等级、责任人员、联系方式
 * 2. 实验室简介面板：显示实验室的详细介绍
 * 3. 实验室规章制度面板：显示实验室的规章制度内容
 * 
 * 使用的 Compose 组件：
 * - Column: 垂直布局容器，用于垂直排列子组件
 * - Card: Material3 卡片组件，用于创建信息面板
 * - Text: 文本显示组件
 * - Spacer: 间距组件，用于添加垂直间距
 * - verticalScroll: 滚动修饰符，使内容可滚动
 */
@Composable
fun LeftColumn(
    modifier: Modifier = Modifier
) {
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
 * 显示实验室的基本信息：
 * - 实验室号
 * - 实验室名
 * - 安全等级
 * - 责任人员
 * - 联系方式
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
 * 显示实验室的详细介绍信息
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
 * 显示实验室的规章制度内容
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
 * 用于显示标签和值的组合
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

@Preview(showBackground = true, widthDp = 300, heightDp = 800)
@Composable
fun LeftColumnPreview() {
    MaterialTheme {
        LeftColumn(
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 200)
@Composable
fun LabInfoCardPreview() {
    MaterialTheme {
        LabInfoCard()
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 200)
@Composable
fun LabIntroductionCardPreview() {
    MaterialTheme {
        LabIntroductionCard()
    }
}

@Preview(showBackground = true, widthDp = 300, heightDp = 200)
@Composable
fun LabRegulationsCardPreview() {
    MaterialTheme {
        LabRegulationsCard()
    }
}
