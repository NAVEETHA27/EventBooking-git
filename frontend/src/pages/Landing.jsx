import { useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from 'react-query';
import { motion, useReducedMotion } from 'framer-motion';
import {
  FiArrowRight,
  FiAward,
  FiBarChart2,
  FiCalendar,
  FiCheckCircle,
  FiClock,
  FiMapPin,
  FiSearch,
  FiShield,
  FiSliders,
  FiStar,
  FiUsers,
} from 'react-icons/fi';
import { MdSchool } from 'react-icons/md';
import { eventsAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';
import EventCard from '../components/common/EventCard';
import Spinner from '../components/common/Spinner';

const fadeUp = {
  hidden: { opacity: 0, y: 28 },
  show: { opacity: 1, y: 0, transition: { duration: 0.55, ease: [0.22, 1, 0.36, 1] } },
};

const stagger = {
  hidden: {},
  show: { transition: { staggerChildren: 0.08 } },
};

const categories = [
  { label: 'Hackathons', value: 'HACKATHON', icon: FiBarChart2, tone: 'blue' },
  { label: 'Workshops', value: 'WORKSHOP', icon: FiSliders, tone: 'green' },
  { label: 'Symposiums', value: 'TECHNICAL_SYMPOSIUM', icon: FiStar, tone: 'violet' },
  { label: 'Coding', value: 'CODING_COMPETITION', icon: FiShield, tone: 'orange' },
  { label: 'Seminars', value: 'SEMINAR', icon: FiUsers, tone: 'rose' },
  { label: 'Exhibitions', value: 'PROJECT_EXHIBITION', icon: FiAward, tone: 'teal' },
];

const journey = [
  { title: 'Discover', text: 'Filter verified college events by category, location, date, and eligibility.' },
  { title: 'Register', text: 'Reserve a seat quickly while keeping your booking history in one place.' },
  { title: 'Attend', text: 'Use clear event details, venue context, and ticket information before the day.' },
  { title: 'Grow', text: 'Track participation, certificates, and event outcomes from your dashboard.' },
];

const organizerFeatures = [
  'Publish events with pricing, seats, venue, and certificate details',
  'Monitor bookings, attendees, and high-level event performance',
  'Keep participants informed through the existing notification flow',
];

const quickTags = ['Hackathon', 'Workshop', 'Symposium', 'Free Events'];

function Reveal({ children, className = '' }) {
  const reduceMotion = useReducedMotion();
  return (
    <motion.div
      initial={reduceMotion ? false : 'hidden'}
      whileInView="show"
      viewport={{ once: true, margin: '-80px' }}
      variants={fadeUp}
      className={className}
    >
      {children}
    </motion.div>
  );
}

export default function Landing() {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [search, setSearch] = useState('');
  const [college, setCollege] = useState('');

  const { data, isLoading } = useQuery(
    'events-landing',
    () => eventsAPI.search({ size: 8, status: 'PUBLISHED' }).then((r) => r.data?.data),
    { staleTime: 60_000, retry: 1 }
  );

  const events = useMemo(() => data?.content ?? [], [data]);

  const handleSearch = (event) => {
    event.preventDefault();
    const params = new URLSearchParams();
    if (search.trim()) params.set('keyword', search.trim());
    if (college.trim()) params.set('collegeName', college.trim());
    navigate(`/events?${params.toString()}`);
  };

  return (
    <div className="landing-shell">
      <section className="landing-hero">
        <div className="hero-media" aria-hidden="true">
          <img
            src="https://images.unsplash.com/photo-1523580494863-6f3031224c94?auto=format&fit=crop&w=1600&q=80"
            alt=""
          />
          <div className="hero-overlay" />
        </div>

        <motion.div
          variants={stagger}
          initial="hidden"
          animate="show"
          className="relative z-10 mx-auto grid min-h-[calc(100vh-4rem)] max-w-7xl items-center gap-10 px-4 py-16 sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:px-8"
        >
          <div className="max-w-3xl text-white">
            <motion.div variants={fadeUp} className="hero-kicker">
              <FiCheckCircle className="h-4 w-4" />
              Trusted campus event booking
            </motion.div>
            <motion.h1 variants={fadeUp} className="hero-title">
              Discover, book, and manage college events with less friction.
            </motion.h1>
            <motion.p variants={fadeUp} className="hero-copy">
              A polished event hub for students and organizers: find relevant events, reserve seats, and keep every
              booking detail easy to scan.
            </motion.p>

            <motion.form variants={fadeUp} onSubmit={handleSearch} className="hero-search">
              <label className="search-field">
                <FiSearch className="h-4 w-4" />
                <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search event type" />
              </label>
              <label className="search-field">
                <MdSchool className="h-4 w-4" />
                <input value={college} onChange={(e) => setCollege(e.target.value)} placeholder="College name" />
              </label>
              <motion.button whileHover={{ y: -2 }} whileTap={{ scale: 0.98 }} type="submit" className="search-button">
                Search
                <FiArrowRight className="h-4 w-4" />
              </motion.button>
            </motion.form>

            <motion.div variants={fadeUp} className="mt-5 flex flex-wrap gap-2">
              {quickTags.map((tag) => (
                <button key={tag} onClick={() => navigate(`/events?keyword=${encodeURIComponent(tag)}`)} className="quick-chip">
                  {tag}
                </button>
              ))}
            </motion.div>
          </div>

          <motion.div variants={fadeUp} className="hero-panel">
            <div className="flex items-center justify-between border-b border-slate-200 pb-4">
              <div>
                <p className="text-xs font-bold uppercase tracking-[0.18em] text-slate-400">Live Overview</p>
                <h2 className="mt-1 text-xl font-bold text-slate-950">Campus Activity</h2>
              </div>
              <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-bold text-emerald-700">Active</span>
            </div>
            <div className="mt-5 grid grid-cols-2 gap-3">
              {[
                ['500+', 'Events hosted'],
                ['10K+', 'Students reached'],
                ['200+', 'Colleges'],
                ['95%', 'Satisfaction'],
              ].map(([value, label]) => (
                <div key={label} className="metric-tile">
                  <div className="text-2xl font-black text-slate-950">{value}</div>
                  <div className="mt-1 text-xs font-medium text-slate-500">{label}</div>
                </div>
              ))}
            </div>
            <div className="mt-5 space-y-3">
              {journey.slice(0, 3).map((item, index) => (
                <div key={item.title} className="timeline-row">
                  <span>{index + 1}</span>
                  <div>
                    <p className="font-bold text-slate-900">{item.title}</p>
                    <p className="text-sm text-slate-500">{item.text}</p>
                  </div>
                </div>
              ))}
            </div>
          </motion.div>
        </motion.div>
      </section>

      <section className="bg-white py-16">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <Reveal className="section-heading">
            <p>For Every Campus Role</p>
            <h2>Two clear paths, one professional event flow.</h2>
          </Reveal>
          <div className="mt-9 grid gap-5 lg:grid-cols-2">
            <Reveal className="role-card role-card-student">
              <div className="role-icon"><FiUsers /></div>
              <h3>Students</h3>
              <p>Browse events, compare the essentials, book a seat, and return to your dashboard for history.</p>
              <Link to={user ? '/events' : '/register'} className="role-link">
                Start exploring <FiArrowRight />
              </Link>
            </Reveal>
            <Reveal className="role-card role-card-organizer">
              <div className="role-icon"><FiCalendar /></div>
              <h3>Organizers</h3>
              <p>Create event pages that feel credible, then manage bookings and attendee details in the same system.</p>
              <Link to={user ? '/organizer/dashboard' : '/register?role=organizer'} className="role-link">
                Organize an event <FiArrowRight />
              </Link>
            </Reveal>
          </div>
        </div>
      </section>

      <section className="landing-band py-16">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <Reveal className="section-heading">
            <p>Browse By Intent</p>
            <h2>Find the right opportunity without digging.</h2>
          </Reveal>
          <div className="mt-9 grid grid-cols-2 gap-4 md:grid-cols-3 lg:grid-cols-6">
            {categories.map((category, index) => {
              const Icon = category.icon;
              return (
                <motion.button
                  key={category.value}
                  initial={{ opacity: 0, y: 22 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: index * 0.04, duration: 0.42 }}
                  whileHover={{ y: -6 }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => navigate(`/events?category=${category.value}`)}
                  className={`category-card tone-${category.tone}`}
                >
                  <Icon className="h-5 w-5" />
                  <span>{category.label}</span>
                </motion.button>
              );
            })}
          </div>
        </div>
      </section>

      <section className="bg-white py-16">
        <div className="mx-auto grid max-w-7xl gap-10 px-4 sm:px-6 lg:grid-cols-[0.9fr_1.1fr] lg:px-8">
          <Reveal className="section-heading text-left">
            <p>Designed For Momentum</p>
            <h2>Professional pages for browsing, booking, and operations.</h2>
            <span>
              The interface keeps key decisions visible: date, venue, seats, price, status, and organizer context.
            </span>
          </Reveal>
          <div className="grid gap-4 sm:grid-cols-2">
            {[
              { icon: FiSearch, title: 'Fast discovery', text: 'Search and filters guide users to relevant events quickly.' },
              { icon: FiClock, title: 'Clear timing', text: 'Dates and availability are presented prominently.' },
              { icon: FiMapPin, title: 'Venue context', text: 'College, department, and location details stay easy to scan.' },
              { icon: FiShield, title: 'Managed access', text: 'Authentication and role flows remain unchanged underneath.' },
            ].map((feature) => {
              const Icon = feature.icon;
              return (
                <Reveal key={feature.title} className="feature-card">
                  <Icon className="h-5 w-5" />
                  <h3>{feature.title}</h3>
                  <p>{feature.text}</p>
                </Reveal>
              );
            })}
          </div>
        </div>
      </section>

      <section id="events" className="landing-band py-16">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <Reveal className="mb-9 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
            <div className="section-heading m-0 text-left">
              <p>Featured Events</p>
              <h2>Upcoming events worth a closer look.</h2>
            </div>
            <Link to="/events" className="view-all-link">
              View all events <FiArrowRight />
            </Link>
          </Reveal>

          {isLoading ? (
            <div className="flex justify-center py-16"><Spinner /></div>
          ) : events.length ? (
            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
              {events.slice(0, 8).map((event, index) => (
                <motion.div
                  key={event.id}
                  initial={{ opacity: 0, y: 24 }}
                  whileInView={{ opacity: 1, y: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: index * 0.05, duration: 0.45 }}
                >
                  <EventCard event={event} />
                </motion.div>
              ))}
            </div>
          ) : (
            <div className="empty-state">
              <FiCalendar className="h-8 w-8" />
              <p>No published events are available yet.</p>
            </div>
          )}
        </div>
      </section>

      <section className="bg-white py-16">
        <div className="mx-auto grid max-w-7xl gap-8 px-4 sm:px-6 lg:grid-cols-[1fr_0.9fr] lg:px-8">
          <Reveal className="closing-panel">
            <p className="text-xs font-bold uppercase tracking-[0.2em] text-cyan-200">Organizer Toolkit</p>
            <h2>Host with a cleaner, more confident front door.</h2>
            <ul>
              {organizerFeatures.map((feature) => (
                <li key={feature}><FiCheckCircle /> {feature}</li>
              ))}
            </ul>
            <Link to="/register?role=organizer" className="closing-cta">
              Get started <FiArrowRight />
            </Link>
          </Reveal>
          <Reveal className="quote-panel">
            <FiStar className="h-8 w-8 text-amber-400" />
            <p>
              Clean event pages reduce uncertainty. Students see what matters, organizers keep control, and the system
              still does exactly what it was built to do.
            </p>
          </Reveal>
        </div>
      </section>
    </div>
  );
}
