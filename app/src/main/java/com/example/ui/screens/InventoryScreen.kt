package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.MaterialItem
import com.example.data.model.PricingEngine
import com.example.data.model.StudioConstants
import com.example.ui.StudioViewModel
import com.example.ui.components.ConfirmModal
import com.example.ui.components.ShuruKaiseKareinCard
import com.example.ui.components.StudioImageOrPlaceholder
import com.example.ui.theme.WarningAmber
import java.util.UUID
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
  items: List<MaterialItem>,
  categories: List<String>,
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("All") }
  var onlyLowStock by remember { mutableStateOf(false) }

  var showAddEditSheet by remember { mutableStateOf(false) }
  var editingItem by remember { mutableStateOf<MaterialItem?>(null) }
  var pendingDeleteItem by remember { mutableStateOf<MaterialItem?>(null) }
  var showAddSectionDialog by remember { mutableStateOf(false) }
  var showBackupDialog by remember { mutableStateOf(false) }

  val createBackupLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) {
      uri ->
      uri?.let { viewModel.saveBackupToUri(it) }
    }

  val restoreBackupLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      uri?.let { viewModel.restoreBackupFromUri(it) }
    }

  val lowStockCount = remember(items) { items.count { it.isLowStock } }
  val filteredItems =
    remember(items, searchQuery, selectedCategory, onlyLowStock) {
      items.filter { item ->
        val matchesCat =
          selectedCategory == "All" || item.cat.equals(selectedCategory, ignoreCase = true)
        val matchesQuery =
          searchQuery.isBlank() ||
            item.name.contains(searchQuery, ignoreCase = true) ||
            item.cat.contains(searchQuery, ignoreCase = true)
        val matchesLow = !onlyLowStock || item.isLowStock
        matchesCat && matchesQuery && matchesLow
      }
    }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    floatingActionButton = {
      FloatingActionButton(
        onClick = {
          editingItem = null
          showAddEditSheet = true
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("add_material_fab"),
      ) {
        Icon(Icons.Filled.Add, contentDescription = "Naya Material Add Karein")
      }
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Search + Backup bar
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Material ya section search karein...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
              if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { searchQuery = "" }) {
                  Icon(Icons.Filled.Close, contentDescription = "Clear search")
                }
              }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.weight(1f).testTag("inventory_search_input"),
          )
          Surface(
            onClick = { showBackupDialog = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.height(54.dp).testTag("inventory_backup_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                Icons.Filled.Backup,
                contentDescription = "Backup JSON",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
              )
              Text(
                "Backup",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }
      }

      // Section Filter Chips + Add Section
      item {
        Row(
          modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          FilterChip(
            selected = selectedCategory == "All",
            onClick = { selectedCategory = "All" },
            label = { Text("Sab (${items.size})") },
          )
          if (lowStockCount > 0) {
            FilterChip(
              selected = onlyLowStock,
              onClick = { onlyLowStock = !onlyLowStock },
              leadingIcon = {
                Icon(
                  Icons.Filled.WarningAmber,
                  contentDescription = null,
                  tint = WarningAmber,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text("Low Stock ($lowStockCount)") },
            )
          }
          categories.forEach { cat ->
            FilterChip(
              selected = selectedCategory.equals(cat, ignoreCase = true),
              onClick = { selectedCategory = if (selectedCategory == cat) "All" else cat },
              label = { Text(cat) },
            )
          }
          AssistChip(
            onClick = { showAddSectionDialog = true },
            leadingIcon = {
              Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            },
            label = { Text("Naya Section") },
            modifier = Modifier.testTag("add_section_chip"),
          )
        }
      }

      // Low Stock Alert Banner
      if (lowStockCount > 0 && !onlyLowStock) {
        item {
          Surface(
            color = WarningAmber.copy(alpha = 0.12f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().clickable { onlyLowStock = true },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = WarningAmber)
              Text(
                text = "$lowStockCount material low stock par hain — dekhne ke liye tap karein",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
              )
            }
          }
        }
      }

      // Empty State or List
      if (items.isEmpty()) {
        item {
          ShuruKaiseKareinCard(
            onLoadExampleData = { viewModel.loadExampleData() },
            onAddManual = {
              editingItem = null
              showAddEditSheet = true
            },
          )
        }
      } else if (filteredItems.isEmpty()) {
        item {
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                "Is filter mein koi material nahi mila",
                style = MaterialTheme.typography.titleMedium,
              )
              TextButton(
                onClick = {
                  searchQuery = ""
                  selectedCategory = "All"
                  onlyLowStock = false
                }
              ) {
                Text("Filters Clear Karein")
              }
            }
          }
        }
      } else {
        items(filteredItems, key = { it.id }) { item ->
          MaterialItemCard(
            item = item,
            onEdit = {
              editingItem = item
              showAddEditSheet = true
            },
            onDelete = { pendingDeleteItem = item },
            onAdjustStock = { delta -> viewModel.adjustStock(item.id, delta) },
          )
        }
      }
    }
  }

  if (showAddEditSheet) {
    MaterialFormSheet(
      initial = editingItem,
      categories = categories,
      viewModel = viewModel,
      onDismiss = { showAddEditSheet = false },
      onSave = { saved ->
        viewModel.saveMaterial(saved)
        showAddEditSheet = false
      },
    )
  }

  pendingDeleteItem?.let { target ->
    ConfirmModal(
      title = "Material Delete Karein?",
      message = "\"${target.name}\" inventory se hat jayega. Jo products is material ko use karte hain unka rate ₹0 ho jayega.",
      onConfirm = {
        viewModel.deleteMaterial(target)
        pendingDeleteItem = null
      },
      onDismiss = { pendingDeleteItem = null },
    )
  }

  if (showAddSectionDialog) {
    var sectionName by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showAddSectionDialog = false },
      title = { Text("Naya Section Banayein", style = MaterialTheme.typography.titleLarge) },
      text = {
        OutlinedTextField(
          value = sectionName,
          onValueChange = { sectionName = it },
          label = { Text("Section ka naam (jaise: Foil, Glitter)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.addCustomCategory(sectionName)
            showAddSectionDialog = false
          }
        ) {
          Text("Add Karein")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddSectionDialog = false }) { Text("Cancel") }
      },
    )
  }

  if (showBackupDialog) {
    AlertDialog(
      onDismissRequest = { showBackupDialog = false },
      title = { Text("Data Backup & Restore", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Apne saare materials, catalog products, orders aur settings ka JSON backup save ya share karein.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Button(
            onClick = {
              showBackupDialog = false
              createBackupLauncher.launch("kamar_taj_backup_${PricingEngine.todayIso()}.json")
            },
            modifier = Modifier.fillMaxWidth().testTag("download_backup_json_button"),
          ) {
            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("JSON File Download Karein")
          }
          OutlinedButton(
            onClick = {
              showBackupDialog = false
              viewModel.shareBackupJson()
            },
            modifier = Modifier.fillMaxWidth().testTag("share_backup_json_button"),
          ) {
            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("WhatsApp / Share Sheet se Bhejein")
          }
          OutlinedButton(
            onClick = {
              showBackupDialog = false
              restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
            },
            modifier = Modifier.fillMaxWidth().testTag("restore_backup_json_button"),
          ) {
            Icon(Icons.Filled.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("JSON Backup Restore Karein")
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showBackupDialog = false }) { Text("Band Karein") }
      },
    )
  }
}

@Composable
private fun MaterialItemCard(
  item: MaterialItem,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
  onAdjustStock: (Double) -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable { onEdit() },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    shape = RoundedCornerShape(18.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      StudioImageOrPlaceholder(
        path = item.img,
        contentDescription = item.name,
        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)),
      )

      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(6.dp),
          ) {
            Text(
              text = item.cat,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            )
          }
          if (item.isLowStock) {
            Surface(
              color = WarningAmber.copy(alpha = 0.16f),
              shape = RoundedCornerShape(6.dp),
            ) {
              Text(
                text = "LOW STOCK",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = WarningAmber,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }

        Text(
          text = item.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        Text(
          text =
            "${PricingEngine.formatRupees(item.price)} / ${PricingEngine.formatQty(item.qty)} ${item.unit}  •  " +
              "${PricingEngine.formatRupees(item.unitCost, true)}/${item.unit}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (item.stock != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "Stock: ${PricingEngine.formatQty(item.stock)} ${item.unit}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = if (item.isLowStock) WarningAmber else MaterialTheme.colorScheme.primary,
            )
            Surface(
              onClick = { onAdjustStock(-1.0) },
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.size(26.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text("-", style = MaterialTheme.typography.labelLarge)
              }
            }
            Surface(
              onClick = { onAdjustStock(1.0) },
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier.size(26.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text("+", style = MaterialTheme.typography.labelLarge)
              }
            }
          }
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        IconButton(onClick = onEdit) {
          Icon(
            Icons.Filled.Edit,
            contentDescription = "Edit Material",
            tint = MaterialTheme.colorScheme.primary,
          )
        }
        IconButton(onClick = onDelete) {
          Icon(
            Icons.Filled.DeleteOutline,
            contentDescription = "Delete Material",
            tint = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MaterialFormSheet(
  initial: MaterialItem?,
  categories: List<String>,
  viewModel: StudioViewModel,
  onDismiss: () -> Unit,
  onSave: (MaterialItem) -> Unit,
) {
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var cat by remember { mutableStateOf(initial?.cat ?: categories.firstOrNull() ?: "Resin") }
  var customCatInput by remember { mutableStateOf("") }
  var name by remember { mutableStateOf(initial?.name ?: "") }
  var price by remember { mutableStateOf(initial?.price?.let(PricingEngine::formatQty) ?: "") }
  var qty by remember { mutableStateOf(initial?.qty?.let(PricingEngine::formatQty) ?: "") }
  var unit by remember { mutableStateOf(initial?.unit ?: "gm") }
  var stock by remember { mutableStateOf(initial?.stock?.let(PricingEngine::formatQty) ?: "") }
  var low by remember { mutableStateOf(initial?.low?.let(PricingEngine::formatQty) ?: "") }
  var imgPath by remember { mutableStateOf(initial?.img) }

  val photoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
      if (uri != null) {
        scope.launch {
          val saved = viewModel.savePickedMaterialPhoto(uri)
          if (saved != null) imgPath = saved
          else viewModel.showToast("Photo save nahi ho payi", isError = true)
        }
      }
    }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = if (initial == null) "Naya Material Add Karein" else "Material Edit Karein",
        style = MaterialTheme.typography.titleLarge,
      )

      // Photo row
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier =
            Modifier
              .size(72.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .clickable {
                photoPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              },
          contentAlignment = Alignment.Center,
        ) {
          if (imgPath != null) {
            StudioImageOrPlaceholder(
              path = imgPath,
              contentDescription = "Material Photo",
              modifier = Modifier.fillMaxSize(),
            )
          } else {
            Icon(
              Icons.Filled.AddPhotoAlternate,
              contentDescription = "Photo Chunein",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
        }
        Column(modifier = Modifier.weight(1f)) {
          Text("Material Photo (Optional)", style = MaterialTheme.typography.titleSmall)
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
              onClick = {
                photoPicker.launch(
                  PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
              }
            ) {
              Text(if (imgPath == null) "Photo Chunein" else "Photo Badlein")
            }
            if (imgPath != null) {
              TextButton(onClick = { imgPath = null }) {
                Text("Hatayein", color = MaterialTheme.colorScheme.error)
              }
            }
          }
        }
      }

      // Section picker
      Text("Section / Category", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        categories.forEach { c ->
          FilterChip(
            selected = cat.equals(c, ignoreCase = true) && customCatInput.isBlank(),
            onClick = {
              cat = c
              customCatInput = ""
            },
            label = { Text(c) },
          )
        }
      }
      OutlinedTextField(
        value = customCatInput,
        onValueChange = { customCatInput = it },
        label = { Text("Ya naya section naam likhein (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Material ka naam *") },
        placeholder = { Text("Jaise: Crystal Clear Epoxy Resin") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("material_name_input"),
      )

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = price,
          onValueChange = { price = it },
          label = { Text("Total Price (₹) *") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("material_price_input"),
        )
        OutlinedTextField(
          value = qty,
          onValueChange = { qty = it },
          label = { Text("Total Qty *") },
          placeholder = { Text("1000 / 20 uses") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f).testTag("material_qty_input"),
        )
      }

      // Unit chips
      Text("Unit", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        StudioConstants.DEFAULT_UNITS.forEach { u ->
          FilterChip(
            selected = unit == u,
            onClick = { unit = u },
            label = { Text(u) },
          )
        }
      }

      val parsedPrice = price.toDoubleOrNull() ?: 0.0
      val parsedQty = qty.toDoubleOrNull() ?: 0.0
      if (parsedPrice > 0.0 && parsedQty > 0.0) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text =
              "Per-$unit Cost: ${PricingEngine.formatRupees(parsedPrice / parsedQty, decimals = true)}/$unit",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
          )
        }
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = stock,
          onValueChange = { stock = it },
          label = { Text("Current Stock ($unit)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = low,
          onValueChange = { low = it },
          label = { Text("Low Alert Level") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      Button(
        onClick = {
          val finalName = name.trim()
          val finalPrice = price.toDoubleOrNull()
          val finalQty = qty.toDoubleOrNull()
          if (finalName.isEmpty() || finalPrice == null || finalQty == null || finalQty <= 0.0) {
            viewModel.showToast("Naam, Price aur Qty sahi se bharein", isError = true)
            return@Button
          }
          val finalCat = customCatInput.trim().ifEmpty { cat }
          onSave(
            MaterialItem(
              id = initial?.id ?: "mat_${UUID.randomUUID()}",
              cat = finalCat,
              name = finalName,
              price = finalPrice,
              qty = finalQty,
              unit = unit,
              img = imgPath,
              stock = stock.toDoubleOrNull(),
              low = low.toDoubleOrNull(),
            )
          )
        },
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_material_button"),
      ) {
        Text(if (initial == null) "Material Save Karein" else "Changes Save Karein")
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
