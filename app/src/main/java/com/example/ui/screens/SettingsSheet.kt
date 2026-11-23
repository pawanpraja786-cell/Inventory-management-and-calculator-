package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BrandMeta
import com.example.data.model.StudioConstants
import com.example.ui.StudioViewModel
import com.example.ui.components.KamarTajLogoMark
import com.example.ui.theme.contrastingOnColor
import com.example.ui.theme.parseHexColorOrDefault

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
  brand: BrandMeta,
  viewModel: StudioViewModel,
  onOpenGuide: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var name by remember(brand.name) { mutableStateOf(brand.name) }
  var contact by remember(brand.contact) { mutableStateOf(brand.contact) }
  var colorHex by remember(brand.color) { mutableStateOf(brand.color) }
  var mode by remember(brand.mode) { mutableStateOf(brand.mode) }

  val logoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      uri?.let { viewModel.updateBrandLogo(it) }
    }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text("Studio Settings", style = MaterialTheme.typography.headlineSmall)

      // Logo Upload / Remove
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        KamarTajLogoMark(brand = brand.copy(color = colorHex), size = 64.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("Business Logo", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                logoPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
              modifier = Modifier.testTag("upload_logo_button"),
            ) {
              Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("Upload")
            }
            if (brand.logo != null) {
              TextButton(
                onClick = { viewModel.removeBrandLogo() },
                modifier = Modifier.testTag("remove_logo_button"),
              ) {
                Icon(
                  Icons.Filled.DeleteOutline,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Hatayein", color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Business Naam") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("settings_business_name_input"),
      )

      OutlinedTextField(
        value = contact,
        onValueChange = { contact = it },
        label = { Text("Phone / Instagram Handle (Card & Quotation footer)") },
        placeholder = { Text("+91 98xxx xxxxx • @kamartaj.resin") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("settings_contact_input"),
      )

      // 8 Theme Color Presets + Custom Hex
      Text("Theme Colour (8 Presets + Custom)", style = MaterialTheme.typography.titleSmall)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        StudioConstants.THEME_COLOR_PRESETS.forEach { (hex, label) ->
          val swatchColor = parseHexColorOrDefault(hex)
          val selected = colorHex.equals(hex, ignoreCase = true)
          Box(
            modifier =
              Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(swatchColor)
                .border(
                  width = if (selected) 3.dp else 1.dp,
                  color =
                    if (selected) MaterialTheme.colorScheme.onSurface
                    else Color.White.copy(alpha = 0.4f),
                  shape = CircleShape,
                )
                .clickable { colorHex = hex },
            contentAlignment = Alignment.Center,
          ) {
            if (selected) {
              Icon(
                Icons.Filled.Check,
                contentDescription = label,
                tint = swatchColor.contrastingOnColor(),
                modifier = Modifier.size(20.dp),
              )
            }
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(38.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(parseHexColorOrDefault(colorHex))
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
        )
        OutlinedTextField(
          value = colorHex,
          onValueChange = { colorHex = it },
          label = { Text("Custom Hex (#82091c)") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      // Theme Mode
      Text("Display Mode", style = MaterialTheme.typography.titleSmall)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark").forEach { (key, label) ->
          FilterChip(
            selected = mode.equals(key, ignoreCase = true),
            onClick = { mode = key },
            label = { Text(label) },
          )
        }
      }

      Button(
        onClick = {
          val normalizedHex =
            if (colorHex.trim().startsWith("#")) colorHex.trim() else "#${colorHex.trim()}"
          val validHex =
            runCatching {
              android.graphics.Color.parseColor(normalizedHex)
              normalizedHex
            }.getOrElse { "#82091c" }
          viewModel.saveBrandSettings(name = name, contact = contact, colorHex = validHex, mode = mode)
          onDismiss()
        },
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_settings_button"),
      ) {
        Text("Settings Save Karein")
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = {
            onDismiss()
            onOpenGuide()
          },
          modifier = Modifier.weight(1f),
        ) {
          Text("Shuru Kaise Karein")
        }
        OutlinedButton(
          onClick = {
            viewModel.loadExampleData()
            onDismiss()
          },
          modifier = Modifier.weight(1f),
        ) {
          Text("Example Data")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
