ALTER TABLE etfs
  ADD COLUMN metadata_source varchar(40),
  ADD COLUMN look_through_coverage numeric(5,2),
  ADD COLUMN metadata_updated_at date;

UPDATE etfs
SET metadata_source = 'local-metadata',
    look_through_coverage = CASE ticker
      WHEN 'IUSQ' THEN 85.00
      WHEN 'EEMA' THEN 82.00
      WHEN 'VGVF' THEN 84.00
      WHEN 'ICOM' THEN 96.00
      WHEN 'SPYD' THEN 83.00
      WHEN 'EXXY' THEN 81.00
      WHEN 'IUSN' THEN 72.00
      WHEN 'NUKL' THEN 90.00
      WHEN 'CBUK' THEN 88.00
      WHEN 'CQQQ' THEN 88.00
      WHEN 'XDWT' THEN 97.00
      ELSE look_through_coverage
    END,
    metadata_updated_at = DATE '2026-06-16'
WHERE ticker IN ('IUSQ','EEMA','VGVF','ICOM','SPYD','EXXY','IUSN','NUKL','CBUK','CQQQ','XDWT');
