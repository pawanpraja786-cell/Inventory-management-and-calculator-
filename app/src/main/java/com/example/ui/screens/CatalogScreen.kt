package com.example.ui.screens

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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BrandMeta
import com.example.data.model.ComputedPrice
import com.example.data.model.PricingEngine
import com.example.data.model.Product
import com.example.ui.StudioTab
import com.example.ui.StudioViewModel
import com.example.ui.components.ConfirmModal
import com.example.ui.components.KamarTajLogoMark
import com.example.ui.components.StudioImageOrPlaceholder

@Composable
fun CatalogScreen(
  products: List<Product>,
  productPrices: Map<String, ComputedPrice>,
  brand: BrandMeta,
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedTag by remember { mutableStateOf("All") }
  var shareTarget by remember { mutableStateOf<Product?>(null) }
  var showFullCatalogDialog by remember { mutableStateOf(false) }
  var pendingDeleteProduct by remember { mutableStateOf<Product?>(null) }

  val tags =
    remember(products) {
      products.map { it.cat.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    }

  val filteredProducts =
    remember(products, searchQuery, selectedTag) {
      products.filter { prod ->
        val matchesTag = selectedTag == "All" || prod.cat.equals(selectedTag, ignoreCase = true)
        val matchesQuery =
          searchQuery.isBlank() ||
            prod.name.contains(searchQuery, ignoreCase = true) ||
            prod.sku.contains(searchQuery, ignoreCase = true) ||
            prod.cat.contains(searchQuery, ignoreCase = true)
        matchesTag && matchesQuery
      }
    }

  LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 168.dp),
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Search + Full Catalog PDF button
    item(span = { GridItemSpan(maxLineSpan) }) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Product naam ya SKU search...") },
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
            modifier = Modifier.weight(1f).testTag("catalog_search_input"),
          )
          Surface(
            onClick = { showFullCatalogDialog = true },
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.height(54.dp).testTag("full_catalog_pdf_button"),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                Icons.Filled.PictureAsPdf,
                contentDescription = "Full Catalog PDF",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
              )
              Text(
                "PDF",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
              )
            }
          }
        }

        if (tags.isNotEmpty()) {
          Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = selectedTag == "All",
              onClick = { selectedTag = "All" },
              label = { Text("Sab (${products.size})") },
            )
            tags.forEach { tag ->
              FilterChip(
                selected = selectedTag.equals(tag, ignoreCase = true),
                onClick = { selectedTag = if (selectedTag == tag) "All" else tag },
                label = { Text(tag) },
              )
            }
          }
        }
      }
    }

    if (products.isEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(20.dp),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text("Catalog Abhi Khaali Hai", style = MaterialTheme.typography.titleLarge)
            Text(
              "Calculator tab mein product banayein ya Inventory se example materials & coaster load karein.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = {
                  viewModel.resetCalculatorDraft()
                  viewModel.selectTab(StudioTab.CALCULATOR)
                }
              ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Calculator Kholien")
              }
              OutlinedButton(onClick = { viewModel.loadExampleData() }) {
                Text("Example Load Karein")
              }
            }
          }
        }
      }
    } else if (filteredProducts.isEmpty()) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Text("Koi matching product nahi mila", style = MaterialTheme.typography.titleMedium)
            TextButton(
              onClick = {
                searchQuery = ""
                selectedTag = "All"
              }
            ) {
              Text("Filters Reset Karein")
            }
          }
        }
      }
    } else {
      items(filteredProducts, key = { it.id }) { prod ->
        val price = productPrices[prod.id]
        CatalogProductCard(
          product = prod,
          price = price,
          onShare = { shareTarget = prod },
          onEdit = { viewModel.loadProductIntoCalculator(prod) },
          onCopy = { viewModel.copyProductWithNewSku(prod) },
          onDelete = { pendingDeleteProduct = prod },
        )
      }
    }
  }

  shareTarget?.let { prod ->
    val price = productPrices[prod.id] ?: PricingEngine.computeProductPrice(prod, emptyMap())
    ProductSharePreviewDialog(
      product = prod,
      price = price,
      brand = brand,
      onDismiss = { shareTarget = null },
      onSharePng = { showPrice -> viewModel.shareProductPng(prod, showPrice) },
      onSharePdf = { showPrice -> viewModel.shareProductPdf(prod, showPrice) },
    )
  }

  if (showFullCatalogDialog) {
    var includePrice by remember { mutableStateOf(true) }
    AlertDialog(
      onDismissRequest = { showFullCatalogDialog = false },
      title = { Text("Poore Catalog ka PDF", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Text(
            "${filteredProducts.size} products ka printable/WhatsApp-ready PDF banega. Customer ko cost ya profit kabhi nahi dikhega.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Text("PDF mein Retail Price dikhao", style = MaterialTheme.typography.titleSmall)
            Switch(checked = includePrice, onCheckedChange = { includePrice = it })
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            showFullCatalogDialog = false
            viewModel.shareFullCatalogPdf(filteredProducts, includePrice)
          }
        ) {
          Text("PDF Share Karein")
        }
      },
      dismissButton = {
        TextButton(onClick = { showFullCatalogDialog = false }) { Text("Cancel") }
      },
    )
  }

  pendingDeleteProduct?.let { target ->
    ConfirmModal(
      title = "Product Delete Karein?",
      message = "\"${target.name}\" (SKU: ${target.sku}) catalog se delete ho jayega.",
      onConfirm = {
        viewModel.deleteProduct(target)
        pendingDeleteProduct = null
      },
      onDismiss = { pendingDeleteProduct = null },
    )
  }
}

@Composable
private fun CatalogProductCard(
  product: Product,
  price: ComputedPrice?,
  onShare: () -> Unit,
  onEdit: () -> Unit,
  onCopy: () -> Unit,
  onDelete: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth().clickable { onShare() },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.1f)) {
        StudioImageOrPlaceholder(
          path = product.imgs.firstOrNull(),
          contentDescription = product.name,
          modifier = Modifier.fillMaxSize(),
        )
        if (product.cat.isNotBlank()) {
          Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
          ) {
            Text(
              text = product.cat,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            )
          }
        }
        if (product.imgs.size > 1) {
          Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
          ) {
            Text(
              text = "${product.imgs.size} photos",
              style = MaterialTheme.typography.labelSmall,
              color = Color.White,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
      }

      Column(
        modifier = Modifier.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = product.sku,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = product.name,
          style = MaterialTheme.typography.titleSmall,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = PricingEngine.formatRupees(price?.retail ?: 0.0),
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text =
            "Cost ${PricingEngine.formatRupees(price?.cost ?: 0.0)} • WS ${PricingEngine.formatRupees(price?.wholesale ?: 0.0)}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )

        HorizontalDivider(
          modifier = Modifier.padding(vertical = 4.dp),
          color = MaterialTheme.colorScheme.outlineVariant,
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          IconButton(onClick = onShare, modifier = Modifier.size(36.dp)) {
            Icon(
              Icons.Filled.Share,
              contentDescription = "Share Product Card",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(19.dp),
            )
          }
          IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(
              Icons.Filled.Edit,
              contentDescription = "Edit Product",
              modifier = Modifier.size(19.dp),
            )
          }
          IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(
              Icons.Filled.ContentCopy,
              contentDescription = "Copy Product",
              modifier = Modifier.size(18.dp),
            )
          }
          IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
              Icons.Filled.DeleteOutline,
              contentDescription = "Delete Product",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(19.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ProductSharePreviewDialog(
  product: Product,
  price: ComputedPrice,
  brand: BrandMeta,
  onDismiss: () -> Unit,
  onSharePng: (Boolean) -> Unit,
  onSharePdf: (Boolean) -> Unit,
) {
  var showPrice by remember { mutableStateOf(true) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("Customer Share Card", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onDismiss) {
          Icon(Icons.Filled.Close, contentDescription = "Close")
        }
      }
    },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // Live customer-facing preview (NO cost or profit!)
        Surface(
          shape = RoundedCornerShape(16.dp),
          tonalElevation = 2.dp,
          modifier =
            Modifier
              .fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        ) {
          Column {
            Row(
              modifier =
                Modifier
                  .fillMaxWidth()
                  .background(MaterialTheme.colorScheme.primary)
                  .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              KamarTajLogoMark(brand = brand, size = 34.dp)
              Text(
                text = brand.name.ifBlank { "Kamar Taj" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
              )
            }

            StudioImageOrPlaceholder(
              path = product.imgs.firstOrNull(),
              contentDescription = product.name,
              modifier = Modifier.fillMaxWidth().height(170.dp),
            )

            if (product.imgs.size > 1) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                product.imgs.take(4).forEach { path ->
                  StudioImageOrPlaceholder(
                    path = path,
                    contentDescription = product.name,
                    modifier =
                      Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(8.dp)),
                  )
                }
              }
            }

            Column(
              modifier = Modifier.padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              Text(
                text = "SKU: ${product.sku}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(text = product.name, style = MaterialTheme.typography.titleMedium)
              if (showPrice) {
                Text(
                  text = PricingEngine.formatRupees(price.retail),
                  style = MaterialTheme.typography.titleLarge,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.Bold,
                )
              }
              if (brand.contact.isNotBlank()) {
                Text(
                  text = brand.contact,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Price Dikhao", style = MaterialTheme.typography.titleSmall)
            Text(
              "Cost aur profit customer ko kabhi nahi dikhega",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Switch(checked = showPrice, onCheckedChange = { showPrice = it })
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = { onSharePng(showPrice) },
            modifier = Modifier.weight(1f).testTag("share_product_png_button"),
          ) {
            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("PNG")
          }
          OutlinedButton(
            onClick = { onSharePdf(showPrice) },
            modifier = Modifier.weight(1f).testTag("share_product_pdf_button"),
          ) {
            Icon(
              Icons.Filled.PictureAsPdf,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("PDF")
          }
        }
      }
    },
    confirmButton = {},
  )
}
