import { useState } from 'react';
import { useQuery } from 'react-query';
import { aiInsightsAPI } from '../../services/api';
import Spinner from '../../components/common/Spinner';
import {
  BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid,
  PieChart, Pie, Cell, LineChart, Line, ResponsiveContainer, Legend
} from 'recharts';
import { FiAlertTriangle, FiTrendingUp, FiUsers, FiAward, FiStar, FiShield } from 'react-icons/fi';

const COLORS = ['#2563eb','#7c3aed','#059669','#d97706','#dc2626','#0891b2','#db2777'];

export default function AIInsights() {
  const [tab, setTab] = useState('platform');

  const { data: platform, isLoading: pLoading } = useQuery(
    'ai-insights-platform',
    () => aiInsightsAPI.platform().then(r => r.data?.data),
    { staleTime: 10 * 60 * 1000 }
  );
  const { data: fraud, isLoading: fLoading } = useQuery(
    'ai-insights-fraud',
    () => aiInsightsAPI.fraud().then(r => r.data?.data),
    { enabled: tab === 'fraud', staleTime: 5 * 60 * 1000 }
  );

  const tabs = [
    { key: 'platform', label: 'Platform Insights', icon: FiTrendingUp },
    { key: 'fraud',    label: 'Fraud Detection',   icon: FiShield },
  ];

  const monthlyData = platform?.eventGrowthByMonth
    ? Object.entries(platform.eventGrowthByMonth)
        .sort(([a], [b]) => a.localeCompare(b))
        .map(([month, count]) => ({ month, events: count }))
    : [];

  const deptData = platform?.departmentParticipation
    ? Object.entries(platform.departmentParticipation)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 8)
        .map(([name, value]) => ({ name: name.length > 20 ? name.slice(0, 18) + '…' : name, value }))
    : [];

  const ratingData = platform?.ratingDistribution
    ? Object.entries(platform.ratingDistribution)
        .sort(([a], [b]) => Number(a) - Number(b))
        .map(([star, count]) => ({ star: `${star}★`, count }))
    : [];

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-8">
      <div className="mx-auto max-w-6xl">
        <div className="mb-6">
          <p className="text-xs font-black uppercase tracking-widest text-purple-600">Admin</p>
          <h1 className="text-2xl font-extrabold text-slate-900" style={{ fontFamily: 'Space Grotesk,sans-serif' }}>
            Platform Insights Dashboard
          </h1>
        </div>

        {/* Tabs */}
        <div className="flex gap-2 mb-6 border-b border-slate-200">
          {tabs.map(t => {
            const Icon = t.icon;
            return (
              <button key={t.key} onClick={() => setTab(t.key)}
                className={`flex items-center gap-2 pb-3 px-4 text-sm font-semibold border-b-2 transition-colors ${
                  tab === t.key ? 'border-purple-600 text-purple-700' : 'border-transparent text-slate-500 hover:text-slate-700'
                }`}>
                <Icon className="h-4 w-4" />{t.label}
              </button>
            );
          })}
        </div>

        {/* Platform tab */}
        {tab === 'platform' && (
          pLoading ? <div className="flex justify-center py-20"><Spinner /></div> :
          !platform ? <p className="text-slate-400 text-center py-20">No data available.</p> :
          <div className="space-y-6">
            {/* KPI cards */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              {[
                { label: 'Total Events',  value: platform.totalEvents,  icon: FiTrendingUp, color: 'blue' },
                { label: 'Total Users',   value: platform.totalUsers,   icon: FiUsers,      color: 'purple' },
                { label: 'Top Events',    value: platform.topRatedEvents?.length || 0, icon: FiStar, color: 'amber' },
                { label: 'Departments',   value: deptData.length,       icon: FiAward,      color: 'green' },
              ].map(s => (
                <div key={s.label} className="rounded-xl border border-slate-200 bg-white p-4">
                  <p className="text-2xl font-extrabold text-slate-900">{s.value}</p>
                  <p className="text-xs text-slate-500 mt-0.5">{s.label}</p>
                </div>
              ))}
            </div>

            {/* Charts */}
            <div className="grid gap-5 md:grid-cols-2">
              {monthlyData.length > 0 && (
                <div className="rounded-xl border border-slate-200 bg-white p-5">
                  <h3 className="font-bold text-slate-900 text-sm mb-4">Event Growth (12 months)</h3>
                  <ResponsiveContainer width="100%" height={200}>
                    <LineChart data={monthlyData}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="month" tick={{ fontSize: 10 }} />
                      <YAxis tick={{ fontSize: 10 }} />
                      <Tooltip />
                      <Line type="monotone" dataKey="events" stroke="#7c3aed" strokeWidth={2} dot={{ r: 3 }} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              )}

              {deptData.length > 0 && (
                <div className="rounded-xl border border-slate-200 bg-white p-5">
                  <h3 className="font-bold text-slate-900 text-sm mb-4">Department Participation</h3>
                  <ResponsiveContainer width="100%" height={200}>
                    <BarChart data={deptData} layout="vertical">
                      <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                      <XAxis type="number" tick={{ fontSize: 10 }} />
                      <YAxis type="category" dataKey="name" tick={{ fontSize: 9 }} width={110} />
                      <Tooltip />
                      <Bar dataKey="value" fill="#2563eb" radius={[0, 4, 4, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}

              {ratingData.length > 0 && (
                <div className="rounded-xl border border-slate-200 bg-white p-5">
                  <h3 className="font-bold text-slate-900 text-sm mb-4">Rating Distribution</h3>
                  <ResponsiveContainer width="100%" height={200}>
                    <BarChart data={ratingData}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} />
                      <XAxis dataKey="star" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 10 }} />
                      <Tooltip />
                      <Bar dataKey="count" fill="#d97706" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                </div>
              )}

              {/* College leaderboard */}
              {platform.collegeLeaderboard?.length > 0 && (
                <div className="rounded-xl border border-slate-200 bg-white p-5">
                  <h3 className="font-bold text-slate-900 text-sm mb-4">College Leaderboard</h3>
                  <div className="space-y-2 max-h-52 overflow-y-auto">
                    {platform.collegeLeaderboard.map((c, i) => (
                      <div key={i} className="flex items-center justify-between text-sm rounded-lg bg-slate-50 px-3 py-2">
                        <span className="flex items-center gap-2">
                          <span className="font-bold text-slate-400 w-5">{i + 1}</span>
                          <span className="text-slate-700 truncate max-w-[160px]">{c.college}</span>
                        </span>
                        <span className="font-bold text-blue-700">{c.participants}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Fraud tab */}
        {tab === 'fraud' && (
          fLoading ? <div className="flex justify-center py-20"><Spinner /></div> :
          !fraud ? <p className="text-slate-400 text-center py-20">No data available.</p> :
          <div className="space-y-5">
            {/* Risk KPIs */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              {[
                ['Suspicious Accounts', fraud.suspiciousAccountCount, 'rose'],
                ['Bot Bookings',        fraud.botBookingCount,        'orange'],
                ['Fake Reviews',        fraud.fakeReviewCount,        'yellow'],
                ['Overall Risk',        `${fraud.riskScore || 0}/100`, 'red'],
              ].map(([label, val, color]) => (
                <div key={label} className={`rounded-xl border p-4 ${
                  color === 'red' ? 'border-red-200 bg-red-50' : 'border-slate-200 bg-white'
                }`}>
                  <p className={`text-2xl font-extrabold ${color === 'red' ? 'text-red-700' : 'text-slate-900'}`}>{val}</p>
                  <p className="text-xs text-slate-500 mt-0.5">{label}</p>
                </div>
              ))}
            </div>

            {/* Fake reviews */}
            {fraud.fakeReviews?.length > 0 && (
              <div className="rounded-xl border border-yellow-200 bg-yellow-50 p-5">
                <h3 className="font-bold text-yellow-900 flex items-center gap-2 mb-3 text-sm">
                  <FiAlertTriangle /> Flagged Reviews ({fraud.fakeReviews.length})
                </h3>
                <div className="space-y-2">
                  {fraud.fakeReviews.slice(0, 10).map(r => (
                    <div key={r.ratingId} className="rounded-lg bg-white border border-yellow-100 px-4 py-2 text-xs flex gap-4">
                      <span className="text-slate-500">Review #{r.ratingId}</span>
                      <span className="text-slate-600">User: {r.userId}</span>
                      <span className="font-semibold text-amber-700">{r.rating}★</span>
                      <span className={`font-semibold ${r.verified ? 'text-green-600' : 'text-red-500'}`}>
                        {r.verified ? 'Verified' : 'Unverified'}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {fraud.riskScore === 0 && fraud.fakeReviews?.length === 0 && (
              <div className="rounded-xl border border-green-200 bg-green-50 py-12 text-center">
                <FiShield className="mx-auto h-10 w-10 text-green-500 mb-3" />
                <p className="font-semibold text-green-800">No fraud signals detected</p>
                <p className="text-sm text-green-600 mt-1">Platform looks healthy. Scan runs daily at 2 AM.</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
