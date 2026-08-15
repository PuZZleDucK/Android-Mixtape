package com.example.androidmixtape.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidmixtape.R
import com.example.androidmixtape.viewmodel.MixtapeHandwritingFont

fun MixtapeHandwritingFont.cassetteHandwritingFontFamily(): FontFamily = when (this) {
    MixtapeHandwritingFont.Kalam -> FontFamily(Font(R.font.kalam))
    MixtapeHandwritingFont.PatrickHand -> FontFamily(Font(R.font.patrick_hand))
    MixtapeHandwritingFont.Caveat -> FontFamily(Font(R.font.caveat))
    MixtapeHandwritingFont.NanumPenScript -> FontFamily(Font(R.font.nanum_pen_script))
    MixtapeHandwritingFont.IndieFlower -> FontFamily(Font(R.font.indie_flower))
    MixtapeHandwritingFont.GloriaHallelujah -> FontFamily(Font(R.font.gloria_hallelujah))
    MixtapeHandwritingFont.ArchitectsDaughter -> FontFamily(Font(R.font.architects_daughter))
    MixtapeHandwritingFont.ShadowsIntoLight -> FontFamily(Font(R.font.shadows_into_light))
}

fun MixtapeHandwritingFont.cassetteSpineFontSize(): TextUnit = when (this) {
    MixtapeHandwritingFont.Kalam,
    MixtapeHandwritingFont.PatrickHand,
    -> 52.sp

    MixtapeHandwritingFont.IndieFlower,
    MixtapeHandwritingFont.ArchitectsDaughter,
    -> 50.sp

    MixtapeHandwritingFont.GloriaHallelujah,
    MixtapeHandwritingFont.ShadowsIntoLight,
    -> 48.sp

    MixtapeHandwritingFont.Caveat,
    MixtapeHandwritingFont.NanumPenScript,
    -> 54.sp
}

fun MixtapeHandwritingFont.cassetteSpineVerticalOffset(): Dp = when (this) {
    MixtapeHandwritingFont.GloriaHallelujah -> (-6).dp
    MixtapeHandwritingFont.ShadowsIntoLight -> (-3).dp
    else -> 0.dp
}

fun MixtapeHandwritingFont.effectiveCassetteWeight(preferred: FontWeight): FontWeight =
    if (this == MixtapeHandwritingFont.IndieFlower || this == MixtapeHandwritingFont.ShadowsIntoLight) {
        maxOf(preferred, FontWeight.Bold)
    } else {
        preferred
    }
