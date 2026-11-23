package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.BrandMeta
import com.example.data.model.CostLineDto
import com.example.data.model.MaterialItem
import com.example.data.model.OrderRecord
import com.example.data.model.Product
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "items")
data class ItemEntity(
  @PrimaryKey val id: String,
  val cat: String,
  val name: String,
  val price: Double,
  val qty: Double,
  val unit: String,
  val img: String? = null,
  val stock: Double? = null,
  val low: Double? = null,
  val updatedAt: Long = System.currentTimeMillis(),
) {
  fun toDomain(): MaterialItem =
    MaterialItem(
      id = id,
      cat = cat,
      name = name,
      price = price,
      qty = qty,
      unit = unit,
      img = img,
      stock = stock,
      low = low,
      updatedAt = updatedAt,
    )

  companion object {
    fun fromDomain(item: MaterialItem): ItemEntity =
      ItemEntity(
        id = item.id,
        cat = item.cat,
        name = item.name,
        price = item.price,
        qty = item.qty,
        unit = item.unit,
        img = item.img,
        stock = item.stock,
        low = item.low,
        updatedAt = item.updatedAt,
      )
  }
}

@Entity(tableName = "prods")
data class ProductEntity(
  @PrimaryKey val id: String,
  val name: String,
  val sku: String,
  val cat: String,
  val imgs: List<String>,
  val profit: Double,
  val fee: Double,
  val wp: Double,
  val notes: String,
  val lines: List<CostLineDto>,
  val updatedAt: Long = System.currentTimeMillis(),
) {
  fun toDomain(): Product =
    Product(
      id = id,
      name = name,
      sku = sku,
      cat = cat,
      imgs = imgs,
      profit = profit,
      fee = fee,
      wp = wp,
      notes = notes,
      lines = lines,
      updatedAt = updatedAt,
    )

  companion object {
    fun fromDomain(product: Product): ProductEntity =
      ProductEntity(
        id = product.id,
        name = product.name,
        sku = product.sku,
        cat = product.cat,
        imgs = product.imgs,
        profit = product.profit,
        fee = product.fee,
        wp = product.wp,
        notes = product.notes,
        lines = product.lines,
        updatedAt = product.updatedAt,
      )
  }
}

@Entity(tableName = "orders")
data class OrderEntity(
  @PrimaryKey val id: String,
  val cust: String,
  val phone: String,
  val pid: String,
  val pname: String,
  val sku: String,
  val qty: Int,
  val price: Double,
  val cost: Double,
  val advance: Double,
  val date: String,
  val status: String,
  val notes: String,
  val used: Map<String, Double>,
  val restored: Boolean,
  val createdAt: Long = System.currentTimeMillis(),
) {
  fun toDomain(): OrderRecord =
    OrderRecord(
      id = id,
      cust = cust,
      phone = phone,
      pid = pid,
      pname = pname,
      sku = sku,
      qty = qty,
      price = price,
      cost = cost,
      advance = advance,
      date = date,
      status = status,
      notes = notes,
      used = used,
      restored = restored,
      createdAt = createdAt,
    )

  companion object {
    fun fromDomain(order: OrderRecord): OrderEntity =
      OrderEntity(
        id = order.id,
        cust = order.cust,
        phone = order.phone,
        pid = order.pid,
        pname = order.pname,
        sku = order.sku,
        qty = order.qty,
        price = order.price,
        cost = order.cost,
        advance = order.advance,
        date = order.date,
        status = order.status,
        notes = order.notes,
        used = order.used,
        restored = order.restored,
        createdAt = order.createdAt,
      )
  }
}

@Entity(tableName = "meta")
data class MetaEntity(
  @PrimaryKey val id: String = "brand",
  val name: String = "Kamar Taj",
  val logo: String? = null,
  val contact: String = "",
  val color: String = "#82091c",
  val mode: String = "auto",
  val customCats: List<String> = emptyList(),
) {
  fun toDomain(): BrandMeta =
    BrandMeta(
      id = id,
      name = name,
      logo = logo,
      contact = contact,
      color = color,
      mode = mode,
      customCats = customCats,
    )

  companion object {
    fun fromDomain(meta: BrandMeta): MetaEntity =
      MetaEntity(
        id = meta.id,
        name = meta.name,
        logo = meta.logo,
        contact = meta.contact,
        color = meta.color,
        mode = meta.mode,
        customCats = meta.customCats,
      )
  }
}

class StudioTypeConverters {
  private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

  private val stringListAdapter =
    moshi.adapter<List<String>>(
      Types.newParameterizedType(List::class.java, String::class.java)
    )

  private val costLineListAdapter =
    moshi.adapter<List<CostLineDto>>(
      Types.newParameterizedType(List::class.java, CostLineDto::class.java)
    )

  private val usedMapAdapter =
    moshi.adapter<Map<String, Double>>(
      Types.newParameterizedType(Map::class.java, String::class.java, Double::class.javaObjectType)
    )

  @TypeConverter
  fun fromStringList(value: List<String>?): String = stringListAdapter.toJson(value ?: emptyList())

  @TypeConverter
  fun toStringList(json: String?): List<String> =
    if (json.isNullOrBlank()) emptyList() else stringListAdapter.fromJson(json) ?: emptyList()

  @TypeConverter
  fun fromCostLineList(value: List<CostLineDto>?): String =
    costLineListAdapter.toJson(value ?: emptyList())

  @TypeConverter
  fun toCostLineList(json: String?): List<CostLineDto> =
    if (json.isNullOrBlank()) emptyList() else costLineListAdapter.fromJson(json) ?: emptyList()

  @TypeConverter
  fun fromUsedMap(value: Map<String, Double>?): String = usedMapAdapter.toJson(value ?: emptyMap())

  @TypeConverter
  fun toUsedMap(json: String?): Map<String, Double> =
    if (json.isNullOrBlank()) emptyMap() else usedMapAdapter.fromJson(json) ?: emptyMap()
}

@Dao
interface StudioDao {
  @Query("SELECT * FROM items ORDER BY updatedAt DESC")
  fun observeItems(): Flow<List<ItemEntity>>

  @Query("SELECT * FROM items")
  suspend fun getAllItemsOnce(): List<ItemEntity>

  @Query("SELECT * FROM items WHERE id = :id LIMIT 1")
  suspend fun getItemById(id: String): ItemEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertItem(item: ItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertItems(items: List<ItemEntity>)

  @Query("DELETE FROM items WHERE id = :id")
  suspend fun deleteItemById(id: String)

  @Query("DELETE FROM items")
  suspend fun clearItems()

  @Query("SELECT * FROM prods ORDER BY updatedAt DESC")
  fun observeProducts(): Flow<List<ProductEntity>>

  @Query("SELECT * FROM prods")
  suspend fun getAllProductsOnce(): List<ProductEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProduct(product: ProductEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProducts(products: List<ProductEntity>)

  @Query("DELETE FROM prods WHERE id = :id")
  suspend fun deleteProductById(id: String)

  @Query("DELETE FROM prods")
  suspend fun clearProducts()

  @Query("SELECT * FROM orders ORDER BY createdAt DESC")
  fun observeOrders(): Flow<List<OrderEntity>>

  @Query("SELECT * FROM orders")
  suspend fun getAllOrdersOnce(): List<OrderEntity>

  @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
  suspend fun getOrderById(id: String): OrderEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertOrder(order: OrderEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertOrders(orders: List<OrderEntity>)

  @Query("DELETE FROM orders WHERE id = :id")
  suspend fun deleteOrderById(id: String)

  @Query("DELETE FROM orders")
  suspend fun clearOrders()

  @Query("SELECT * FROM meta WHERE id = 'brand' LIMIT 1")
  fun observeBrandMeta(): Flow<MetaEntity?>

  @Query("SELECT * FROM meta WHERE id = 'brand' LIMIT 1")
  suspend fun getBrandMetaOnce(): MetaEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBrandMeta(meta: MetaEntity)
}

@Database(
  entities = [ItemEntity::class, ProductEntity::class, OrderEntity::class, MetaEntity::class],
  version = 1,
  exportSchema = false,
)
@TypeConverters(StudioTypeConverters::class)
abstract class StudioDatabase : RoomDatabase() {
  abstract fun studioDao(): StudioDao

  companion object {
    @Volatile private var INSTANCE: StudioDatabase? = null

    fun getInstance(context: Context): StudioDatabase {
      return INSTANCE
        ?: synchronized(this) {
          val instance =
            Room.databaseBuilder(
                context.applicationContext,
                StudioDatabase::class.java,
                "kamar_taj_studio.db",
              )
              .build()
          INSTANCE = instance
          instance
        }
    }
  }
}
