package com.stemaker.openromme.ui

import android.view.View
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGDrawable

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val svg = remember {
        context.assets.open("open_romme_logo.svg").use { SVG.getFromInputStream(it) }
    }
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                setImageDrawable(SVGDrawable(svg))
                scaleType = ImageView.ScaleType.FIT_CENTER
            }
        },
        modifier = modifier
    )
}
