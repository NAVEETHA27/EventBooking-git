import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { motion } from 'framer-motion';
import { bookingsAPI } from '../../services/api';
import Spinner from '../../components/common/Spinner';
import QueryError from '../../components/common/QueryError';
import { toast } from 'react-toastify';
import { FiCalendar, FiMapPin, FiX, FiEye } from 'react-icons/fi';
import { MdSchool } from 'react-icons/md';
import { QRCodeSVG } from 'qrcode.react';
import { format } from 'date-fns';

const STATUS_CLS = { CONFIRMED:'badge-green', CANCELLED:'badge-red', PENDING:'badge-yellow', EXPIRED:'badge-red' };

export default function MyBookings() {
  const qc = useQueryClient();
  const [page, setPage]    = useState(0);
  const [qrModal, setQrModal] = useState(null);

  const { data, isLoading, isError, error, refetch } = useQuery(['my-bookings', page],
    () => bookingsAPI.myBookings({ page, size:10 }).then(r => r.data?.data),
    { keepPreviousData:true });

  const cancelMutation = useMutation(
    ({ id }) => bookingsAPI.cancel(id, 'User requested cancellation'),
    { onSuccess:() => { toast.success('Booking cancelled. Refund initiated.'); qc.invalidateQueries('my-bookings'); } }
  );

  if (isLoading) return <Spinner />;
  if (isError) return (
    <div style={{ background:'#F0F4FF', minHeight:'100vh' }} className="px-4 py-10 max-w-4xl mx-auto">
      <QueryError message={error?.response?.data?.message} onRetry={refetch} />
    </div>
  );

  return (
    <div style={{ background:'#F0F4FF', minHeight:'100vh' }} className="px-4 sm:px-6 lg:px-8 py-10">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-extrabold text-blue-900 mb-8" style={{ fontFamily:'Space Grotesk,sans-serif' }}>
          My Bookings
        </h1>

        {!data?.content?.length ? (
          <div className="bg-white rounded-3xl p-16 text-center border border-blue-100 shadow-card">
            <div className="text-5xl mb-4">🎟️</div>
            <h3 className="text-xl font-bold text-blue-900 mb-2">No bookings yet</h3>
            <p className="text-gray-500 mb-6 text-sm">Browse college events and register for your first one!</p>
            <Link to="/events" className="btn-primary">Browse Events</Link>
          </div>
        ) : (
          <div className="space-y-4">
            {data.content.map(b => (
              (() => {
                const inactive = b.ticketStatus === 'CANCELLED' || b.ticketStatus === 'EXPIRED' || b.bookingStatus === 'CANCELLED' || b.bookingStatus === 'EXPIRED';
                const statusText = b.ticketStatus === 'EXPIRED' || b.bookingStatus === 'EXPIRED' ? 'Ticket Expired' : b.ticketStatus === 'CANCELLED' || b.bookingStatus === 'CANCELLED' ? 'Ticket Cancelled' : b.bookingStatus;
                return (
              <motion.div key={b.id} initial={{ opacity:0,y:12 }} animate={{ opacity:1,y:0 }}
                className={`bg-white rounded-2xl p-5 flex flex-col sm:flex-row gap-4 border shadow-card ${inactive ? 'border-red-200 opacity-85' : 'border-blue-100'}`}>
                {/* Banner thumb */}
                <div className="w-full sm:w-28 h-24 rounded-xl overflow-hidden bg-blue-50 flex-shrink-0 flex items-center justify-center">
                  {b.event?.eventBanner
                    ? <img src={b.event.eventBanner} alt="" className="w-full h-full object-cover" />
                    : <span className="text-2xl">🎓</span>}
                </div>

                <div className="flex-1 min-w-0">
                  <div className="flex items-start justify-between gap-2 flex-wrap mb-2">
                    <Link to={`/events/${b.event?.id}`}
                      className="font-bold text-blue-900 hover:text-blue-600 transition-colors text-sm leading-snug line-clamp-2">
                      {b.event?.eventName}
                    </Link>
                    <span className={`badge ${STATUS_CLS[b.bookingStatus] || 'badge-gray'}`}>{statusText}</span>
                  </div>

                  <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500 mb-3">
                    <span className="flex items-center gap-1">
                      <FiCalendar className="text-blue-400" />
                      {b.event?.eventDate ? format(new Date(b.event.eventDate), 'MMM d, yyyy') : '—'}
                    </span>
                    <span className="flex items-center gap-1">
                      <FiMapPin className="text-red-400" />
                      {b.event?.location || b.event?.venueName || '—'}
                    </span>
                  </div>

                  <div className="flex flex-wrap items-center gap-3 text-xs">
                    <code className="bg-blue-50 text-blue-700 px-2 py-1 rounded-lg font-mono border border-blue-100">
                      {b.ticketId}
                    </code>
                    <span className="text-gray-500">
                      {b.quantity} ticket{b.quantity > 1 ? 's' : ''} · ₹{Number(b.totalAmount).toLocaleString()}
                    </span>
                  </div>

                  <div className="flex gap-3 mt-4">
                    <button onClick={() => setQrModal(b)}
                      className="flex items-center gap-1.5 text-xs btn-outline py-1.5 px-3">
                      <FiEye className="w-3.5 h-3.5" /> View Ticket
                    </button>
                    {b.cancelledAt && <span className="text-xs text-red-500">Cancelled {format(new Date(b.cancelledAt), 'MMM d, yyyy h:mm a')}</span>}
                    {b.bookingStatus === 'CONFIRMED' && !inactive && (
                      <button onClick={() => { if (window.confirm('Cancel this booking?')) cancelMutation.mutate({ id:b.id }); }}
                        className="flex items-center gap-1.5 text-xs text-red-500 hover:text-red-700 font-semibold transition-colors">
                        <FiX className="w-3.5 h-3.5" /> Cancel
                      </button>
                    )}
                  </div>
                </div>
              </motion.div>
                );
              })()
            ))}

            {data.totalPages > 1 && (
              <div className="flex justify-center gap-3 pt-4">
                <button onClick={() => setPage(p => p-1)} disabled={data.first}
                  className="btn-outline py-2 px-5 disabled:opacity-40 text-sm">← Prev</button>
                <span className="px-4 py-2 text-sm text-gray-500 font-medium">
                  Page {data.page+1} of {data.totalPages}
                </span>
                <button onClick={() => setPage(p => p+1)} disabled={data.last}
                  className="btn-outline py-2 px-5 disabled:opacity-40 text-sm">Next →</button>
              </div>
            )}
          </div>
        )}

        {/* QR Modal */}
        {qrModal && (
          <div className="fixed inset-0 bg-black/40 backdrop-blur-sm flex items-center justify-center z-50 p-4"
               onClick={() => setQrModal(null)}>
            <motion.div initial={{ scale:0.9,opacity:0 }} animate={{ scale:1,opacity:1 }}
              className="bg-white rounded-3xl p-8 max-w-sm w-full text-center shadow-2xl border border-blue-100"
              onClick={e => e.stopPropagation()}>
              <h3 className="text-lg font-bold text-blue-900 mb-1">Your Ticket</h3>
              <p className="text-gray-500 text-sm mb-5">{qrModal.event?.eventName}</p>
              <div className="flex justify-center mb-4">
                {qrModal.ticketStatus === 'CANCELLED' || qrModal.ticketStatus === 'EXPIRED' || qrModal.bookingStatus === 'CANCELLED' || qrModal.bookingStatus === 'EXPIRED'
                  ? <div className="w-48 h-48 rounded-3xl bg-red-50 border border-red-200 flex items-center justify-center text-red-700 font-extrabold">
                      {qrModal.ticketStatus === 'EXPIRED' || qrModal.bookingStatus === 'EXPIRED' ? 'Ticket Expired' : 'Ticket Cancelled'}
                    </div>
                  : <QRCodeSVG value={qrModal.ticketId} size={190} />}
              </div>
              <code className="text-sm bg-blue-50 text-blue-700 px-4 py-2 rounded-xl block mb-5 font-mono border border-blue-100">
                {qrModal.ticketId}
              </code>
              <button onClick={() => setQrModal(null)} className="btn-primary w-full">Close</button>
            </motion.div>
          </div>
        )}
      </div>
    </div>
  );
}
