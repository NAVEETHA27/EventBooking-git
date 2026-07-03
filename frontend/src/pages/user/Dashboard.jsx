import { Link } from 'react-router-dom';
import { useQuery } from 'react-query';
import { motion } from 'framer-motion';
import { bookingsAPI, eventsAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import EventCard from '../../components/common/EventCard';
import Spinner from '../../components/common/Spinner';
import { FiArrowRight, FiAward, FiBell, FiCalendar, FiCheckCircle, FiSearch, FiUser, FiXCircle } from 'react-icons/fi';

const fadeUp = { initial: { opacity: 0, y: 18 }, animate: { opacity: 1, y: 0, transition: { duration: 0.4 } } };

export default function UserDashboard() {
  const { user } = useAuth();

  const { data: bookings, isLoading: bLoading } = useQuery('dash-bookings',
    () => bookingsAPI.myBookings({ page: 0, size: 5 }).then((r) => r.data?.data));

  const { data: events, isLoading: eLoading } = useQuery('dash-events',
    () => eventsAPI.search({ sortBy: 'date_asc', size: 4 }).then((r) => r.data?.data));

  const bookingList = bookings?.content ?? [];
  const total = bookings?.totalElements ?? 0;
  const confirmed = bookingList.filter((b) => b.bookingStatus === 'CONFIRMED').length;
  const cancelled = bookingList.filter((b) => b.bookingStatus === 'CANCELLED').length;
  const certs = bookingList.filter((b) => b.event?.hasCertificate && b.bookingStatus === 'CONFIRMED').length;

  const stats = [
    { label: 'Total Bookings', value: total, icon: <FiCalendar />, cls: 'text-blue-600 bg-blue-50 border-blue-100' },
    { label: 'Confirmed', value: confirmed, icon: <FiCheckCircle />, cls: 'text-green-600 bg-green-50 border-green-100' },
    { label: 'Cancelled', value: cancelled, icon: <FiXCircle />, cls: 'text-rose-600 bg-rose-50 border-rose-100' },
    { label: 'Certificates', value: certs, icon: <FiAward />, cls: 'text-amber-600 bg-amber-50 border-amber-100' },
  ];

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-10 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-7xl">
        <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} className="mb-10 rounded-2xl bg-gradient-to-br from-slate-950 to-teal-800 p-8 text-white shadow-card">
          <p className="mb-2 text-xs font-black uppercase tracking-[0.22em] text-teal-200">Student Dashboard</p>
          <h1 className="mb-1 text-3xl font-extrabold" style={{ fontFamily: 'Space Grotesk,sans-serif' }}>
            Welcome back, {user?.name?.split(' ')[0] || 'Student'}
          </h1>
          <p className="text-slate-200">Track bookings, discover events, and keep your profile ready for the next opportunity.</p>
          <div className="mt-6 flex flex-wrap gap-3">
            {[
              { to: '/events', label: 'Browse Events', icon: <FiSearch /> },
              { to: '/bookings', label: 'My Bookings', icon: <FiCalendar /> },
              { to: '/notifications', label: 'Notifications', icon: <FiBell /> },
              { to: '/profile', label: 'Profile', icon: <FiUser /> },
            ].map((action) => (
              <Link key={action.to} to={action.to} className="inline-flex items-center gap-2 rounded-xl bg-white/12 px-4 py-2 text-sm font-bold text-white ring-1 ring-white/15 transition hover:bg-white/20">
                {action.icon}{action.label}
              </Link>
            ))}
          </div>
        </motion.div>

        <div className="mb-10 grid grid-cols-2 gap-5 lg:grid-cols-4">
          {stats.map((stat, index) => (
            <motion.div key={stat.label} {...fadeUp} transition={{ delay: index * 0.05 }} className="rounded-xl border bg-white p-5 shadow-card">
              <div className={`mb-3 grid h-10 w-10 place-items-center rounded-xl border ${stat.cls}`}>{stat.icon}</div>
              <div className="text-3xl font-extrabold text-slate-950" style={{ fontFamily: 'Space Grotesk,sans-serif' }}>{stat.value}</div>
              <div className="mt-0.5 text-xs font-semibold text-slate-500">{stat.label}</div>
            </motion.div>
          ))}
        </div>

        <div className="grid gap-8 lg:grid-cols-[0.9fr_1.1fr]">
          <section>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-950">Recent Bookings</h2>
              <Link to="/bookings" className="flex items-center gap-1 text-sm font-semibold text-teal-700 hover:text-teal-900">
                View all <FiArrowRight className="h-4 w-4" />
              </Link>
            </div>
            {bLoading ? <Spinner /> : (
              <div className="space-y-3">
                {bookingList.slice(0, 5).map((booking) => (
                  <Link key={booking.id} to={`/bookings/${booking.id}`} className="flex items-center gap-4 rounded-xl border border-slate-200 bg-white p-4 shadow-card transition hover:-translate-y-0.5 hover:border-teal-200">
                    <div className="grid h-11 w-11 flex-shrink-0 place-items-center rounded-xl bg-teal-50 text-teal-700"><FiCalendar /></div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold text-slate-950">{booking.event?.eventName}</p>
                      <p className="mt-0.5 font-mono text-xs text-slate-400">{booking.ticketId}</p>
                    </div>
                    <span className={`badge ${booking.bookingStatus === 'CONFIRMED' ? 'badge-green' : 'badge-red'}`}>{booking.bookingStatus}</span>
                  </Link>
                ))}
                {!bookingList.length && <p className="rounded-xl border border-slate-200 bg-white py-8 text-center text-sm text-slate-400">No bookings yet.</p>}
              </div>
            )}
          </section>

          <section>
            <div className="mb-4 flex items-center justify-between">
              <h2 className="text-lg font-bold text-slate-950">Upcoming Events</h2>
              <Link to="/events" className="flex items-center gap-1 text-sm font-semibold text-teal-700 hover:text-teal-900">
                Browse all <FiArrowRight className="h-4 w-4" />
              </Link>
            </div>
            {eLoading ? <Spinner /> : (
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {(events?.content ?? []).slice(0, 4).map((event) => <EventCard key={event.id} event={event} compact />)}
              </div>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
