package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AutoAwesomeMosaic
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesomeMosaic
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.InAppToastBanner
import com.example.ui.components.KamarTajLogoMark
import com.example.ui.components.StudioGuideDialog
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.InventoryScreen
import com.example.ui.screens.OrdersAndReportScreen
import com.example.ui.screens.SettingsSheet
import com.example.ui.theme.KamarTajTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KamarTajRoot(viewModel: StudioViewModel) {
  val brand by viewModel.brandMeta.collectAsStateWithLifecycle()
  val items by viewModel.items.collectAsStateWithLifecycle()
  val itemsById by viewModel.itemsById.collectAsStateWithLifecycle()
  val products by viewModel.products.collectAsStateWithLifecycle()
  val productPrices by viewModel.productPrices.collectAsStateWithLifecycle()
  val orders by viewModel.orders.collectAsStateWithLifecycle()
  val categories by viewModel.allMaterialCategories.collectAsStateWithLifecycle()
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val ordersSubTab by viewModel.ordersSubTab.collectAsStateWithLifecycle()
  val draft by viewModel.calculatorDraft.collectAsStateWithLifecycle()
  val toast by viewModel.toast.collectAsStateWithLifecycle()
  val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

  var showSettingsSheet by remember { mutableStateOf(false) }
  var showGuideDialog by remember { mutableStateOf(false) }

  val lowStockCount = remember(items) { items.count { it.isLowStock } }

  if (activeTab != StudioTab.INVENTORY) {
    BackHandler {
      if (activeTab == StudioTab.ORDERS && ordersSubTab == 1) {
        viewModel.selectOrdersSubTab(0)
      } else {
        viewModel.selectTab(StudioTab.INVENTORY)
      }
    }
  }

  KamarTajTheme(accentHex = brand.color, themeMode = brand.mode) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useRail = maxWidth >= 600.dp

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
          Column {
            TopAppBar(
              colors =
                TopAppBarDefaults.topAppBarColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
              title = {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  KamarTajLogoMark(brand = brand, size = 36.dp)
                  Column {
                    Text(
                      text = brand.name.ifBlank { "Kamar Taj" },
                      style = MaterialTheme.typography.titleLarge,
                      fontWeight = FontWeight.Bold,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                      text =
                        if (activeTab == StudioTab.ORDERS && ordersSubTab == 1) "Monthly Report"
                        else activeTab.title,
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              },
              actions = {
                IconButton(
                  onClick = { viewModel.openMonthlyReport() },
                  modifier = Modifier.testTag("topbar_report_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Assessment,
                    contentDescription = "Monthly Report",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }
                IconButton(
                  onClick = { showGuideDialog = true },
                  modifier = Modifier.testTag("topbar_guide_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.HelpOutline,
                    contentDescription = "Shuru Kaise Karein Guide",
                  )
                }
                IconButton(
                  onClick = { showSettingsSheet = true },
                  modifier = Modifier.testTag("topbar_settings_button"),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Studio Settings",
                  )
                }
              },
            )
            if (isBusy) {
              LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
          }
        },
        bottomBar = {
          if (!useRail) {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.navigationBarsPadding(),
            ) {
              StudioTab.entries.forEach { tab ->
                val selected = activeTab == tab
                NavigationBarItem(
                  selected = selected,
                  onClick = { viewModel.selectTab(tab) },
                  icon = {
                    TabIconWithBadge(
                      tab = tab,
                      selected = selected,
                      lowStockCount = lowStockCount,
                    )
                  },
                  label = { Text(tab.title, maxLines = 1) },
                  modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
          if (useRail) {
            NavigationRail(
              containerColor = MaterialTheme.colorScheme.surface,
              modifier = Modifier.fillMaxHeight(),
            ) {
              StudioTab.entries.forEach { tab ->
                val selected = activeTab == tab
                NavigationRailItem(
                  selected = selected,
                  onClick = { viewModel.selectTab(tab) },
                  icon = {
                    TabIconWithBadge(
                      tab = tab,
                      selected = selected,
                      lowStockCount = lowStockCount,
                    )
                  },
                  label = { Text(tab.title) },
                  modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                )
              }
            }
          }

          Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            when (activeTab) {
              StudioTab.INVENTORY ->
                InventoryScreen(
                  items = items,
                  categories = categories,
                  viewModel = viewModel,
                )
              StudioTab.CALCULATOR ->
                CalculatorScreen(
                  draft = draft,
                  items = items,
                  itemsById = itemsById,
                  viewModel = viewModel,
                )
              StudioTab.CATALOG ->
                CatalogScreen(
                  products = products,
                  productPrices = productPrices,
                  brand = brand,
                  viewModel = viewModel,
                )
              StudioTab.ORDERS ->
                OrdersAndReportScreen(
                  orders = orders,
                  products = products,
                  productPrices = productPrices,
                  itemsById = itemsById,
                  subTab = ordersSubTab,
                  viewModel = viewModel,
                )
            }
          }
        }
      }

      // Floating In-App Toast Banner
      InAppToastBanner(
        toast = toast,
        onDismiss = { viewModel.dismissToast() },
        modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 60.dp),
      )
    }

    if (showSettingsSheet) {
      SettingsSheet(
        brand = brand,
        viewModel = viewModel,
        onOpenGuide = { showGuideDialog = true },
        onDismiss = { showSettingsSheet = false },
      )
    }

    if (showGuideDialog) {
      StudioGuideDialog(
        onLoadExampleData = { viewModel.loadExampleData() },
        onDismiss = { showGuideDialog = false },
      )
    }
  }
}

@Composable
private fun TabIconWithBadge(
  tab: StudioTab,
  selected: Boolean,
  lowStockCount: Int,
) {
  val icon =
    when (tab) {
      StudioTab.INVENTORY -> if (selected) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2
      StudioTab.CALCULATOR -> if (selected) Icons.Filled.Calculate else Icons.Outlined.Calculate
      StudioTab.CATALOG ->
        if (selected) Icons.Filled.AutoAwesomeMosaic else Icons.Outlined.AutoAwesomeMosaic
      StudioTab.ORDERS -> if (selected) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong
    }

  if (tab == StudioTab.INVENTORY && lowStockCount > 0) {
    BadgedBox(badge = { Badge { Text(lowStockCount.toString()) } }) {
      Icon(icon, contentDescription = tab.title)
    }
  } else {
    Icon(icon, contentDescription = tab.title)
  }
}
