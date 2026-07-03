import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useQuery } from 'react-query';
import { AnimatePresence, motion } from 'framer-motion';
import { eventsAPI } from '../services/api';
import EventCard from '../components/common/EventCard';
import Spinner from '../components/common/Spinner';
import QueryError from '../components/common/QueryError';
import {
  FiCalendar,
  FiChevronDown,
  FiFilter,
  FiMapPin,
  FiSearch,
  FiSliders,
  FiTrendingUp,
  FiX,
} from 'react-icons/fi';

const CATS = ['HACKATHON', 'WORKSHOP', 'TECHNICAL_SYMPOSIUM', 'CODING_COMPETITION', 'SEMINAR',
  'PROJECT_EXHIBITION', 'PLACEMENT_PREP', 'TECHNICAL_TRAINING', 'CULTURAL', 'SPORTS',
  'INTER_COLLEGE', 'INTRA_COLLEGE', 'OTHER'];
const CAT_LABELS = {
  HACKATHON: 'Hackathon',
  WORKSHOP: 'Workshop',
  TECHNICAL_SYMPOSIUM: 'Symposium',
  CODING_COMPETITION: 'Coding Comp',
  SEMINAR: 'Seminar',
  PROJECT_EXHIBITION: 'Project Expo',
  PLACEMENT_PREP: 'Placement Prep',
  TECHNICAL_TRAINING: 'Training',
  CULTURAL: 'Cultural',
  SPORTS: 'Sports',
  INTER_COLLEGE: 'Inter-College',
  INTRA_COLLEGE: 'Intra-College',
  OTHER: 'Other',
};
const SORT = [
  { value: 'newest', label: 'Newest', icon: FiTrendingUp },
  { value: 'popular', label: 'Popular', icon: FiTrendingUp },
  { value: 'price_asc', label: 'Price Low', icon: FiSliders },
  { value: 'price_desc', label: 'Price High', icon: FiSliders },
  { value: 'date_asc', label: 'Date', icon: FiCalendar },
  { value: 'date_desc', label: 'Latest Date', icon: FiCalendar },
  { value: 'oldest', label: 'Oldest', icon: FiTrendingUp },
];
const MODES = [
  { label: 'Online', value: 'ONLINE' },
  { label: 'Offline', value: 'OFFLINE' },
  { label: 'Hybrid', value: 'HYBRID' },
];
const AVAILABILITY = [
  { label: 'Available', value: 'available' },
  { label: 'Almost Full', value: 'almostFull' },
  { label: 'Sold Out', value: 'soldOut' },
];
const initialFilters = {
  keyword: '',
  category: '',
  collegeName: '',
  departmentName: '',
  location: '',
  venueName: '',
  dateFrom: '',
  dateTo: '',
  eventType: '',
  priceMin: '',
  priceMax: '',
  freeOnly: false,
  paidOnly: false,
  interCollege: false,
  intraCollege: false,
  past: false,
  sortBy: 'date_asc',
  availability: '',
};

export default function Events() {
  const [searchParams] = useSearchParams();
  const [filtersOpen, setFiltersOpen] = useState(false);
  const [page, setPage] = useState(0);
  const [inputKw, setInputKw] = useState(searchParams.get('keyword') || '');
  const [filters, setFilters] = useState({
    ...initialFilters,
    keyword: searchParams.get('keyword') || '',
    category: searchParams.get('category') || '',
    collegeName: searchParams.get('collegeName') || '',
    departmentName: searchParams.get('departmentName') || '',
  });

  useEffect(() => {
    const t = setTimeout(() => {
      setFilter('keyword', inputKw, false);
    }, 360);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [inputKw]);

  const apiFilters = useMemo(() => {
    const { availability, ...rest } = filters;
    return rest;
  }, [filters]);

  const { data, isLoading, isFetching, isError, error, refetch } = useQuery(
    ['events', apiFilters, page],
    () => eventsAPI.search({ ...apiFilters, page, size: 12 }).then(r => r.data?.data),
    { keepPreviousData: true, retry: 1 }
  );

  const displayedEvents = useMemo(() => {
    let list = data?.content ?? [];
    if (filters.availability) {
      list = list.filter(ev => {
        const total = Number(ev.totalSeats ?? ev.capacity ?? 0);
        const available = Number(ev.availableSeats ?? 0);
        if (filters.availability === 'soldOut') return total > 0 && available <= 0;
        if (filters.availability === 'almostFull') return total > 0 && available > 0 && available / total <= 0.2;
        return available > 0 || total === 0;
      });
    }
    if (filters.sortBy === 'alphabetical') {
      list = [...list].sort((a, b) => String(a.eventName || '').localeCompare(String(b.eventName || '')));
    }
    return list;
  }, [data?.content, filters.availability, filters.sortBy]);

  const setFilter = (key, value, resetPage = true) => {
    setFilters(f => ({ ...f, [key]: value }));
    if (resetPage) setPage(0);
  };

  const clearAll = () => {
    setFilters(initialFilters);
    setInputKw('');
    setPage(0);
  };

  const activeBadges = useMemo(() => buildBadges(filters), [filters]);
  const activeCount = activeBadges.length;

  return (
    <div className="min-h-screen bg-[#F0F4FF]">
      <div className="border-b border-blue-100 bg-white px-4 py-8">
        <div className="mx-auto max-w-7xl">
          <p className="mb-1 text-xs font-bold uppercase tracking-widest text-blue-500">Discover</p>
          <h1 className="section-title mb-1">College Events</h1>
          <p className="text-sm text-gray-500">
            {data?.totalElements != null ? `${data.totalElements.toLocaleString()} events found` : 'Find your next opportunity'}
          </p>
        </div>
      </div>

      <div className="sticky top-16 z-30 border-b border-blue-100/80 bg-[#F0F4FF]/90 px-4 py-4 backdrop-blur-xl sm:px-6 lg:px-8">
        <div className="mx-auto max-w-7xl">
          <div className="flex flex-col gap-3 lg:flex-row">
            <label className="relative flex-1" aria-label="Search events">
              <motion.span animate={{ x: inputKw ? 2 : 0, rotate: inputKw ? -8 : 0 }} className="absolute left-4 top-1/2 -translate-y-1/2 text-blue-500">
                <FiSearch className="h-5 w-5" />
              </motion.span>
              <input
                value={inputKw}
                onChange={e => setInputKw(e.target.value)}
                placeholder="Search events, colleges, venues..."
                className="h-12 w-full rounded-2xl border border-blue-100 bg-white pl-12 pr-4 text-sm shadow-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100"
              />
            </label>

            <Segmented value={filters.sortBy} onChange={v => setFilter('sortBy', v)} options={[...SORT, { value: 'alphabetical', label: 'A-Z', icon: FiSliders }]} ariaLabel="Sort events" />

            <motion.button
              whileHover={{ scale: 1.02 }}
              whileTap={{ scale: 0.97 }}
              onClick={() => setFiltersOpen(v => !v)}
              className="inline-flex h-12 items-center justify-center gap-2 rounded-2xl border border-blue-100 bg-white px-4 text-sm font-bold text-blue-800 shadow-sm transition hover:bg-blue-50 focus:outline-none focus:ring-4 focus:ring-blue-100"
              aria-expanded={filtersOpen}
              aria-controls="event-filter-panel"
            >
              <FiFilter />
              Filters
              {activeCount > 0 && <span className="grid h-5 w-5 place-items-center rounded-full bg-blue-700 text-[10px] text-white">{activeCount}</span>}
            </motion.button>
          </div>

          <AnimatePresence>
            {activeBadges.length > 0 && (
              <motion.div initial={{ opacity: 0, height: 0 }} animate={{ opacity: 1, height: 'auto' }} exit={{ opacity: 0, height: 0 }} className="mt-3 flex flex-wrap gap-2 overflow-hidden">
                {activeBadges.map(b => (
                  <button
                    key={b.key}
                    onClick={() => setFilter(b.key, b.empty)}
                    className="inline-flex items-center gap-1.5 rounded-full border border-blue-100 bg-white px-3 py-1.5 text-xs font-semibold text-blue-800 shadow-sm transition hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-300"
                  >
                    {b.label}
                    <FiX className="h-3.5 w-3.5" />
                  </button>
                ))}
                <button onClick={clearAll} className="rounded-full px-3 py-1.5 text-xs font-bold text-red-500 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-200">
                  Clear all
                </button>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>

      <main className="mx-auto max-w-7xl px-4 py-7 sm:px-6 lg:px-8">
        <AnimatePresence>
          {filtersOpen && (
            <motion.section
              id="event-filter-panel"
              initial={{ opacity: 0, y: -10, height: 0 }}
              animate={{ opacity: 1, y: 0, height: 'auto' }}
              exit={{ opacity: 0, y: -10, height: 0 }}
              transition={{ duration: 0.24 }}
              className="mb-6 overflow-hidden"
            >
              <div className="rounded-2xl border border-blue-100 bg-white p-5 shadow-card">
                <div className="mb-5 flex items-center justify-between gap-3">
                  <div>
                    <h2 className="text-lg font-extrabold text-blue-950">Refine events</h2>
                    <p className="text-xs text-slate-500">Filters update results automatically.</p>
                  </div>
                  <button onClick={() => setFiltersOpen(false)} className="rounded-full p-2 text-slate-500 hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-blue-300" aria-label="Close filters">
                    <FiX />
                  </button>
                </div>

                <div className="grid gap-5 lg:grid-cols-[1.35fr_1fr_1fr]">
                  <FilterGroup title="Category">
                    <div className="flex flex-wrap gap-2">
                      {CATS.map(cat => (
                        <Chip key={cat} active={filters.category === cat} onClick={() => setFilter('category', filters.category === cat ? '' : cat)}>
                          {CAT_LABELS[cat]}
                        </Chip>
                      ))}
                    </div>
                  </FilterGroup>

                  <FilterGroup title="Date">
                    <div className="grid grid-cols-2 gap-2">
                      <DateInput label="From" value={filters.dateFrom} onChange={v => setFilter('dateFrom', v)} />
                      <DateInput label="To" value={filters.dateTo} onChange={v => setFilter('dateTo', v)} />
                    </div>
                    <Toggle active={filters.past} onClick={() => setFilter('past', !filters.past)}>Include past events</Toggle>
                  </FilterGroup>

                  <FilterGroup title="Location">
                    <IconInput icon={FiMapPin} value={filters.location} onChange={v => setFilter('location', v)} placeholder="Search city or area" />
                    <IconInput value={filters.venueName} onChange={v => setFilter('venueName', v)} placeholder="Venue name" />
                  </FilterGroup>

                  <FilterGroup title="Price">
                    <div className="flex gap-2">
                      <Chip active={filters.freeOnly} onClick={() => { setFilter('freeOnly', !filters.freeOnly); if (!filters.freeOnly) setFilter('paidOnly', false); }}>Free</Chip>
                      <Chip active={filters.paidOnly} onClick={() => { setFilter('paidOnly', !filters.paidOnly); if (!filters.paidOnly) setFilter('freeOnly', false); }}>Paid</Chip>
                    </div>
                    <div className="grid grid-cols-2 gap-2">
                      <IconInput type="number" min="0" value={filters.priceMin} onChange={v => setFilter('priceMin', v)} placeholder="Min ₹" />
                      <IconInput type="number" min="0" value={filters.priceMax} onChange={v => setFilter('priceMax', v)} placeholder="Max ₹" />
                    </div>
                    <input aria-label="Maximum price" type="range" min="0" max="10000" step="100" value={filters.priceMax || 0} onChange={e => setFilter('priceMax', e.target.value)} className="w-full accent-blue-700" />
                  </FilterGroup>

                  <FilterGroup title="Mode">
                    <div className="flex flex-wrap gap-2">
                      {MODES.map(m => <Chip key={m.value} active={filters.eventType === m.value} onClick={() => setFilter('eventType', filters.eventType === m.value ? '' : m.value)}>{m.label}</Chip>)}
                    </div>
                  </FilterGroup>

                  <FilterGroup title="Availability">
                    <div className="flex flex-wrap gap-2">
                      {AVAILABILITY.map(a => <Chip key={a.value} active={filters.availability === a.value} onClick={() => setFilter('availability', filters.availability === a.value ? '' : a.value)}>{a.label}</Chip>)}
                    </div>
                  </FilterGroup>

                  <FilterGroup title="Organizer / College">
                    <IconInput value={filters.collegeName} onChange={v => setFilter('collegeName', v)} placeholder="College or organizer" />
                    <IconInput value={filters.departmentName} onChange={v => setFilter('departmentName', v)} placeholder="Department" />
                  </FilterGroup>

                  <FilterGroup title="Audience">
                    <div className="flex flex-wrap gap-2">
                      <Chip active={filters.interCollege} onClick={() => setFilter('interCollege', !filters.interCollege)}>Inter College</Chip>
                      <Chip active={filters.intraCollege} onClick={() => setFilter('intraCollege', !filters.intraCollege)}>Intra College</Chip>
                    </div>
                  </FilterGroup>
                </div>
              </div>
            </motion.section>
          )}
        </AnimatePresence>

        <div className="mb-4 flex items-center justify-between gap-3">
          <p className="text-sm font-semibold text-slate-600">
            {isFetching ? 'Updating results...' : `${displayedEvents.length} shown${data?.totalElements != null ? ` · ${data.totalElements} total` : ''}`}
          </p>
          {isFetching && <span className="h-2 w-28 overflow-hidden rounded-full bg-blue-100"><motion.span animate={{ x: ['-40%', '120%'] }} transition={{ repeat: Infinity, duration: 1.1 }} className="block h-full w-12 rounded-full bg-blue-600" /></span>}
        </div>

        {isLoading ? <SkeletonGrid /> : isError ? (
          <QueryError message={error?.response?.data?.message} onRetry={refetch} />
        ) : (
          <>
            <motion.div layout className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3">
              {displayedEvents.map(ev => (
                <motion.div key={ev.id} layout initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} whileHover={{ y: -4 }} transition={{ duration: 0.18 }}>
                  <EventCard event={ev} />
                </motion.div>
              ))}
            </motion.div>

            {!displayedEvents.length && (
              <motion.div initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} className="rounded-2xl border border-blue-100 bg-white py-20 text-center shadow-card">
                <div className="mx-auto mb-5 grid h-20 w-20 place-items-center rounded-full bg-blue-50 text-blue-700">
                  <FiSearch className="h-9 w-9" />
                </div>
                <h3 className="mb-2 text-xl font-bold text-blue-950">No events found</h3>
                <p className="mb-6 text-sm text-gray-500">Try clearing a filter or searching for another event.</p>
                <button onClick={clearAll} className="btn-primary">Clear Filters</button>
              </motion.div>
            )}

            {(data?.totalPages ?? 0) > 1 && (
              <div className="mt-10 flex items-center justify-center gap-3">
                <button onClick={() => setPage(p => p - 1)} disabled={data.first || isFetching} className="btn-outline px-5 py-2 text-sm disabled:opacity-40">Prev</button>
                <span className="text-sm font-medium text-gray-500">{data.page + 1} / {data.totalPages}</span>
                <button onClick={() => setPage(p => p + 1)} disabled={data.last || isFetching} className="btn-outline px-5 py-2 text-sm disabled:opacity-40">Next</button>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}

function FilterGroup({ title, children }) {
  const [open, setOpen] = useState(true);
  return (
    <div className="rounded-2xl border border-slate-100 bg-slate-50/70 p-4">
      <button type="button" onClick={() => setOpen(!open)} className="mb-3 flex w-full items-center justify-between text-left focus:outline-none focus:ring-2 focus:ring-blue-300" aria-expanded={open}>
        <span className="text-xs font-extrabold uppercase tracking-wider text-blue-700">{title}</span>
        <motion.span animate={{ rotate: open ? 180 : 0 }}><FiChevronDown className="text-slate-400" /></motion.span>
      </button>
      <AnimatePresence initial={false}>
        {open && <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="space-y-3 overflow-hidden">{children}</motion.div>}
      </AnimatePresence>
    </div>
  );
}

function Chip({ active, onClick, children }) {
  return (
    <motion.button
      type="button"
      whileHover={{ scale: 1.03 }}
      whileTap={{ scale: 0.96 }}
      onClick={onClick}
      className={`rounded-full border px-3 py-1.5 text-xs font-bold transition focus:outline-none focus:ring-2 focus:ring-blue-300 ${active ? 'border-blue-700 bg-blue-700 text-white shadow-md shadow-blue-700/20' : 'border-blue-100 bg-white text-slate-600 hover:bg-blue-50'}`}
      aria-pressed={active}
    >
      {children}
    </motion.button>
  );
}

function Toggle({ active, onClick, children }) {
  return (
    <button type="button" onClick={onClick} className="flex items-center gap-2 text-sm text-slate-600 focus:outline-none focus:ring-2 focus:ring-blue-300">
      <span className={`h-5 w-9 rounded-full p-0.5 transition ${active ? 'bg-blue-700' : 'bg-slate-300'}`}>
        <motion.span animate={{ x: active ? 16 : 0 }} className="block h-4 w-4 rounded-full bg-white shadow" />
      </span>
      {children}
    </button>
  );
}

function IconInput({ icon: Icon, value, onChange, placeholder, type = 'text', ...props }) {
  return (
    <label className="relative block">
      {Icon && <Icon className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-blue-500" />}
      <input
        type={type}
        value={value}
        onChange={e => onChange(e.target.value)}
        placeholder={placeholder}
        className={`h-10 w-full rounded-xl border border-blue-100 bg-white px-3 text-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100 ${Icon ? 'pl-9' : ''}`}
        {...props}
      />
    </label>
  );
}

function DateInput({ label, value, onChange }) {
  return (
    <label className="block">
      <span className="mb-1 block text-[10px] font-bold uppercase tracking-wider text-slate-400">{label}</span>
      <input type="date" value={value} onChange={e => onChange(e.target.value)} className="h-10 w-full rounded-xl border border-blue-100 bg-white px-3 text-sm outline-none transition focus:border-blue-400 focus:ring-4 focus:ring-blue-100" />
    </label>
  );
}

function Segmented({ value, onChange, options, ariaLabel }) {
  return (
    <div className="flex h-12 max-w-full items-center gap-1 overflow-x-auto rounded-2xl border border-blue-100 bg-white p-1 shadow-sm" role="radiogroup" aria-label={ariaLabel}>
      {options.slice(0, 5).map(({ value: v, label, icon: Icon }) => (
        <button key={v} type="button" role="radio" aria-checked={value === v} onClick={() => onChange(v)} className={`relative flex h-9 shrink-0 items-center gap-1.5 rounded-xl px-3 text-xs font-bold transition focus:outline-none focus:ring-2 focus:ring-blue-300 ${value === v ? 'text-white' : 'text-slate-600 hover:bg-blue-50'}`}>
          {value === v && <motion.span layoutId="sort-pill" className="absolute inset-0 rounded-xl bg-blue-700" />}
          <span className="relative flex items-center gap-1.5">{Icon && <Icon className="h-3.5 w-3.5" />}{label}</span>
        </button>
      ))}
    </div>
  );
}

function SkeletonGrid() {
  return (
    <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 xl:grid-cols-3" aria-label="Loading events">
      {Array.from({ length: 6 }).map((_, i) => <div key={i} className="h-72 animate-pulse rounded-2xl bg-white shadow-card"><div className="h-36 rounded-t-2xl bg-blue-100/70" /><div className="space-y-3 p-4"><div className="h-4 w-2/3 rounded bg-slate-100" /><div className="h-3 rounded bg-slate-100" /><div className="h-3 w-1/2 rounded bg-slate-100" /></div></div>)}
    </div>
  );
}

function buildBadges(filters) {
  const labels = {
    category: filters.category ? CAT_LABELS[filters.category] : '',
    eventType: filters.eventType ? MODES.find(m => m.value === filters.eventType)?.label : '',
    collegeName: filters.collegeName,
    departmentName: filters.departmentName,
    location: filters.location,
    venueName: filters.venueName,
    dateFrom: filters.dateFrom ? `From ${filters.dateFrom}` : '',
    dateTo: filters.dateTo ? `To ${filters.dateTo}` : '',
    priceMin: filters.priceMin ? `Min ₹${filters.priceMin}` : '',
    priceMax: filters.priceMax ? `Max ₹${filters.priceMax}` : '',
    availability: filters.availability ? AVAILABILITY.find(a => a.value === filters.availability)?.label : '',
    freeOnly: filters.freeOnly ? 'Free' : '',
    paidOnly: filters.paidOnly ? 'Paid' : '',
    interCollege: filters.interCollege ? 'Inter College' : '',
    intraCollege: filters.intraCollege ? 'Intra College' : '',
    past: filters.past ? 'Past events' : '',
  };
  return Object.entries(labels)
    .filter(([, label]) => Boolean(label))
    .map(([key, label]) => ({ key, label, empty: typeof filters[key] === 'boolean' ? false : '' }));
}
