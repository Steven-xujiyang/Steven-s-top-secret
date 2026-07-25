import client from './client';

export interface BacktestRequest {
  strategyClass: string;
  symbols: string[];
  startDate: string;
  endDate: string;
  initialCapital: number;
  frequency?: string;
  params?: Record<string, unknown>;
}

export interface BacktestResponse {
  jobId: string;
  status: string;
  message: string;
  strategyName?: string;
  metrics?: Record<string, number>;
  navCurve?: Array<{ date: string; nav: number }>;
}

export const backtestApi = {
  submit: (req: BacktestRequest) => client.post<BacktestResponse>('/backtest', req),

  getStatus: (jobId: string) => client.get<BacktestResponse>(`/backtest/${jobId}`),

  getResult: (jobId: string) => client.get<BacktestResponse>(`/backtest/${jobId}/result`),

  listJobs: () => client.get<BacktestResponse[]>('/backtest/list'),

  cancel: (jobId: string) => client.delete(`/backtest/${jobId}`),
};
