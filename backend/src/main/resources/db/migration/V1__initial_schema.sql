CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE app_users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  email varchar(180) NOT NULL UNIQUE,
  password_hash varchar(255) NOT NULL,
  display_name varchar(120) NOT NULL,
  active boolean NOT NULL
);

CREATE TABLE user_roles (
  user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  role varchar(40) NOT NULL,
  PRIMARY KEY (user_id, role)
);

CREATE TABLE etfs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  ticker varchar(32) NOT NULL UNIQUE,
  isin varchar(32) UNIQUE,
  name varchar(240) NOT NULL,
  provider varchar(120),
  ter numeric(8,4),
  assets_under_management numeric(20,2),
  currency varchar(12),
  asset_class varchar(80),
  region varchar(80),
  distribution_policy varchar(32) NOT NULL,
  benchmark_index varchar(180),
  risk_level integer,
  last_price numeric(20,6),
  last_price_at timestamptz,
  performance_1y numeric(10,4),
  performance_3y numeric(10,4),
  performance_5y numeric(10,4)
);

CREATE INDEX idx_etfs_ticker ON etfs(ticker);

CREATE TABLE etf_exposures (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  etf_id uuid NOT NULL REFERENCES etfs(id) ON DELETE CASCADE,
  type varchar(40) NOT NULL,
  name varchar(140) NOT NULL,
  weight numeric(9,4) NOT NULL
);

CREATE INDEX idx_etf_exposures_etf_type ON etf_exposures(etf_id, type);

CREATE TABLE etf_holdings (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  etf_id uuid NOT NULL REFERENCES etfs(id) ON DELETE CASCADE,
  symbol varchar(40) NOT NULL,
  name varchar(180) NOT NULL,
  country varchar(80),
  sector varchar(100),
  industry varchar(120),
  weight numeric(9,4) NOT NULL
);

CREATE INDEX idx_etf_holdings_etf_weight ON etf_holdings(etf_id, weight DESC);

CREATE TABLE portfolios (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  name varchar(120) NOT NULL,
  base_currency varchar(12) NOT NULL
);

CREATE INDEX idx_portfolios_user ON portfolios(user_id);

CREATE TABLE positions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  portfolio_id uuid NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
  etf_id uuid NOT NULL REFERENCES etfs(id),
  broker varchar(60) NOT NULL,
  external_symbol varchar(80),
  quantity numeric(24,8) NOT NULL,
  average_cost numeric(20,6) NOT NULL,
  current_price numeric(20,6) NOT NULL,
  currency varchar(12) NOT NULL
);

CREATE INDEX idx_positions_portfolio ON positions(portfolio_id);

CREATE INDEX idx_positions_etf ON positions(etf_id);

CREATE TABLE investment_transactions (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  position_id uuid NOT NULL REFERENCES positions(id) ON DELETE CASCADE,
  type varchar(40) NOT NULL,
  transaction_date date NOT NULL,
  quantity numeric(24,8),
  price numeric(20,6),
  fees numeric(20,6),
  currency varchar(12) NOT NULL
);

CREATE INDEX idx_transactions_position_date ON investment_transactions(position_id, transaction_date);

CREATE TABLE import_jobs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  created_at timestamptz NOT NULL,
  updated_at timestamptz NOT NULL,
  user_id uuid NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
  portfolio_id uuid NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
  broker varchar(60) NOT NULL,
  status varchar(40) NOT NULL,
  file_name varchar(255) NOT NULL,
  imported_positions integer NOT NULL,
  error_message varchar(1000)
);

CREATE INDEX idx_import_jobs_user ON import_jobs(user_id, created_at DESC);
