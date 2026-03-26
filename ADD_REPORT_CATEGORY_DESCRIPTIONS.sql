-- Add description column to report_categories table
ALTER TABLE report_categories 
ADD COLUMN IF NOT EXISTS description TEXT;

-- Update existing categories with descriptions
UPDATE report_categories 
SET description = 'Report damaged or poorly maintained roads, potholes, cracks, or unsafe road conditions that need repair.'
WHERE name = 'Road Damage';

UPDATE report_categories 
SET description = 'Report broken or malfunctioning streetlights that affect safety and visibility in your area.'
WHERE name = 'Streetlight Issue';

UPDATE report_categories 
SET description = 'Report illegal dumping, overflowing trash bins, uncollected garbage, or areas that need cleaning.'
WHERE name = 'Garbage/Waste';

UPDATE report_categories 
SET description = 'Report water leaks, pipe bursts, low water pressure, or contaminated water supply issues.'
WHERE name = 'Water Supply Problem';

UPDATE report_categories 
SET description = 'Report power outages, damaged electrical lines, exposed wires, or faulty electrical infrastructure.'
WHERE name = 'Electrical Problem';

UPDATE report_categories 
SET description = 'Report blocked drains, flooding, sewage overflow, or drainage system problems in your area.'
WHERE name = 'Drainage/Flooding';

UPDATE report_categories 
SET description = 'Report noise disturbances, loud parties, construction noise, or other noise-related complaints.'
WHERE name = 'Noise Complaint';

UPDATE report_categories 
SET description = 'Report stray animals, animal abuse, dangerous animals, or pet-related concerns in the community.'
WHERE name = 'Stray Animals';

UPDATE report_categories 
SET description = 'Report vandalism, graffiti, property damage, or destruction of public or private property.'
WHERE name = 'Vandalism';

UPDATE report_categories 
SET description = 'Report suspicious activities, safety concerns, or potential security threats in your neighborhood.'
WHERE name = 'Security Concern';

UPDATE report_categories 
SET description = 'Report air pollution, water pollution, illegal burning, or environmental hazards affecting the community.'
WHERE name = 'Environmental Hazard';

UPDATE report_categories 
SET description = 'Report damaged sidewalks, missing railings, broken stairs, or unsafe pedestrian pathways.'
WHERE name = 'Sidewalk/Pathway Issue';

-- Note: "Others" category should not have a description as users will provide their own
UPDATE report_categories 
SET description = NULL
WHERE name = 'Others';

-- Verify the updates
SELECT name, description, points, sort_order 
FROM report_categories 
ORDER BY sort_order;
