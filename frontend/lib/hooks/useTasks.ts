import { useCallback } from 'react';
import { useDashboardStore } from '../stores/useDashboardStore';
import type { TaskItem } from '../types/dashboard';

export function useTasks() {
  const { tasks, tickProgress } = useDashboardStore();

  const runningTasks = tasks.filter((task: TaskItem) => task.status === 'running');
  const queuedTasks = tasks.filter((task: TaskItem) => task.status === 'queued');
  const completedTasks = tasks.filter((task: TaskItem) =>
    task.status === 'done' || task.status === 'failed'
  );
  const failedTasks = tasks.filter((task: TaskItem) => task.status === 'failed');

  const hasActiveTasks = runningTasks.length > 0 || queuedTasks.length > 0;

  return {
    tasks,
    runningTasks,
    queuedTasks,
    completedTasks,
    failedTasks,
    hasActiveTasks,
    tickProgress,
  };
}
