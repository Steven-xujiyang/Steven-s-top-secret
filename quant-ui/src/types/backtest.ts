export interface BacktestJob {
  jobId: string;
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED' | 'CANCELLED';
  message: string;
  strategyName?: string;
  metrics?: BacktestMetrics;
  navCurve?: NavPoint[];
}

export interface BacktestMetrics {
  totalReturn: number;
  annualReturn: number;
  annualVolatility: number;
  sharpeRatio: number;
  maxDrawdown: number;
  calmarRatio: number;
  totalTrades: number;
  winRate: number;
  profitFactor: number;
  totalCommission: number;
}

export interface NavPoint {
  date: string;
  nav: number;
}

export interface OptimizationRequest {
  strategyClass: string;
  symbols: string[];
  startDate: string;
  endDate: string;
  algorithm: 'GRID_SEARCH' | 'GENETIC';
  paramSpace: Record<string, { min: number; max: number; step: number }>;
  objective: string;
}
