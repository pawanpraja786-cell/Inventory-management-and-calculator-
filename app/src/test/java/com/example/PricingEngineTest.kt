package com.example

import com.example.data.model.CostLine
import com.example.data.model.MaterialItem
import com.example.data.model.PricingEngine
import com.example.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PricingEngineTest {

  @Test
  fun volumeLine_calculatesResinGramsAndCostAccurately() {
    val resin = MaterialItem(id = "r1", cat = "Resin", name = "Epoxy", price = 1000.0, qty = 1000.0, unit = "gm")
    val vol =
      CostLine.VolLine(
        id = "r1",
        shape = "r",
        a = 10.0,
        b = 10.0,
        u = "cm",
        th = 10.0, // 1.0 cm
        waste = 10.0,
        dens = 1.1,
      )
    // area = 100 cm2, thickness = 1 cm, dens = 1.1, waste = 10% => 100 * 1 * 1.1 * 1.1 = 121 gm
    assertEquals(121.0, vol.grams(), 0.01)
    val computed = PricingEngine.computeProductPrice(listOf(vol), 50.0, 0.0, 25.0, mapOf("r1" to resin))
    assertEquals(121.0, computed.cost, 0.01)
    assertEquals(181.5, computed.retail, 0.01)
    assertEquals(151.25, computed.wholesale, 0.01)
  }

  @Test
  fun materialPriceUpdate_automaticallyUpdatesSavedProductPrices() {
    val resinOld = MaterialItem(id = "r1", cat = "Resin", name = "Epoxy", price = 800.0, qty = 1000.0, unit = "gm")
    val resinNew = resinOld.copy(price = 1200.0)
    val product =
      Product(
        id = "p1",
        name = "Coaster",
        sku = "KT-1",
        cat = "Coaster",
        profit = 50.0,
        fee = 10.0,
        wp = 20.0,
        lines = listOf(CostLine.ItemLine("r1", 100.0).toDto(), CostLine.FixLine("Labour", 20.0).toDto()),
      )

    val before = PricingEngine.computeProductPrice(product, mapOf("r1" to resinOld))
    val after = PricingEngine.computeProductPrice(product, mapOf("r1" to resinNew))

    // Old: 100*0.8 + 20 = 100; retail = 100 * 1.5 / 0.9 = 166.67
    assertEquals(100.0, before.cost, 0.01)
    assertEquals(166.67, before.retail, 0.05)

    // New: 100*1.2 + 20 = 140; retail = 140 * 1.5 / 0.9 = 233.33
    assertEquals(140.0, after.cost, 0.01)
    assertEquals(233.33, after.retail, 0.05)
    assertTrue(after.retail > before.retail)
  }

  @Test
  fun orderUsedMaterials_combinesItemAndVolumeLinesMultipliedByQty() {
    val product =
      Product(
        id = "p1",
        name = "Clock",
        sku = "KT-2",
        cat = "Clock",
        lines =
          listOf(
            CostLine.VolLine(id = "r1", shape = "c", a = 10.0, u = "cm", th = 10.0, waste = 0.0, dens = 1.0)
              .toDto(),
            CostLine.ItemLine(id = "m1", q = 2.0).toDto(),
          ),
      )

    val used = PricingEngine.computeUsedMaterialsForOrder(product, orderQty = 3)
    assertEquals(6.0, used["m1"] ?: 0.0, 0.01)
    // Round diameter 10cm => r=5cm => area = pi*25 = 78.5398 * 1cm * 1.0 * 3 = 235.62 gm
    assertEquals(235.62, used["r1"] ?: 0.0, 0.1)
  }
}
