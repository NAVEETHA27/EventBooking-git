import { useQuery } from 'react-query';
import { Link } from 'react-router-dom';
import { portfolioAPI, aiAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import Spinner from '../../components/common/Spinner';
import {
  PieChart, Pie, Cell, Tooltip, ResponsiveContainer,
  BarChart, Bar, XAxis, YAxis, CartesianGrid
} from 'recharts';
import { FiAward, FiStar, FiBookOpen, FiCheckCircle, FiDownload, FiZap, FiTrendingUp, FiTarget } from 'react-icons/fi';
import { MdSchool } from 'react-icons/md';

const COLORS = ['#2563eb', '#7c3aed', '#059669', '#d97706', '#dc2626', '#0891b2'];
const LEVELS = { 1: 'Beginner', 2: 'Explorer', 3: 'Achiever', 4: 'Champion', 5: 'Legend' };

export default function Portfolio() {
  const { user } = useAuth();
  const { data: portfolio, isLoading } = useQuery(
    'my-portfolio', () => portfolioAPI.my().then(r => r.data?.data), { staleTime: 5 * 60 * 1000 }
  );
  const { data: careerData } = useQuery(
    'career-guidance', () => aiAPI.careerGuidance().then(r => r.data?.data),
    { staleTime: 30 * 60 * 1000, retry: false }
  );

  if (isLoading) return <div className="flex justify-center py-20"><Spinner /></div>;
  if (!portfolio) return (
    <div className="text-center py-20">
      <MdSchool className="mx-auto h-12 w-12 text-slate-300 mb-3" />
      <p className="text-slate-500">Portfolio not available.</p>
    </div>
  );

  const { gamification, categoryDistribution = {}, certificates = [], aiSummary, skillsData = [] } = portfolio;
  const catData = Object.entries(categoryDistribution).map(([name, value]) => ({ name: name.replace(/_/g, ' '), value }));
  const badges = gamification?.badges || [];
  const level = LEVELS[gamification?.currentLevel] || 'Beginner';
  const progress = gamification?.progressToNextLevel || 0;

  return (
    <div className="mx-auto max-w-5xl px-4 py-8 space-y-6">
      {/* ── Hero header ─────────────────────────────────────── */}
      <div className="rounded-2xl bg-gradient-to-br from-blue-700 to-indigo-800 p-8 text-white">
        <div className="flex flex-col md:flex-row items-start md:items-center gap-6">
          <div className="h-20 w-20 shrink-0 rounded-2xl bg-white/20 flex items-center justify-center text-3xl font-extrabold">
            {portfolio.name?.[0]?.toUpperCase()}
          </div>
          <div className="flex-1 min-w-0">
            <h1 className="text-2xl font-extrabold" style={{ fontFamily: 'Space Grotesk,sans-serif' }}>{portfolio.name}</h1>
            {(portfolio.department || portfolio.college) && (
              <p className="text-blue-200 text-sm mt-0.5">{[portfolio.department, portfolio.college].filter(Boolean).join(' · ')}</p>
            )}
            {portfolio.careerGoal && <p className="text-blue-300 text-xs mt-1">🎯 {portfolio.careerGoal}</p>}
            <div className="mt-4 flex flex-wrap gap-5">
              {[
                { label: 'Registered', value: portfolio.totalRegistrations ?? 0 },
                { label: 'Confirmed', value: portfolio.confirmedBookings ?? 0 },
                { label: 'Certificates', value: portfolio.certificatesEarned ?? 0 },
                { label: 'Reviews', value: portfolio.reviewsGiven ?? 0 },
                { label: 'Level', value: level },
                { label: 'XP', value: gamification?.totalXp ?? 0 },
              ].map(s => (
                <div key={s.label} className="text-center">
                  <p className="text-xl font-bold">{s.value}</p>
                  <p className="text-[10px] text-blue-200 uppercase tracking-wide">{s.label}</p>
                </div>
              ))}
            </div>
          </div>
          <button onClick={() => window.print()}
            className="shrink-0 flex items-center gap-2 rounded-xl bg-white/20 hover:bg-white/30 px-4 py-2 text-sm font-semibold transition-colors">
            <FiDownload className="h-4 w-4" /> Export PDF
          </button>
        </div>
      </div>

      {/* ── AI Summary ─────────────────────────────────────── */}
      {aiSummary && (
        <div className="rounded-xl border border-blue-200 bg-blue-50 p-5">
          <h2 className="flex items-center gap-2 font-bold text-blue-900 mb-2 text-sm">
            <FiStar className="text-blue-600" /> Profile Summary
          </h2>
          <p className="text-sm text-blue-800 leading-relaxed">{aiSummary}</p>
        </div>
      )}

      {/* ── XP Progress ─────────────────────────────────────── */}
      {gamification && (
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <h3 className="font-bold text-slate-900 flex items-center gap-2 mb-4 text-sm">
            <FiAward className="text-amber-500" /> Level Progress
          </h3>
          <div className="flex items-center gap-4 mb-4">
            <div className="flex-1">
              <div className="flex justify-between text-xs mb-1">
                <span className="font-semibold text-slate-700">Level {gamification.currentLevel} — {level}</span>
                <span className="text-slate-400">{gamification.totalXp} XP</span>
              </div>
              <div className="h-3 rounded-full bg-slate-100 overflow-hidden">
                <div className="h-3 rounded-full bg-gradient-to-r from-blue-500 to-indigo-600 transition-all duration-700"
                  style={{ width: `${progress}%` }} />
              </div>
              <p className="mt-1 text-xs text-slate-400">{gamification.nextLevelXp} XP to next level</p>
            </div>
          </div>
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            {[
              { label: 'Events Attended', value: gamification.eventsAttended },
              { label: 'Events Registered', value: gamification.eventsRegistered },
              { label: 'Reviews Given', value: gamification.reviewsGiven },
              { label: 'Certificates', value: gamification.certificatesEarned },
            ].map(s => (
              <div key={s.label} className="rounded-lg bg-slate-50 px-3 py-2 text-center">
                <p className="text-xl font-bold text-blue-700">{s.value}</p>
                <p className="text-[10px] text-slate-500 mt-0.5">{s.label}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Charts row ─────────────────────────────────────── */}
      <div className="grid gap-5 md:grid-cols-2">
        {catData.length > 0 && (
          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <h3 className="font-bold text-slate-900 flex items-center gap-2 mb-4 text-sm">
              <FiBookOpen className="text-blue-600" /> Event Categories
            </h3>
            <ResponsiveContainer width="100%" height={200}>
              <PieChart>
                <Pie data={catData} cx="50%" cy="50%" outerRadius={75} dataKey="value" label={({ name }) => name.substring(0, 10)}>
                  {catData.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        )}

        {skillsData.length > 0 && (
          <div className="rounded-xl border border-slate-200 bg-white p-5">
            <h3 className="font-bold text-slate-900 flex items-center gap-2 mb-4 text-sm">
              <FiTrendingUp className="text-green-600" /> Skills & Activities
            </h3>
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={skillsData.slice(0, 8)} layout="vertical">
                <CartesianGrid strokeDasharray="3 3" horizontal={false} />
                <XAxis type="number" tick={{ fontSize: 10 }} />
                <YAxis type="category" dataKey="name" tick={{ fontSize: 10 }} width={80} />
                <Tooltip />
                <Bar dataKey="value" fill="#2563eb" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        )}
      </div>

      {/* ── Badges ─────────────────────────────────────────── */}
      {badges.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <h3 className="font-bold text-slate-900 flex items-center gap-2 mb-4 text-sm">
            <FiAward className="text-amber-500" /> Badges & Achievements
          </h3>
          <div className="flex flex-wrap gap-3">
            {badges.map(badge => (
              <div key={badge.id}
                className="flex items-center gap-2 rounded-xl border border-amber-200 bg-amber-50 px-4 py-2.5">
                <span className="text-2xl">{badge.iconUrl}</span>
                <div>
                  <p className="text-sm font-semibold text-amber-900">{badge.badgeName}</p>
                  <p className="text-xs text-amber-600">{badge.badgeDescription}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── Certificates ───────────────────────────────────── */}
      {certificates.length > 0 && (
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <h3 className="font-bold text-slate-900 flex items-center gap-2 mb-4 text-sm">
            <FiCheckCircle className="text-green-600" /> Certificates Earned ({certificates.length})
          </h3>
          <div className="space-y-2">
            {certificates.map((cert, i) => (
              <div key={i} className="flex items-center justify-between rounded-lg bg-slate-50 px-4 py-3">
                <div>
                  <p className="text-sm font-semibold text-slate-800">{cert.eventName}</p>
                  <p className="text-xs text-slate-400 font-mono">{cert.certificateId}</p>
                </div>
                <span className="text-xs text-slate-400">
                  {cert.issuedAt ? new Date(cert.issuedAt).toLocaleDateString() : ''}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* ── AI Career Guidance ─────────────────────────────── */}
      {careerData && (
        <div className="rounded-xl border border-purple-200 bg-purple-50 p-5">
          <h3 className="font-bold text-purple-900 flex items-center gap-2 mb-3 text-sm">
            <FiTarget className="text-purple-600" /> Career Guidance
          </h3>
          <p className="text-sm text-purple-800 whitespace-pre-line leading-relaxed">{careerData}</p>
        </div>
      )}

      {/* ── Update interests CTA ────────────────────────────── */}
      <div className="rounded-xl border border-slate-200 bg-white p-5 flex items-center justify-between gap-4">
        <div>
          <p className="font-semibold text-slate-800 text-sm">Improve Your Recommendations</p>
          <p className="text-xs text-slate-500 mt-0.5">Update your interests, skills, and department for better recommendations.</p>
        </div>
        <Link to="/discover" className="btn-primary text-sm px-4 py-2 shrink-0 flex items-center gap-2">
          <FiZap className="h-4 w-4" /> Discover Events
        </Link>
      </div>
    </div>
  );
}
