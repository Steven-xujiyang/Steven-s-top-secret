import { create } from 'zustand';

interface BacktestJob {
  jobId: string;
  status: string;
  message: string;
  strategyName?: string;
  metrics?: Record<string, number>;
}

interface BacktestStore {
  jobs: BacktestJob[];
  runningJobId: string | null;
  setJobs: (jobs: BacktestJob[]) => void;
  addJob: (job: BacktestJob) => void;
  updateJob: (jobId: string, updates: Partial<BacktestJob>) => void;
  setRunningJobId: (jobId: string | null) => void;
}

export const useBacktestStore = create<BacktestStore>((set) => ({
  jobs: [],
  runningJobId: null,
  setJobs: (jobs) => set({ jobs }),
  addJob: (job) => set((state) => ({ jobs: [job, ...state.jobs] })),
  updateJob: (jobId, updates) =>
    set((state) => ({
      jobs: state.jobs.map((j) => (j.jobId === jobId ? { ...j, ...updates } : j)),
    })),
  setRunningJobId: (jobId) => set({ runningJobId: jobId }),
}));
