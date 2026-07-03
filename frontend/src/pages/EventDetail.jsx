import { useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { motion } from 'framer-motion';
import { eventsAPI, bookingsAPI, paymentsAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import Spinner from '../components/common/Spinner';
import QueryError from '../components/common/QueryError';
import { toast } from 'react-toastify';
import { FiCalendar, FiMapPin, FiUsers, FiMinus, FiPlus, FiShare2, FiExternalLink, FiClock, FiAward } from 'react-icons/fi';
import { MdSchool } from 'react-icons/md';
import { format } from 'date-fns';

const CAT_LABELS = { HACKATHON:'🚀 Hackathon',WORKSHOP:'🔧 Workshop',TECHNICAL_SYMPOSIUM:'⚡ Symposium',
  CODING_COMPETITION:'💻 Coding Comp',SEMINAR:'📋 Seminar',PROJECT_EXHIBITION:'🏆 Exhibition',
  PLACEMENT_PREP:'💼 Placement Prep',TECHNICAL_TRAINING:'📚 Training',CULTURAL:'🎭 Cultural',SPORTS:'⚽ Sports' };

export default function EventDetail() {
  const { id }       = useParams();
  const { user }     = useAuth();
  const navigate     = useNavigate();
  const qc           = useQueryClient();
  const [qty, setQty] = useState(1);
  const emptyParticipant = { name:'', email:'', department:'', college:'' };
  const [participants, setParticipants] = useState([emptyParticipant]);

  const { data, isLoading, isError, error, refetch } = useQuery(['event', id], () => eventsAPI.getById(id).then(r => r.data?.data));

  const bookMutation = useMutation(
    () => bookingsAPI.book({
      eventId: Number(id),
      quantity: qty,
      participants,
    }).then(r => r.data),
    {
      onSuccess: async (res) => {
        const booking = res.data;
        qc.invalidateQueries(['event', id]);
        qc.invalidateQueries('events');
        qc.invalidateQueries('dash-bookings');
        qc.invalidateQueries('my-bookings');
        qc.invalidateQueries('notifs');
        if (!booking?.id) {
          toast.error('Booking was created, but checkout could not be opened.');
          navigate('/bookings');
          return;
        }

        if (!booking.totalAmount || Number(booking.totalAmount) === 0) {
          try {
            await paymentsAPI.createRazorpayOrder(booking.id);
            toast.success(`Free booking confirmed! Ticket: ${booking.ticketId}`);
            navigate(`/bookings/${booking.id}`);
          } catch (err) {
            toast.error(err?.response?.data?.message || 'Could not confirm free booking.');
            navigate(`/checkout/${booking.id}`);
          }
          return;
        }

        toast.success('Seats reserved. Complete payment to confirm your ticket.');
        navigate(`/checkout/${booking.id}`);
      },
      onError: (err) => {
        toast.error(err?.response?.data?.message || 'Could not reserve tickets. Please try again.');
      },
    }
  );

  if (isLoading) return <Spinner full />;
  if (isError) return (
    <div className="max-w-lg mx-auto py-20 px-4">
      <QueryError message={error?.response?.data?.message} onRetry={refetch} />
    </div>
  );
  if (!data) return (
    <div className="text-center py-20 text-blue-900">
      <div className="text-5xl mb-4">😕</div>
      <h2 className="text-2xl font-bold">Event not found</h2>
      <Link to="/events" className="btn-primary inline-block mt-4">Browse Events</Link>
    </div>
  );

  const ev       = data;
  const isFree   = !ev.ticketPrice || Number(ev.ticketPrice) === 0;
  const total    = isFree ? 0 : Number(ev.ticketPrice) * qty;
  const canBook  = ev.availableSeats > 0 && ev.status !== 'CANCELLED' && ev.status !== 'COMPLETED';
  const catLabel = CAT_LABELS[ev.category] || '📅 Event';

  return (
    <div style={{ background:'#F0F4FF', minHeight:'100vh' }}>
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="grid lg:grid-cols-3 gap-10">

          {/* Left */}
          <div className="lg:col-span-2 space-y-7">
            {/* Banner */}
            <div className="rounded-3xl overflow-hidden h-72 md:h-96 bg-gradient-to-br from-blue-100 to-indigo-100 shadow-card">
              {ev.eventBanner
                ? <img src={ev.eventBanner} alt={ev.eventName} onError={(e) => { e.currentTarget.style.display = 'none'; }} className="w-full h-full object-cover" />
                : <div className="w-full h-full flex items-center justify-center text-8xl opacity-30">🎓</div>}
            </div>

            {/* Title */}
            <div>
              <div className="flex flex-wrap gap-2 mb-3">
                <span className="badge badge-blue">{catLabel}</span>
                {ev.hasCertificate && <span className="badge badge-yellow">🏅 Certificate</span>}
              </div>
              <h1 className="text-3xl font-extrabold text-blue-900 mb-5" style={{ fontFamily:'Space Grotesk,sans-serif' }}>
                {ev.eventName}
              </h1>

              <div className="grid sm:grid-cols-2 gap-3">
                {[
                  { icon:<FiCalendar className="text-blue-500" />, label:ev.eventDate ? format(new Date(ev.eventDate), 'EEEE, MMMM d yyyy') : '—' },
                  { icon:<FiClock className="text-blue-500" />,    label:ev.eventTime || '—' },
                  { icon:<FiMapPin className="text-red-400" />,    label:[ev.venueName,ev.location].filter(Boolean).join(', ') || '—' },
                  { icon:<FiUsers className="text-blue-500" />,    label:`${ev.availableSeats} / ${ev.totalSeats} seats available` },
                  ev.collegeName && { icon:<MdSchool className="text-blue-400" />, label:ev.collegeName },
                  ev.departmentName && { icon:<MdSchool className="text-indigo-400" />, label:ev.departmentName },
                ].filter(Boolean).map((item,i) => (
                  <div key={i} className="flex items-start gap-3 bg-white rounded-2xl px-4 py-3 border border-blue-100">
                    <span className="mt-0.5 text-lg">{item.icon}</span>
                    <span className="text-sm text-gray-700">{item.label}</span>
                  </div>
                ))}
              </div>
            </div>

            {/* Description */}
            {ev.description && (
              <div className="bg-white rounded-2xl p-6 border border-blue-100 shadow-card">
                <h2 className="text-lg font-bold text-blue-900 mb-3">About this Event</h2>
                <p className="text-gray-600 leading-relaxed text-sm whitespace-pre-line">{ev.description}</p>
              </div>
            )}

            {/* Organizer */}
            {ev.organizer && (
              <div className="bg-white rounded-2xl p-5 flex items-center gap-4 border border-blue-100 shadow-card">
                <div className="w-14 h-14 rounded-2xl flex items-center justify-center text-white font-bold text-xl overflow-hidden"
                  style={{ background:'linear-gradient(135deg,#1565C0,#D32F2F)' }}>
                  {ev.organizer.organizationLogo
                    ? <img src={ev.organizer.organizationLogo} alt="" className="w-full h-full object-cover" />
                    : ev.organizer.organizerName?.charAt(0)}
                </div>
                <div>
                  <p className="font-bold text-blue-900">{ev.organizer.organizerName}</p>
                  <p className="text-sm text-gray-500">{ev.organizer.organizationName}</p>
                </div>
              </div>
            )}

            {ev.googleMapsUrl && (
              <a href={ev.googleMapsUrl} target="_blank" rel="noopener noreferrer"
                className="flex items-center gap-2 text-blue-600 hover:text-blue-800 text-sm font-medium">
                <FiExternalLink /> View on Google Maps
              </a>
            )}
          </div>

          {/* Right — booking card */}
          <div className="lg:col-span-1">
            <div className="bg-white rounded-3xl p-6 sticky top-24 space-y-5 border border-blue-100 shadow-blue">
              <div>
                <p className="text-xs text-gray-500 mb-1 font-medium">Ticket Price</p>
                {isFree
                  ? <p className="text-3xl font-extrabold text-green-600">Free</p>
                  : <p className="text-3xl font-extrabold text-blue-900">₹{Number(ev.ticketPrice).toLocaleString()}<span className="text-sm font-normal text-gray-400"> / ticket</span></p>}
              </div>

              {canBook && (
                <div>
                  <p className="text-sm font-semibold text-blue-900 mb-3">Quantity</p>
                  <div className="flex items-center gap-4">
                    <button onClick={() => {
                      setQty(q => {
                        const next = Math.max(1, q - 1);
                        setParticipants(ps => ps.slice(0, next));
                        return next;
                      });
                    }}
                      className="w-10 h-10 rounded-xl border border-blue-200 flex items-center justify-center hover:bg-blue-50 transition-colors text-blue-700">
                      <FiMinus />
                    </button>
                    <span className="text-xl font-bold text-blue-900 w-8 text-center">{qty}</span>
                    <button onClick={() => {
                      setQty(q => {
                        const next = Math.min(10, q + 1, ev.availableSeats);
                        setParticipants(ps => [...ps, ...Array.from({ length: next - ps.length }, () => ({ ...emptyParticipant }))]);
                        return next;
                      });
                    }}
                      className="w-10 h-10 rounded-xl border border-blue-200 flex items-center justify-center hover:bg-blue-50 transition-colors text-blue-700">
                      <FiPlus />
                    </button>
                  </div>
                </div>
              )}

              {canBook && (
                <div className="space-y-3 max-h-[26rem] overflow-y-auto pr-1">
                  <p className="text-sm font-semibold text-blue-900">Participant Details</p>
                  {participants.map((p, index) => (
                    <div key={index} className="border border-blue-100 rounded-2xl p-3 bg-blue-50/40 space-y-2">
                      <div className="text-xs font-bold text-blue-700">Participant {index + 1}</div>
                      <input className="input-field text-sm bg-white" placeholder="Name" value={p.name}
                        onChange={e => setParticipants(ps => ps.map((x, i) => i === index ? { ...x, name:e.target.value } : x))} />
                      <input className="input-field text-sm bg-white" placeholder="Email" type="email" value={p.email}
                        onChange={e => setParticipants(ps => ps.map((x, i) => i === index ? { ...x, email:e.target.value } : x))} />
                      <div className="grid grid-cols-2 gap-2">
                        <input className="input-field text-sm bg-white" placeholder="Department" value={p.department}
                          onChange={e => setParticipants(ps => ps.map((x, i) => i === index ? { ...x, department:e.target.value } : x))} />
                        <input className="input-field text-sm bg-white" placeholder="College" value={p.college}
                          onChange={e => setParticipants(ps => ps.map((x, i) => i === index ? { ...x, college:e.target.value } : x))} />
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {!isFree && canBook && (
                <div className="flex items-center justify-between pt-3 border-t border-blue-100">
                  <span className="font-semibold text-gray-700">Total</span>
                  <span className="text-2xl font-extrabold text-blue-900">₹{total.toLocaleString()}</span>
                </div>
              )}

              {canBook ? (
                <motion.button onClick={() => {
                  if (!user) return navigate('/login');
                  if (user.role !== 'USER') return toast.error('Only students can book tickets');
                  if (participants.some(p => !p.name.trim() || !p.email.trim())) return toast.error('Enter name and email for every participant');
                  bookMutation.mutate();
                }} disabled={bookMutation.isLoading}
                  whileHover={{ scale:1.02 }} whileTap={{ scale:0.98 }}
                  className="btn-primary w-full py-4 text-base font-bold disabled:opacity-60">
                  {bookMutation.isLoading ? '⏳ Processing…' : '🎫 Register Now'}
                </motion.button>
              ) : (
                <div className="bg-red-50 border border-red-200 text-red-700 text-center py-4 rounded-2xl font-semibold text-sm">
                  {ev.status === 'CANCELLED' ? '❌ Event Cancelled' : ev.status === 'COMPLETED' ? '✅ Event Completed' : '⚠️ No seats available'}
                </div>
              )}

              <button onClick={() => navigator.clipboard.writeText(window.location.href).then(() => toast.success('Link copied!'))}
                className="flex items-center justify-center gap-2 w-full py-3 rounded-2xl border border-blue-200 text-sm font-medium text-blue-600 hover:bg-blue-50 transition-colors">
                <FiShare2 /> Share Event
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
