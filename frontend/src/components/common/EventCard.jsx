import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiCalendar, FiMapPin, FiUsers } from 'react-icons/fi';
import { MdSchool } from 'react-icons/md';
import { format } from 'date-fns';

const CAT = {
  HACKATHON:            { cls: 'cat-hackathon', label: 'Hackathon',     emoji: '🚀', grad: 'linear-gradient(135deg,#1e3a5f,#0f766e)' },
  TECHNICAL_SYMPOSIUM:  { cls: 'cat-symposium', label: 'Symposium',     emoji: '⚡', grad: 'linear-gradient(135deg,#312e81,#1e40af)' },
  CODING_COMPETITION:   { cls: 'cat-coding',    label: 'Coding Comp',   emoji: '💻', grad: 'linear-gradient(135deg,#064e3b,#065f46)' },
  WORKSHOP:             { cls: 'cat-workshop',  label: 'Workshop',      emoji: '🔧', grad: 'linear-gradient(135deg,#78350f,#92400e)' },
  SEMINAR:              { cls: 'cat-seminar',   label: 'Seminar',       emoji: '📋', grad: 'linear-gradient(135deg,#1e3a5f,#1e40af)' },
  PROJECT_EXHIBITION:   { cls: 'cat-coding',    label: 'Exhibition',    emoji: '🏆', grad: 'linear-gradient(135deg,#7c2d12,#92400e)' },
  PLACEMENT_PREP:       { cls: 'cat-workshop',  label: 'Placement Prep',emoji: '💼', grad: 'linear-gradient(135deg,#1e3a5f,#374151)' },
  TECHNICAL_TRAINING:   { cls: 'cat-workshop',  label: 'Training',      emoji: '📚', grad: 'linear-gradient(135deg,#134e4a,#0f766e)' },
  CULTURAL:             { cls: 'cat-cultural',  label: 'Cultural',      emoji: '🎭', grad: 'linear-gradient(135deg,#86198f,#a21caf)' },
  SPORTS:               { cls: 'cat-sports',    label: 'Sports',        emoji: '⚽', grad: 'linear-gradient(135deg,#14532d,#15803d)' },
  INTER_COLLEGE:        { cls: 'cat-symposium', label: 'Inter-College', emoji: '🎓', grad: 'linear-gradient(135deg,#1e3a5f,#1d4ed8)' },
  INTRA_COLLEGE:        { cls: 'cat-coding',    label: 'Intra-College', emoji: '🏫', grad: 'linear-gradient(135deg,#312e81,#4338ca)' },
  CLUB_ACTIVITY:        { cls: 'cat-default',   label: 'Club',          emoji: '🌟', grad: 'linear-gradient(135deg,#831843,#9d174d)' },
  OTHER:                { cls: 'cat-default',   label: 'Event',         emoji: '📅', grad: 'linear-gradient(135deg,#1e293b,#334155)' },
};

const STATUS_CLS = {
  UPCOMING:  'badge-green',
  LIVE:      'badge-blue',
  ONGOING:   'badge-blue',
  COMPLETED: 'badge-gray',
  EXPIRED:   'badge-red',
  CANCELLED: 'badge-red',
  PUBLISHED: 'badge-blue',
  DRAFT:     'badge-yellow',
};

// Statuses where registration/action is disabled
const ENDED_STATUSES = new Set(['COMPLETED', 'EXPIRED', 'CANCELLED']);

export default function EventCard({ event, compact = false }) {
  const dateStr = event.eventDate ? format(new Date(event.eventDate), 'EEE, MMM d, yyyy') : '-';
  const isFree  = !event.ticketPrice || Number(event.ticketPrice) === 0;
  const cat     = CAT[event.category] ?? CAT.OTHER;
  const ended   = ENDED_STATUSES.has(event.status);

  // Human-readable status label
  const statusLabel = {
    LIVE: 'Live Now',
    UPCOMING: 'Upcoming',
    COMPLETED: 'Completed',
    EXPIRED: 'Expired',
    CANCELLED: 'Cancelled',
    PUBLISHED: 'Open',
    ONGOING: 'Ongoing',
    DRAFT: 'Draft',
  }[event.status] ?? event.status;

  return (
    <motion.div
      whileHover={{ y: -6, boxShadow: '0 20px 46px rgba(15,23,42,0.14)' }}
      transition={{ duration: 0.25, ease: [0.4, 0, 0.2, 1] }}
      className="h-full rounded-xl"
    >
      <Link
        to={`/events/${event.id}`}
        className="group flex h-full flex-col overflow-hidden rounded-xl bg-white"
        style={{ border: '1px solid #E2E8F0', boxShadow: '0 10px 30px rgba(15,23,42,0.06)' }}
      >
        <div
          className="relative flex-shrink-0 overflow-hidden"
          style={{
            height: compact ? '130px' : '185px',
            background: 'linear-gradient(135deg,#E0F2FE,#F8FAFC,#CCFBF1)',
          }}
        >
          {event.eventBanner ? (
            <img
              src={event.eventBanner}
              alt={event.eventName}
              onError={(e) => { e.currentTarget.style.display = 'none'; }}
              className="h-full w-full object-cover transition-transform duration-500 group-hover:scale-105"
            />
          ) : (
            <div
              className="flex h-full w-full flex-col items-center justify-center gap-2"
              style={{ background: cat.grad }}
            >
              <span className="text-5xl opacity-80">{cat.emoji}</span>
              <span className="text-xs font-bold text-white/70 uppercase tracking-widest">{cat.label}</span>
            </div>
          )}

          <div className="absolute left-3 top-3 flex flex-col gap-1.5">
            <span className={`badge ${STATUS_CLS[event.status] ?? 'badge-blue'}`}>{statusLabel}</span>
          </div>
          <div className="absolute right-3 top-3 flex flex-col items-end gap-1.5">
            {isFree && <span className="badge badge-green">FREE</span>}
            {event.hasCertificate && <span className="badge badge-yellow">Cert</span>}
          </div>
        </div>

        <div className="flex flex-1 flex-col gap-2.5 p-4">
          <span className={`badge w-fit text-[10px] ${cat.cls}`}>{cat.label}</span>

          <h3 className="line-clamp-2 text-sm font-bold leading-snug text-slate-950 transition-colors group-hover:text-teal-700">
            {event.eventName}
          </h3>

          {(event.collegeName || event.departmentName) && (
            <div className="flex items-center gap-1.5 text-xs text-slate-500">
              <MdSchool className="h-3 w-3 flex-shrink-0" />
              <span className="truncate">{event.departmentName || event.collegeName}</span>
            </div>
          )}

          <div className="space-y-1 text-xs text-slate-500">
            <div className="flex items-center gap-1.5">
              <FiCalendar className="h-3 w-3 flex-shrink-0 text-teal-500" />
              <span>{dateStr}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <FiMapPin className="h-3 w-3 flex-shrink-0 text-rose-500" />
              <span className="truncate">{event.venueName || event.location || '-'}</span>
            </div>
            <div className="flex items-center gap-1.5">
              <FiUsers className="h-3 w-3 flex-shrink-0 text-cyan-600" />
              <span>{event.availableSeats} seats left</span>
            </div>
          </div>

          <div className="mt-auto flex items-center justify-between border-t border-slate-100 pt-3">
            {isFree ? (
              <span className="text-sm font-extrabold text-green-600">Free</span>
            ) : (
              <span className="text-sm font-extrabold text-slate-950">
                Rs. {Number(event.ticketPrice).toLocaleString()}
              </span>
            )}
            {ended ? (
              <span className="rounded-lg border border-slate-200 bg-slate-100 px-3 py-1.5 text-[11px] font-bold text-slate-400 cursor-not-allowed">
                {event.status === 'COMPLETED' ? 'Completed' : event.status === 'EXPIRED' ? 'Expired' : 'Cancelled'}
              </span>
            ) : (
              <span className="rounded-lg border border-teal-100 bg-teal-50 px-3 py-1.5 text-[11px] font-bold text-teal-700 transition-all duration-200 group-hover:bg-teal-600 group-hover:text-white">
                Register
              </span>
            )}
          </div>
        </div>
      </Link>
    </motion.div>
  );
}
