package com.alaa.tvlauncher

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.*

/** Vector line icons stay crisp on 4K TVs; all marks are authored for this screen. */
@Composable
internal fun PrayerSymbol(index:Int,modifier:Modifier) {
    Canvas(modifier) {
        val w=size.width;val h=size.height;val c=Color.White;val stroke=1.5f * density
        if(index==4) {
            val p=Path().apply {
                moveTo(w*.68f,h*.12f)
                cubicTo(w*.03f,h*.05f,w*.04f,h*.97f,w*.72f,h*.88f)
                cubicTo(w*.35f,h*.76f,w*.36f,h*.29f,w*.68f,h*.12f)
                close()
            }
            drawPath(p,c,style=Stroke(stroke))
            drawCircle(c,w*.045f,Offset(w*.82f,h*.25f))
        } else {
            val cy=if(index==1) h*.48f else h*.55f
            val center=Offset(w*.5f,cy);val radius=w*.19f
            if(index==1) drawCircle(c,radius,center,style=Stroke(stroke))
            else drawArc(c,180f,180f,false,Offset(center.x-radius,cy-radius),androidx.compose.ui.geometry.Size(radius*2,radius*2),style=Stroke(stroke))
            for(i in 0..6) {
                val angle=PI+i*PI/6
                val inner=radius*1.45f;val outer=radius*1.85f
                drawLine(c,Offset(center.x+cos(angle).toFloat()*inner,cy+sin(angle).toFloat()*inner),Offset(center.x+cos(angle).toFloat()*outer,cy+sin(angle).toFloat()*outer),stroke,StrokeCap.Round)
            }
            if(index!=1) drawLine(c,Offset(w*.12f,h*.59f),Offset(w*.88f,h*.59f),stroke,StrokeCap.Round)
            if(index==3) {
                drawLine(c,Offset(w*.25f,h*.76f),Offset(w*.75f,h*.76f),stroke,StrokeCap.Round)
                drawLine(c,Offset(w*.37f,h*.9f),Offset(w*.63f,h*.9f),stroke,StrokeCap.Round)
            } else if(index==0) {
                drawLine(c,Offset(w*.5f,h*.9f),Offset(w*.5f,h*.7f),stroke,StrokeCap.Round)
                drawLine(c,Offset(w*.5f,h*.7f),Offset(w*.4f,h*.8f),stroke)
                drawLine(c,Offset(w*.5f,h*.7f),Offset(w*.6f,h*.8f),stroke)
            } else if(index==2) {
                drawLine(c,Offset(w*.38f,h*.75f),Offset(w*.84f,h*.75f),stroke,StrokeCap.Round)
            }
        }
    }
}
