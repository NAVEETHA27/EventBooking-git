import { useParams, Link } from 'react-router-dom';
import { useQuery } from 'react-query';
import { bookingsAPI } from '../../services/api';
import Spinner from '../../components/common/Spinner';
import { QRCodeSVG } from 'qrcode.react';
import { FiArrowLeft, FiCalendar, FiCreditCard, FiMapPin, FiUsers } from 'react-icons/fi';
import { format } from 'date-fns';

export default function BookingDetail() {
  const { id } = useParams();
  const { data, isLoading } = useQuery(['booking', id],
    () => bookingsAPI.getById(id).then(r => r.data?.data));

  if (isLoading) return <Spinner full />;
  if (!data) return (
    <div className="text-center py-20">
      <h2 className="text-2xl font-bold text-blue-900">Booking not found</h2>
      <Link to="/bookings" className="btn-primary inline-block mt-4">My Bookings</Link>
    </div>
  );

  const b = data;
  const isFree = !b.totalAmount || Number(b.totalAmount) === 0;
  const inactive = b.ticketStatus === 'CANCELLED' || b.ticketStatus === 'EXPIRED' || b.bookingStatus === 'CANCELLED' || b.bookingStatus === 'EXPIRED';
  const statusText = b.ticketStatus === 'EXPIRED' || b.bookingStatus === 'EXPIRED' ? 'Ticket Expired' : b.ticketStatus === 'CANCELLED' || b.bookingStatus === 'CANCELLED' ? 'Ticket Cancelled' : b.bookingStatus;

  return (
    <div style={{ background:'#F0F4FF', minHeight:'100vh' }} className="px-4 sm:px-6 lg:px-8 py-10">
      <div className="max-w-2xl mx-auto">
        <div className="flex items-center gap-3 mb-8">
          <Link to="/bookings" className="p-2 rounded-xl hover:bg-blue-100 transition-colors text-blue-700">
            <FiArrowLeft className="w-5 h-5" />
          </Link>
          <h1 className="text-2xl font-extrabold text-blue-900" style={{ fontFamily:'Space Grotesk,sans-serif' }}>
            Booking Details
          </h1>
        </div>

        <div className={`bg-white rounded-3xl p-8 border shadow-card space-y-6 ${inactive ? 'border-red-200 bg-gradient-to-b from-white to-red-50' : 'border-blue-100'}`}>
          {/* QR */}
          <div className="flex flex-col items-center py-5 border-b border-blue-100">
            {inactive ? (
              <div className="w-44 h-44 rounded-3xl bg-red-50 border border-red-200 flex items-center justify-center text-center px-4 text-red-700 font-extrabold">
                {statusText}
              </div>
            ) : (
              <QRCodeSVG value={b.ticketId} size={180} />
            )}
            <code className="text-sm bg-blue-50 text-blue-700 px-4 py-2 rounded-xl mt-4 font-mono border border-blue-100">
              {b.ticketId}
            </code>
            <span className={`badge mt-3 ${b.bookingStatus === 'CONFIRMED' ? 'badge-green' : 'badge-red'}`}>
              {statusText}
            </span>
            {b.cancelledAt && <p className="text-xs text-red-500 mt-2">Cancelled at {format(new Date(b.cancelledAt), 'MMM d, yyyy h:mm a')}</p>}
            {b.expiredAt && <p className="text-xs text-red-500 mt-2">Expired at {format(new Date(b.expiredAt), 'MMM d, yyyy h:mm a')}</p>}
          </div>

          {/* Event info */}
          <div className="space-y-3">
            <h2 className="text-xl font-bold text-blue-900">{b.event?.eventName}</h2>
            <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-gray-500">
              <span className="flex items-center gap-1.5">
                <FiCalendar className="text-blue-400" />
                {b.event?.eventDate ? format(new Date(b.event.eventDate), 'EEE, MMM d yyyy') : '—'}
              </span>
              <span className="flex items-center gap-1.5">
                <FiMapPin className="text-red-400" />
                {b.event?.venueName || b.event?.location || '—'}
              </span>
              <span className="flex items-center gap-1.5">
                <FiUsers className="text-blue-400" />
                {b.quantity} ticket{b.quantity > 1 ? 's' : ''}
              </span>
            </div>
          </div>

          {/* Payment */}
          <div className="bg-blue-50 rounded-2xl p-5 space-y-2 border border-blue-100">
            <div className="flex justify-between text-sm">
              <span className="text-gray-500">Unit Price</span>
              <span className="font-medium text-gray-700">
                {isFree ? 'Free' : `₹${(Number(b.totalAmount) / b.quantity).toLocaleString()}`}
              </span>
            </div>
            <div className="flex justify-between text-sm">
              <span className="text-gray-500">Quantity</span>
              <span className="font-medium text-gray-700">{b.quantity}</span>
            </div>
            <div className="flex justify-between border-t border-blue-200 pt-2 mt-2">
              <span className="font-bold text-blue-900">Total Paid</span>
              <span className="text-xl font-extrabold text-blue-700">
                {isFree ? 'Free' : `₹${Number(b.totalAmount).toLocaleString()}`}
              </span>
            </div>
          </div>

          {!!b.participants?.length && (
            <div className="space-y-3">
              <h3 className="font-bold text-blue-900">Participants</h3>
              {b.participants.map((p, index) => (
                <div key={p.id || index} className="rounded-2xl border border-blue-100 p-4 bg-white">
                  <div className="font-semibold text-gray-800">{index + 1}. {p.name}</div>
                  <div className="text-sm text-gray-500">{p.email}</div>
                  <div className="text-xs text-gray-500">{[p.department, p.college].filter(Boolean).join(' · ')}</div>
                </div>
              ))}
            </div>
          )}

          {b.bookingStatus === 'PENDING' && !isFree && !inactive && (
            <Link
              to={`/checkout/${b.id}`}
              className="flex items-center justify-center gap-2 rounded-2xl bg-blue-700 px-4 py-3 text-sm font-bold text-white shadow-sm hover:bg-blue-800"
            >
              <FiCreditCard /> Complete Payment
            </Link>
          )}

          <p className="text-xs text-gray-400 text-center">
            {inactive ? 'This ticket is kept for booking history and audit records.' : 'Present this QR code at the venue entrance for check-in.'}
          </p>
        </div>
      </div>
    </div>
  );
}
