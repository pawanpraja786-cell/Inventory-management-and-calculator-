package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BrandMeta
import com.example.ui.ToastMessage
import com.example.ui.theme.parseHexColorOrDefault
import java.io.File

@Composable
fun KamarTajLogoMark(
  brand: BrandMeta,
  size: Dp = 40.dp,
  modifier: Modifier = Modifier,
) {
  val logoFile = brand.logo?.let(::File)?.takeIf { it.exists() }
  val accentColor = parseHexColorOrDefault(brand.color)

  if (logoFile != null) {
    AsyncImage(
      model = logoFile,
      contentDescription = "${brand.name} logo",
      contentScale = ContentScale.Crop,
      modifier =
        modifier
          .size(size)
          .clip(RoundedCornerShape(12.dp))
          .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
    )
  } else {
    Canvas(modifier = modifier.size(size)) {
      val w = this.size.width
      val h = this.size.height
      drawRoundRect(
        color = accentColor,
        cornerRadius = CornerRadius(w * 0.24f, w * 0.24f),
      )

      fun px(v: Float) = (v / 108f) * w
      fun py(v: Float) = (v / 108f) * h

      val archPath =
        Path().apply {
          moveTo(px(28f), py(84f))
          lineTo(px(28f), py(48f))
          cubicTo(px(28f), py(32f), px(41f), py(23f), px(54f), py(19f))
          cubicTo(px(67f), py(23f), px(80f), py(32f), px(80f), py(48f))
          lineTo(px(80f), py(84f))
          close()
        }
      drawPath(archPath, color = Color.White)

      val kPath =
        Path().apply {
          moveTo(px(41f), py(42f))
          lineTo(px(47.5f), py(42f))
          lineTo(px(47.5f), py(53.5f))
          lineTo(px(60f), py(42f))
          lineTo(px(68f), py(42f))
          lineTo(px(55f), py(55.5f))
          lineTo(px(68.5f), py(72f))
          lineTo(px(60.5f), py(72f))
          lineTo(px(47.5f), py(57f))
          lineTo(px(47.5f), py(72f))
          lineTo(px(41f), py(72f))
          close()
        }
      drawPath(kPath, color = accentColor)
    }
  }
}

@Composable
fun InAppToastBanner(
  toast: ToastMessage?,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = toast != null,
    enter = slideInVertically { -it } + fadeIn(),
    exit = slideOutVertically { -it } + fadeOut(),
    modifier = modifier,
  ) {
    toast?.let { msg ->
      val bg =
        if (msg.isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.inverseSurface
      val fg =
        if (msg.isError) MaterialTheme.colorScheme.onError
        else MaterialTheme.colorScheme.inverseOnSurface

      Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onDismiss() },
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = if (msg.isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = msg.text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
  }
}

@Composable
fun ConfirmModal(
  title: String,
  message: String,
  confirmLabel: String = "Haan, Delete Karein",
  dismissLabel: String = "Nahi",
  isDestructive: Boolean = true,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors =
          if (isDestructive) {
            ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError,
            )
          } else {
            ButtonDefaults.buttonColors()
          },
        modifier = Modifier.testTag("modal_confirm_button"),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("modal_cancel_button"),
      ) {
        Text(dismissLabel)
      }
    },
  )
}

@Composable
fun ShuruKaiseKareinCard(
  onLoadExampleData: () -> Unit,
  onAddManual: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(20.dp),
  ) {
    Column(
      modifier = Modifier.padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.primaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
        }
        Column {
          Text(
            text = "Shuru Kaise Karein?",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "Kamar Taj Resin Studio Guide",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
        }
      }

      val steps =
        listOf(
          "1. Inventory mein Resin, Molds, Pigments, Flowers aur Packaging add karein (total price aur qty ke saath).",
          "2. Calculator tab mein product banaye — size ke hisaab se resin grams aur making cost apne aap niklegi.",
          "3. Catalog mein save karein aur customer ko PNG ya PDF card WhatsApp par share karein (cost kabhi nahi dikhegi).",
          "4. Orders tab mein order book karein — stock apne aap ghatega aur PNG/PDF Quotation ban jayega!",
        )
      steps.forEach { step ->
        Text(
          text = step,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Button(
          onClick = onLoadExampleData,
          modifier = Modifier.weight(1f).testTag("load_example_materials_button"),
        ) {
          Text("Example Materials Daalein")
        }
        OutlinedButton(
          onClick = onAddManual,
          modifier = Modifier.testTag("add_first_material_button"),
        ) {
          Text("Khud Add Karein")
        }
      }
    }
  }
}

@Composable
fun StudioGuideDialog(
  onLoadExampleData: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text("Shuru Kaise Karein", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Close guide")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        GuideStepRow("1. Inventory", "Apne saare materials (Resin, Mold, Pigment, Flowers, Stand) total rate aur quantity ke saath daalein. Per-gm / per-use rate apne aap banega.")
        GuideStepRow("2. Calculator", "Product ka naam, SKU, 4 tak photos, materials, aur 'Size se Resin' daalein. Profit % aur Fee % set karke live Retail & Wholesale price dekhein.")
        GuideStepRow("3. Catalog", "Customer ko bhejne ke liye PNG card ya PDF banayein. Price dikhana ya chhupana toggle karein. Cost aur profit customer ko kabhi nahi dikhta.")
        GuideStepRow("4. Orders & Report", "Order daalte hi material stock kam hoga; Cancel ya Delete par stock wapas aa jayega. Har order ka Quotation share karein aur Mahine ka Report dekhein.")
        GuideStepRow("5. Settings (⚙)", "Apna business logo, naam, contact aur theme colour set karein. Inventory se JSON Backup zaroor rakhein!")
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onLoadExampleData()
          onDismiss()
        }
      ) {
        Text("Example Data Daalein")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Samajh Gaya") }
    },
  )
}

@Composable
private fun GuideStepRow(title: String, body: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleSmall,
      color = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = body,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
fun StudioImageOrPlaceholder(
  path: String?,
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  val file = path?.let(::File)?.takeIf { it.exists() }
  if (file != null) {
    AsyncImage(
      model = file,
      contentDescription = contentDescription,
      contentScale = ContentScale.Crop,
      modifier = modifier,
    )
  } else {
    Box(
      modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Outlined.Inventory2,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.size(28.dp),
      )
    }
  }
}
