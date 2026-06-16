INSERT INTO etfs (id, created_at, updated_at, ticker, isin, name, provider, ter, assets_under_management, currency, asset_class, region, distribution_policy, benchmark_index, risk_level, last_price, performance_1y, performance_3y, performance_5y)
VALUES
  (gen_random_uuid(), now(), now(), 'VWCE', 'IE00BK5BQT80', 'Vanguard FTSE All-World UCITS ETF Acc', 'Vanguard', 0.2200, 18500000000, 'EUR', 'Equity', 'Global', 'ACCUMULATING', 'FTSE All-World', 4, 128.40, 18.2000, 36.7000, 72.3000),
  (gen_random_uuid(), now(), now(), 'IWDA', 'IE00B4L5Y983', 'iShares Core MSCI World UCITS ETF', 'iShares', 0.2000, 72000000000, 'EUR', 'Equity', 'Developed Markets', 'ACCUMULATING', 'MSCI World', 4, 96.80, 17.1000, 35.9000, 75.8000),
  (gen_random_uuid(), now(), now(), 'EIMI', 'IE00BKM4GZ66', 'iShares Core MSCI EM IMI UCITS ETF', 'iShares', 0.1800, 21500000000, 'EUR', 'Equity', 'Emerging Markets', 'ACCUMULATING', 'MSCI Emerging Markets IMI', 5, 32.10, 10.4000, 12.8000, 28.6000),
  (gen_random_uuid(), now(), now(), 'VUSA', 'IE00B3XXRP09', 'Vanguard S&P 500 UCITS ETF', 'Vanguard', 0.0700, 42000000000, 'EUR', 'Equity', 'United States', 'DISTRIBUTING', 'S&P 500', 4, 103.20, 21.6000, 44.9000, 91.1000),
  (gen_random_uuid(), now(), now(), 'AGGH', 'IE00BDBRDM35', 'iShares Core Global Aggregate Bond UCITS ETF', 'iShares', 0.1000, 12800000000, 'EUR', 'Fixed Income', 'Global', 'ACCUMULATING', 'Bloomberg Global Aggregate Bond', 2, 5.25, 4.2000, -3.6000, -5.1000)
ON CONFLICT (ticker) DO NOTHING;

INSERT INTO etf_exposures (id, created_at, updated_at, etf_id, type, name, weight)
SELECT gen_random_uuid(), now(), now(), e.id, x.type, x.name, x.weight
FROM etfs e
JOIN (VALUES
  ('VWCE','COUNTRY','United States',62.0),('VWCE','COUNTRY','Europe',18.0),('VWCE','COUNTRY','Japan',6.0),('VWCE','COUNTRY','Emerging Markets',10.0),('VWCE','COUNTRY','Other',4.0),
  ('VWCE','SECTOR','Technology',24.0),('VWCE','SECTOR','Financial Services',15.0),('VWCE','SECTOR','Healthcare',12.5),('VWCE','SECTOR','Industrials',10.0),('VWCE','SECTOR','Consumer Defensive',7.0),('VWCE','SECTOR','Other',31.5),
  ('VWCE','INDUSTRY','Semiconductors',8.0),('VWCE','INDUSTRY','Software',7.5),('VWCE','INDUSTRY','Banks',7.0),('VWCE','INDUSTRY','Drug Manufacturers',5.5),('VWCE','INDUSTRY','Other',72.0),
  ('VWCE','CURRENCY','USD',64.0),('VWCE','CURRENCY','EUR',13.0),('VWCE','CURRENCY','JPY',6.0),('VWCE','CURRENCY','GBP',4.0),('VWCE','CURRENCY','Other',13.0),('VWCE','ASSET_CLASS','Equity',100.0),
  ('IWDA','COUNTRY','United States',70.0),('IWDA','COUNTRY','Europe',17.0),('IWDA','COUNTRY','Japan',6.0),('IWDA','COUNTRY','Other Developed',7.0),
  ('IWDA','SECTOR','Technology',26.0),('IWDA','SECTOR','Financial Services',14.0),('IWDA','SECTOR','Healthcare',13.0),('IWDA','SECTOR','Industrials',11.0),('IWDA','SECTOR','Other',36.0),
  ('IWDA','INDUSTRY','Software',8.5),('IWDA','INDUSTRY','Semiconductors',8.0),('IWDA','INDUSTRY','Banks',6.0),('IWDA','INDUSTRY','Other',77.5),
  ('IWDA','CURRENCY','USD',71.0),('IWDA','CURRENCY','EUR',10.0),('IWDA','CURRENCY','JPY',6.0),('IWDA','CURRENCY','Other',13.0),('IWDA','ASSET_CLASS','Equity',100.0),
  ('EIMI','COUNTRY','China',25.0),('EIMI','COUNTRY','India',20.0),('EIMI','COUNTRY','Taiwan',18.0),('EIMI','COUNTRY','South Korea',12.0),('EIMI','COUNTRY','Other Emerging Markets',25.0),
  ('EIMI','SECTOR','Technology',22.0),('EIMI','SECTOR','Financial Services',21.0),('EIMI','SECTOR','Consumer Cyclical',13.0),('EIMI','SECTOR','Communication Services',10.0),('EIMI','SECTOR','Other',34.0),
  ('EIMI','INDUSTRY','Semiconductors',12.0),('EIMI','INDUSTRY','Banks',11.0),('EIMI','INDUSTRY','Internet Retail',7.0),('EIMI','INDUSTRY','Other',70.0),
  ('EIMI','CURRENCY','USD',38.0),('EIMI','CURRENCY','TWD',18.0),('EIMI','CURRENCY','INR',15.0),('EIMI','CURRENCY','HKD',12.0),('EIMI','CURRENCY','Other',17.0),('EIMI','ASSET_CLASS','Equity',100.0),
  ('VUSA','COUNTRY','United States',100.0),('VUSA','SECTOR','Technology',31.0),('VUSA','SECTOR','Healthcare',12.0),('VUSA','SECTOR','Financial Services',13.0),('VUSA','SECTOR','Consumer Cyclical',10.0),('VUSA','SECTOR','Other',34.0),
  ('VUSA','INDUSTRY','Software',11.0),('VUSA','INDUSTRY','Semiconductors',10.0),('VUSA','INDUSTRY','Drug Manufacturers',5.0),('VUSA','INDUSTRY','Other',74.0),('VUSA','CURRENCY','USD',100.0),('VUSA','ASSET_CLASS','Equity',100.0),
  ('AGGH','COUNTRY','United States',39.0),('AGGH','COUNTRY','Europe',28.0),('AGGH','COUNTRY','Japan',12.0),('AGGH','COUNTRY','Other',21.0),('AGGH','SECTOR','Government Bonds',54.0),('AGGH','SECTOR','Corporate Bonds',28.0),('AGGH','SECTOR','Securitized',12.0),('AGGH','SECTOR','Other',6.0),
  ('AGGH','INDUSTRY','Sovereign Debt',54.0),('AGGH','INDUSTRY','Investment Grade Credit',28.0),('AGGH','INDUSTRY','Other',18.0),('AGGH','CURRENCY','USD',45.0),('AGGH','CURRENCY','EUR',25.0),('AGGH','CURRENCY','JPY',12.0),('AGGH','CURRENCY','Other',18.0),('AGGH','ASSET_CLASS','Fixed Income',100.0)
) AS x(ticker, type, name, weight) ON x.ticker = e.ticker;

INSERT INTO etf_holdings (id, created_at, updated_at, etf_id, symbol, name, country, sector, industry, weight)
SELECT gen_random_uuid(), now(), now(), e.id, x.symbol, x.name, x.country, x.sector, x.industry, x.weight
FROM etfs e
JOIN (VALUES
  ('VWCE','AAPL','Apple','United States','Technology','Consumer Electronics',4.1),('VWCE','MSFT','Microsoft','United States','Technology','Software',3.9),('VWCE','NVDA','NVIDIA','United States','Technology','Semiconductors',3.2),('VWCE','AMZN','Amazon','United States','Consumer Cyclical','Internet Retail',2.1),('VWCE','TSM','Taiwan Semiconductor','Taiwan','Technology','Semiconductors',1.4),
  ('IWDA','AAPL','Apple','United States','Technology','Consumer Electronics',5.0),('IWDA','MSFT','Microsoft','United States','Technology','Software',4.8),('IWDA','NVDA','NVIDIA','United States','Technology','Semiconductors',4.0),('IWDA','AMZN','Amazon','United States','Consumer Cyclical','Internet Retail',2.7),('IWDA','META','Meta Platforms','United States','Communication Services','Internet Content',1.8),
  ('EIMI','TSM','Taiwan Semiconductor','Taiwan','Technology','Semiconductors',6.3),('EIMI','TCEHY','Tencent','China','Communication Services','Internet Content',3.8),('EIMI','BABA','Alibaba','China','Consumer Cyclical','Internet Retail',2.4),('EIMI','RELIANCE','Reliance Industries','India','Energy','Oil and Gas',1.8),('EIMI','INFY','Infosys','India','Technology','Information Technology Services',1.2),
  ('VUSA','AAPL','Apple','United States','Technology','Consumer Electronics',7.1),('VUSA','MSFT','Microsoft','United States','Technology','Software',6.8),('VUSA','NVDA','NVIDIA','United States','Technology','Semiconductors',6.0),('VUSA','AMZN','Amazon','United States','Consumer Cyclical','Internet Retail',3.7),('VUSA','GOOGL','Alphabet','United States','Communication Services','Internet Content',2.2),
  ('AGGH','UST','US Treasury','United States','Government Bonds','Sovereign Debt',12.0),('AGGH','JGB','Japan Government Bond','Japan','Government Bonds','Sovereign Debt',5.5),('AGGH','BUND','Germany Federal Bond','Germany','Government Bonds','Sovereign Debt',4.4),('AGGH','FRTR','France Government Bond','France','Government Bonds','Sovereign Debt',3.2),('AGGH','IGCORP','Global Investment Grade Credit','Global','Corporate Bonds','Investment Grade Credit',18.0)
) AS x(ticker, symbol, name, country, sector, industry, weight) ON x.ticker = e.ticker;
