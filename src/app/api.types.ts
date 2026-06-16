export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface UserResponse {
  id: string;
  email: string;
  displayName: string;
  roles: string[];
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
  user: UserResponse;
}

export interface PositionResponse {
  id: string;
  ticker: string;
  name: string;
  broker: string;
  quantity: number;
  averageCost: number;
  currentPrice: number;
  investedCapital: number;
  currentValue: number;
  profitLoss: number;
  profitLossPercentage: number;
  currency: string;
}

export interface PortfolioResponse {
  id: string;
  name: string;
  baseCurrency: string;
  totalValue: number;
  totalInvested: number;
  positions: PositionResponse[];
}

export interface AllocationSliceResponse {
  label: string;
  value: number;
  percentage: number;
}

export interface ExposureMappingIssueResponse {
  ticker: string;
  name: string;
  portfolioPercentage: number;
  unclassifiedPortfolioPercentage: number;
  holdingCoveragePercentage: number;
  sourceType: string;
  reason: string;
}

export interface ExposureDataQualityResponse {
  coveragePercentage: number;
  unclassifiedPercentage: number;
  lookThroughCoveragePercentage: number;
  estimatedPercentage: number;
  affectedEtfs: ExposureMappingIssueResponse[];
}

export interface DashboardResponse {
  netWorth: number;
  totalInvestedCapital: number;
  totalProfitLoss: number;
  profitLossPercentage: number;
  portfolioCount: number;
  positionCount: number;
  assetAllocation: AllocationSliceResponse[];
  etfAllocation: AllocationSliceResponse[];
  industryAllocation: AllocationSliceResponse[];
  sectorAllocation: AllocationSliceResponse[];
  geographicAllocation: AllocationSliceResponse[];
  currencyAllocation: AllocationSliceResponse[];
  bestPerformingPositions: PositionResponse[];
  worstPerformingPositions: PositionResponse[];
}

export interface PortfolioAnalyticsResponse {
  totalPortfolioValue: number;
  totalInvestedCapital: number;
  totalProfitLoss: number;
  profitLossPercentage: number;
  portfolioAllocation: AllocationSliceResponse[];
  etfAllocation: AllocationSliceResponse[];
  countryExposure: AllocationSliceResponse[];
  sectorExposure: AllocationSliceResponse[];
  industryExposure: AllocationSliceResponse[];
  currencyExposure: AllocationSliceResponse[];
  countryDataQuality: ExposureDataQualityResponse;
  sectorDataQuality: ExposureDataQualityResponse;
  industryDataQuality: ExposureDataQualityResponse;
  topHoldingsExposure: Array<{ symbol: string; name: string; value: number; percentage: number }>;
  bestPerformingPositions: PositionResponse[];
  worstPerformingPositions: PositionResponse[];
}

export interface ImportJobResponse {
  id: string;
  portfolioId: string;
  broker: string;
  status: string;
  fileName: string;
  importedPositions: number;
  errorMessage?: string;
  createdAt: string;
}

export interface EtfPerformanceApiResponse {
  '1Y': number | null;
  '3Y': number | null;
  '5Y': number | null;
}

export interface QuoteSnapshotApiResponse {
  currentPrice: number | null;
  change: number | null;
  changePercent: number | null;
  dayHigh: number | null;
  dayLow: number | null;
  open: number | null;
  previousClose: number | null;
}

export interface EtfResponse {
  id: string;
  ticker: string;
  name: string;
  isin?: string;
  provider?: string;
  ter?: number | null;
  assetClass?: string;
  region?: string;
  indexTracked?: string;
  distributionPolicy?: 'Accumulating' | 'Distributing' | null;
  fundSize?: number | null;
  currency?: string;
  riskLevel?: number | null;
  performance: EtfPerformanceApiResponse;
  quote: QuoteSnapshotApiResponse;
}

export interface ExposureApiResponse {
  type: string;
  name: string;
  weight: number;
}

export interface HoldingApiResponse {
  symbol: string;
  name: string;
  country?: string;
  sector?: string;
  industry?: string;
  weight: number;
}

export interface EtfDetailApiResponse {
  etf: EtfResponse;
  exposures: ExposureApiResponse[];
  holdings: HoldingApiResponse[];
  warnings: string[];
}

export interface EtfCompareApiResponse {
  etfs: EtfResponse[];
  warnings: string[];
}
