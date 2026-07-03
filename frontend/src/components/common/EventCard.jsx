import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';
import { FiCalendar, FiMapPin, FiUsers } from 'react-icons/fi';
import { MdSchool } from 'react-icons/md';
import { format } from 'date-fns';

const CAT = {
  HACKATHON: { cls: 'cat-hackathon', label: 'Hackathon' },
  TECHNICAL_SYMPOSIUM: { cls: 'cat-symposium', label: 'Symposium' },
  CODING_COMPETITION: { cls: 'cat-coding', label: 'Coding Comp' },
  WORKSHOP: { cls: 'cat-workshop', label: 'Workshop' },
  SEMINAR: { cls: 'cat-seminar', label: 'Seminar' },
  PROJECT_EXHIBITION: { cls: 'cat-coding', label: 'Exhibition' },
  PLACEMENT_PREP: { cls: 'cat-workshop', label: 'Placement Prep' },
  TECHNICAL_TRAINING: { cls: 'cat-workshop', label: 'Training' },
  CULTURAL: { cls: 'cat-cultural', label: 'Cultural' },
  SPORTS: { cls: 'cat-sports', label: 'Sports' },
  INTER_COLLEGE: { cls: 'cat-symposium', label: 'Inter-College' },
  INTRA_COLLEGE: { cls: 'cat-coding', label: 'Intra-College' },
  OTHER: { cls: 'cat-default', label: 'Event' },
};

const STATUS_CLS = {
  UPCOMING: 'badge-green',
  ONGOING: 'badge-blue',
  COMPLETED: 'badge-gray',
  CANCELLED: 'badge-red',
  PUBLISHED: 'badge-blue',
  DRAFT: 'badge-yellow',
};

export default function EventCard({ event, compact = false }) {
  const dateStr = event.eventDate ? format(new Date(event.eventDate), 'EEE, MMM d, yyyy') : '-';
  const isFree = !event.ticketPrice || Number(event.ticketPrice) === 0;
  const cat = CAT[event.category] ?? CAT.OTHER;

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
              className="flex h-full w-full items-center justify-center"
              style={{ background: 'linear-gradient(135deg,#E0F2FE,#F8FAFC,#CCFBF1)' }}
            >
              <MdSchool className="h-12 w-12 text-slate-300" />
            </div>
          )}

          <div className="absolute left-3 top-3 flex flex-col gap-1.5">
            <span className={`badge ${STATUS_CLS[event.status] ?? 'badge-blue'}`}>{event.status}</span>
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
            <span className="rounded-lg border border-teal-100 bg-teal-50 px-3 py-1.5 text-[11px] font-bold text-teal-700 transition-all duration-200 group-hover:bg-teal-600 group-hover:text-white">
              Register
            </span>
          </div>
        </div>
      </Link>
    </motion.div>
  );
}
