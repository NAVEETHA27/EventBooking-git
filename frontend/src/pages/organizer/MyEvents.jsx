import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { motion } from 'framer-motion';
import { certificatesAPI, eventsAPI, organizerAPI } from '../../services/api';
import { toast } from 'react-toastify';
import Spinner from '../../components/common/Spinner';
import { FiAward, FiEdit2, FiEye, FiImage, FiMessageCircle, FiTrash2, FiXCircle, FiCheckCircle, FiPlus, FiCamera } from 'react-icons/fi';

const STATUS_BADGE = { PUBLISHED:'badge-green', LIVE:'badge-orange', UPCOMING:'badge-blue', APPROVED:'badge-green', DRAFT:'badge-yellow', CANCELLED:'badge-red', COMPLETED:'badge-gray' };

function refreshEventState(qc) {
  qc.invalidateQueries('org-events');
  qc.invalidateQueries('org-events-dash');
  qc.invalidateQueries('org-dash');
  qc.invalidateQueries('events');
  qc.invalidateQueries('discover');
  qc.invalidateQueries('notifs');
}

export default function MyEvents() {
  const qc = useQueryClient();
  const [page, setPage]     = useState(0);
  const [filter, setFilter] = useState('');

  const { data, isLoading } = useQuery(['org-events', page, filter],
    () => eventsAPI.myEvents({ page, size:10, status:filter||undefined }).then(r => r.data?.data),
    { keepPreviousData:true });

  const cancelMutation  = useMutation((id) => eventsAPI.cancel(id),  { onSuccess:() => { toast.success('Event cancelled'); refreshEventState(qc); } });
  const publishMutation = useMutation((id) => eventsAPI.publish(id), { onSuccess:() => { toast.success('Event published!'); refreshEventState(qc); } });
  const deleteMutation  = useMutation((id) => eventsAPI.delete(id),  { onSuccess:() => { toast.success('Event deleted'); refreshEventState(qc); } });
  const templateMutation = useMutation(({ id, formData, signatureName }) => certificatesAPI.uploadTemplate(id, formData, signatureName), {
    onSuccess:() => { toast.success('Certificate template uploaded'); refreshEventState(qc); }
  });
  const posterMutation = useMutation((id) => organizerAPI.generatePoster(id).then(r => r.data?.data), {
    onSuccess:(url) => { toast.success('Poster generated'); if (url) window.open(url, '_blank'); }
  });

  const uploadTemplate = (eventId) => {
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.pdf,.png,.jpg,.jpeg';
    input.onchange = () => {
      const file = input.files?.[0];
      if (!file) return;
      const signatureName = window.prompt('Signature name for certificates', '') || '';
      const formData = new FormData();
      formData.append('file', file);
      templateMutation.mutate({ id: eventId, formData, signatureName });
    };
    input.click();
  };

  if (isLoading) return <Spinner />;

  return (
    <div style={{ background:'#F0F4FF', minHeight:'100vh' }} className="px-4 sm:px-6 lg:px-8 py-10">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <h1 className="text-3xl font-extrabold text-blue-900" style={{ fontFamily:'Space Grotesk,sans-serif' }}>My Events</h1>
          <Link to="/organizer/events/create" className="btn-primary flex items-center gap-2">
            <FiPlus /> New Event
          </Link>
        </div>

        {/* Filters */}
        <div className="flex gap-2 mb-6 flex-wrap">
          {['','DRAFT','PUBLISHED','UPCOMING','LIVE','COMPLETED','CANCELLED'].map(s => (
            <button key={s} onClick={() => { setFilter(s); setPage(0); }}
              className={`px-4 py-2 rounded-xl text-sm font-semibold transition-all ${
                filter===s ? 'bg-blue-600 text-white shadow-blue' : 'bg-white text-gray-600 border border-blue-100 hover:bg-blue-50'
              }`}>
              {s || 'All'}
            </button>
          ))}
        </div>

        {!data?.content?.length ? (
          <div className="bg-white rounded-3xl p-16 text-center border border-blue-100 shadow-card">
            <div className="text-5xl mb-4">📅</div>
            <p className="text-gray-500 mb-4">No events yet.</p>
            <Link to="/organizer/events/create" className="btn-primary">Create your first event</Link>
          </div>
        ) : (
          <>
            <div className="bg-white rounded-2xl overflow-hidden border border-blue-100 shadow-card">
              <table className="data-table">
                <thead>
                  <tr>{['Event','Category','Date','Sold','Revenue','Status','Actions'].map(h => <th key={h}>{h}</th>)}</tr>
                </thead>
                <tbody>
                  {data.content.map(ev => {
                    const sold    = ev.totalSeats - ev.availableSeats;
                    const revenue = (sold * Number(ev.ticketPrice)).toLocaleString();
                    return (
                      <tr key={ev.id}>
                        <td className="font-semibold text-blue-900 max-w-xs truncate">{ev.eventName}</td>
                        <td><span className="badge badge-blue text-[10px]">{ev.category?.replace(/_/g,' ')}</span></td>
                        <td className="text-gray-500 whitespace-nowrap">{ev.eventDate}</td>
                        <td className="text-gray-500">{sold}/{ev.totalSeats}</td>
                        <td className="font-semibold text-blue-900">₹{revenue}</td>
                        <td><span className={`badge ${STATUS_BADGE[ev.status]||'badge-gray'}`}>{ev.status}</span></td>
                        <td>
                          <div className="flex items-center gap-2">
                            <Link to={`/events/${ev.id}?tab=community`} title="Open community chat"
                              className="p-1.5 rounded-lg text-indigo-600 hover:bg-indigo-50 transition-colors">
                              <FiMessageCircle className="w-4 h-4" />
                            </Link>
                            <Link to={`/events/${ev.id}`} title="View event"
                              className="p-1.5 rounded-lg text-slate-600 hover:bg-slate-50 transition-colors">
                              <FiEye className="w-4 h-4" />
                            </Link>
                            <Link to={`/organizer/events/${ev.id}/edit`} title="Edit"
                              className="p-1.5 rounded-lg text-blue-500 hover:bg-blue-50 transition-colors">
                              <FiEdit2 className="w-4 h-4" />
                            </Link>
                            <Link to={`/organizer/events/${ev.id}/attendance`} title="Attendance scanner"
                              className="p-1.5 rounded-lg text-emerald-600 hover:bg-emerald-50 transition-colors">
                              <FiCamera className="w-4 h-4" />
                            </Link>
                            {ev.hasCertificate && (
                              <button onClick={() => uploadTemplate(ev.id)} title="Upload certificate template"
                                className="p-1.5 rounded-lg text-violet-600 hover:bg-violet-50 transition-colors">
                                <FiAward className="w-4 h-4" />
                              </button>
                            )}
                            <button onClick={() => posterMutation.mutate(ev.id)} title="Generate poster"
                              className="p-1.5 rounded-lg text-teal-600 hover:bg-teal-50 transition-colors">
                              <FiImage className="w-4 h-4" />
                            </button>
                            {ev.status === 'DRAFT' && (
                              <button onClick={() => publishMutation.mutate(ev.id)} title="Publish"
                                className="p-1.5 rounded-lg text-green-600 hover:bg-green-50 transition-colors">
                                <FiCheckCircle className="w-4 h-4" />
                              </button>
                            )}
                            {['PUBLISHED','DRAFT','APPROVED','UPCOMING','LIVE'].includes(ev.status) && (
                              <button onClick={() => { if(window.confirm('Cancel event?')) cancelMutation.mutate(ev.id); }} title="Cancel"
                                className="p-1.5 rounded-lg text-orange-500 hover:bg-orange-50 transition-colors">
                                <FiXCircle className="w-4 h-4" />
                              </button>
                            )}
                            {ev.status === 'DRAFT' && (
                              <button onClick={() => { if(window.confirm('Delete event?')) deleteMutation.mutate(ev.id); }} title="Delete"
                                className="p-1.5 rounded-lg text-red-500 hover:bg-red-50 transition-colors">
                                <FiTrash2 className="w-4 h-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>

            {data.totalPages > 1 && (
              <div className="flex justify-center gap-3 mt-6">
                <button onClick={() => setPage(p => p-1)} disabled={data.first} className="btn-outline py-2 px-5 text-sm disabled:opacity-40">← Prev</button>
                <span className="px-4 py-2 text-sm text-gray-500 font-medium">{data.page+1}/{data.totalPages}</span>
                <button onClick={() => setPage(p => p+1)} disabled={data.last}  className="btn-outline py-2 px-5 text-sm disabled:opacity-40">Next →</button>
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
