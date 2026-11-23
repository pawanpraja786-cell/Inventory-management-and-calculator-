# Prompt: Kamar Taj Resin Studio ka Android app banao

Main ek resin art business chalata hu. Maine ek web app (single HTML file, `studio-final.html`) banwaya hai jo Claude ke andar chalta hai. Ab mujhe ise **Android phone par alag app** ki tarah chahiye. Neeche diye spec ke hisaab se banao. Saath mein `studio-final.html` ka poora code attach hai, use reference ki tarah padho aur same logic aur UI rakho.

## Kya banana hai
Ek Android app (ya installable PWA, jo bhi simple ho) jisme 4 tabs hon: **Inventory, Calculator, Catalog, Orders**, aur ek **Settings** (⚙). UI Hinglish mein, mobile-first, light/dark mode.

## Important: kya badalna hai
Reference file mein 3 cheezein Claude ke platform par depend karti hain. Inhe replace karo:
1. `claude.use("db")` (shared database) ko **local storage** se badlo (IndexedDB ya SQLite/Room). Collections: `items`, `prods`, `orders`, `meta`. Har doc ka id string hota hai.
2. `claude.use("downloads")` ko normal file save/share se badlo (Android Share sheet, jisse PNG/PDF WhatsApp par bheji ja sake).
3. Optional: multiple phones ka sync chahiye to Firebase Firestore ya Supabase use karo (sab users ek hi data dekhein).

## Data model
- **items** (materials): `cat` (section, user naye bana sakta hai), `name`, `price` (total ₹), `qty` (total quantity, mold ke liye kitni baar use), `unit` (gm/pc/use), `img`, `stock` (optional), `low` (low-stock alert, optional). Per-unit cost = price / qty.
- **prods** (catalog products): `name`, `sku` (unique), `cat` (tag), `imgs` (max 4 photos, pehli main), `profit` %, `fee` % (platform fee), `wp` % (wholesale margin), `notes`, `lines` (cost lines).
- **lines** ke 3 type:
  - `item`: `{id, q}` cost = unit cost of item x q
  - `vol` (size se resin): `{id (resin item), shape ("r" rectangle / "c" round), a, b, u ("in" ya "cm"), th (mm), waste %, dens (default 1.1)}`
    grams = area(cm2) x thickness(cm) x density x (1 + waste/100); cost = grams x resin unit cost
  - `fix`: `{label, amt}` fixed charge (labour, finishing, packaging, shipping, coating)
- **Price formulas:** `cost = sum(lines)`; `retail = cost x (1 + profit/100) / (1 - fee/100)`; `wholesale = cost x (1 + wp/100) / (1 - fee/100)`.
- **orders**: `cust`, `phone`, `pid` (ya `"__c"` custom order), `pname`, `sku`, `qty`, `price`, `cost`, `advance`, `date` (delivery), `status` (Pending, Ban raha hai, Ready, Delivered, Cancelled), `notes`, `used` ({itemId: quantity}), `restored` (bool).
- **meta/brand**: `name`, `logo` (image), `contact` (phone/Instagram), `color` (theme colour hex), `mode` (auto/light/dark).

## Features (sab chahiye)
1. **Inventory:** materials sections ke saath (resin, mold, pigment, stone, flower, stand, other + user ke apne naye sections), photo, search, stock aur low-stock warning, backup (JSON download).
2. **Calculator:** product naam, SKU, tag, 4 tak photos, cost lines (material, size se resin, packaging, shipping, fixed), profit %, platform fee %, wholesale %. Total cost, retail price, wholesale price live dikhe. "Catalog mein save" button. Material ka rate badalne par saare saved products ka price apne aap update ho (lines id se material ko refer karti hain).
3. **Catalog:** products ka grid, search, tag filter, Edit, Copy (naya SKU), Delete. Har product ka **Photo (PNG) aur PDF** share card: logo + business naam header, main photo, 3-4 chhoti photos, naam, SKU, price (toggle: price dikhao ya nahi), neeche contact. Poore catalog ka PDF bhi. Cost aur profit customer ko kabhi na dikhe.
4. **Orders:** customer, phone, product (catalog ya Custom), qty, price (auto = retail x qty, edit ho sake), advance, delivery date, status. Order bante hi `used` materials ka stock ghate; Cancel ya delete par stock wapas aaye. Har order ka **quotation** (PNG/PDF): customer, product, total, advance, baaki.
5. **Report:** mahine ke hisaab se orders, total sale, cost, profit, advance, baaki payment.
6. **Settings:** business naam, logo upload ya hatana, phone/Instagram, theme colour (8 presets + custom), mode (auto/light/dark).
7. Koi native `alert/confirm/prompt` nahi, in-app toast aur modal use karo.
8. Khaali app par "Shuru kaise karein" guide aur example materials daalne ka button.

## Branding
Business naam: **Kamar Taj**. Default theme colour: maroon `#82091c`. Logo: white arch par K ka mark, maroon background par (alag se image dunga, Settings se upload hoga).

## Output chahiye
Poora working code, build/run steps, aur Android par install karne ka tarika (APK ya PWA "Add to Home screen"). Pehle ek chhota plan do, phir code.
