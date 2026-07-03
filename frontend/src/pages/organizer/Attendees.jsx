import React from 'react';
import { useQuery } from 'react-query';
import { organizerAPI } from '../../services/api';
import Spinner from '../../components/common/Spinner';
import { FiUsers } from 'react-icons/fi';

export default function OrganizerAttendees() {
  const { data: participants, isLoading } = useQuery(
    'org-participants',
    () => organizerAPI.participants().then(r => r.data.data)
  );

  if (isLoading) return <Spinner />;

  const totalAttendees = participants?.length || 0;
  const totalEvents = new Set((participants || []).map(p => p.eventName)).size;

  return (
    <div className="max-w-5xl mx-auto px-4 py-10 animate-fade-in">
      <div className="flex items-center gap-3 mb-8">
        <FiUsers className="text-primary w-6 h-6" />
        <h1 className="section-title">Attendees Overview</h1>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 mb-8">
        {[
          { label: 'Total Events',    value: totalEvents },
          { label: 'Total Attendees', value: totalAttendees },
          { label: 'Avg. per Event',  value: totalEvents ? Math.round(totalAttendees / totalEvents) : 0 },
        ].map(s => (
          <div key={s.label} className="card p-6 text-center">
            <div className="text-3xl font-extrabold gradient-text">{s.value}</div>
            <div className="text-sm text-gray-500 mt-1">{s.label}</div>
          </div>
        ))}
      </div>

      <div className="card overflow-hidden">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 dark:bg-dark-border">
            <tr>
              {['Participant', 'Email', 'Event', 'Department', 'College'].map(h => (
                <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500 uppercase">{h}</th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100 dark:divide-dark-border">
            {participants?.map(p => (
                <tr key={p.id} className="hover:bg-gray-50 dark:hover:bg-dark-border/50 transition-colors">
                  <td className="px-5 py-4 font-medium text-gray-900 dark:text-white max-w-xs truncate">
                    {p.name}
                  </td>
                  <td className="px-5 py-4 text-gray-500">{p.email}</td>
                  <td className="px-5 py-4 text-gray-500">{p.eventName}</td>
                  <td className="px-5 py-4 text-gray-500">{p.department || '-'}</td>
                  <td className="px-5 py-4 text-gray-500">{p.college || '-'}</td>
                </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
