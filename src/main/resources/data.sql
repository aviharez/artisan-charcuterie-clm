-- Seed Farms
INSERT INTO farms (name, location, breed, contact_info, certifications, created_at)
VALUES
    ('Parma Heritage Farm', 'Parma, Emilia-Romagna, Italy', 'Large White x Landrace', 'info@parmaheritage.it', 'DOP, IGP, Organic EU', NOW()),
    ('Valle Verde Ranch', 'Brescia, Lombardy, Italy', 'Chianina', 'contact@valleverde.it', 'IGP, Free Range', NOW()),
    ('Culatello Estate', 'Zibello, Parma, Italy', 'Mora Romagnola', 'estate@culatello.it', 'DOP, Traditional Methods', NOW()),
    ('Black Berkshire Farms', 'Somerset, England, UK', 'Berkshire', 'hello@blackberkshire.co.uk', 'RSPCA Assured, Free Range', NOW());

-- Seed Chambers
INSERT INTO chambers (name, chamber_type, target_temperature_celsius, target_humidity_percent, capacity, is_active, created_at)
VALUES
    ('Cold Smoke House A', 'COLD_SMOKE', 12.00, 70.00, 20, true, NOW()),
    ('Fermentation Room 1', 'FERMENTATION_ROOM', 18.00, 85.00, 30, true, NOW()),
    ('Primary Aging Cellar Alpha', 'PRIMARY_AGING_CELLAR', 14.00, 75.00, 50, true, NOW()),
    ('Primary Aging Cellar Beta', 'PRIMARY_AGING_CELLAR', 14.00, 75.00, 50, true, NOW()),
    ('Cold Smoke House B', 'COLD_SMOKE', 12.00, 70.00, 15, true, NOW()),
    ('Fermentation Room 2', 'FERMENTATION_ROOM', 18.00, 85.00, 25, true, NOW());

-- Seed Market Spot Prices (USD per kg)
INSERT INTO market_spot_prices (product_type, price_per_kg, currency, effective_date, updated_at)
VALUES
    ('PROSCIUTTO', 85.00, 'USD', CURRENT_DATE, NOW()),
    ('BRESAOLA', 95.00, 'USD', CURRENT_DATE, NOW()),
    ('CULATELLO', 145.00, 'USD', CURRENT_DATE, NOW())