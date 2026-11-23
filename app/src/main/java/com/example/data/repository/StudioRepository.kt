package com.example.data.repository

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.example.data.local.ItemEntity
import com.example.data.local.MetaEntity
import com.example.data.local.OrderEntity
import com.example.data.local.ProductEntity
import com.example.data.local.StudioDatabase
import com.example.data.model.BackupPayload
import com.example.data.model.BrandMeta
import com.example.data.model.CostLine
import com.example.data.model.MaterialItem
import com.example.data.model.OrderRecord
import com.example.data.model.OrderStatus
import com.example.data.model.Product
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.max

class StudioRepository(
  private val context: Context,
  private val database: StudioDatabase,
) {
  private val dao = database.studioDao()
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
  private val backupAdapter = moshi.adapter(BackupPayload::class.java).indent("  ")

  val items: Flow<List<MaterialItem>> =
    dao.observeItems().map { list -> list.map { it.toDomain() } }

  val products: Flow<List<Product>> =
    dao.observeProducts().map { list -> list.map { it.toDomain() } }

  val orders: Flow<List<OrderRecord>> =
    dao.observeOrders().map { list -> list.map { it.toDomain() } }

  val brandMeta: Flow<BrandMeta> =
    dao.observeBrandMeta().map { it?.toDomain() ?: BrandMeta() }

  suspend fun ensureInitialMeta() {
    if (dao.getBrandMetaOnce() == null) {
      dao.upsertBrandMeta(MetaEntity.fromDomain(BrandMeta()))
    }
  }

  suspend fun upsertMaterial(item: MaterialItem) {
    dao.upsertItem(ItemEntity.fromDomain(item.copy(updatedAt = System.currentTimeMillis())))
  }

  suspend fun adjustMaterialStock(itemId: String, delta: Double) {
    val existing = dao.getItemById(itemId) ?: return
    val current = existing.stock ?: 0.0
    dao.upsertItem(
      existing.copy(
        stock = max(0.0, current + delta),
        updatedAt = System.currentTimeMillis(),
      )
    )
  }

  suspend fun deleteMaterial(id: String) {
    dao.deleteItemById(id)
  }

  suspend fun upsertProduct(product: Product) {
    dao.upsertProduct(ProductEntity.fromDomain(product.copy(updatedAt = System.currentTimeMillis())))
  }

  suspend fun deleteProduct(id: String) {
    dao.deleteProductById(id)
  }

  suspend fun saveBrandMeta(meta: BrandMeta) {
    dao.upsertBrandMeta(MetaEntity.fromDomain(meta))
  }

  suspend fun addCustomCategory(category: String) {
    val clean = category.trim()
    if (clean.isEmpty()) return
    val current = dao.getBrandMetaOnce()?.toDomain() ?: BrandMeta()
    if (current.customCats.none { it.equals(clean, ignoreCase = true) }) {
      dao.upsertBrandMeta(
        MetaEntity.fromDomain(current.copy(customCats = current.customCats + clean))
      )
    }
  }

  /**
   * Creates or updates an order while accurately keeping material stock synchronized:
   * - Active (non-Cancelled) orders deduct `used` quantities when created or un-cancelled.
   * - Cancelled orders restore `used` quantities once (`restored = true`).
   */
  suspend fun saveOrderWithStockSync(newOrder: OrderRecord) {
    database.withTransaction {
      val existing = dao.getOrderById(newOrder.id)?.toDomain()
      val isNewCancelled = newOrder.status == OrderStatus.CANCELLED.label

      if (existing == null) {
        if (!isNewCancelled) {
          applyStockDelta(newOrder.used, deduct = true)
        }
        dao.upsertOrder(
          OrderEntity.fromDomain(newOrder.copy(restored = isNewCancelled))
        )
      } else {
        // Restore previous order's deducted stock if it had not been restored yet
        if (!existing.restored) {
          applyStockDelta(existing.used, deduct = false)
        }
        // Deduct new order's stock if the new status is active
        if (!isNewCancelled) {
          applyStockDelta(newOrder.used, deduct = true)
        }
        dao.upsertOrder(
          OrderEntity.fromDomain(newOrder.copy(restored = isNewCancelled))
        )
      }
    }
  }

  suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
    database.withTransaction {
      val existing = dao.getOrderById(orderId)?.toDomain() ?: return@withTransaction
      if (existing.status == newStatus.label) return@withTransaction
      val becomingCancelled = newStatus == OrderStatus.CANCELLED
      var restored = existing.restored
      if (becomingCancelled && !existing.restored) {
        applyStockDelta(existing.used, deduct = false)
        restored = true
      } else if (!becomingCancelled && existing.restored) {
        applyStockDelta(existing.used, deduct = true)
        restored = false
      }
      dao.upsertOrder(
        OrderEntity.fromDomain(existing.copy(status = newStatus.label, restored = restored))
      )
    }
  }

  suspend fun deleteOrderWithStockRestore(orderId: String) {
    database.withTransaction {
      val existing = dao.getOrderById(orderId)?.toDomain() ?: return@withTransaction
      if (!existing.restored) {
        applyStockDelta(existing.used, deduct = false)
      }
      dao.deleteOrderById(orderId)
    }
  }

  private suspend fun applyStockDelta(used: Map<String, Double>, deduct: Boolean) {
    used.forEach { (itemId, qty) ->
      if (qty > 0.0) {
        val item = dao.getItemById(itemId) ?: return@forEach
        val currentStock = item.stock
        if (currentStock != null) {
          val updated = if (deduct) max(0.0, currentStock - qty) else currentStock + qty
          dao.upsertItem(item.copy(stock = updated, updatedAt = System.currentTimeMillis()))
        }
      }
    }
  }

  suspend fun insertExampleStudioData() {
    database.withTransaction {
      val now = System.currentTimeMillis()
      val resinId = "mat_epoxy_resin"
      val moldCoasterId = "mat_coaster_mold"
      val pigmentGoldId = "mat_gold_mica"
      val flowerRoseId = "mat_rose_petals"
      val stoneCrystalId = "mat_quartz_chips"
      val standWoodId = "mat_easel_stand"
      val boxOtherId = "mat_gift_box"

      val exampleItems =
        listOf(
          MaterialItem(
            id = resinId,
            cat = "Resin",
            name = "Crystal Clear Epoxy Resin (2:1)",
            price = 950.0,
            qty = 1000.0,
            unit = "gm",
            stock = 2400.0,
            low = 500.0,
            updatedAt = now,
          ),
          MaterialItem(
            id = moldCoasterId,
            cat = "Mold",
            name = "Geode Wave Coaster Silicone Mold",
            price = 360.0,
            qty = 20.0,
            unit = "use",
            stock = 18.0,
            low = 4.0,
            updatedAt = now - 100,
          ),
          MaterialItem(
            id = pigmentGoldId,
            cat = "Pigment",
            name = "24K Gold Foil Flakes & Mica",
            price = 280.0,
            qty = 50.0,
            unit = "gm",
            stock = 42.0,
            low = 10.0,
            updatedAt = now - 200,
          ),
          MaterialItem(
            id = flowerRoseId,
            cat = "Flower",
            name = "Pressed Dried Crimson Rose Petals",
            price = 220.0,
            qty = 40.0,
            unit = "pc",
            stock = 35.0,
            low = 8.0,
            updatedAt = now - 300,
          ),
          MaterialItem(
            id = stoneCrystalId,
            cat = "Stone",
            name = "Crushed White Quartz Crystals",
            price = 180.0,
            qty = 200.0,
            unit = "gm",
            stock = 160.0,
            low = 40.0,
            updatedAt = now - 400,
          ),
          MaterialItem(
            id = standWoodId,
            cat = "Stand",
            name = "Brass Coaster Display Holder",
            price = 480.0,
            qty = 6.0,
            unit = "pc",
            stock = 5.0,
            low = 2.0,
            updatedAt = now - 500,
          ),
          MaterialItem(
            id = boxOtherId,
            cat = "Other",
            name = "Kamar Taj Kraft Gift Box + Foam",
            price = 450.0,
            qty = 10.0,
            unit = "pc",
            stock = 9.0,
            low = 3.0,
            updatedAt = now - 600,
          ),
        )
      dao.upsertItems(exampleItems.map { ItemEntity.fromDomain(it) })

      val existingProds = dao.getAllProductsOnce()
      if (existingProds.isEmpty()) {
        val sampleProd =
          Product(
            id = "prod_royal_geode_coaster",
            name = "Royal Crimson & Gold Geode Coaster",
            sku = "KT-CST-01",
            cat = "Coaster",
            imgs = emptyList(),
            profit = 60.0,
            fee = 0.0,
            wp = 30.0,
            notes = "Hand-poured glossy epoxy coaster with crimson petals & gold edge.",
            lines =
              listOf(
                CostLine.VolLine(
                    id = resinId,
                    shape = "c",
                    a = 4.0,
                    b = 4.0,
                    u = "in",
                    th = 6.0,
                    waste = 10.0,
                    dens = 1.1,
                  )
                  .toDto(),
                CostLine.ItemLine(id = moldCoasterId, q = 1.0).toDto(),
                CostLine.ItemLine(id = pigmentGoldId, q = 2.0).toDto(),
                CostLine.ItemLine(id = flowerRoseId, q = 3.0).toDto(),
                CostLine.ItemLine(id = stoneCrystalId, q = 12.0).toDto(),
                CostLine.FixLine(label = "Labour & Finishing", amt = 75.0).toDto(),
                CostLine.FixLine(label = "Packaging", amt = 35.0).toDto(),
              ),
            updatedAt = now,
          )
        dao.upsertProduct(ProductEntity.fromDomain(sampleProd))
      }
    }
  }

  suspend fun exportBackupJson(): String =
    withContext(Dispatchers.IO) {
      val items = dao.getAllItemsOnce().map { it.toDomain() }
      val prods = dao.getAllProductsOnce().map { it.toDomain() }
      val orders = dao.getAllOrdersOnce().map { it.toDomain() }
      val meta = dao.getBrandMetaOnce()?.toDomain() ?: BrandMeta()

      val imageMap = mutableMapOf<String, String>()
      val paths = buildList {
        items.forEach { it.img?.let(::add) }
        prods.forEach { addAll(it.imgs) }
        meta.logo?.let(::add)
      }
      paths.distinct().forEach { path ->
        val f = File(path)
        if (f.exists() && f.isFile && f.length() < 2_500_000L) {
          runCatching {
            val encoded = Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
            imageMap[path] = encoded
          }
        }
      }

      val payload =
        BackupPayload(
          version = 1,
          exportedAt = System.currentTimeMillis(),
          items = items,
          prods = prods,
          orders = orders,
          meta = meta,
          embeddedImages = imageMap,
        )
      backupAdapter.toJson(payload)
    }

  suspend fun importBackupJson(json: String): Result<String> =
    withContext(Dispatchers.IO) {
      runCatching {
        val payload =
          backupAdapter.fromJson(json) ?: error("Invalid backup JSON file.")

        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val remappedPaths = mutableMapOf<String, String>()
        payload.embeddedImages.forEach { (oldPath, b64) ->
          runCatching {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val ext = if (oldPath.endsWith(".png", ignoreCase = true)) "png" else "jpg"
            val newFile = File(mediaDir, "restored_${UUID.randomUUID()}.$ext")
            newFile.writeBytes(bytes)
            remappedPaths[oldPath] = newFile.absolutePath
          }
        }

        val restoredItems =
          payload.items.map { item ->
            item.copy(img = item.img?.let { remappedPaths[it] ?: it })
          }
        val restoredProds =
          payload.prods.map { prod ->
            prod.copy(imgs = prod.imgs.map { remappedPaths[it] ?: it })
          }
        val restoredMeta =
          payload.meta.copy(
            id = "brand",
            logo = payload.meta.logo?.let { remappedPaths[it] ?: it },
          )

        database.withTransaction {
          dao.clearItems()
          dao.clearProducts()
          dao.clearOrders()
          dao.upsertItems(restoredItems.map { ItemEntity.fromDomain(it) })
          dao.upsertProducts(restoredProds.map { ProductEntity.fromDomain(it) })
          dao.upsertOrders(payload.orders.map { OrderEntity.fromDomain(it) })
          dao.upsertBrandMeta(MetaEntity.fromDomain(restoredMeta))
        }

        "${restoredItems.size} materials, ${restoredProds.size} products, ${payload.orders.size} orders restored!"
      }
    }
}
