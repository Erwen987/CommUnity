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

-- Verify the changes
SELECT id, document_type, quantity, status, created_at 
FROM requests 
ORDER BY created_at DESC 
LIMIT 10;
