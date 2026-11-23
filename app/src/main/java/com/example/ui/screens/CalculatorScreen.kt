package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CostLine
import com.example.data.model.MaterialItem
import com.example.data.model.PricingEngine
import com.example.data.model.StudioConstants
import com.example.ui.CalculatorDraft
import com.example.ui.StudioViewModel
import com.example.ui.components.StudioImageOrPlaceholder

@Composable
fun CalculatorScreen(
  draft: CalculatorDraft,
  items: List<MaterialItem>,
  itemsById: Map<String, MaterialItem>,
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier,
) {
  var showAddMaterialDialog by remember { mutableStateOf(false) }
  var showAddVolDialog by remember { mutableStateOf(false) }
  var showAddFixDialog by remember { mutableStateOf(false) }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      uri?.let { viewModel.addProductPhoto(it) }
    }

  val profitVal = draft.profit.toDoubleOrNull() ?: 50.0
  val feeVal = draft.fee.toDoubleOrNull() ?: 0.0
  val wpVal = draft.wp.toDoubleOrNull() ?: 25.0

  val computed =
    remember(draft.lines, profitVal, feeVal, wpVal, itemsById) {
      PricingEngine.computeProductPrice(
        lines = draft.lines,
        profitPercent = profitVal,
        feePercent = feeVal,
        wholesalePercent = wpVal,
        itemsById = itemsById,
      )
    }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Live Price Hero Card
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(22.dp),
      ) {
        Column(
          modifier = Modifier.padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text =
                if (draft.editingId != null) "Editing Product" else "Naya Product Price Calculator",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
            )
            TextButton(
              onClick = { viewModel.resetCalculatorDraft() },
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            ) {
              Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(Modifier.width(4.dp))
              Text("Reset", color = MaterialTheme.colorScheme.onPrimary)
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            PriceMetricColumn(
              label = "Making Cost",
              value = PricingEngine.formatRupees(computed.cost),
              textColor = MaterialTheme.colorScheme.onPrimary,
            )
            PriceMetricColumn(
              label = "Retail Price",
              value = PricingEngine.formatRupees(computed.retail),
              textColor = MaterialTheme.colorScheme.onPrimary,
              emphasized = true,
            )
            PriceMetricColumn(
              label = "Wholesale",
              value = PricingEngine.formatRupees(computed.wholesale),
              textColor = MaterialTheme.colorScheme.onPrimary,
            )
          }

          Surface(
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              Text(
                text = "Net Profit: ${PricingEngine.formatRupees(computed.netProfit)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
              )
              if (computed.feeAmount > 0.5) {
                Text(
                  text = "Platform Fee: ${PricingEngine.formatRupees(computed.feeAmount)}",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                )
              }
            }
          }
        }
      }
    }

    // Product Basic Details + Photos
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Product Details", style = MaterialTheme.typography.titleMedium)

          OutlinedTextField(
            value = draft.name,
            onValueChange = { v -> viewModel.updateDraft { it.copy(name = v) } },
            label = { Text("Product Naam *") },
            placeholder = { Text("Jaise: Royal Geode Wall Clock") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("calc_product_name_input"),
          )

          Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = draft.sku,
              onValueChange = { v -> viewModel.updateDraft { it.copy(sku = v) } },
              label = { Text("SKU (Unique)") },
              placeholder = { Text("KT-001") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("calc_sku_input"),
            )
            OutlinedTextField(
              value = draft.cat,
              onValueChange = { v -> viewModel.updateDraft { it.copy(cat = v) } },
              label = { Text("Tag / Category") },
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          // Quick Tag chips
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            StudioConstants.DEFAULT_PRODUCT_TAGS.forEach { tag ->
              FilterChip(
                selected = draft.cat.equals(tag, ignoreCase = true),
                onClick = { viewModel.updateDraft { it.copy(cat = tag) } },
                label = { Text(tag) },
              )
            }
          }

          // Up to 4 Photos
          Text(
            "Photos (${draft.imgs.size}/4) — Pehli photo Main hogi",
            style = MaterialTheme.typography.labelLarge,
          )
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            draft.imgs.forEachIndexed { idx, path ->
              Box(modifier = Modifier.size(82.dp)) {
                StudioImageOrPlaceholder(
                  path = path,
                  contentDescription = "Product Photo ${idx + 1}",
                  modifier =
                    Modifier
                      .fillMaxSize()
                      .clip(RoundedCornerShape(14.dp))
                      .border(
                        width = if (idx == 0) 2.dp else 1.dp,
                        color =
                          if (idx == 0) MaterialTheme.colorScheme.primary
                          else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp),
                      )
                      .clickable {
                        if (idx > 0) {
                          viewModel.updateDraft { d ->
                            val mutable = d.imgs.toMutableList()
                            val chosen = mutable.removeAt(idx)
                            mutable.add(0, chosen)
                            d.copy(imgs = mutable)
                          }
                          viewModel.showToast("Main photo set ho gayi")
                        }
                      },
                )
                if (idx == 0) {
                  Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
                  ) {
                    Row(
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                      verticalAlignment = Alignment.CenterVertically,
                    ) {
                      Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(10.dp),
                      )
                      Text(
                        "Main",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                      )
                    }
                  }
                }
                Surface(
                  onClick = {
                    viewModel.updateDraft { d ->
                      d.copy(imgs = d.imgs.filterIndexed { i, _ -> i != idx })
                    }
                  },
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.align(Alignment.TopEnd).padding(3.dp).size(22.dp),
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(
                      Icons.Filled.Close,
                      contentDescription = "Remove Photo",
                      tint = MaterialTheme.colorScheme.onError,
                      modifier = Modifier.size(14.dp),
                    )
                  }
                }
              }
            }

            if (draft.imgs.size < 4) {
              Box(
                modifier =
                  Modifier
                    .size(82.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                      photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                      )
                    },
                contentAlignment = Alignment.Center,
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    Icons.Filled.AddPhotoAlternate,
                    contentDescription = "Add Photo",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Text("Add", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }

          OutlinedTextField(
            value = draft.notes,
            onValueChange = { v -> viewModel.updateDraft { it.copy(notes = v) } },
            label = { Text("Customer Notes / Description (Optional)") },
            maxLines = 2,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }
    }

    // Cost Lines Section
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text("Cost Lines (${draft.lines.size})", style = MaterialTheme.typography.titleMedium)

          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            AssistChip(
              onClick = {
                if (items.isEmpty()) {
                  viewModel.showToast("Pehle Inventory mein materials add karein", isError = true)
                } else {
                  showAddMaterialDialog = true
                }
              },
              leadingIcon = {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              label = { Text("Material") },
              modifier = Modifier.testTag("calc_add_material_line_button"),
            )
            AssistChip(
              onClick = {
                if (items.isEmpty()) {
                  viewModel.showToast("Pehle Inventory mein Resin add karein", isError = true)
                } else {
                  showAddVolDialog = true
                }
              },
              leadingIcon = {
                Icon(
                  Icons.Filled.SquareFoot,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text("Size se Resin") },
              modifier = Modifier.testTag("calc_add_volume_line_button"),
            )
            AssistChip(
              onClick = { showAddFixDialog = true },
              leadingIcon = {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              },
              label = { Text("Packaging / Labour") },
              modifier = Modifier.testTag("calc_add_fixed_line_button"),
            )
          }

          if (draft.lines.isEmpty()) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text =
                  "Upar diye buttons se Material, Size se Resin, ya Labour/Packaging charge add karein.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
              )
            }
          } else {
            draft.lines.forEachIndexed { idx, line ->
              val info = computed.breakdowns.getOrNull(idx)
              if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = info?.title ?: "Cost Line",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color =
                      if (info?.missingItem == true) MaterialTheme.colorScheme.error
                      else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = info?.subtitle ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Text(
                  text = PricingEngine.formatRupees(info?.cost ?: 0.0, decimals = true),
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
                IconButton(
                  onClick = {
                    viewModel.updateDraft { d ->
                      d.copy(lines = d.lines.filterIndexed { i, _ -> i != idx })
                    }
                  }
                ) {
                  Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Remove line",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
          }
        }
      }
    }

    // Margins & Save
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text("Profit & Margin %", style = MaterialTheme.typography.titleMedium)

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = draft.profit,
              onValueChange = { v -> viewModel.updateDraft { it.copy(profit = v) } },
              label = { Text("Profit %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = draft.fee,
              onValueChange = { v -> viewModel.updateDraft { it.copy(fee = v) } },
              label = { Text("Fee %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
              value = draft.wp,
              onValueChange = { v -> viewModel.updateDraft { it.copy(wp = v) } },
              label = { Text("Wholesale %") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }

          Button(
            onClick = { viewModel.saveDraftToCatalog() },
            modifier =
              Modifier.fillMaxWidth().height(52.dp).testTag("save_to_catalog_button"),
          ) {
            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
              if (draft.editingId != null) "Catalog Product Update Karein"
              else "Catalog mein Save Karein"
            )
          }
        }
      }
    }
  }

  if (showAddMaterialDialog) {
    AddMaterialLineDialog(
      items = items,
      onDismiss = { showAddMaterialDialog = false },
      onAdd = { line ->
        viewModel.updateDraft { it.copy(lines = it.lines + line) }
        showAddMaterialDialog = false
      },
    )
  }

  if (showAddVolDialog) {
    AddVolumeLineDialog(
      items = items,
      onDismiss = { showAddVolDialog = false },
      onAdd = { line ->
        viewModel.updateDraft { it.copy(lines = it.lines + line) }
        showAddVolDialog = false
      },
    )
  }

  if (showAddFixDialog) {
    AddFixedLineDialog(
      onDismiss = { showAddFixDialog = false },
      onAdd = { line ->
        viewModel.updateDraft { it.copy(lines = it.lines + line) }
        showAddFixDialog = false
      },
    )
  }
}

@Composable
private fun PriceMetricColumn(
  label: String,
  value: String,
  textColor: androidx.compose.ui.graphics.Color,
  emphasized: Boolean = false,
) {
  Column {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = textColor.copy(alpha = 0.8f),
    )
    Text(
      text = value,
      style =
        if (emphasized) MaterialTheme.typography.headlineSmall
        else MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.Bold,
      color = textColor,
    )
  }
}

@Composable
private fun AddMaterialLineDialog(
  items: List<MaterialItem>,
  onDismiss: () -> Unit,
  onAdd: (CostLine.ItemLine) -> Unit,
) {
  var selected by remember { mutableStateOf(items.first()) }
  var qty by remember { mutableStateOf("1") }
  var search by remember { mutableStateOf("") }

  val filtered =
    remember(items, search) {
      if (search.isBlank()) items
      else items.filter { it.name.contains(search, ignoreCase = true) || it.cat.contains(search, true) }
    }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Material Add Karein", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedTextField(
          value = search,
          onValueChange = { search = it },
          label = { Text("Search material...") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          filtered.take(12).forEach { mat ->
            val isChosen = mat.id == selected.id
            Surface(
              onClick = { selected = mat },
              color =
                if (isChosen) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(mat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                  Text(
                    "${mat.cat} • ${PricingEngine.formatRupees(mat.unitCost, true)}/${mat.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }

        OutlinedTextField(
          value = qty,
          onValueChange = { qty = it },
          label = { Text("Kitna use hoga (${selected.unit})") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        val qVal = qty.toDoubleOrNull() ?: 0.0
        Text(
          text = "Line Cost: ${PricingEngine.formatRupees(selected.unitCost * qVal, true)}",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val qVal = qty.toDoubleOrNull() ?: 0.0
          if (qVal > 0.0) onAdd(CostLine.ItemLine(selected.id, qVal))
        }
      ) {
        Text("Add")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AddVolumeLineDialog(
  items: List<MaterialItem>,
  onDismiss: () -> Unit,
  onAdd: (CostLine.VolLine) -> Unit,
) {
  val resinDefault =
    items.firstOrNull { it.cat.equals("Resin", ignoreCase = true) } ?: items.first()
  var selectedResin by remember { mutableStateOf(resinDefault) }
  var shape by remember { mutableStateOf("r") } // r or c
  var unit by remember { mutableStateOf("in") } // in or cm
  var a by remember { mutableStateOf("4") }
  var b by remember { mutableStateOf("4") }
  var th by remember { mutableStateOf("5") }
  var waste by remember { mutableStateOf("10") }
  var dens by remember { mutableStateOf("1.1") }

  val previewLine =
    CostLine.VolLine(
      id = selectedResin.id,
      shape = shape,
      a = a.toDoubleOrNull() ?: 0.0,
      b = b.toDoubleOrNull() ?: 0.0,
      u = unit,
      th = th.toDoubleOrNull() ?: 0.0,
      waste = waste.toDoubleOrNull() ?: 10.0,
      dens = dens.toDoubleOrNull() ?: 1.1,
    )
  val grams = previewLine.grams()
  val cost = grams * selectedResin.unitCost

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Size se Resin Calculate Karein", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("Resin Chunein", style = MaterialTheme.typography.labelMedium)
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items.forEach { mat ->
            FilterChip(
              selected = mat.id == selectedResin.id,
              onClick = { selectedResin = mat },
              label = { Text(mat.name, maxLines = 1) },
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = shape == "r",
            onClick = { shape = "r" },
            label = { Text("Rectangle / Square") },
          )
          FilterChip(
            selected = shape == "c",
            onClick = { shape = "c" },
            label = { Text("Round") },
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          FilterChip(
            selected = unit == "in",
            onClick = { unit = "in" },
            label = { Text("Inches (in)") },
          )
          FilterChip(
            selected = unit == "cm",
            onClick = { unit = "cm" },
            label = { Text("Centimeters (cm)") },
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = a,
            onValueChange = { a = it },
            label = { Text(if (shape == "c") "Diameter ($unit)" else "Length ($unit)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          if (shape == "r") {
            OutlinedTextField(
              value = b,
              onValueChange = { b = it },
              label = { Text("Width ($unit)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
              singleLine = true,
              modifier = Modifier.weight(1f),
            )
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = th,
            onValueChange = { th = it },
            label = { Text("Thickness (mm)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = waste,
            onValueChange = { waste = it },
            label = { Text("Waste %") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = dens,
            onValueChange = { dens = it },
            label = { Text("Density") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp)) {
            Text(
              text = "Resin Required: ${PricingEngine.formatQty(grams)} gm",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
              text = "Estimated Cost: ${PricingEngine.formatRupees(cost, true)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (grams > 0.0) onAdd(previewLine)
        }
      ) {
        Text("Add Resin Line")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}

@Composable
private fun AddFixedLineDialog(
  onDismiss: () -> Unit,
  onAdd: (CostLine.FixLine) -> Unit,
) {
  var label by remember { mutableStateOf("Labour") }
  var amount by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Fixed Charge Add Karein", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          StudioConstants.FIXED_CHARGE_PRESETS.forEach { preset ->
            FilterChip(
              selected = label == preset,
              onClick = { label = preset },
              label = { Text(preset) },
            )
          }
        }
        OutlinedTextField(
          value = label,
          onValueChange = { label = it },
          label = { Text("Charge ka naam") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = amount,
          onValueChange = { amount = it },
          label = { Text("Amount (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val amt = amount.toDoubleOrNull() ?: 0.0
          if (amt > 0.0) onAdd(CostLine.FixLine(label.trim().ifBlank { "Charge" }, amt))
        }
      ) {
        Text("Add")
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
  )
}
