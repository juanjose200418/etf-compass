INSERT INTO etfs (id, created_at, updated_at, ticker, isin, name, provider, ter, assets_under_management, currency, asset_class, region, distribution_policy, benchmark_index, risk_level, last_price, performance_1y, performance_3y, performance_5y)
VALUES
  (gen_random_uuid(), now(), now(), 'SPY', 'US78462F1030', 'SPDR S&P 500 ETF Trust', 'SPDR', 0.0945, 630000000000, 'USD', 'Equity', 'United States', 'DISTRIBUTING', 'S&P 500', null, null, null, null, null),
  (gen_random_uuid(), now(), now(), 'AGG', 'US4642872265', 'iShares Core U.S. Aggregate Bond ETF', 'iShares', 0.0300, 125000000000, 'USD', 'Fixed Income', 'United States', 'DISTRIBUTING', 'Bloomberg U.S. Aggregate Bond Index', null, null, null, null, null),
  (gen_random_uuid(), now(), now(), 'XLV', 'US81369Y2090', 'Health Care Select Sector SPDR Fund', 'SPDR', 0.0900, 36000000000, 'USD', 'Equity', 'United States', 'DISTRIBUTING', 'Health Care Select Sector Index', null, null, null, null, null),
  (gen_random_uuid(), now(), now(), 'DIA', 'US78467X1090', 'SPDR Dow Jones Industrial Average ETF Trust', 'SPDR', 0.1600, 34000000000, 'USD', 'Equity', 'United States', 'DISTRIBUTING', 'Dow Jones Industrial Average', null, null, null, null, null)
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
  last_price = null,
  performance_1y = null,
  performance_3y = null,
  performance_5y = null;
