DELETE FROM etf_holdings
WHERE etf_id IN (SELECT id FROM etfs WHERE ticker IN ('EEMA','ICOM','XDWT','CQQQ'));

DELETE FROM etf_exposures
WHERE etf_id IN (SELECT id FROM etfs WHERE ticker IN ('EEMA','ICOM','XDWT','CQQQ'));

INSERT INTO etfs (id, created_at, updated_at, ticker, isin, name, provider, ter, assets_under_management, currency, asset_class, region, distribution_policy, benchmark_index, risk_level, last_price, performance_1y, performance_3y, performance_5y)
VALUES
  (gen_random_uuid(), now(), now(), 'EEMA', 'IE00B5L8K969', 'iShares MSCI EM Asia UCITS ETF', 'iShares', 0.2000, 3400000000, 'USD', 'Equity', 'Emerging Asia', 'ACCUMULATING', 'MSCI EM Asia', 5, 70.20, 13.1000, 8.4000, 30.2000),
  (gen_random_uuid(), now(), now(), 'ICOM', 'IE00BDFL4P12', 'iShares Diversified Commodity Swap UCITS ETF', 'iShares', 0.1900, 2100000000, 'USD', 'Commodity', 'Global', 'ACCUMULATING', 'Bloomberg Commodity', 4, 6.85, 2.8000, 17.3000, 39.0000),
  (gen_random_uuid(), now(), now(), 'XDWT', 'IE00BM67HT60', 'Xtrackers MSCI World Information Technology UCITS ETF', 'Xtrackers', 0.2500, 5200000000, 'USD', 'Equity', 'Developed Markets', 'ACCUMULATING', 'MSCI World Information Technology', 5, 108.40, 31.2000, 68.5000, 142.3000),
  (gen_random_uuid(), now(), now(), 'CQQQ', 'IE00BMMV5105', 'Invesco China Technology UCITS ETF', 'Invesco', 0.7000, 980000000, 'USD', 'Equity', 'China', 'ACCUMULATING', 'FTSE China Incl A 25% Technology Capped', 5, 38.10, 12.9000, -14.5000, -5.3000)
ON CONFLICT (ticker) DO UPDATE SET
  updated_at = now(),
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
  ('EEMA','COUNTRY','China',28.0),('EEMA','COUNTRY','India',23.0),('EEMA','COUNTRY','Taiwan',21.0),('EEMA','COUNTRY','South Korea',14.0),('EEMA','COUNTRY','Other Emerging Asia',14.0),
  ('EEMA','SECTOR','Technology',25.0),('EEMA','SECTOR','Financial Services',20.0),('EEMA','SECTOR','Consumer Cyclical',14.0),('EEMA','SECTOR','Communication Services',10.0),('EEMA','SECTOR','Industrials',7.0),('EEMA','SECTOR','Other',24.0),
  ('EEMA','INDUSTRY','Semiconductors',13.0),('EEMA','INDUSTRY','Banks',10.0),('EEMA','INDUSTRY','Internet Retail',7.0),('EEMA','INDUSTRY','Software',5.0),('EEMA','INDUSTRY','Insurance',4.0),('EEMA','INDUSTRY','Other',61.0),
  ('EEMA','CURRENCY','USD',36.0),('EEMA','CURRENCY','TWD',21.0),('EEMA','CURRENCY','INR',18.0),('EEMA','CURRENCY','HKD',13.0),('EEMA','CURRENCY','Other',12.0),('EEMA','ASSET_CLASS','Equity',100.0),

  ('ICOM','COUNTRY','Global Commodity Futures',100.0),('ICOM','SECTOR','Energy Commodities',34.0),('ICOM','SECTOR','Agriculture',28.0),('ICOM','SECTOR','Industrial Metals',18.0),('ICOM','SECTOR','Precious Metals',16.0),('ICOM','SECTOR','Livestock',4.0),
  ('ICOM','INDUSTRY','Oil and Gas Futures',25.0),('ICOM','INDUSTRY','Grains',16.0),('ICOM','INDUSTRY','Gold',11.0),('ICOM','INDUSTRY','Copper',8.0),('ICOM','INDUSTRY','Natural Gas',7.0),('ICOM','INDUSTRY','Other Commodities',33.0),
  ('ICOM','CURRENCY','USD',100.0),('ICOM','ASSET_CLASS','Commodity',100.0),

  ('XDWT','COUNTRY','United States',86.0),('XDWT','COUNTRY','Europe',8.0),('XDWT','COUNTRY','Japan',3.0),('XDWT','COUNTRY','Other Developed',3.0),
  ('XDWT','SECTOR','Technology',100.0),
  ('XDWT','INDUSTRY','Semiconductors',32.0),('XDWT','INDUSTRY','Software',27.0),('XDWT','INDUSTRY','Consumer Electronics',14.0),('XDWT','INDUSTRY','Information Technology Services',10.0),('XDWT','INDUSTRY','Semiconductor Equipment',7.0),('XDWT','INDUSTRY','Other Technology',10.0),
  ('XDWT','CURRENCY','USD',87.0),('XDWT','CURRENCY','EUR',6.0),('XDWT','CURRENCY','JPY',3.0),('XDWT','CURRENCY','Other',4.0),('XDWT','ASSET_CLASS','Equity',100.0),

  ('CQQQ','COUNTRY','China',88.0),('CQQQ','COUNTRY','Taiwan',4.0),('CQQQ','COUNTRY','United States',3.0),('CQQQ','COUNTRY','Other',5.0),
  ('CQQQ','SECTOR','Technology',42.0),('CQQQ','SECTOR','Communication Services',25.0),('CQQQ','SECTOR','Consumer Cyclical',18.0),('CQQQ','SECTOR','Industrials',6.0),('CQQQ','SECTOR','Other',9.0),
  ('CQQQ','INDUSTRY','Internet Content',18.0),('CQQQ','INDUSTRY','Internet Retail',17.0),('CQQQ','INDUSTRY','Software',12.0),('CQQQ','INDUSTRY','Semiconductors',9.0),('CQQQ','INDUSTRY','Electronic Components',6.0),('CQQQ','INDUSTRY','Other',38.0),
  ('CQQQ','CURRENCY','HKD',53.0),('CQQQ','CURRENCY','CNY',28.0),('CQQQ','CURRENCY','USD',14.0),('CQQQ','CURRENCY','Other',5.0),('CQQQ','ASSET_CLASS','Equity',100.0)
) AS x(ticker, type, name, weight) ON x.ticker = e.ticker;

INSERT INTO etf_holdings (id, created_at, updated_at, etf_id, symbol, name, country, sector, industry, weight)
SELECT gen_random_uuid(), now(), now(), e.id, x.symbol, x.name, x.country, x.sector, x.industry, x.weight
FROM etfs e
JOIN (VALUES
  ('EEMA','TSM','Taiwan Semiconductor','Taiwan','Technology','Semiconductors',8.2),('EEMA','TCEHY','Tencent','China','Communication Services','Internet Content',4.4),('EEMA','BABA','Alibaba','China','Consumer Cyclical','Internet Retail',3.1),('EEMA','RELIANCE','Reliance Industries','India','Energy','Oil and Gas',2.4),('EEMA','SAMSUNG','Samsung Electronics','South Korea','Technology','Consumer Electronics',3.5),
  ('ICOM','WTI','WTI Crude Oil Futures','Global','Energy Commodities','Oil and Gas Futures',10.0),('ICOM','GOLD','Gold Futures','Global','Precious Metals','Gold',11.0),('ICOM','CORN','Corn Futures','Global','Agriculture','Grains',5.0),('ICOM','COPPER','Copper Futures','Global','Industrial Metals','Copper',8.0),('ICOM','NG','Natural Gas Futures','Global','Energy Commodities','Natural Gas',7.0),
  ('XDWT','AAPL','Apple','United States','Technology','Consumer Electronics',18.0),('XDWT','MSFT','Microsoft','United States','Technology','Software',17.0),('XDWT','NVDA','NVIDIA','United States','Technology','Semiconductors',14.0),('XDWT','AVGO','Broadcom','United States','Technology','Semiconductors',5.8),('XDWT','ASML','ASML','Netherlands','Technology','Semiconductor Equipment',3.2),
  ('CQQQ','TCEHY','Tencent','China','Communication Services','Internet Content',10.0),('CQQQ','BABA','Alibaba','China','Consumer Cyclical','Internet Retail',8.0),('CQQQ','JD','JD.com','China','Consumer Cyclical','Internet Retail',5.0),('CQQQ','BIDU','Baidu','China','Communication Services','Internet Content',4.0),('CQQQ','XIAOMI','Xiaomi','China','Technology','Consumer Electronics',4.0)
) AS x(ticker, symbol, name, country, sector, industry, weight) ON x.ticker = e.ticker;
