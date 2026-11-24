package com.redx.codex.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.redx.codex.MainActivity

class RedXWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .cornerRadius(16.dp)
                        .background(ColorProvider(Color(0xFF161616)))
                        .padding(14.dp)
                        .clickable(actionStartActivity<MainActivity>())
                ) {
                    Column(
                        modifier = GlanceModifier.fillMaxSize(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Vertical.CenterVertically
                        ) {
                            Text(
                                text = "RedX",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF0A84FF)),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(GlanceModifier.defaultWeight())
                            Text(
                                text = "● Ready",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF82CB93)),
                                    fontSize = 12.sp
                                )
                            )
                        }

                        Spacer(GlanceModifier.height(8.dp))

                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .cornerRadius(10.dp)
                                .background(ColorProvider(Color(0xFF232323)))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Tap to open chat session...",
                                style = TextStyle(
                                    color = ColorProvider(Color(0xFF8E8E93)),
                                    fontSize = 13.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
