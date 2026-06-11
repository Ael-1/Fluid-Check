package com.example.fluidcheck.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fluidcheck.ui.theme.*

// ── Premium Color Palette ──────────────────────────────────────────────
private val PremiumColorLight = Color(0xFF60A5FA) // Light electric blue
private val PremiumColorDark = Color(0xFF2563EB)  // Vibrant Blue 600
private val PremiumColorDeep = Color(0xFF1D4ED8)  // Deep royal blue

// Matches the application's light-mode design tokens
private val PremiumSurface = Color.White
private val PremiumSurfaceLight = Color(0xFFF8FAFC)
private val PremiumTextSecondary = MutedForeground // App's default gray text (0xFF64748B)
private val PremiumDivider = Color(0xFFE2E8F0) // Clean light border color
private val PremiumSuccess = SuccessGreen // App's default green (0xFF22C55E)
private val PremiumBadgeBg = Color(0xFFDBEAFE) // Light blue badge background

private val PremiumGradient = Brush.linearGradient(
    colors = listOf(PremiumColorLight, PremiumColorDark, PremiumColorDeep)
)

private val PremiumCardGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFFF0F9FF), // Top matches androidx.compose.material3.MaterialTheme.colorScheme.background (soft sky blue)
        Color.White,       // Transitions to clean white surface
        Color.White
    )
)

// ── Benefit Item Data ──────────────────────────────────────────────────
private data class BenefitItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val premiumBenefits = listOf(
    BenefitItem(
        icon = Icons.Outlined.LocalDrink,
        title = "All Fluid Types + Custom",
        description = "Access every fluid type and create your own custom types"
    ),
    BenefitItem(
        icon = Icons.Outlined.BarChart,
        title = "Full Progress Views",
        description = "Daily, Weekly, Monthly & Yearly progress tracking"
    ),
    BenefitItem(
        icon = Icons.Outlined.AutoAwesome,
        title = "AI-Powered Hydration",
        description = "Calculate ideal intake, AI coaching, habit analysis & smart insights"
    ),
    BenefitItem(
        icon = Icons.Outlined.WbSunny,
        title = "Weather Integration",
        description = "Dynamic daily goals based on real-time weather conditions"
    ),
    BenefitItem(
        icon = Icons.Outlined.FlashOn,
        title = "Quick-Add Feature",
        description = "Customizable quick-add buttons for fast fluid logging"
    ),
    BenefitItem(
        icon = Icons.Outlined.EmojiEvents,
        title = "Gamified Experience",
        description = "Missions, badges, streak shields & achievement rewards"
    ),
    BenefitItem(
        icon = Icons.Outlined.Notifications,
        title = "Smart Notifications",
        description = "Intelligent reminders tailored to your hydration patterns"
    ),
    BenefitItem(
        icon = Icons.Outlined.CloudSync,
        title = "Unlimited Cloud Backup",
        description = "Full cloud data backup with unlimited history access"
    ),
    BenefitItem(
        icon = Icons.Outlined.WifiOff,
        title = "Offline Mode",
        description = "Log offline — data syncs automatically when reconnected"
    ),
    BenefitItem(
        icon = Icons.Outlined.Palette,
        title = "Exclusive Customization",
        description = "UI skins, dark mode, custom backgrounds, app logo & progress meters"
    ),
    BenefitItem(
        icon = Icons.Outlined.Block,
        title = "No Ads",
        description = "Enjoy a completely ad-free experience"
    )
)

// ── Main Paywall Dialog ────────────────────────────────────────────────
@Composable
fun PremiumPaywallDialog(
    onDismiss: () -> Unit,
    onSubscribeMonthly: () -> Unit = {},
    onSubscribeYearly: () -> Unit = {}
) {
    var selectedPlan by remember { mutableStateOf("yearly") }
    val scrollState = rememberScrollState()

    // Shimmer animation for crown
    val infiniteTransition = rememberInfiniteTransition(label = "crown_shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.88f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* consume click */ },
                shape = RoundedCornerShape(32.dp),
                color = Color.Transparent,
                shadowElevation = 24.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(PremiumCardGradient, RoundedCornerShape(32.dp))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ── Top Bar with Close ──
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, end = 16.dp)
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(36.dp)
                                    .background(
                                        PremiumDivider.copy(alpha = 0.5f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = PremiumTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // ── Scrollable Content ──
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(scrollState)
                                .padding(horizontal = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Crown icon
                            Box(
                                modifier = Modifier.size(72.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = Gold,
                                    modifier = Modifier.size(52.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Title
                            Text(
                                text = "Upgrade to Premium",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Unlock the full FluidCheck experience",
                                fontSize = 15.sp,
                                color = PremiumTextSecondary,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(28.dp))

                            // ── Pricing Toggle Cards ──
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                PricingCard(
                                    modifier = Modifier.weight(1f),
                                    planId = "monthly",
                                    label = "Monthly",
                                    price = "₱129",
                                    period = "/month",
                                    isSelected = selectedPlan == "monthly",
                                    badge = null,
                                    onClick = { selectedPlan = "monthly" }
                                )
                                PricingCard(
                                    modifier = Modifier.weight(1f),
                                    planId = "yearly",
                                    label = "Yearly",
                                    price = "₱1,299",
                                    period = "/year",
                                    isSelected = selectedPlan == "yearly",
                                    badge = "SAVE 16%",
                                    onClick = { selectedPlan = "yearly" }
                                )
                            }

                            // Yearly savings hint
                            AnimatedVisibility(visible = selectedPlan == "yearly") {
                                Text(
                                    text = "That's only ₱108.25/month — save ₱249/year!",
                                    fontSize = 13.sp,
                                    color = PremiumSuccess,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 10.dp),
                                    textAlign = TextAlign.Center
                                )
                            }

                            Spacer(modifier = Modifier.height(28.dp))

                            // ── Section Label ──
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = PremiumDivider
                                )
                                Text(
                                    text = "  PREMIUM FEATURES  ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PremiumColorDark,
                                    letterSpacing = 2.sp
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = PremiumDivider
                                )
                            }

                            // ── Benefits List ──
                            premiumBenefits.forEach { benefit ->
                                BenefitRow(benefit)
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Cloud backup note
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "If your subscription expires, your cloud data is preserved. Standard users can access the most recent 7 days of cloud data.",
                                        fontSize = 12.sp,
                                        color = PremiumTextSecondary,
                                        lineHeight = 17.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // ── Bottom CTA ──
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = PremiumSurface,
                            shadowElevation = 16.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 24.dp,
                                    end = 24.dp,
                                    top = 16.dp,
                                    bottom = 24.dp
                                ),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Button(
                                    onClick = {
                                        if (selectedPlan == "monthly") onSubscribeMonthly()
                                        else onSubscribeYearly()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent
                                    ),
                                    contentPadding = PaddingValues()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.horizontalGradient(
                                                    colors = listOf(
                                                        PremiumColorDark,
                                                        PremiumColorLight,
                                                        PremiumColorDeep
                                                    )
                                                ),
                                                RoundedCornerShape(16.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Default.WorkspacePremium,
                                                contentDescription = null,
                                                tint = Gold,
                                                modifier = Modifier.size(22.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (selectedPlan == "monthly")
                                                    "Subscribe — ₱129/month"
                                                else
                                                    "Subscribe — ₱1,299/year",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = PremiumSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Restore Purchase",
                                    fontSize = 14.sp,
                                    color = PremiumTextSecondary,
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable { /* restore flow */ }
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Cancel anytime. Terms & conditions apply.",
                                    fontSize = 11.sp,
                                    color = PremiumTextSecondary.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Pricing Card ───────────────────────────────────────────────────────
@Composable
private fun PricingCard(
    modifier: Modifier = Modifier,
    planId: String,
    label: String,
    price: String,
    period: String,
    isSelected: Boolean,
    badge: String?,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PremiumColorDark else PremiumDivider
    val bgColor = if (isSelected) PremiumColorDark.copy(alpha = 0.08f) else Color.Transparent

    Surface(
        modifier = modifier
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = borderColor
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Badge
            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PremiumSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = badge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PremiumSuccess,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                Spacer(modifier = Modifier.height(21.dp))
            }

            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = PremiumTextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = price,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) PremiumColorDark else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = period,
                fontSize = 13.sp,
                color = PremiumTextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Selection indicator
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        color = if (isSelected) PremiumColorDark else Color.Transparent,
                        shape = CircleShape
                    )
                    .then(
                        if (!isSelected) Modifier.background(
                            Color.Transparent,
                            CircleShape
                        ) else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = PremiumSurface,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(Color.Transparent, CircleShape)
                            .then(
                                Modifier.background(
                                    Color.Transparent,
                                    CircleShape
                                )
                            )
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(2.dp, PremiumDivider)
                        ) {}
                    }
                }
            }
        }
    }
}

// ── Benefit Row ────────────────────────────────────────────────────────
@Composable
private fun BenefitRow(benefit: BenefitItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    PremiumColorDark.copy(alpha = 0.1f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = benefit.icon,
                contentDescription = null,
                tint = PremiumColorLight,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = benefit.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = benefit.description,
                fontSize = 13.sp,
                color = PremiumTextSecondary,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
