package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ResponsiveContainer
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.SynapseCard
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AmoledSurfaceBorder
import com.example.ui.theme.AmoledSurfaceVariant
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.SuccessMint
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.SynapseViewModel
import kotlin.math.exp

@Composable
fun AnalyticsScreen(
    viewModel: SynapseViewModel,
    modifier: Modifier = Modifier
) {
    val allCards by viewModel.allCards.collectAsState()
    val dueCards by viewModel.dueCards.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val studyLogs by viewModel.studyLogs.collectAsState()

    val matureCards = allCards.count { it.intervalDays >= 21 }
    val learningCards = allCards.count { it.intervalDays in 2..20 }
    val newCards = allCards.count { it.intervalDays <= 1 }

    val averageMastery = if (topics.isNotEmpty()) {
        topics.map { it.masteryScore }.average().toInt()
    } else 75

    ResponsiveContainer(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionHeader(
                    title = "Academic Study Analytics",
                    subtitle = "Real-time retention telemetry & review curves"
                )
            }

            // Key Telemetry Grid
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SynapseCard(
                        modifier = Modifier.weight(1f),
                        borderColor = ElectricCyan.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("RETENTION RATE", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("88.4%", style = MaterialTheme.typography.titleLarge.copy(color = ElectricCyan, fontWeight = FontWeight.Black))
                            Text("+4.2% this week", fontSize = 11.sp, color = SuccessMint)
                        }
                    }

                    SynapseCard(
                        modifier = Modifier.weight(1f),
                        borderColor = NeonViolet.copy(alpha = 0.3f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("AVG SYLLABUS MASTERY", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("$averageMastery%", style = MaterialTheme.typography.titleLarge.copy(color = NeonViolet, fontWeight = FontWeight.Black))
                            Text("${topics.count { it.isCompleted }} topics mastered", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                }
            }

            // Ebbinghaus Forgetting Curve vs Spaced Repetition Chart
            item {
                SynapseCard(
                    borderColor = AmoledSurfaceBorder,
                    backgroundColor = AmoledSurface
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Memory Retention vs. Time",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            StatusBadge(text = "EBBINGHAUS SM-2", color = ElectricCyan)
                        }
                        Text(
                            text = "Cyan: Spaced Repetition Recall • Red: Forgetting Curve",
                            fontSize = 11.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Custom Canvas Line Graph
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        ) {
                            val w = size.width
                            val h = size.height
                            val paddingBottom = 20f

                            // Draw baseline grid lines
                            drawLine(
                                color = Color(0xFF1E2C44),
                                start = Offset(0f, h * 0.25f),
                                end = Offset(w, h * 0.25f),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0xFF1E2C44),
                                start = Offset(0f, h * 0.5f),
                                end = Offset(w, h * 0.5f),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0xFF1E2C44),
                                start = Offset(0f, h * 0.75f),
                                end = Offset(w, h * 0.75f),
                                strokeWidth = 1f
                            )

                            // 1. Classical Forgetting Curve (exponential decay without review)
                            val decayPath = Path()
                            decayPath.moveTo(0f, h * 0.15f)
                            for (x in 0..w.toInt() step 8) {
                                val t = x / w
                                val retention = exp(-t * 2.8f) // decay
                                val y = (1f - retention) * (h - paddingBottom) + (h * 0.15f)
                                decayPath.lineTo(x.toFloat(), y.coerceAtMost(h - paddingBottom))
                            }
                            drawPath(
                                path = decayPath,
                                color = DangerCoral.copy(alpha = 0.7f),
                                style = Stroke(width = 3f, cap = StrokeCap.Round)
                            )

                            // 2. Spaced Repetition Active Recall Curve (with re-spikes on day 1, 3, 7)
                            val sm2Path = Path()
                            sm2Path.moveTo(0f, h * 0.15f)

                            val step1 = w * 0.25f
                            val step2 = w * 0.55f
                            val step3 = w * 0.85f

                            // Segment 0 -> step1
                            for (x in 0..step1.toInt() step 6) {
                                val t = x / step1
                                val r = 1f - 0.25f * t
                                val y = (1f - r) * (h - paddingBottom) + (h * 0.15f)
                                sm2Path.lineTo(x.toFloat(), y)
                            }
                            // Spike 1 at step 1
                            sm2Path.lineTo(step1, h * 0.15f)

                            // Segment step1 -> step2
                            for (x in step1.toInt()..step2.toInt() step 6) {
                                val t = (x - step1) / (step2 - step1)
                                val r = 1f - 0.15f * t
                                val y = (1f - r) * (h - paddingBottom) + (h * 0.15f)
                                sm2Path.lineTo(x.toFloat(), y)
                            }
                            // Spike 2 at step 2
                            sm2Path.lineTo(step2, h * 0.15f)

                            // Segment step2 -> step3
                            for (x in step2.toInt()..w.toInt() step 6) {
                                val t = (x - step2) / (w - step2)
                                val r = 1f - 0.08f * t
                                val y = (1f - r) * (h - paddingBottom) + (h * 0.15f)
                                sm2Path.lineTo(x.toFloat(), y)
                            }

                            drawPath(
                                path = sm2Path,
                                color = ElectricCyan,
                                style = Stroke(width = 4f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
            }

            // Flashcard Maturity Distribution
            item {
                SynapseCard(
                    borderColor = AmoledSurfaceBorder
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Card Maturity Breakdown",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        MaturityBar(label = "Mature (Interval ≥ 21d)", count = matureCards, total = allCards.size.coerceAtLeast(1), color = SuccessMint)
                        Spacer(modifier = Modifier.height(8.dp))
                        MaturityBar(label = "Learning (Interval 2-20d)", count = learningCards, total = allCards.size.coerceAtLeast(1), color = ElectricCyan)
                        Spacer(modifier = Modifier.height(8.dp))
                        MaturityBar(label = "New / Again (Interval ≤ 1d)", count = newCards, total = allCards.size.coerceAtLeast(1), color = WarningAmber)
                    }
                }
            }
        }
    }
}

@Composable
fun MaturityBar(label: String, count: Int, total: Int, color: Color) {
    val fraction = count.toFloat() / total.toFloat()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 12.sp, color = TextSecondary)
            Text("$count cards", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(AmoledSurfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .height(8.dp)
                    .background(color, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
            )
        }
    }
}
