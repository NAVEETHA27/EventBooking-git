import { useQuery } from 'react-query';
import { analyticsAPI } from '../services/api';
import Spinner from '../components/common/Spinner';
import { FiAward, FiUsers } from 'react-icons/fi';

export default function Leaderboard() {
  const { data: leaderboard, isLoading } = useQuery(
    'college-leaderboard',
    () => analyticsAPI.leaderboard(30).then(r => r.data?.data || []),
    { staleTime: 10 * 60 * 1000 }
  );

  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <div className="mb-8 text-center">
        <FiAward className="mx-auto mb-3 h-12 w-12 text-amber-500" />
        <h1 className="text-3xl font-bold text-gray-900">College Leaderboard</h1>
        <p className="mt-2 text-gray-500">Top colleges by event participation</p>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-16"><Spinner /></div>
      ) : !leaderboard?.length ? (
        <div className="text-center py-20 text-gray-400">No data yet. Participate in events!</div>
      ) : (
        <div className="space-y-3">
          {leaderboard.map((entry, i) => (
            <div key={i}
              className={`flex items-center gap-4 rounded-xl border p-4 shadow-sm transition hover:shadow-md
                ${i === 0 ? 'border-amber-300 bg-amber-50' : i === 1 ? 'border-gray-300 bg-gray-50' : i === 2 ? 'border-orange-200 bg-orange-50' : 'border-gray-200 bg-white'}`}>
              {/* Rank */}
              <div className={`flex h-10 w-10 shrink-0 items-center justify-center rounded-full font-bold text-lg
                ${i === 0 ? 'bg-amber-400 text-white' : i === 1 ? 'bg-gray-400 text-white' : i === 2 ? 'bg-orange-400 text-white' : 'bg-gray-100 text-gray-500'}`}>
                {i < 3 ? ['🥇','🥈','🥉'][i] : i + 1}
              </div>
              <div className="flex-1">
                <p className="font-semibold text-gray-900">{entry.college}</p>
                <p className="text-sm text-gray-500 flex items-center gap-1">
                  <FiUsers className="h-3.5 w-3.5" /> {entry.participants} participants
                </p>
              </div>
              <div className="text-right">
                <div className="text-lg font-bold text-blue-700">{entry.participants}</div>
                <div className="text-xs text-gray-400">participants</div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
