DELETE FROM etf_holdings
WHERE etf_id IN (SELECT id FROM etfs WHERE ticker IN ('IUSQ','CEBL','VGVF','EXXY','SPYD','AYEW','IUSN','NUKL','CBUK'));

DELETE FROM etf_exposures
WHERE etf_id IN (SELECT id FROM etfs WHERE ticker IN ('IUSQ','CEBL','VGVF','EXXY','SPYD','AYEW','IUSN','NUKL','CBUK'));

INSERT INTO etfs (id, created_at, updated_at, ticker, isin, name, provider, ter, assets_under_management, currency, asset_class, region, distribution_policy, benchmark_index, risk_level, last_price, performance_1y, performance_3y, performance_5y)
VALUES
  (gen_random_uuid(), now(), now(), 'IUSQ', 'IE00B6R52259', 'iShares MSCI ACWI UCITS ETF', 'iShares', 0.2000, 15500000000, 'EUR', 'Equity', 'Global', 'ACCUMULATING', 'MSCI ACWI', 4, 78.50, 17.8000, 34.4000, 68.9000),
  (gen_random_uuid(), now(), now(), 'CEBL', 'IE00B1XNHC34', 'iShares Global Clean Energy UCITS ETF', 'iShares', 0.6500, 2800000000, 'EUR', 'Equity', 'Global Clean Energy', 'DISTRIBUTING', 'S&P Global Clean Energy', 5, 8.40, -7.3000, -22.8000, 18.7000),
  (gen_random_uuid(), now(), now(), 'VGVF', 'IE00BK5BQW10', 'Vanguard FTSE Developed World UCITS ETF', 'Vanguard', 0.1200, 6200000000, 'EUR', 'Equity', 'Developed Markets', 'ACCUMULATING', 'FTSE Developed', 4, 103.20, 16.9000, 33.7000, 70.2000),
  (gen_random_uuid(), now(), now(), 'EXXY', 'LU0292097991', 'Xtrackers STOXX Europe 600 UCITS ETF', 'Xtrackers', 0.2000, 4100000000, 'EUR', 'Equity', 'Europe', 'ACCUMULATING', 'STOXX Europe 600', 4, 122.60, 10.8000, 23.1000, 48.5000),
  (gen_random_uuid(), now(), now(), 'SPYD', 'IE00B6YX5C33', 'SPDR S&P US Dividend Aristocrats UCITS ETF', 'SPDR', 0.3500, 3500000000, 'EUR', 'Equity', 'United States', 'DISTRIBUTING', 'S&P High Yield Dividend Aristocrats', 4, 66.30, 9.4000, 19.6000, 44.1000),
  (gen_random_uuid(), now(), now(), 'AYEW', 'IE00BDBRDM43', 'iShares Core Global Aggregate Bond UCITS ETF', 'iShares', 0.1000, 12800000000, 'EUR', 'Fixed Income', 'Global', 'ACCUMULATING', 'Bloomberg Global Aggregate Bond', 2, 5.25, 4.2000, -3.6000, -5.1000),
  (gen_random_uuid(), now(), now(), 'IUSN', 'IE00BF4RFH31', 'iShares MSCI World Small Cap UCITS ETF', 'iShares', 0.3500, 7200000000, 'EUR', 'Equity', 'Developed Small Cap', 'ACCUMULATING', 'MSCI World Small Cap', 5, 7.85, 12.6000, 18.8000, 47.4000),
  (gen_random_uuid(), now(), now(), 'NUKL', 'IE000M7V94E1', 'VanEck Uranium and Nuclear Technologies UCITS ETF', 'VanEck', 0.5500, 620000000, 'EUR', 'Equity', 'Global Thematic', 'ACCUMULATING', 'MarketVector Global Uranium and Nuclear Energy', 5, 24.70, 29.1000, 0.0000, 0.0000),
  (gen_random_uuid(), now(), now(), 'CBUK', 'IE00BD3VFW73', 'iShares Core FTSE 100 UCITS ETF', 'iShares', 0.0700, 14500000000, 'GBP', 'Equity', 'United Kingdom', 'DISTRIBUTING', 'FTSE 100', 4, 190.40, 8.8000, 24.2000, 39.6000)
ON CONFLICT (ticker) DO UPDATE SET
  updated_at = now(),
  isin = EXCLUDED.isin,
  name = EXCLUDED.name,
  provider = EXCLUDED.provider,
  ter = EXCLUDED.ter,
  assets_under_management = EXCLUDED.assets_under_management,
  currency = EXCLUDED.currency,
  asset_class = EXCLUDED.asset_class,
  region = EXCLUDED.region,
  distribution_policy = EXCLUDED.distribution_policy,
  benchmark_index = EXCLUDED.benchmark_index,
  risk_level = EXCLUDED.risk_level,
  last_price = EXCLUDED.last_price,
  performance_1y = EXCLUDED.performance_1y,
  performance_3y = EXCLUDED.performance_3y,
  performance_5y = EXCLUDED.performance_5y;

INSERT INTO etf_exposures (id, created_at, updated_at, etf_id, type, name, weight)
SELECT gen_random_uuid(), now(), now(), e.id, x.type, x.name, x.weight
FROM etfs e
JOIN (VALUES
  ('IUSQ','COUNTRY','United States',61.0),('IUSQ','COUNTRY','Europe',17.0),('IUSQ','COUNTRY','Japan',5.5),('IUSQ','COUNTRY','Emerging Markets',12.0),('IUSQ','COUNTRY','Other',4.5),
  ('IUSQ','SECTOR','Technology',24.0),('IUSQ','SECTOR','Financial Services',16.0),('IUSQ','SECTOR','Healthcare',12.0),('IUSQ','SECTOR','Industrials',10.0),('IUSQ','SECTOR','Consumer Defensive',7.0),('IUSQ','SECTOR','Other',31.0),
  ('IUSQ','INDUSTRY','Semiconductors',8.0),('IUSQ','INDUSTRY','Software',7.0),('IUSQ','INDUSTRY','Banks',6.5),('IUSQ','INDUSTRY','Drug Manufacturers',5.5),('IUSQ','INDUSTRY','Insurance',3.5),('IUSQ','INDUSTRY','Other',69.5),
  ('IUSQ','CURRENCY','USD',63.0),('IUSQ','CURRENCY','EUR',11.0),('IUSQ','CURRENCY','JPY',5.5),('IUSQ','CURRENCY','GBP',4.0),('IUSQ','CURRENCY','Other',16.5),('IUSQ','ASSET_CLASS','Equity',100.0),

  ('CEBL','COUNTRY','United States',36.0),('CEBL','COUNTRY','Europe',31.0),('CEBL','COUNTRY','Japan',4.0),('CEBL','COUNTRY','Emerging Markets',21.0),('CEBL','COUNTRY','Other',8.0),
  ('CEBL','SECTOR','Utilities',33.0),('CEBL','SECTOR','Industrials',27.0),('CEBL','SECTOR','Technology',18.0),('CEBL','SECTOR','Basic Materials',12.0),('CEBL','SECTOR','Other',10.0),
  ('CEBL','INDUSTRY','Renewable Utilities',25.0),('CEBL','INDUSTRY','Electrical Equipment',21.0),('CEBL','INDUSTRY','Solar',17.0),('CEBL','INDUSTRY','Semiconductors',8.0),('CEBL','INDUSTRY','Specialty Chemicals',7.0),('CEBL','INDUSTRY','Other',22.0),
  ('CEBL','CURRENCY','USD',38.0),('CEBL','CURRENCY','EUR',24.0),('CEBL','CURRENCY','CNY',13.0),('CEBL','CURRENCY','DKK',8.0),('CEBL','CURRENCY','Other',17.0),('CEBL','ASSET_CLASS','Equity',100.0),

  ('VGVF','COUNTRY','United States',69.0),('VGVF','COUNTRY','Europe',17.5),('VGVF','COUNTRY','Japan',6.5),('VGVF','COUNTRY','Emerging Markets',0.0),('VGVF','COUNTRY','Other',7.0),
  ('VGVF','SECTOR','Technology',26.0),('VGVF','SECTOR','Financial Services',14.0),('VGVF','SECTOR','Healthcare',13.0),('VGVF','SECTOR','Industrials',11.0),('VGVF','SECTOR','Consumer Defensive',7.0),('VGVF','SECTOR','Other',29.0),
  ('VGVF','INDUSTRY','Software',8.5),('VGVF','INDUSTRY','Semiconductors',8.0),('VGVF','INDUSTRY','Banks',5.5),('VGVF','INDUSTRY','Drug Manufacturers',5.0),('VGVF','INDUSTRY','Insurance',4.0),('VGVF','INDUSTRY','Other',69.0),
  ('VGVF','CURRENCY','USD',70.0),('VGVF','CURRENCY','EUR',10.5),('VGVF','CURRENCY','JPY',6.5),('VGVF','CURRENCY','GBP',4.5),('VGVF','CURRENCY','Other',8.5),('VGVF','ASSET_CLASS','Equity',100.0),

  ('EXXY','COUNTRY','United States',0.0),('EXXY','COUNTRY','Europe',92.0),('EXXY','COUNTRY','Japan',0.0),('EXXY','COUNTRY','Emerging Markets',0.0),('EXXY','COUNTRY','Other',8.0),
  ('EXXY','SECTOR','Financial Services',18.0),('EXXY','SECTOR','Industrials',17.0),('EXXY','SECTOR','Healthcare',15.0),('EXXY','SECTOR','Consumer Defensive',11.0),('EXXY','SECTOR','Technology',8.0),('EXXY','SECTOR','Other',31.0),
  ('EXXY','INDUSTRY','Banks',12.0),('EXXY','INDUSTRY','Drug Manufacturers',9.0),('EXXY','INDUSTRY','Insurance',8.0),('EXXY','INDUSTRY','Specialty Chemicals',6.0),('EXXY','INDUSTRY','Industrial Machinery',6.0),('EXXY','INDUSTRY','Other',59.0),
  ('EXXY','CURRENCY','EUR',54.0),('EXXY','CURRENCY','GBP',18.0),('EXXY','CURRENCY','CHF',14.0),('EXXY','CURRENCY','DKK',5.0),('EXXY','CURRENCY','Other',9.0),('EXXY','ASSET_CLASS','Equity',100.0),

  ('SPYD','COUNTRY','United States',100.0),('SPYD','COUNTRY','Europe',0.0),('SPYD','COUNTRY','Japan',0.0),('SPYD','COUNTRY','Emerging Markets',0.0),('SPYD','COUNTRY','Other',0.0),
  ('SPYD','SECTOR','Financial Services',20.0),('SPYD','SECTOR','Industrials',16.0),('SPYD','SECTOR','Consumer Defensive',14.0),('SPYD','SECTOR','Healthcare',11.0),('SPYD','SECTOR','Energy',10.0),('SPYD','SECTOR','Other',29.0),
  ('SPYD','INDUSTRY','REITs',12.0),('SPYD','INDUSTRY','Banks',11.0),('SPYD','INDUSTRY','Utilities Regulated',9.0),('SPYD','INDUSTRY','Oil and Gas',8.0),('SPYD','INDUSTRY','Food Products',7.0),('SPYD','INDUSTRY','Other',53.0),
  ('SPYD','CURRENCY','USD',100.0),('SPYD','ASSET_CLASS','Equity',100.0),

  ('AYEW','COUNTRY','United States',39.0),('AYEW','COUNTRY','Europe',28.0),('AYEW','COUNTRY','Japan',12.0),('AYEW','COUNTRY','Emerging Markets',8.0),('AYEW','COUNTRY','Other',13.0),
  ('AYEW','SECTOR','Government Bonds',54.0),('AYEW','SECTOR','Corporate Bonds',28.0),('AYEW','SECTOR','Securitized',12.0),('AYEW','SECTOR','Cash',2.0),('AYEW','SECTOR','Other',4.0),
  ('AYEW','INDUSTRY','Sovereign Debt',54.0),('AYEW','INDUSTRY','Investment Grade Credit',28.0),('AYEW','INDUSTRY','Mortgage Backed Securities',8.0),('AYEW','INDUSTRY','Asset Backed Securities',4.0),('AYEW','INDUSTRY','Other',6.0),
  ('AYEW','CURRENCY','USD',45.0),('AYEW','CURRENCY','EUR',25.0),('AYEW','CURRENCY','JPY',12.0),('AYEW','CURRENCY','GBP',5.0),('AYEW','CURRENCY','Other',13.0),('AYEW','ASSET_CLASS','Fixed Income',100.0),

  ('IUSN','COUNTRY','United States',58.0),('IUSN','COUNTRY','Europe',24.0),('IUSN','COUNTRY','Japan',10.0),('IUSN','COUNTRY','Emerging Markets',0.0),('IUSN','COUNTRY','Other',8.0),
  ('IUSN','SECTOR','Industrials',18.0),('IUSN','SECTOR','Financial Services',15.0),('IUSN','SECTOR','Technology',14.0),('IUSN','SECTOR','Healthcare',12.0),('IUSN','SECTOR','Consumer Cyclical',11.0),('IUSN','SECTOR','Other',30.0),
  ('IUSN','INDUSTRY','Industrial Machinery',9.0),('IUSN','INDUSTRY','Software',6.5),('IUSN','INDUSTRY','Banks',6.0),('IUSN','INDUSTRY','Medical Devices',5.0),('IUSN','INDUSTRY','Specialty Retail',4.5),('IUSN','INDUSTRY','Other',69.0),
  ('IUSN','CURRENCY','USD',59.0),('IUSN','CURRENCY','EUR',15.0),('IUSN','CURRENCY','JPY',10.0),('IUSN','CURRENCY','GBP',6.0),('IUSN','CURRENCY','Other',10.0),('IUSN','ASSET_CLASS','Equity',100.0),

  ('NUKL','COUNTRY','United States',34.0),('NUKL','COUNTRY','Europe',18.0),('NUKL','COUNTRY','Japan',9.0),('NUKL','COUNTRY','Emerging Markets',18.0),('NUKL','COUNTRY','Other',21.0),
  ('NUKL','SECTOR','Energy',35.0),('NUKL','SECTOR','Industrials',22.0),('NUKL','SECTOR','Utilities',18.0),('NUKL','SECTOR','Basic Materials',15.0),('NUKL','SECTOR','Technology',5.0),('NUKL','SECTOR','Other',5.0),
  ('NUKL','INDUSTRY','Uranium',28.0),('NUKL','INDUSTRY','Electrical Equipment',18.0),('NUKL','INDUSTRY','Renewable Utilities',14.0),('NUKL','INDUSTRY','Engineering and Construction',10.0),('NUKL','INDUSTRY','Industrial Machinery',8.0),('NUKL','INDUSTRY','Other',22.0),
  ('NUKL','CURRENCY','USD',42.0),('NUKL','CURRENCY','CAD',18.0),('NUKL','CURRENCY','EUR',14.0),('NUKL','CURRENCY','JPY',9.0),('NUKL','CURRENCY','Other',17.0),('NUKL','ASSET_CLASS','Equity',100.0),

  ('CBUK','COUNTRY','United States',0.0),('CBUK','COUNTRY','Europe',100.0),('CBUK','COUNTRY','Japan',0.0),('CBUK','COUNTRY','Emerging Markets',0.0),('CBUK','COUNTRY','Other',0.0),
  ('CBUK','SECTOR','Financial Services',20.0),('CBUK','SECTOR','Consumer Defensive',17.0),('CBUK','SECTOR','Healthcare',13.0),('CBUK','SECTOR','Energy',12.0),('CBUK','SECTOR','Basic Materials',10.0),('CBUK','SECTOR','Other',28.0),
  ('CBUK','INDUSTRY','Banks',12.0),('CBUK','INDUSTRY','Oil and Gas',10.0),('CBUK','INDUSTRY','Drug Manufacturers',9.0),('CBUK','INDUSTRY','Beverages',8.0),('CBUK','INDUSTRY','Mining',7.0),('CBUK','INDUSTRY','Other',54.0),
  ('CBUK','CURRENCY','GBP',100.0),('CBUK','ASSET_CLASS','Equity',100.0)
) AS x(ticker, type, name, weight) ON x.ticker = e.ticker;

INSERT INTO etf_holdings (id, created_at, updated_at, etf_id, symbol, name, country, sector, industry, weight)
SELECT gen_random_uuid(), now(), now(), e.id, x.symbol, x.name, x.country, x.sector, x.industry, x.weight
FROM etfs e
JOIN (VALUES
  ('IUSQ','AAPL','Apple','United States','Technology','Consumer Electronics',4.4),('IUSQ','MSFT','Microsoft','United States','Technology','Software',4.1),('IUSQ','NVDA','NVIDIA','United States','Technology','Semiconductors',3.5),('IUSQ','AMZN','Amazon','United States','Consumer Cyclical','Internet Retail',2.2),('IUSQ','TSM','Taiwan Semiconductor','Taiwan','Technology','Semiconductors',1.4),
  ('CEBL','FSLR','First Solar','United States','Technology','Solar',8.5),('CEBL','ENPH','Enphase Energy','United States','Technology','Solar',6.5),('CEBL','VWS','Vestas Wind Systems','Denmark','Industrials','Electrical Equipment',6.2),('CEBL','ORSTED','Orsted','Denmark','Utilities','Renewable Utilities',5.6),('CEBL','PLUG','Plug Power','United States','Industrials','Electrical Equipment',4.8),
  ('VGVF','AAPL','Apple','United States','Technology','Consumer Electronics',5.0),('VGVF','MSFT','Microsoft','United States','Technology','Software',4.8),('VGVF','NVDA','NVIDIA','United States','Technology','Semiconductors',4.0),('VGVF','NOVN','Novartis','Switzerland','Healthcare','Drug Manufacturers',1.1),('VGVF','ASML','ASML','Netherlands','Technology','Semiconductor Equipment',1.0),
  ('EXXY','NESN','Nestle','Switzerland','Consumer Defensive','Food Products',3.2),('EXXY','ASML','ASML','Netherlands','Technology','Semiconductor Equipment',2.8),('EXXY','NOVN','Novartis','Switzerland','Healthcare','Drug Manufacturers',2.4),('EXXY','SAP','SAP','Germany','Technology','Software',2.1),('EXXY','SHEL','Shell','United Kingdom','Energy','Oil and Gas',2.0),
  ('SPYD','XOM','Exxon Mobil','United States','Energy','Oil and Gas',2.5),('SPYD','T','AT&T','United States','Communication Services','Telecom Services',2.2),('SPYD','O','Realty Income','United States','Real Estate','REITs',2.0),('SPYD','IBM','IBM','United States','Technology','Information Technology Services',1.8),('SPYD','KO','Coca-Cola','United States','Consumer Defensive','Beverages',1.7),
  ('AYEW','UST','US Treasury','United States','Government Bonds','Sovereign Debt',12.0),('AYEW','JGB','Japan Government Bond','Japan','Government Bonds','Sovereign Debt',5.5),('AYEW','BUND','Germany Federal Bond','Germany','Government Bonds','Sovereign Debt',4.4),('AYEW','FRTR','France Government Bond','France','Government Bonds','Sovereign Debt',3.2),('AYEW','IGCORP','Global Investment Grade Credit','Global','Corporate Bonds','Investment Grade Credit',18.0),
  ('IUSN','SMCI','Super Micro Computer','United States','Technology','Computer Hardware',0.35),('IUSN','FIX','Comfort Systems','United States','Industrials','Engineering and Construction',0.30),('IUSN','SAAB','Saab','Sweden','Industrials','Aerospace and Defense',0.25),('IUSN','FN','Fabrinet','United States','Technology','Electronic Components',0.24),('IUSN','RBC','RBC Bearings','United States','Industrials','Industrial Machinery',0.22),
  ('NUKL','CCJ','Cameco','Canada','Energy','Uranium',12.0),('NUKL','KAP','Kazatomprom','Kazakhstan','Energy','Uranium',9.5),('NUKL','CEG','Constellation Energy','United States','Utilities','Renewable Utilities',8.0),('NUKL','BWXT','BWX Technologies','United States','Industrials','Aerospace and Defense',6.5),('NUKL','UEC','Uranium Energy','United States','Energy','Uranium',4.5),
  ('CBUK','AZN','AstraZeneca','United Kingdom','Healthcare','Drug Manufacturers',8.0),('CBUK','SHEL','Shell','United Kingdom','Energy','Oil and Gas',7.5),('CBUK','HSBA','HSBC','United Kingdom','Financial Services','Banks',6.2),('CBUK','ULVR','Unilever','United Kingdom','Consumer Defensive','Household Products',5.0),('CBUK','BP','BP','United Kingdom','Energy','Oil and Gas',4.2)
) AS x(ticker, symbol, name, country, sector, industry, weight) ON x.ticker = e.ticker;
