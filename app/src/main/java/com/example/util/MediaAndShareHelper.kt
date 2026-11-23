package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.BrandMeta
import com.example.data.model.ComputedPrice
import com.example.data.model.OrderRecord
import com.example.data.model.PricingEngine
import com.example.data.model.Product
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object MediaAndShareHelper {

  suspend fun copyUriToInternalMedia(
    context: Context,
    uri: Uri,
    prefix: String = "img",
  ): String? =
    withContext(Dispatchers.IO) {
      runCatching {
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
        val original = input.use { BitmapFactory.decodeStream(it) } ?: return@withContext null
        val scaled = scaleBitmapDown(original, 1200)
        val outFile = File(mediaDir, "${prefix}_${UUID.randomUUID()}.jpg")
        FileOutputStream(outFile).use { fos ->
          scaled.compress(Bitmap.CompressFormat.JPEG, 86, fos)
        }
        if (scaled !== original) scaled.recycle()
        original.recycle()
        outFile.absolutePath
      }.getOrNull()
    }

  private fun scaleBitmapDown(bitmap: Bitmap, maxDim: Int): Bitmap {
    val w = bitmap.width
    val h = bitmap.height
    if (w <= maxDim && h <= maxDim) return bitmap
    val ratio = min(maxDim.toFloat() / w, maxDim.toFloat() / h)
    val nw = max(1, (w * ratio).toInt())
    val nh = max(1, (h * ratio).toInt())
    return Bitmap.createScaledBitmap(bitmap, nw, nh, true)
  }

  private fun loadSafeBitmap(path: String?, maxDim: Int = 1000): Bitmap? {
    if (path.isNullOrBlank()) return null
    val file = File(path)
    if (!file.exists()) return null
    return runCatching {
      val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFile(path, opts)
      var sample = 1
      while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
        sample *= 2
      }
      val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
      BitmapFactory.decodeFile(path, decodeOpts)
    }.getOrNull()
  }

  private fun parseThemeColor(hex: String): Int {
    return runCatching { Color.parseColor(hex) }.getOrElse { Color.parseColor("#82091c") }
  }

  private fun drawKamarTajDefaultMark(canvas: Canvas, bounds: RectF, accentColor: Int) {
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
    canvas.drawRoundRect(bounds, bounds.width() * 0.2f, bounds.height() * 0.2f, bgPaint)

    val w = bounds.width()
    val h = bounds.height()
    val ox = bounds.left
    val oy = bounds.top

    fun px(v: Float) = ox + (v / 108f) * w
    fun py(v: Float) = oy + (v / 108f) * h

    val archPath =
      Path().apply {
        moveTo(px(30f), py(82f))
        lineTo(px(30f), py(48f))
        cubicTo(px(30f), py(33f), px(42f), py(25f), px(54f), py(21f))
        cubicTo(px(66f), py(25f), px(78f), py(33f), px(78f), py(48f))
        lineTo(px(78f), py(82f))
        close()
      }
    val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    canvas.drawPath(archPath, whitePaint)

    val kPath =
      Path().apply {
        moveTo(px(42f), py(43f))
        lineTo(px(48f), py(43f))
        lineTo(px(48f), py(54f))
        lineTo(px(60f), py(43f))
        lineTo(px(67f), py(43f))
        lineTo(px(54.5f), py(55.5f))
        lineTo(px(67.5f), py(71f))
        lineTo(px(60f), py(71f))
        lineTo(px(48f), py(57f))
        lineTo(px(48f), py(71f))
        lineTo(px(42f), py(71f))
        close()
      }
    val kPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentColor }
    canvas.drawPath(kPath, kPaint)
  }

  private fun drawCenterCroppedBitmap(canvas: Canvas, bmp: Bitmap, dst: RectF, radius: Float) {
    canvas.save()
    val clipPath = Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) }
    canvas.clipPath(clipPath)
    val srcRatio = bmp.width.toFloat() / bmp.height.toFloat()
    val dstRatio = dst.width() / dst.height()
    val srcRect: Rect =
      if (srcRatio > dstRatio) {
        val newW = (bmp.height * dstRatio).toInt()
        val left = (bmp.width - newW) / 2
        Rect(left, 0, left + newW, bmp.height)
      } else {
        val newH = (bmp.width / dstRatio).toInt()
        val top = (bmp.height - newH) / 2
        Rect(0, top, bmp.width, top + newH)
      }
    canvas.drawBitmap(bmp, srcRect, dst, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    canvas.restore()
  }

  private fun drawTextWrapped(
    canvas: Canvas,
    text: String,
    x: Float,
    y: Float,
    width: Int,
    paint: TextPaint,
    maxLines: Int = 3,
  ): Float {
    val layout =
      StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setMaxLines(maxLines)
        .build()
    canvas.save()
    canvas.translate(x, y)
    layout.draw(canvas)
    canvas.restore()
    return y + layout.height
  }

  suspend fun shareProductPng(
    context: Context,
    product: Product,
    price: ComputedPrice,
    brand: BrandMeta,
    showPrice: Boolean,
  ) {
    val file =
      withContext(Dispatchers.IO) {
        val width = 1080
        val hasThumbs = product.imgs.size > 1
        val height = if (hasThumbs) 1520 else 1320
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val themeColor = parseThemeColor(brand.color)

        // Cream studio paper background
        canvas.drawColor(Color.parseColor("#FAF6F3"))

        // Top Brand Header Bar
        val headerHeight = 170f
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = themeColor }
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, headerPaint)

        // Logo
        val logoRect = RectF(48f, 35f, 148f, 135f)
        val logoBmp = loadSafeBitmap(brand.logo, 300)
        if (logoBmp != null) {
          drawCenterCroppedBitmap(canvas, logoBmp, logoRect, 20f)
          logoBmp.recycle()
        } else {
          drawKamarTajDefaultMark(canvas, logoRect, themeColor)
          val border =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              style = Paint.Style.STROKE
              strokeWidth = 3f
              color = Color.WHITE
            }
          canvas.drawRoundRect(logoRect, 20f, 20f, border)
        }

        val brandNamePaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
          }
        canvas.drawText(brand.name.ifBlank { "Kamar Taj" }, 176f, 92f, brandNamePaint)

        val subHeaderPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5E6E8")
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
          }
        canvas.drawText("Handcrafted Resin Studio", 176f, 130f, subHeaderPaint)

        // Main photo container
        val mainPhotoTop = 210f
        val mainPhotoSize = 720f
        val mainRect = RectF(60f, mainPhotoTop, width - 60f, mainPhotoTop + mainPhotoSize)
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(mainRect, 36f, 36f, cardBgPaint)

        val mainBmp = loadSafeBitmap(product.imgs.firstOrNull(), 1080)
        if (mainBmp != null) {
          drawCenterCroppedBitmap(canvas, mainBmp, mainRect, 36f)
          mainBmp.recycle()
        } else {
          val emptyBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFE7E4") }
          canvas.drawRoundRect(mainRect, 36f, 36f, emptyBg)
          val markRect = RectF(width / 2f - 110f, mainPhotoTop + 250f, width / 2f + 110f, mainPhotoTop + 470f)
          drawKamarTajDefaultMark(canvas, markRect, themeColor)
        }

        var cursorY = mainRect.bottom + 28f

        // Thumbnail strip (up to 4 photos)
        if (hasThumbs) {
          val thumbs = product.imgs.take(4)
          val gap = 20f
          val thumbSize = (width - 120f - gap * 3f) / 4f
          thumbs.forEachIndexed { idx, path ->
            val left = 60f + idx * (thumbSize + gap)
            val rect = RectF(left, cursorY, left + thumbSize, cursorY + thumbSize * 0.78f)
            val tb = loadSafeBitmap(path, 400)
            if (tb != null) {
              drawCenterCroppedBitmap(canvas, tb, rect, 20f)
              tb.recycle()
            }
          }
          cursorY += thumbSize * 0.78f + 32f
        }

        // Tag & SKU pills
        val pillBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFE6E4") }
        val pillText =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = themeColor
            textSize = 26f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          }
        val skuText = "SKU: ${product.sku.ifBlank { "KT-ART" }}"
        val skuW = pillText.measureText(skuText) + 44f
        canvas.drawRoundRect(RectF(60f, cursorY, 60f + skuW, cursorY + 48f), 24f, 24f, pillBg)
        canvas.drawText(skuText, 82f, cursorY + 33f, pillText)

        if (product.cat.isNotBlank()) {
          val tagW = pillText.measureText(product.cat) + 44f
          val tagLeft = 60f + skuW + 18f
          canvas.drawRoundRect(RectF(tagLeft, cursorY, tagLeft + tagW, cursorY + 48f), 24f, 24f, pillBg)
          canvas.drawText(product.cat, tagLeft + 22f, cursorY + 33f, pillText)
        }
        cursorY += 72f

        // Product Name
        val titlePaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1617")
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
          }
        cursorY = drawTextWrapped(canvas, product.name, 60f, cursorY, width - 120, titlePaint, 2)
        cursorY += 16f

        if (product.notes.isNotBlank()) {
          val notePaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = Color.parseColor("#5C5052")
              textSize = 28f
            }
          cursorY = drawTextWrapped(canvas, product.notes, 60f, cursorY, width - 120, notePaint, 2)
        }

        // Price Badge (ONLY if showPrice == true, NEVER cost or profit)
        if (showPrice) {
          val priceBoxTop = height - 245f
          val priceBox = RectF(60f, priceBoxTop, width - 60f, priceBoxTop + 110f)
          val priceBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = themeColor }
          canvas.drawRoundRect(priceBox, 28f, 28f, priceBg)

          val lblPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = Color.parseColor("#F5DCE0")
              textSize = 28f
            }
          canvas.drawText("Price", 96f, priceBoxTop + 65f, lblPaint)

          val valPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = Color.WHITE
              textSize = 52f
              typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
              textAlign = Paint.Align.RIGHT
            }
          canvas.drawText(PricingEngine.formatRupees(price.retail), width - 96f, priceBoxTop + 72f, valPaint)
        }

        // Bottom Contact Footer
        val footerTop = height - 105f
        val linePaint = Paint().apply {
          color = Color.parseColor("#DECFC9")
          strokeWidth = 2f
        }
        canvas.drawLine(60f, footerTop, width - 60f, footerTop, linePaint)

        val contactPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B3F41")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          }
        val contactText =
          brand.contact.ifBlank { "Order & Custom Queries: ${brand.name.ifBlank { "Kamar Taj" }}" }
        canvas.drawText(contactText, 60f, footerTop + 56f, contactPaint)

        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(outDir, "card_${product.sku.ifBlank { "product" }}_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        outFile
      }

    shareFile(context, file, "image/png", "Share Product Card")
  }

  suspend fun shareProductPdf(
    context: Context,
    product: Product,
    price: ComputedPrice,
    brand: BrandMeta,
    showPrice: Boolean,
  ) {
    val file =
      withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val themeColor = parseThemeColor(brand.color)

        canvas.drawColor(Color.parseColor("#FAF6F3"))
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = themeColor }
        canvas.drawRect(0f, 0f, 595f, 92f, headerPaint)

        val logoRect = RectF(28f, 18f, 84f, 74f)
        val logoBmp = loadSafeBitmap(brand.logo, 200)
        if (logoBmp != null) {
          drawCenterCroppedBitmap(canvas, logoBmp, logoRect, 12f)
          logoBmp.recycle()
        } else {
          drawKamarTajDefaultMark(canvas, logoRect, themeColor)
        }

        val brandPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
          }
        canvas.drawText(brand.name.ifBlank { "Kamar Taj" }, 100f, 48f, brandPaint)

        val subPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5DCE0")
            textSize = 13f
          }
        canvas.drawText("Product Showcase Card", 100f, 68f, subPaint)

        // Main photo
        val mainRect = RectF(36f, 114f, 559f, 464f)
        val mainBmp = loadSafeBitmap(product.imgs.firstOrNull(), 900)
        if (mainBmp != null) {
          drawCenterCroppedBitmap(canvas, mainBmp, mainRect, 18f)
          mainBmp.recycle()
        } else {
          val emptyBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFE7E4") }
          canvas.drawRoundRect(mainRect, 18f, 18f, emptyBg)
          drawKamarTajDefaultMark(canvas, RectF(247f, 239f, 347f, 339f), themeColor)
        }

        var y = 482f
        if (product.imgs.size > 1) {
          val gap = 12f
          val thumbW = (523f - gap * 3f) / 4f
          product.imgs.take(4).forEachIndexed { i, path ->
            val left = 36f + i * (thumbW + gap)
            val rect = RectF(left, y, left + thumbW, y + 84f)
            loadSafeBitmap(path, 300)?.let {
              drawCenterCroppedBitmap(canvas, it, rect, 10f)
              it.recycle()
            }
          }
          y += 104f
        }

        val metaPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = themeColor
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          }
        canvas.drawText(
          "SKU: ${product.sku}   •   ${product.cat.ifBlank { "Resin Art" }}",
          36f,
          y + 16f,
          metaPaint,
        )
        y += 32f

        val namePaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1617")
            textSize = 24f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
          }
        y = drawTextWrapped(canvas, product.name, 36f, y, 523, namePaint, 2) + 10f

        if (product.notes.isNotBlank()) {
          val notePaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = Color.parseColor("#5C5052")
              textSize = 13f
            }
          y = drawTextWrapped(canvas, product.notes, 36f, y, 523, notePaint, 3) + 16f
        }

        if (showPrice) {
          val priceBox = RectF(36f, 706f, 559f, 764f)
          canvas.drawRoundRect(priceBox, 14f, 14f, headerPaint)
          val pLbl =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = Color.parseColor("#F5DCE0")
              textSize = 15f
            }
          canvas.drawText("Price", 56f, 741f, pLbl)
          val pVal =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = Color.WHITE
              textSize = 26f
              typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
              textAlign = Paint.Align.RIGHT
            }
          canvas.drawText(PricingEngine.formatRupees(price.retail), 539f, 744f, pVal)
        }

        val contactPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B3F41")
            textSize = 13f
          }
        canvas.drawText(
          brand.contact.ifBlank { brand.name.ifBlank { "Kamar Taj" } },
          36f,
          808f,
          contactPaint,
        )

        doc.finishPage(page)
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(outDir, "product_${product.sku.ifBlank { "card" }}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        outFile
      }

    shareFile(context, file, "application/pdf", "Share Product PDF")
  }

  suspend fun shareFullCatalogPdf(
    context: Context,
    items: List<Pair<Product, ComputedPrice>>,
    brand: BrandMeta,
    showPrice: Boolean,
  ) {
    val file =
      withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val themeColor = parseThemeColor(brand.color)
        val perPage = 4
        val pages = items.chunked(perPage).ifEmpty { listOf(emptyList()) }

        pages.forEachIndexed { pageIndex, chunk ->
          val pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageIndex + 1).create()
          val page = doc.startPage(pageInfo)
          val canvas = page.canvas

          canvas.drawColor(Color.parseColor("#FAF6F3"))
          val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = themeColor }
          canvas.drawRect(0f, 0f, 595f, 78f, headerPaint)

          val logoRect = RectF(24f, 14f, 74f, 64f)
          val logoBmp = loadSafeBitmap(brand.logo, 180)
          if (logoBmp != null) {
            drawCenterCroppedBitmap(canvas, logoBmp, logoRect, 10f)
            logoBmp.recycle()
          } else {
            drawKamarTajDefaultMark(canvas, logoRect, themeColor)
          }

          val titlePaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = Color.WHITE
              textSize = 22f
              typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
          canvas.drawText("${brand.name.ifBlank { "Kamar Taj" }} — Catalog", 90f, 44f, titlePaint)

          val pagePaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = Color.parseColor("#F5DCE0")
              textSize = 12f
              textAlign = Paint.Align.RIGHT
            }
          canvas.drawText("Page ${pageIndex + 1}/${pages.size}", 570f, 44f, pagePaint)

          val cardW = 252f
          val cardH = 330f
          val startX = 32f
          val startY = 98f
          val gapX = 27f
          val gapY = 22f

          chunk.forEachIndexed { idx, (prod, price) ->
            val col = idx % 2
            val row = idx / 2
            val left = startX + col * (cardW + gapX)
            val top = startY + row * (cardH + gapY)
            val cardRect = RectF(left, top, left + cardW, top + cardH)

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(cardRect, 16f, 16f, cardPaint)
            val strokePaint =
              Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 1f
                color = Color.parseColor("#E5D8D5")
              }
            canvas.drawRoundRect(cardRect, 16f, 16f, strokePaint)

            val imgRect = RectF(left + 10f, top + 10f, left + cardW - 10f, top + 205f)
            val bmp = loadSafeBitmap(prod.imgs.firstOrNull(), 500)
            if (bmp != null) {
              drawCenterCroppedBitmap(canvas, bmp, imgRect, 12f)
              bmp.recycle()
            } else {
              val emptyBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#EFE7E4") }
              canvas.drawRoundRect(imgRect, 12f, 12f, emptyBg)
              drawKamarTajDefaultMark(
                canvas,
                RectF(left + cardW / 2f - 32f, top + 75f, left + cardW / 2f + 32f, top + 139f),
                themeColor,
              )
            }

            val skuPaint =
              TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = themeColor
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
              }
            canvas.drawText(
              "${prod.sku}${if (prod.cat.isNotBlank()) " • ${prod.cat}" else ""}",
              left + 14f,
              top + 225f,
              skuPaint,
            )

            val namePaint =
              TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#1D1617")
                textSize = 15f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
              }
            drawTextWrapped(canvas, prod.name, left + 14f, top + 234f, (cardW - 28f).toInt(), namePaint, 2)

            if (showPrice) {
              val pricePaint =
                TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                  color = themeColor
                  textSize = 18f
                  typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                }
              canvas.drawText(
                PricingEngine.formatRupees(price.retail),
                left + 14f,
                top + cardH - 18f,
                pricePaint,
              )
            }
          }

          val footerPaint =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = Color.parseColor("#5C5052")
              textSize = 12f
            }
          canvas.drawText(
            brand.contact.ifBlank { brand.name.ifBlank { "Kamar Taj" } },
            32f,
            816f,
            footerPaint,
          )
          doc.finishPage(page)
        }

        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(outDir, "kamar_taj_catalog_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        outFile
      }

    shareFile(context, file, "application/pdf", "Share Full Catalog PDF")
  }

  suspend fun shareOrderQuotationPng(
    context: Context,
    order: OrderRecord,
    brand: BrandMeta,
  ) {
    val file =
      withContext(Dispatchers.IO) {
        val width = 1080
        val height = 1320
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val themeColor = parseThemeColor(brand.color)

        canvas.drawColor(Color.parseColor("#FAF6F3"))
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = themeColor }
        canvas.drawRect(0f, 0f, width.toFloat(), 200f, headerPaint)

        val logoRect = RectF(56f, 44f, 164f, 152f)
        val logoBmp = loadSafeBitmap(brand.logo, 300)
        if (logoBmp != null) {
          drawCenterCroppedBitmap(canvas, logoBmp, logoRect, 22f)
          logoBmp.recycle()
        } else {
          drawKamarTajDefaultMark(canvas, logoRect, themeColor)
          val border =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
              style = Paint.Style.STROKE
              strokeWidth = 3f
              color = Color.WHITE
            }
          canvas.drawRoundRect(logoRect, 22f, 22f, border)
        }

        val brandPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 48f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
          }
        canvas.drawText(brand.name.ifBlank { "Kamar Taj" }, 192f, 102f, brandPaint)

        val qBadgePaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5DCE0")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          }
        canvas.drawText("ORDER QUOTATION", 192f, 145f, qBadgePaint)

        // Main Slip Card
        val cardRect = RectF(56f, 240f, width - 56f, height - 140f)
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(cardRect, 32f, 32f, cardBg)

        val labelPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#736567")
            textSize = 26f
          }
        val valuePaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1617")
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          }

        var y = 308f
        canvas.drawText("CUSTOMER", 96f, y, labelPaint)
        y += 46f
        canvas.drawText(order.cust.ifBlank { "Customer" }, 96f, y, valuePaint)
        if (order.phone.isNotBlank()) {
          y += 42f
          canvas.drawText("Phone: ${order.phone}", 96f, y, labelPaint)
        }

        // Delivery Date & Status on right
        val rightLabel =
          TextPaint(labelPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightValue =
          TextPaint(valuePaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("DELIVERY DATE", width - 96f, 308f, rightLabel)
        canvas.drawText(order.date.ifBlank { PricingEngine.todayIso() }, width - 96f, 354f, rightValue)

        y += 50f
        val divPaint = Paint().apply {
          color = Color.parseColor("#EADFD9")
          strokeWidth = 2f
        }
        canvas.drawLine(96f, y, width - 96f, y, divPaint)
        y += 54f

        canvas.drawText("PRODUCT / ORDER DETAILS", 96f, y, labelPaint)
        y += 46f
        val prodTitlePaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1617")
            textSize = 40f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
          }
        y = drawTextWrapped(canvas, order.pname, 96f, y, width - 192, prodTitlePaint, 2) + 18f

        val qtySku = buildString {
          append("Quantity: ${order.qty}")
          if (order.sku.isNotBlank()) append("   •   SKU: ${order.sku}")
        }
        canvas.drawText(qtySku, 96f, y + 26f, labelPaint)
        y += 72f

        if (order.notes.isNotBlank()) {
          y = drawTextWrapped(canvas, "Note: ${order.notes}", 96f, y, width - 192, labelPaint, 3) + 28f
        }

        // Totals Box
        val totalsTop = height - 470f
        val totalsBox = RectF(96f, totalsTop, width - 96f, totalsTop + 280f)
        val totalsBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FAF6F3") }
        canvas.drawRoundRect(totalsBox, 24f, 24f, totalsBg)

        fun drawSummaryRow(rowY: Float, lbl: String, amt: Double, bold: Boolean = false, highlightColor: Int? = null) {
          val lp =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = highlightColor ?: Color.parseColor("#3E3335")
              textSize = if (bold) 36f else 30f
              typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
            }
          val vp =
            TextPaint(lp).apply {
              textAlign = Paint.Align.RIGHT
              typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
          canvas.drawText(lbl, 132f, rowY, lp)
          canvas.drawText(PricingEngine.formatRupees(amt), width - 132f, rowY, vp)
        }

        drawSummaryRow(totalsTop + 68f, "Total Amount", order.price)
        drawSummaryRow(totalsTop + 132f, "Advance Paid", order.advance)
        canvas.drawLine(132f, totalsTop + 165f, width - 132f, totalsTop + 165f, divPaint)
        drawSummaryRow(
          totalsTop + 230f,
          "Baaki Payment",
          order.balanceDue,
          bold = true,
          highlightColor = themeColor,
        )

        // Contact Footer
        val footerPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B3F41")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          }
        canvas.drawText(
          brand.contact.ifBlank { "Thank you for ordering from ${brand.name.ifBlank { "Kamar Taj" }}!" },
          56f,
          height - 62f,
          footerPaint,
        )

        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(outDir, "quotation_${order.id.take(8)}_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        outFile
      }

    shareFile(context, file, "image/png", "Share Order Quotation")
  }

  suspend fun shareOrderQuotationPdf(
    context: Context,
    order: OrderRecord,
    brand: BrandMeta,
  ) {
    val file =
      withContext(Dispatchers.IO) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val themeColor = parseThemeColor(brand.color)

        canvas.drawColor(Color.parseColor("#FAF6F3"))
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = themeColor }
        canvas.drawRect(0f, 0f, 595f, 108f, headerPaint)

        val logoRect = RectF(32f, 22f, 94f, 84f)
        val logoBmp = loadSafeBitmap(brand.logo, 200)
        if (logoBmp != null) {
          drawCenterCroppedBitmap(canvas, logoBmp, logoRect, 14f)
          logoBmp.recycle()
        } else {
          drawKamarTajDefaultMark(canvas, logoRect, themeColor)
        }

        val brandPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 26f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
          }
        canvas.drawText(brand.name.ifBlank { "Kamar Taj" }, 112f, 54f, brandPaint)

        val subPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F5DCE0")
            textSize = 14f
          }
        canvas.drawText("ORDER QUOTATION / SLIP", 112f, 78f, subPaint)

        val cardRect = RectF(32f, 136f, 563f, 760f)
        val cardBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(cardRect, 18f, 18f, cardBg)

        val lblPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#736567")
            textSize = 13f
          }
        val valPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1617")
            textSize = 18f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
          }

        canvas.drawText("CUSTOMER", 56f, 176f, lblPaint)
        canvas.drawText(order.cust.ifBlank { "Customer" }, 56f, 202f, valPaint)
        if (order.phone.isNotBlank()) {
          canvas.drawText("Phone: ${order.phone}", 56f, 224f, lblPaint)
        }

        val rightLbl = TextPaint(lblPaint).apply { textAlign = Paint.Align.RIGHT }
        val rightVal = TextPaint(valPaint).apply { textAlign = Paint.Align.RIGHT }
        canvas.drawText("DELIVERY DATE", 539f, 176f, rightLbl)
        canvas.drawText(order.date.ifBlank { PricingEngine.todayIso() }, 539f, 202f, rightVal)
        canvas.drawText("Status: ${order.status}", 539f, 224f, rightLbl)

        val divPaint = Paint().apply {
          color = Color.parseColor("#EADFD9")
          strokeWidth = 1.5f
        }
        canvas.drawLine(56f, 250f, 539f, 250f, divPaint)

        canvas.drawText("ITEM DETAILS", 56f, 282f, lblPaint)
        val prodPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1D1617")
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
          }
        var y = drawTextWrapped(canvas, order.pname, 56f, 296f, 483, prodPaint, 2) + 12f
        canvas.drawText(
          "Quantity: ${order.qty}${if (order.sku.isNotBlank()) "  •  SKU: ${order.sku}" else ""}",
          56f,
          y + 14f,
          lblPaint,
        )
        y += 38f
        if (order.notes.isNotBlank()) {
          drawTextWrapped(canvas, "Notes: ${order.notes}", 56f, y, 483, lblPaint, 3)
        }

        val sumBox = RectF(56f, 530f, 539f, 710f)
        val sumBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FAF6F3") }
        canvas.drawRoundRect(sumBox, 14f, 14f, sumBg)

        fun drawRow(rowY: Float, label: String, amt: Double, isBold: Boolean = false, colorInt: Int? = null) {
          val lp =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
              color = colorInt ?: Color.parseColor("#3E3335")
              textSize = if (isBold) 20f else 16f
              typeface = Typeface.create(Typeface.SANS_SERIF, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            }
          val vp =
            TextPaint(lp).apply {
              textAlign = Paint.Align.RIGHT
              typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
          canvas.drawText(label, 78f, rowY, lp)
          canvas.drawText(PricingEngine.formatRupees(amt), 517f, rowY, vp)
        }

        drawRow(574f, "Total Amount", order.price)
        drawRow(614f, "Advance Paid", order.advance)
        canvas.drawLine(78f, 636f, 517f, 636f, divPaint)
        drawRow(678f, "Baaki Payment", order.balanceDue, isBold = true, colorInt = themeColor)

        val footerPaint =
          TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#4B3F41")
            textSize = 13f
          }
        canvas.drawText(
          brand.contact.ifBlank { brand.name.ifBlank { "Kamar Taj" } },
          32f,
          804f,
          footerPaint,
        )

        doc.finishPage(page)
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(outDir, "quotation_${order.id.take(8)}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outFile).use { doc.writeTo(it) }
        doc.close()
        outFile
      }

    shareFile(context, file, "application/pdf", "Share Order Quotation PDF")
  }

  suspend fun shareJsonBackup(context: Context, json: String) {
    val file =
      withContext(Dispatchers.IO) {
        val outDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val outFile = File(outDir, "kamar_taj_backup_${PricingEngine.todayIso()}.json")
        outFile.writeText(json)
        outFile
      }
    shareFile(context, file, "application/json", "Share Kamar Taj Backup")
  }

  private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent =
      Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
    val chooser =
      Intent.createChooser(intent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    context.startActivity(chooser)
  }
}
