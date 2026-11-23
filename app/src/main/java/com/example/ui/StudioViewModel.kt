package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.KamarTajApp
import com.example.data.model.BrandMeta
import com.example.data.model.ComputedPrice
import com.example.data.model.CostLine
import com.example.data.model.MaterialItem
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.PricingEngine
import com.example.data.model.Product
import com.example.data.model.StudioConstants
import com.example.data.repository.StudioRepository
import com.example.util.MediaAndShareHelper
import java.util.UUID
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class StudioTab(val title: String) {
  INVENTORY("Inventory"),
  CALCULATOR("Calculator"),
  CATALOG("Catalog"),
  ORDERS("Orders"),
}

data class ToastMessage(
  val id: String = UUID.randomUUID().toString(),
  val text: String,
  val isError: Boolean = false,
)

data class CalculatorDraft(
  val editingId: String? = null,
  val name: String = "",
  val sku: String = "",
  val cat: String = "Coaster",
  val imgs: List<String> = emptyList(),
  val profit: String = "50",
  val fee: String = "0",
  val wp: String = "25",
  val notes: String = "",
  val lines: List<CostLine> = emptyList(),
)

class StudioViewModel(
  application: Application,
  private val repository: StudioRepository,
) : AndroidViewModel(application) {

  val items: StateFlow<List<MaterialItem>> =
    repository.items.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val products: StateFlow<List<Product>> =
    repository.products.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val orders: StateFlow<List<OrderRecord>> =
    repository.orders.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val brandMeta: StateFlow<BrandMeta> =
    repository.brandMeta.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrandMeta())

  val itemsById: StateFlow<Map<String, MaterialItem>> =
    items
      .map { list -> list.associateBy { it.id } }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  val productPrices: StateFlow<Map<String, ComputedPrice>> =
    combine(products, itemsById) { prods, matMap ->
        prods.associate { prod -> prod.id to PricingEngine.computeProductPrice(prod, matMap) }
      }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

  val allMaterialCategories: StateFlow<List<String>> =
    combine(items, brandMeta) { mats, meta ->
        val ordered = LinkedHashSet<String>()
        ordered.addAll(StudioConstants.DEFAULT_MATERIAL_CATEGORIES)
        ordered.addAll(meta.customCats.filter { it.isNotBlank() })
        mats.forEach { if (it.cat.isNotBlank()) ordered.add(it.cat) }
        ordered.toList()
      }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StudioConstants.DEFAULT_MATERIAL_CATEGORIES,
      )

  private val _activeTab = MutableStateFlow(StudioTab.INVENTORY)
  val activeTab: StateFlow<StudioTab> = _activeTab.asStateFlow()

  private val _ordersSubTab = MutableStateFlow(0) // 0 = Orders, 1 = Report
  val ordersSubTab: StateFlow<Int> = _ordersSubTab.asStateFlow()

  private val _calculatorDraft = MutableStateFlow(CalculatorDraft())
  val calculatorDraft: StateFlow<CalculatorDraft> = _calculatorDraft.asStateFlow()

  private val _toast = MutableStateFlow<ToastMessage?>(null)
  val toast: StateFlow<ToastMessage?> = _toast.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private var toastJob: Job? = null

  init {
    viewModelScope.launch { repository.ensureInitialMeta() }
  }

  fun selectTab(tab: StudioTab) {
    _activeTab.value = tab
  }

  fun openMonthlyReport() {
    _ordersSubTab.value = 1
    _activeTab.value = StudioTab.ORDERS
  }

  fun selectOrdersSubTab(index: Int) {
    _ordersSubTab.value = index
  }

  fun showToast(text: String, isError: Boolean = false) {
    toastJob?.cancel()
    _toast.value = ToastMessage(text = text, isError = isError)
    toastJob =
      viewModelScope.launch {
        delay(2800)
        _toast.value = null
      }
  }

  fun dismissToast() {
    toastJob?.cancel()
    _toast.value = null
  }

  // --- Materials ---
  fun saveMaterial(item: MaterialItem) {
    viewModelScope.launch {
      if (item.cat.isNotBlank() &&
        StudioConstants.DEFAULT_MATERIAL_CATEGORIES.none { it.equals(item.cat, ignoreCase = true) }
      ) {
        repository.addCustomCategory(item.cat)
      }
      repository.upsertMaterial(item)
      showToast("Material save ho gaya: ${item.name}")
    }
  }

  fun adjustStock(itemId: String, delta: Double) {
    viewModelScope.launch { repository.adjustMaterialStock(itemId, delta) }
  }

  fun deleteMaterial(item: MaterialItem) {
    viewModelScope.launch {
      repository.deleteMaterial(item.id)
      showToast("Material delete ho gaya")
    }
  }

  fun addCustomCategory(category: String) {
    val clean = category.trim()
    if (clean.isEmpty()) {
      showToast("Section ka naam likhein", isError = true)
      return
    }
    viewModelScope.launch {
      repository.addCustomCategory(clean)
      showToast("Naya section '$clean' add ho gaya")
    }
  }

  fun loadExampleData() {
    viewModelScope.launch {
      repository.insertExampleStudioData()
      showToast("Example materials aur sample coaster add ho gaye!")
    }
  }

  // --- Calculator ---
  fun updateDraft(transform: (CalculatorDraft) -> CalculatorDraft) {
    _calculatorDraft.value = transform(_calculatorDraft.value)
  }

  fun resetCalculatorDraft() {
    _calculatorDraft.value = CalculatorDraft(sku = suggestNextSku())
  }

  fun suggestNextSku(): String {
    val existing = products.value.map { it.sku.uppercase() }.toSet()
    var num = products.value.size + 1
    while (existing.contains("KT-${num.toString().padStart(3, '0')}")) {
      num++
    }
    return "KT-${num.toString().padStart(3, '0')}"
  }

  fun loadProductIntoCalculator(product: Product) {
    _calculatorDraft.value =
      CalculatorDraft(
        editingId = product.id,
        name = product.name,
        sku = product.sku,
        cat = product.cat.ifBlank { "Coaster" },
        imgs = product.imgs,
        profit = PricingEngine.formatQty(product.profit),
        fee = PricingEngine.formatQty(product.fee),
        wp = PricingEngine.formatQty(product.wp),
        notes = product.notes,
        lines = product.lines.mapNotNull { CostLine.fromDto(it) },
      )
    _activeTab.value = StudioTab.CALCULATOR
  }

  fun addProductPhoto(uri: Uri) {
    val current = _calculatorDraft.value
    if (current.imgs.size >= 4) {
      showToast("Max 4 photos allowed hain", isError = true)
      return
    }
    viewModelScope.launch {
      val path = MediaAndShareHelper.copyUriToInternalMedia(getApplication(), uri, "prod")
      if (path != null) {
        updateDraft { draft ->
          if (draft.imgs.size < 4) draft.copy(imgs = draft.imgs + path) else draft
        }
      } else {
        showToast("Photo load nahi ho payi", isError = true)
      }
    }
  }

  suspend fun savePickedMaterialPhoto(uri: Uri): String? {
    return MediaAndShareHelper.copyUriToInternalMedia(getApplication(), uri, "mat")
  }

  fun saveDraftToCatalog() {
    val draft = _calculatorDraft.value
    val cleanName = draft.name.trim()
    val cleanSku = draft.sku.trim().ifBlank { suggestNextSku() }
    if (cleanName.isEmpty()) {
      showToast("Product ka naam zaroori hai", isError = true)
      return
    }
    if (draft.lines.isEmpty()) {
      showToast("Kam se kam 1 cost line add karein", isError = true)
      return
    }
    val duplicateSku =
      products.value.any {
        it.sku.equals(cleanSku, ignoreCase = true) && it.id != draft.editingId
      }
    if (duplicateSku) {
      showToast("Yeh SKU ($cleanSku) pehle se maujood hai, naya SKU rakhein", isError = true)
      return
    }
    val profitVal = draft.profit.toDoubleOrNull() ?: 50.0
    val feeVal = (draft.fee.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 95.0)
    val wpVal = draft.wp.toDoubleOrNull() ?: 25.0

    val product =
      Product(
        id = draft.editingId ?: "prod_${UUID.randomUUID()}",
        name = cleanName,
        sku = cleanSku,
        cat = draft.cat.trim().ifBlank { "Coaster" },
        imgs = draft.imgs.take(4),
        profit = profitVal,
        fee = feeVal,
        wp = wpVal,
        notes = draft.notes.trim(),
        lines = draft.lines.map { it.toDto() },
        updatedAt = System.currentTimeMillis(),
      )

    viewModelScope.launch {
      repository.upsertProduct(product)
      _calculatorDraft.value =
        draft.copy(editingId = product.id, sku = cleanSku)
      showToast("Catalog mein save ho gaya: $cleanName")
    }
  }

  // --- Catalog Actions ---
  fun copyProductWithNewSku(product: Product) {
    viewModelScope.launch {
      val existingSkus = products.value.map { it.sku.uppercase() }.toSet()
      var suffix = 2
      var candidate = "${product.sku}-COPY"
      while (existingSkus.contains(candidate.uppercase())) {
        candidate = "${product.sku}-COPY$suffix"
        suffix++
      }
      val copied =
        product.copy(
          id = "prod_${UUID.randomUUID()}",
          name = "${product.name} (Copy)",
          sku = candidate,
          updatedAt = System.currentTimeMillis(),
        )
      repository.upsertProduct(copied)
      showToast("Product copy ho gaya (SKU: $candidate)")
    }
  }

  fun deleteProduct(product: Product) {
    viewModelScope.launch {
      repository.deleteProduct(product.id)
      if (_calculatorDraft.value.editingId == product.id) {
        _calculatorDraft.value = _calculatorDraft.value.copy(editingId = null)
      }
      showToast("Product catalog se hat gaya")
    }
  }

  fun shareProductPng(product: Product, showPrice: Boolean) {
    val price = productPrices.value[product.id] ?: PricingEngine.computeProductPrice(product, itemsById.value)
    viewModelScope.launch {
      _isBusy.value = true
      runCatching {
        MediaAndShareHelper.shareProductPng(getApplication(), product, price, brandMeta.value, showPrice)
      }.onFailure { showToast("Share error: ${it.localizedMessage}", isError = true) }
      _isBusy.value = false
    }
  }

  fun shareProductPdf(product: Product, showPrice: Boolean) {
    val price = productPrices.value[product.id] ?: PricingEngine.computeProductPrice(product, itemsById.value)
    viewModelScope.launch {
      _isBusy.value = true
      runCatching {
        MediaAndShareHelper.shareProductPdf(getApplication(), product, price, brandMeta.value, showPrice)
      }.onFailure { showToast("PDF error: ${it.localizedMessage}", isError = true) }
      _isBusy.value = false
    }
  }

  fun shareFullCatalogPdf(filteredProducts: List<Product>, showPrice: Boolean) {
    if (filteredProducts.isEmpty()) {
      showToast("Catalog mein koi product nahi hai", isError = true)
      return
    }
    val map = productPrices.value
    val pairs =
      filteredProducts.map { prod ->
        prod to (map[prod.id] ?: PricingEngine.computeProductPrice(prod, itemsById.value))
      }
    viewModelScope.launch {
      _isBusy.value = true
      runCatching {
        MediaAndShareHelper.shareFullCatalogPdf(getApplication(), pairs, brandMeta.value, showPrice)
      }.onFailure { showToast("Catalog PDF error: ${it.localizedMessage}", isError = true) }
      _isBusy.value = false
    }
  }

  // --- Orders ---
  fun saveOrder(order: OrderRecord) {
    viewModelScope.launch {
      repository.saveOrderWithStockSync(order)
      showToast("Order save ho gaya: ${order.cust}")
    }
  }

  fun updateOrderStatus(order: OrderRecord, status: OrderStatus) {
    viewModelScope.launch {
      repository.updateOrderStatus(order.id, status)
      val msg =
        if (status == OrderStatus.CANCELLED && order.used.isNotEmpty()) {
          "Order Cancelled — material stock wapas jud gaya"
        } else {
          "Status updated: ${status.label}"
        }
      showToast(msg)
    }
  }

  fun deleteOrder(order: OrderRecord) {
    viewModelScope.launch {
      repository.deleteOrderWithStockRestore(order.id)
      showToast("Order delete ho gaya${if (!order.restored && order.used.isNotEmpty()) " (stock restored)" else ""}")
    }
  }

  fun shareOrderQuotationPng(order: OrderRecord) {
    viewModelScope.launch {
      _isBusy.value = true
      runCatching {
        MediaAndShareHelper.shareOrderQuotationPng(getApplication(), order, brandMeta.value)
      }.onFailure { showToast("Quotation share error: ${it.localizedMessage}", isError = true) }
      _isBusy.value = false
    }
  }

  fun shareOrderQuotationPdf(order: OrderRecord) {
    viewModelScope.launch {
      _isBusy.value = true
      runCatching {
        MediaAndShareHelper.shareOrderQuotationPdf(getApplication(), order, brandMeta.value)
      }.onFailure { showToast("Quotation PDF error: ${it.localizedMessage}", isError = true) }
      _isBusy.value = false
    }
  }

  // --- Brand / Settings ---
  fun saveBrandSettings(
    name: String,
    contact: String,
    colorHex: String,
    mode: String,
  ) {
    viewModelScope.launch {
      val current = brandMeta.value
      repository.saveBrandMeta(
        current.copy(
          name = name.trim().ifBlank { "Kamar Taj" },
          contact = contact.trim(),
          color = colorHex,
          mode = mode,
        )
      )
      showToast("Settings save ho gayi!")
    }
  }

  fun updateBrandLogo(uri: Uri) {
    viewModelScope.launch {
      val path = MediaAndShareHelper.copyUriToInternalMedia(getApplication(), uri, "logo")
      if (path != null) {
        repository.saveBrandMeta(brandMeta.value.copy(logo = path))
        showToast("Logo update ho gaya!")
      } else {
        showToast("Logo load nahi ho paya", isError = true)
      }
    }
  }

  fun removeBrandLogo() {
    viewModelScope.launch {
      repository.saveBrandMeta(brandMeta.value.copy(logo = null))
      showToast("Custom logo hata diya gaya")
    }
  }

  // --- Backup / Restore ---
  fun shareBackupJson() {
    viewModelScope.launch {
      _isBusy.value = true
      runCatching {
        val json = repository.exportBackupJson()
        MediaAndShareHelper.shareJsonBackup(getApplication(), json)
      }.onFailure { showToast("Backup fail: ${it.localizedMessage}", isError = true) }
      _isBusy.value = false
    }
  }

  fun saveBackupToUri(uri: Uri) {
    viewModelScope.launch {
      _isBusy.value = true
      runCatching {
        val json = repository.exportBackupJson()
        getApplication<Application>().contentResolver.openOutputStream(uri)?.use { out ->
          out.write(json.toByteArray(Charsets.UTF_8))
        } ?: error("File open nahi ho payi")
        showToast("JSON Backup download ho gaya!")
      }.onFailure { showToast("Backup save fail: ${it.localizedMessage}", isError = true) }
      _isBusy.value = false
    }
  }

  fun restoreBackupFromUri(uri: Uri) {
    viewModelScope.launch {
      _isBusy.value = true
      runCatching {
        val json =
          getApplication<Application>().contentResolver.openInputStream(uri)?.use {
            it.bufferedReader(Charsets.UTF_8).readText()
          } ?: error("File read nahi ho payi")
        val res = repository.importBackupJson(json)
        res.fold(
          onSuccess = { msg -> showToast(msg) },
          onFailure = { err -> showToast("Restore error: ${err.localizedMessage}", isError = true) },
        )
      }.onFailure { showToast("Restore fail: ${it.localizedMessage}", isError = true) }
      _isBusy.value = false
    }
  }

  class Factory(private val app: KamarTajApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
      return StudioViewModel(app, app.repository) as T
    }
  }
}
