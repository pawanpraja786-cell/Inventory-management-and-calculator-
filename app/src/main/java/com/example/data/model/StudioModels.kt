package com.example.data.model

import com.squareup.moshi.JsonClass
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.max

@JsonClass(generateAdapter = true)
data class MaterialItem(
  val id: String,
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
  val unitCost: Double
    get() = if (qty > 0.0) price / qty else 0.0

  val isLowStock: Boolean
    get() = stock != null && low != null && stock <= low
}

@JsonClass(generateAdapter = true)
data class CostLineDto(
  val type: String, // "item", "vol", "fix"
  val id: String? = null,
  val q: Double? = null,
  val shape: String? = null, // "r" or "c"
  val a: Double? = null,
  val b: Double? = null,
  val u: String? = null, // "in" or "cm"
  val th: Double? = null, // thickness in mm
  val waste: Double? = null,
  val dens: Double? = null,
  val label: String? = null,
  val amt: Double? = null,
)

sealed class CostLine {
  data class ItemLine(val id: String, val q: Double) : CostLine()

  data class VolLine(
    val id: String,
    val shape: String = "r", // "r" rectangle, "c" round
    val a: Double = 4.0,
    val b: Double = 4.0,
    val u: String = "in", // "in" or "cm"
    val th: Double = 5.0, // mm
    val waste: Double = 10.0, // %
    val dens: Double = 1.1, // g/cm3
  ) : CostLine() {
    fun areaCm2(): Double {
      val factor = if (u == "in") 2.54 else 1.0
      val aCm = a * factor
      val bCm = b * factor
      return if (shape == "c") {
        val radius = aCm / 2.0
        PI * radius * radius
      } else {
        aCm * bCm
      }
    }

    fun grams(): Double {
      val thicknessCm = th / 10.0
      return areaCm2() * thicknessCm * dens * (1.0 + waste / 100.0)
    }
  }

  data class FixLine(val label: String, val amt: Double) : CostLine()

  fun toDto(): CostLineDto =
    when (this) {
      is ItemLine -> CostLineDto(type = "item", id = id, q = q)
      is VolLine ->
        CostLineDto(
          type = "vol",
          id = id,
          shape = shape,
          a = a,
          b = b,
          u = u,
          th = th,
          waste = waste,
          dens = dens,
        )
      is FixLine -> CostLineDto(type = "fix", label = label, amt = amt)
    }

  companion object {
    fun fromDto(dto: CostLineDto): CostLine? =
      when (dto.type) {
        "item" -> dto.id?.let { ItemLine(id = it, q = dto.q ?: 0.0) }
        "vol" ->
          dto.id?.let {
            VolLine(
              id = it,
              shape = dto.shape ?: "r",
              a = dto.a ?: 0.0,
              b = dto.b ?: 0.0,
              u = dto.u ?: "in",
              th = dto.th ?: 5.0,
              waste = dto.waste ?: 10.0,
              dens = dto.dens ?: 1.1,
            )
          }
        "fix" -> FixLine(label = dto.label ?: "Charge", amt = dto.amt ?: 0.0)
        else -> null
      }
  }
}

@JsonClass(generateAdapter = true)
data class Product(
  val id: String,
  val name: String,
  val sku: String,
  val cat: String,
  val imgs: List<String> = emptyList(),
  val profit: Double = 50.0,
  val fee: Double = 0.0,
  val wp: Double = 25.0,
  val notes: String = "",
  val lines: List<CostLineDto> = emptyList(),
  val updatedAt: Long = System.currentTimeMillis(),
)

data class LineBreakdown(
  val title: String,
  val subtitle: String,
  val cost: Double,
  val missingItem: Boolean = false,
)

data class ComputedPrice(
  val cost: Double,
  val retail: Double,
  val wholesale: Double,
  val netProfit: Double,
  val feeAmount: Double,
  val breakdowns: List<LineBreakdown>,
)

enum class OrderStatus(val label: String) {
  PENDING("Pending"),
  IN_PROGRESS("Ban raha hai"),
  READY("Ready"),
  DELIVERED("Delivered"),
  CANCELLED("Cancelled");

  companion object {
    fun fromLabel(label: String): OrderStatus =
      entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: PENDING
  }
}

@JsonClass(generateAdapter = true)
data class OrderRecord(
  val id: String,
  val cust: String,
  val phone: String = "",
  val pid: String = "__c",
  val pname: String,
  val sku: String = "",
  val qty: Int = 1,
  val price: Double = 0.0,
  val cost: Double = 0.0,
  val advance: Double = 0.0,
  val date: String = "",
  val status: String = OrderStatus.PENDING.label,
  val notes: String = "",
  val used: Map<String, Double> = emptyMap(),
  val restored: Boolean = false,
  val createdAt: Long = System.currentTimeMillis(),
) {
  val balanceDue: Double
    get() = max(0.0, price - advance)
}

@JsonClass(generateAdapter = true)
data class BrandMeta(
  val id: String = "brand",
  val name: String = "Kamar Taj",
  val logo: String? = null,
  val contact: String = "",
  val color: String = "#82091c",
  val mode: String = "auto", // "auto", "light", "dark"
  val customCats: List<String> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class BackupPayload(
  val version: Int = 1,
  val exportedAt: Long = System.currentTimeMillis(),
  val items: List<MaterialItem> = emptyList(),
  val prods: List<Product> = emptyList(),
  val orders: List<OrderRecord> = emptyList(),
  val meta: BrandMeta = BrandMeta(),
  val embeddedImages: Map<String, String> = emptyMap(),
)

object StudioConstants {
  val DEFAULT_MATERIAL_CATEGORIES =
    listOf("Resin", "Mold", "Pigment", "Stone", "Flower", "Stand", "Other")
  val DEFAULT_UNITS = listOf("gm", "pc", "use", "ml", "set")
  val DEFAULT_PRODUCT_TAGS =
    listOf("Coaster", "Clock", "Tray", "Keychain", "Frame", "Jewelry", "Nameplate", "Gift")
  val FIXED_CHARGE_PRESETS = listOf("Labour", "Finishing", "Packaging", "Shipping", "Coating")
  val THEME_COLOR_PRESETS =
    listOf(
      "#82091c" to "Kamar Taj Maroon",
      "#0d6e6e" to "Royal Teal",
      "#b45309" to "Saffron Gold",
      "#3730a3" to "Indigo Ink",
      "#166534" to "Forest Emerald",
      "#701a75" to "Plum Wine",
      "#9a3412" to "Terracotta",
      "#334155" to "Slate Charcoal",
    )
}

object PricingEngine {
  private val inrFormat: NumberFormat =
    NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 0
      minimumFractionDigits = 0
    }

  private val decimalInrFormat: NumberFormat =
    NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
      maximumFractionDigits = 2
      minimumFractionDigits = 0
    }

  fun formatRupees(amount: Double, decimals: Boolean = false): String {
    val safe = if (amount.isNaN() || amount.isInfinite()) 0.0 else amount
    return if (decimals) "₹${decimalInrFormat.format(safe)}" else "₹${inrFormat.format(safe)}"
  }

  fun formatQty(q: Double): String {
    return if (q % 1.0 == 0.0) q.toLong().toString()
    else String.format(Locale.US, "%.2f", q).trimEnd('0').trimEnd('.')
  }

  fun todayIso(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

  fun currentMonthKey(): String = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

  fun monthDisplay(monthKey: String): String {
    return try {
      val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(monthKey)
      if (parsed != null) SimpleDateFormat("MMM yyyy", Locale("en", "IN")).format(parsed)
      else monthKey
    } catch (_: Exception) {
      monthKey
    }
  }

  fun computeLine(
    line: CostLine,
    itemsById: Map<String, MaterialItem>,
  ): LineBreakdown {
    return when (line) {
      is CostLine.ItemLine -> {
        val mat = itemsById[line.id]
        if (mat == null) {
          LineBreakdown(
            title = "Missing Material",
            subtitle = "Qty: ${formatQty(line.q)}",
            cost = 0.0,
            missingItem = true,
          )
        } else {
          val c = mat.unitCost * line.q
          LineBreakdown(
            title = mat.name,
            subtitle = "${formatQty(line.q)} ${mat.unit} × ${formatRupees(mat.unitCost, true)}",
            cost = c,
          )
        }
      }
      is CostLine.VolLine -> {
        val mat = itemsById[line.id]
        val grams = line.grams()
        val dimLabel =
          if (line.shape == "c") {
            "Round ⌀${formatQty(line.a)}${line.u}"
          } else {
            "${formatQty(line.a)}×${formatQty(line.b)}${line.u}"
          }
        if (mat == null) {
          LineBreakdown(
            title = "Resin Volume (Missing Item)",
            subtitle = "$dimLabel • ${formatQty(line.th)}mm → ${formatQty(grams)} gm",
            cost = 0.0,
            missingItem = true,
          )
        } else {
          val c = grams * mat.unitCost
          LineBreakdown(
            title = "${mat.name} (Size se)",
            subtitle = "$dimLabel, ${formatQty(line.th)}mm → ${formatQty(grams)} gm",
            cost = c,
          )
        }
      }
      is CostLine.FixLine -> {
        LineBreakdown(
          title = line.label.ifBlank { "Fixed Charge" },
          subtitle = "Fixed charge",
          cost = line.amt,
        )
      }
    }
  }

  fun computeProductPrice(
    lines: List<CostLine>,
    profitPercent: Double,
    feePercent: Double,
    wholesalePercent: Double,
    itemsById: Map<String, MaterialItem>,
  ): ComputedPrice {
    val breakdowns = lines.map { computeLine(it, itemsById) }
    val cost = breakdowns.sumOf { it.cost }
    val safeFee = feePercent.coerceIn(0.0, 95.0)
    val feeFactor = 1.0 - (safeFee / 100.0)
    val retail = if (feeFactor > 0.0) cost * (1.0 + profitPercent / 100.0) / feeFactor else cost
    val wholesale =
      if (feeFactor > 0.0) cost * (1.0 + wholesalePercent / 100.0) / feeFactor else cost
    val feeAmount = retail * (safeFee / 100.0)
    val netProfit = retail - feeAmount - cost
    return ComputedPrice(
      cost = cost,
      retail = retail,
      wholesale = wholesale,
      netProfit = netProfit,
      feeAmount = feeAmount,
      breakdowns = breakdowns,
    )
  }

  fun computeProductPrice(
    product: Product,
    itemsById: Map<String, MaterialItem>,
  ): ComputedPrice {
    val parsedLines = product.lines.mapNotNull { CostLine.fromDto(it) }
    return computeProductPrice(
      lines = parsedLines,
      profitPercent = product.profit,
      feePercent = product.fee,
      wholesalePercent = product.wp,
      itemsById = itemsById,
    )
  }

  fun computeUsedMaterialsForOrder(
    product: Product,
    orderQty: Int,
  ): Map<String, Double> {
    if (orderQty <= 0) return emptyMap()
    val map = mutableMapOf<String, Double>()
    product.lines.mapNotNull { CostLine.fromDto(it) }.forEach { line ->
      when (line) {
        is CostLine.ItemLine -> {
          map[line.id] = (map[line.id] ?: 0.0) + (line.q * orderQty)
        }
        is CostLine.VolLine -> {
          map[line.id] = (map[line.id] ?: 0.0) + (line.grams() * orderQty)
        }
        is CostLine.FixLine -> Unit
      }
    }
    return map
  }
}
