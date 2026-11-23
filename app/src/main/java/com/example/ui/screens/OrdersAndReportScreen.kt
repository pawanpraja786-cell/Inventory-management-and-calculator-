package com.example.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ComputedPrice
import com.example.data.model.MaterialItem
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PricingEngine
import com.example.data.model.Product
import com.example.ui.StudioViewModel
import com.example.ui.components.ConfirmModal
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersAndReportScreen(
  orders: List<OrderRecord>,
  products: List<Product>,
  productPrices: Map<String, ComputedPrice>,
  itemsById: Map<String, MaterialItem>,
  subTab: Int,
  viewModel: StudioViewModel,
  modifier: Modifier = Modifier,
) {
  var searchQuery by remember { mutableStateOf("") }
  var statusFilter by remember { mutableStateOf("All") }
  var showOrderFormSheet by remember { mutableStateOf(false) }
  var editingOrder by remember { mutableStateOf<OrderRecord?>(null) }
  var quotationOrder by remember { mutableStateOf<OrderRecord?>(null) }
  var pendingDeleteOrder by remember { mutableStateOf<OrderRecord?>(null) }

  val filteredOrders =
    remember(orders, searchQuery, statusFilter) {
      orders.filter { o ->
        val matchesStatus =
          statusFilter == "All" || o.status.equals(statusFilter, ignoreCase = true)
        val matchesQuery =
          searchQuery.isBlank() ||
            o.cust.contains(searchQuery, ignoreCase = true) ||
            o.pname.contains(searchQuery, ignoreCase = true) ||
            o.phone.contains(searchQuery, ignoreCase = true) ||
            o.sku.contains(searchQuery, ignoreCase = true)
        matchesStatus && matchesQuery
      }
    }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    floatingActionButton = {
      if (subTab == 0) {
        FloatingActionButton(
          onClick = {
            editingOrder = null
            showOrderFormSheet = true
          },
          containerColor = MaterialTheme.colorScheme.primary,
          contentColor = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.testTag("add_order_fab"),
        ) {
          Icon(Icons.Filled.Add, contentDescription = "Naya Order Book Karein")
        }
      }
    },
  ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
      // Segmented switch: Orders vs Report
      SingleChoiceSegmentedButtonRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
      ) {
        SegmentedButton(
          selected = subTab == 0,
          onClick = { viewModel.selectOrdersSubTab(0) },
          shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
          modifier = Modifier.testTag("orders_subtab_button"),
        ) {
          Text("Orders (${orders.size})")
        }
        SegmentedButton(
          selected = subTab == 1,
          onClick = { viewModel.selectOrdersSubTab(1) },
          shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
          modifier = Modifier.testTag("report_subtab_button"),
        ) {
          Text("Report (Hisab)")
        }
      }

      if (subTab == 0) {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          item {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = { Text("Customer, phone ya product search...") },
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
              modifier = Modifier.fillMaxWidth(),
            )
          }

          item {
            Row(
              modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              FilterChip(
                selected = statusFilter == "All",
                onClick = { statusFilter = "All" },
                label = { Text("Sab (${orders.size})") },
              )
              OrderStatus.entries.forEach { st ->
                FilterChip(
                  selected = statusFilter == st.label,
                  onClick = { statusFilter = if (statusFilter == st.label) "All" else st.label },
                  label = { Text(st.label) },
                )
              }
            }
          }

          if (orders.isEmpty()) {
            item {
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
                  Text("Abhi Koi Order Nahi Hai", style = MaterialTheme.typography.titleLarge)
                  Text(
                    "Naya order add karne par materials ka stock apne aap kam hoga, aur customer ko bhejne ke liye PNG/PDF Quotation banega.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Button(
                    onClick = {
                      editingOrder = null
                      showOrderFormSheet = true
                    }
                  ) {
                    Text("+ Pehla Order Book Karein")
                  }
                }
              }
            }
          } else if (filteredOrders.isEmpty()) {
            item {
              Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(24.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                  Text("Koi order match nahi hua", style = MaterialTheme.typography.titleMedium)
                  TextButton(
                    onClick = {
                      searchQuery = ""
                      statusFilter = "All"
                    }
                  ) {
                    Text("Filters Reset Karein")
                  }
                }
              }
            }
          } else {
            items(filteredOrders, key = { it.id }) { order ->
              OrderCard(
                order = order,
                onStatusChange = { newStatus -> viewModel.updateOrderStatus(order, newStatus) },
                onShareQuotation = { quotationOrder = order },
                onEdit = {
                  editingOrder = order
                  showOrderFormSheet = true
                },
                onDelete = { pendingDeleteOrder = order },
              )
            }
          }
        }
      } else {
        MonthlyReportContent(orders = orders)
      }
    }
  }

  if (showOrderFormSheet) {
    OrderFormSheet(
      initial = editingOrder,
      products = products,
      productPrices = productPrices,
      itemsById = itemsById,
      viewModel = viewModel,
      onDismiss = { showOrderFormSheet = false },
      onSave = { savedOrder ->
        viewModel.saveOrder(savedOrder)
        showOrderFormSheet = false
      },
    )
  }

  quotationOrder?.let { target ->
    AlertDialog(
      onDismissRequest = { quotationOrder = null },
      title = { Text("Quotation Share Karein", style = MaterialTheme.typography.titleLarge) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "${target.cust} • ${target.pname} (${target.qty}x)",
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            "Total: ${PricingEngine.formatRupees(target.price)}  |  Advance: ${PricingEngine.formatRupees(target.advance)}  |  Baaki: ${PricingEngine.formatRupees(target.balanceDue)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = {
                quotationOrder = null
                viewModel.shareOrderQuotationPng(target)
              },
              modifier = Modifier.weight(1f).testTag("share_quotation_png_button"),
            ) {
              Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(Modifier.width(6.dp))
              Text("PNG")
            }
            OutlinedButton(
              onClick = {
                quotationOrder = null
                viewModel.shareOrderQuotationPdf(target)
              },
              modifier = Modifier.weight(1f).testTag("share_quotation_pdf_button"),
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
      confirmButton = {
        TextButton(onClick = { quotationOrder = null }) { Text("Band Karein") }
      },
    )
  }

  pendingDeleteOrder?.let { target ->
    ConfirmModal(
      title = "Order Delete Karein?",
      message =
        "\"${target.cust}\" ka order delete ho jayega${if (!target.restored && target.used.isNotEmpty()) " aur use hua material stock wapas jud jayega" else ""}.",
      onConfirm = {
        viewModel.deleteOrder(target)
        pendingDeleteOrder = null
      },
      onDismiss = { pendingDeleteOrder = null },
    )
  }
}

@Composable
private fun OrderCard(
  order: OrderRecord,
  onStatusChange: (OrderStatus) -> Unit,
  onShareQuotation: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  var statusMenuExpanded by remember { mutableStateOf(false) }
  val statusEnum = OrderStatus.fromLabel(order.status)
  val statusColor =
    when (statusEnum) {
      OrderStatus.DELIVERED -> SuccessGreen
      OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
      OrderStatus.READY -> MaterialTheme.colorScheme.primary
      OrderStatus.IN_PROGRESS -> WarningAmber
      OrderStatus.PENDING -> MaterialTheme.colorScheme.secondary
    }

  Card(
    modifier = Modifier.fillMaxWidth().clickable { onEdit() },
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    shape = RoundedCornerShape(18.dp),
  ) {
    Column(
      modifier = Modifier.padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = order.cust,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          if (order.phone.isNotBlank() || order.date.isNotBlank()) {
            Text(
              text =
                listOfNotNull(
                    order.phone.takeIf { it.isNotBlank() },
                    order.date.takeIf { it.isNotBlank() }?.let { "Delivery: $it" },
                  )
                  .joinToString(" • "),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Box {
          Surface(
            onClick = { statusMenuExpanded = true },
            color = statusColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = order.status,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = statusColor,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
          DropdownMenu(
            expanded = statusMenuExpanded,
            onDismissRequest = { statusMenuExpanded = false },
          ) {
            OrderStatus.entries.forEach { st ->
              DropdownMenuItem(
                text = { Text(st.label) },
                onClick = {
                  statusMenuExpanded = false
                  onStatusChange(st)
                },
              )
            }
          }
        }
      }

      Text(
        text =
          "${order.pname}  × ${order.qty}${if (order.sku.isNotBlank()) " (${order.sku})" else ""}",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Text(
          text = "Total: ${PricingEngine.formatRupees(order.price)}",
          style = MaterialTheme.typography.bodySmall,
        )
        Text(
          text = "Adv: ${PricingEngine.formatRupees(order.advance)}",
          style = MaterialTheme.typography.bodySmall,
          color = SuccessGreen,
        )
        Text(
          text = "Baaki: ${PricingEngine.formatRupees(order.balanceDue)}",
          style = MaterialTheme.typography.bodySmall,
          fontWeight = FontWeight.Bold,
          color = if (order.balanceDue > 0.0) MaterialTheme.colorScheme.primary else SuccessGreen,
        )
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(
          onClick = onShareQuotation,
          contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        ) {
          Icon(
            Icons.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text("Quotation Share")
        }

        Row {
          IconButton(onClick = onEdit) {
            Icon(
              Icons.Filled.Edit,
              contentDescription = "Edit Order",
              tint = MaterialTheme.colorScheme.primary,
            )
          }
          IconButton(onClick = onDelete) {
            Icon(
              Icons.Filled.DeleteOutline,
              contentDescription = "Delete Order",
              tint = MaterialTheme.colorScheme.error,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MonthlyReportContent(orders: List<OrderRecord>) {
  var selectedMonth by remember { mutableStateOf(PricingEngine.currentMonthKey()) }

  val monthOrders =
    remember(orders, selectedMonth) {
      orders.filter { order ->
        val orderMonth =
          if (order.date.length >= 7) order.date.take(7)
          else SimpleDateFormat("yyyy-MM", Locale.US).format(order.createdAt)
        orderMonth == selectedMonth
      }
    }

  val activeOrders =
    remember(monthOrders) {
      monthOrders.filter { it.status != OrderStatus.CANCELLED.label }
    }
  val cancelledCount = monthOrders.size - activeOrders.size
  val totalSale = activeOrders.sumOf { it.price }
  val totalCost = activeOrders.sumOf { it.cost }
  val totalProfit = totalSale - totalCost
  val totalAdvance = activeOrders.sumOf { it.advance }
  val totalBaaki = activeOrders.sumOf { it.balanceDue }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Month Switcher
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          IconButton(onClick = { selectedMonth = shiftMonth(selectedMonth, -1) }) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Pichla Mahina")
          }
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = PricingEngine.monthDisplay(selectedMonth),
              style = MaterialTheme.typography.titleLarge,
            )
            Text(
              text = "${activeOrders.size} active orders" +
                if (cancelledCount > 0) " • $cancelledCount cancelled" else "",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          IconButton(onClick = { selectedMonth = shiftMonth(selectedMonth, 1) }) {
            Icon(Icons.Filled.ChevronRight, contentDescription = "Agla Mahina")
          }
        }
      }
    }

    // Metrics Grid
    item {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ReportMetricCard(
            title = "Total Sale",
            value = PricingEngine.formatRupees(totalSale),
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
          )
          ReportMetricCard(
            title = "Total Cost",
            value = PricingEngine.formatRupees(totalCost),
            accent = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f),
          )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          ReportMetricCard(
            title = "Net Profit",
            value = PricingEngine.formatRupees(totalProfit),
            accent = if (totalProfit >= 0) SuccessGreen else MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
          )
          ReportMetricCard(
            title = "Advance Mila",
            value = PricingEngine.formatRupees(totalAdvance),
            accent = SuccessGreen,
            modifier = Modifier.weight(1f),
          )
        }
        ReportMetricCard(
          title = "Baaki Payment (Lena Baaki)",
          value = PricingEngine.formatRupees(totalBaaki),
          accent = if (totalBaaki > 0.0) WarningAmber else SuccessGreen,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }

    if (monthOrders.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
          Text(
            text = "${PricingEngine.monthDisplay(selectedMonth)} mein koi order record nahi hai.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(20.dp),
          )
        }
      }
    } else {
      item {
        Text(
          text = "Mahine ke Orders (${monthOrders.size})",
          style = MaterialTheme.typography.titleMedium,
        )
      }
      items(monthOrders, key = { it.id }) { o ->
        Surface(
          color = MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(o.cust, style = MaterialTheme.typography.titleSmall)
              Text(
                "${o.pname} (${o.qty}x) • ${o.status}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Column(horizontalAlignment = Alignment.End) {
              Text(
                PricingEngine.formatRupees(o.price),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
              )
              if (o.balanceDue > 0.0 && o.status != OrderStatus.CANCELLED.label) {
                Text(
                  "Baaki ${PricingEngine.formatRupees(o.balanceDue)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = WarningAmber,
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ReportMetricCard(
  title: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Card(
    modifier = modifier,
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
  ) {
    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = accent,
      )
    }
  }
}

private fun shiftMonth(monthKey: String, deltaMonths: Int): String {
  return try {
    val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
    val cal = Calendar.getInstance()
    cal.time = sdf.parse(monthKey)!!
    cal.add(Calendar.MONTH, deltaMonths)
    sdf.format(cal.time)
  } catch (_: Exception) {
    PricingEngine.currentMonthKey()
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrderFormSheet(
  initial: OrderRecord?,
  products: List<Product>,
  productPrices: Map<String, ComputedPrice>,
  itemsById: Map<String, MaterialItem>,
  viewModel: StudioViewModel,
  onDismiss: () -> Unit,
  onSave: (OrderRecord) -> Unit,
) {
  val context = LocalContext.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  var cust by remember { mutableStateOf(initial?.cust ?: "") }
  var phone by remember { mutableStateOf(initial?.phone ?: "") }
  var selectedPid by remember {
    mutableStateOf(initial?.pid ?: products.firstOrNull()?.id ?: "__c")
  }
  var customPname by remember { mutableStateOf(initial?.pname ?: "") }
  var qtyText by remember { mutableStateOf((initial?.qty ?: 1).toString()) }

  val initialProd = products.firstOrNull { it.id == selectedPid }
  val initialPrice =
    initial?.price
      ?: (initialProd?.let { (productPrices[it.id]?.retail ?: 0.0) * 1 })
      ?: 0.0
  val initialCost =
    initial?.cost
      ?: (initialProd?.let { (productPrices[it.id]?.cost ?: 0.0) * 1 })
      ?: 0.0

  var priceText by remember {
    mutableStateOf(if (initialPrice > 0.0) PricingEngine.formatQty(initialPrice) else "")
  }
  var costText by remember {
    mutableStateOf(if (initialCost > 0.0) PricingEngine.formatQty(initialCost) else "")
  }
  var advanceText by remember {
    mutableStateOf(initial?.advance?.let(PricingEngine::formatQty) ?: "0")
  }
  var deliveryDate by remember {
    mutableStateOf(initial?.date?.ifBlank { PricingEngine.todayIso() } ?: PricingEngine.todayIso())
  }
  var status by remember {
    mutableStateOf(OrderStatus.fromLabel(initial?.status ?: OrderStatus.PENDING.label))
  }
  var notes by remember { mutableStateOf(initial?.notes ?: "") }

  val qtyInt = max(1, qtyText.toIntOrNull() ?: 1)
  val selectedProduct = products.firstOrNull { it.id == selectedPid }

  val usedPreview =
    remember(selectedProduct, qtyInt, initial) {
      if (selectedProduct != null) {
        PricingEngine.computeUsedMaterialsForOrder(selectedProduct, qtyInt)
      } else {
        initial?.used ?: emptyMap()
      }
    }

  ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = if (initial == null) "Naya Order Book Karein" else "Order Edit Karein",
        style = MaterialTheme.typography.titleLarge,
      )

      OutlinedTextField(
        value = cust,
        onValueChange = { cust = it },
        label = { Text("Customer ka Naam *") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("order_customer_input"),
      )

      OutlinedTextField(
        value = phone,
        onValueChange = { phone = it },
        label = { Text("Phone / WhatsApp Number") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )

      Text("Product Chunein", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        FilterChip(
          selected = selectedPid == "__c",
          onClick = { selectedPid = "__c" },
          label = { Text("Custom Order") },
        )
        products.forEach { prod ->
          FilterChip(
            selected = selectedPid == prod.id,
            onClick = {
              selectedPid = prod.id
              customPname = prod.name
              val pInfo = productPrices[prod.id]
              if (pInfo != null) {
                priceText = PricingEngine.formatQty(pInfo.retail * qtyInt)
                costText = PricingEngine.formatQty(pInfo.cost * qtyInt)
              }
            },
            label = { Text(prod.name, maxLines = 1) },
          )
        }
      }

      if (selectedPid == "__c") {
        OutlinedTextField(
          value = customPname,
          onValueChange = { customPname = it },
          label = { Text("Custom Product Details *") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = qtyText,
          onValueChange = { newVal ->
            qtyText = newVal
            val q = max(1, newVal.toIntOrNull() ?: 1)
            selectedProduct?.let { prod ->
              val pInfo = productPrices[prod.id]
              if (pInfo != null) {
                priceText = PricingEngine.formatQty(pInfo.retail * q)
                costText = PricingEngine.formatQty(pInfo.cost * q)
              }
            }
          },
          label = { Text("Quantity") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = priceText,
          onValueChange = { priceText = it },
          label = { Text("Total Price (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = costText,
          onValueChange = { costText = it },
          label = { Text("Making Cost (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
          value = advanceText,
          onValueChange = { advanceText = it },
          label = { Text("Advance Mila (₹)") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
      }

      // Delivery date with picker
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = deliveryDate,
          onValueChange = { deliveryDate = it },
          label = { Text("Delivery Date (YYYY-MM-DD)") },
          singleLine = true,
          modifier = Modifier.weight(1f),
        )
        IconButton(
          onClick = {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                context,
                { _, y, m, d ->
                  deliveryDate =
                    String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH),
              )
              .show()
          }
        ) {
          Icon(Icons.Filled.CalendarMonth, contentDescription = "Pick Delivery Date")
        }
      }

      Text("Order Status", style = MaterialTheme.typography.labelLarge)
      Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        OrderStatus.entries.forEach { st ->
          FilterChip(
            selected = status == st,
            onClick = { status = st },
            label = { Text(st.label) },
          )
        }
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Order Notes / Customization") },
        maxLines = 2,
        modifier = Modifier.fillMaxWidth(),
      )

      if (usedPreview.isNotEmpty() && status != OrderStatus.CANCELLED) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              "Stock Deduction Preview:",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
            usedPreview.forEach { (itemId, amount) ->
              val mat = itemsById[itemId]
              if (mat != null) {
                Text(
                  "• ${mat.name}: -${PricingEngine.formatQty(amount)} ${mat.unit}",
                  style = MaterialTheme.typography.bodySmall,
                )
              }
            }
          }
        }
      }

      Button(
        onClick = {
          val cleanCust = cust.trim()
          val pname =
            if (selectedPid == "__c") customPname.trim()
            else selectedProduct?.name ?: customPname.trim()
          if (cleanCust.isEmpty() || pname.isEmpty()) {
            viewModel.showToast("Customer naam aur product detail bharein", isError = true)
            return@Button
          }
          onSave(
            OrderRecord(
              id = initial?.id ?: "ord_${UUID.randomUUID()}",
              cust = cleanCust,
              phone = phone.trim(),
              pid = selectedPid,
              pname = pname,
              sku = selectedProduct?.sku ?: (initial?.sku ?: ""),
              qty = qtyInt,
              price = priceText.toDoubleOrNull() ?: 0.0,
              cost = costText.toDoubleOrNull() ?: 0.0,
              advance = advanceText.toDoubleOrNull() ?: 0.0,
              date = deliveryDate.trim().ifBlank { PricingEngine.todayIso() },
              status = status.label,
              notes = notes.trim(),
              used = usedPreview,
              restored = initial?.restored ?: false,
              createdAt = initial?.createdAt ?: System.currentTimeMillis(),
            )
          )
        },
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_order_button"),
      ) {
        Text(if (initial == null) "Order Save Karein" else "Order Update Karein")
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}
