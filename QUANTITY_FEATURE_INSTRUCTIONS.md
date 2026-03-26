# Quantity Feature - Setup Instructions

## Current Status
✅ The quantity UI is implemented and visible in the app
✅ The quantity field works (you can increase/decrease)
✅ Total price calculation works
❌ The quantity is NOT saved to the database yet

## Why It's Not Working
The `quantity` column doesn't exist in your Supabase `requests` table yet. The code has been temporarily disabled to prevent errors.

## How to Enable the Quantity Feature

### Step 1: Add the Quantity Column to Database
Run this SQL in your Supabase SQL Editor:

```sql
-- Add quantity column to requests table
ALTER TABLE requests 
ADD COLUMN IF NOT EXISTS quantity INTEGER DEFAULT 1;

-- Update existing requests to have quantity of 1
UPDATE requests 
SET quantity = 1 
WHERE quantity IS NULL;

-- Add constraint to ensure quantity is between 1 and 10
ALTER TABLE requests 
ADD CONSTRAINT quantity_range CHECK (quantity >= 1 AND quantity <= 10);
```

### Step 2: Enable Quantity in Code

#### In `RequestDocumentActivity.kt`:
Find this line (around line 380):
```kotlin
val result = repository.submitRequest(
    userId        = userId,
    documentType  = finalDocumentType,
    purpose       = purpose,
    paymentMethod = selectedPayment,
    proofUrl      = uploadedProofUrl,
    barangay      = barangay
    // quantity parameter removed temporarily until database is updated
)
```

Change it to:
```kotlin
val result = repository.submitRequest(
    userId        = userId,
    documentType  = finalDocumentType,
    purpose       = purpose,
    paymentMethod = selectedPayment,
    proofUrl      = uploadedProofUrl,
    barangay      = barangay,
    quantity      = currentQuantity
)
```

#### In `RequestRepository.kt`:
Find this section (around line 65):
```kotlin
val data = buildJsonObject {
    put("user_id",       userId)
    put("document_type", documentType)
    put("purpose",       purpose)
    put("payment_method", paymentMethod)
    put("status",        "pending")
    put("resident_name", residentName)
    // quantity field commented out until database column is added
    // put("quantity",      quantity)
    if (proofUrl != null) put("proof_url", proofUrl)
    if (barangay != null) put("barangay",  barangay)
}
```

Change it to:
```kotlin
val data = buildJsonObject {
    put("user_id",       userId)
    put("document_type", documentType)
    put("purpose",       purpose)
    put("payment_method", paymentMethod)
    put("status",        "pending")
    put("resident_name", residentName)
    put("quantity",      quantity)
    if (proofUrl != null) put("proof_url", proofUrl)
    if (barangay != null) put("barangay",  barangay)
}
```

### Step 3: Rebuild and Test
1. Rebuild the Android app
2. Submit a document request
3. Check the database to verify quantity is saved

## Current Behavior (Without Database Column)
- ✅ Users can see and change quantity
- ✅ Total price updates correctly
- ✅ Form submission works
- ❌ Quantity is not saved (defaults to 1 in database)

## After Enabling (With Database Column)
- ✅ Users can see and change quantity
- ✅ Total price updates correctly
- ✅ Form submission works
- ✅ Quantity is saved to database
- ✅ Officials/admins can see the quantity in requests

## Quick Fix (If You Don't Want Quantity Feature)
If you don't want the quantity feature at all, you can hide it by adding this to the layout XML:

In `activity_request_document.xml`, find the quantity section and add `android:visibility="gone"`:

```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical"
    android:visibility="gone">
    <!-- Quantity controls -->
</LinearLayout>
```

## Summary
The app now works without errors. The quantity feature is ready but disabled until you add the database column. Follow the steps above to enable it when ready.
