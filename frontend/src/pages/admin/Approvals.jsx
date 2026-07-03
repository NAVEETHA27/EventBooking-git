import { useEffect, useState } from 'react';
import { useQueryClient } from 'react-query';
import { toast } from 'react-toastify';
import { adminAPI } from '../../services/api';

export default function Approvals() {
  const qc = useQueryClient();
  const [rows, setRows] = useState([]);
  const load = () => adminAPI.approvals({ page: 0, size: 20 }).then((r) => setRows(r.data?.data?.content ?? []));
  useEffect(() => { load(); }, []);

  const review = async (eventId, decision) => {
    const reason = decision === 'APPROVE' ? '' : window.prompt('Reason for organizer') || '';
    await adminAPI.reviewEvent(eventId, { decision, reason });
    toast.success('Review saved');
    qc.invalidateQueries('events');
    qc.invalidateQueries('org-events');
    qc.invalidateQueries('org-events-dash');
    qc.invalidateQueries('org-dash');
    qc.invalidateQueries('notifs');
    load();
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <h1 className="text-3xl font-extrabold text-slate-950">Approval Requests</h1>
      <div className="mt-6 space-y-3">
        {rows.length === 0 && <div className="rounded-lg border border-dashed p-6 text-slate-500">No approval requests.</div>}
        {rows.map((row) => (
          <div key={row.id} className="rounded-lg border border-slate-200 bg-white p-4">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <p className="font-bold text-slate-900">{row.event?.eventName || `Event #${row.event?.id}`}</p>
                <p className="text-sm text-slate-500">Status: {row.status}</p>
              </div>
              <div className="flex flex-wrap gap-2">
                <button className="btn-primary px-4 py-2 text-sm" onClick={() => review(row.event.id, 'APPROVE')}>Approve</button>
                <button className="btn-outline px-4 py-2 text-sm" onClick={() => review(row.event.id, 'REQUEST_MODIFICATIONS')}>Request Changes</button>
                <button className="rounded-lg bg-red-600 px-4 py-2 text-sm font-bold text-white" onClick={() => review(row.event.id, 'REJECT')}>Reject</button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
