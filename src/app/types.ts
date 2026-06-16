export interface ETF {
  ticker: string;
  name: string;
  provider: string | null;
  ter: number | null;
  assetClass: string | null;
  region: string | null;
  indexTracked: string | null;
  distributionPolicy: 'Accumulating' | 'Distributing' | null;
  fundSize: number | null;
  currency: string | null;
  riskLevel: number | null;
  performance: {
    '1Y': number | null;
    '3Y': number | null;
    '5Y': number | null;
  };
  quote: {
    currentPrice: number | null;
    change: number | null;
    changePercent: number | null;
    dayHigh: number | null;
    dayLow: number | null;
    open: number | null;
    previousClose: number | null;
  };
}
