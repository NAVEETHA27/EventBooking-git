import { useEffect, useState } from 'react';
import { adminAPI } from '../../services/api';

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  useEffect(() => { adminAPI.dashboard().then((r) => setStats(r.data?.data ?? {})); }, []);
  const items = [
    ['Total Users', 'totalUsers'], ['Total Organizers', 'totalOrganizers'], ['Total Events', 'totalEvents'],
    ['Pending Approvals', 'pendingApprovals'], ['Total Revenue', 'totalRevenue'], ['Refund Requests', 'refundRequests'],
    ['Active Events', 'activeEvents'], ['Cancelled Events', 'cancelledEvents'],
  ];
  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <h1 className="text-3xl font-extrabold text-slate-950">Admin Dashboard</h1>
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {items.map(([label, key]) => (
          <div key={key} className="rounded-lg border border-slate-200 bg-white p-5">
            <p className="text-sm font-semibold text-slate-500">{label}</p>
            <p className="mt-2 text-2xl font-extrabold text-slate-950">{stats ? String(stats[key] ?? 0) : '...'}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
