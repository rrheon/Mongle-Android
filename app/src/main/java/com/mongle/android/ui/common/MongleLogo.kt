package com.mongle.android.ui.common

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mongle.android.R
import androidx.compose.foundation.Image

enum class MongleLogoSize {
    SMALL, MEDIUM, LARGE
}

@Composable
fun MongleLogo(
    size: MongleLogoSize = MongleLogoSize.MEDIUM,
    modifier: Modifier = Modifier
) {
    val imageSize: Dp = when (size) {
        MongleLogoSize.SMALL -> 64.dp
        MongleLogoSize.MEDIUM -> 96.dp
        MongleLogoSize.LARGE -> 128.dp
    }

    Image(
        painter = painterResource(id = R.drawable.mongle_logo),
        contentDescription = "몽글 로고",
        contentScale = ContentScale.Fit,
        modifier = modifier.size(imageSize)
    )
}
