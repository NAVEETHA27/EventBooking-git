import { useState, useMemo } from 'react';
import { useQuery } from 'react-query';
import { motion } from 'framer-motion';
import { paymentsAPI } from '../../services/api';
import {
  FiRefreshCw, FiAlertTriangle, FiCheckCircle, FiClock,
  FiSearch, FiFilter, FiChevronDown,
} from 'react-icons/fi';

const fmt = (v) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(Number(v || 0));

const REFUND_STEPS = ['REFUND_REQUESTED', 'UNDER_VERIFICATION', 'APPROVED', 'PROCESSING', 'COMPLETED'];

const STATUS_META = {
  INITIATED:          { color: '#546E7A', bg: '#ECEFF1', label: 'Initiated',          icon: <FiClock /> },
  REFUND_REQUESTED:   { color: '#1565C0', bg: '#E3F2FD', label: 'Refund Requested',   icon: <FiClock /> },
  UNDER_VERIFICATION: { color: '#7B1FA2', bg: '#F3E5F5', label: 'Under Verification', icon: <FiClock /> },
  APPROVED:           { color: '#2E7D32', bg: '#E8F5E9', label: 'Approved',           icon: <FiCheckCircle /> },
  PROCESSING:         { color: '#1565C0', bg: '#E3F2FD', label: 'Processing',         icon: <FiRefreshCw className="animate-spin" /> },
  COMPLETED:          { color: '#2E7D32', bg: '#E8F5E9', label: 'Completed',          icon: <FiCheckCircle /> },
  REJECTED:           { color: '#C62828', bg: '#FFEBEE', label: 'Rejected',           icon: <FiAlertTriangle /> },
};

const SORT_OPTIONS = [
  { value: 'newest',  label: 'Newest First' },
  { value: 'oldest',  label: 'Oldest First' },
  { value: 'amount_desc', label: 'Amount: High to Low' },
  { value: 'amount_asc',  label: 'Amount: Low to High' },
];

export default function RefundTracking() {
  const [search, setSearch]       = useState('');
  const [filterStatus, setFilter] = useState('ALL');
  const [sortBy, setSort]         = useState('newest');

  const { data, isLoading, refetch } = useQuery(
    'my-refunds',
    () => paymentsAPI.myRefunds().then(r => r.data?.data ?? []),
    { staleTime: 30_000 }
  );

  const allRefunds = Array.isArray(data) ? data : [];

  const refunds = useMemo(() => {
    let list = [...allRefunds];

    // Search
    if (search.trim()) {
      const s = search.toLowerCase();
      list = list.filter(r =>
        (r.eventName || '').toLowerCase().includes(s) ||
        (r.refundId  || '').toLowerCase().includes(s) ||
        (r.reason    || '').toLowerCase().includes(s)
      );
    }

    // Filter by status
    if (filterStatus !== 'ALL') {
      list = list.filter(r => (r.status || 'INITIATED') === filterStatus);
    }

    // Sort
    list.sort((a, b) => {
      if (sortBy === 'amount_desc') return Number(b.amount || 0) - Number(a.amount || 0);
      if (sortBy === 'amount_asc')  return Number(a.amount || 0) - Number(b.amount || 0);
      if (sortBy === 'oldest') return new Date(a.requestedAt || 0) - new Date(b.requestedAt || 0);
      return new Date(b.requestedAt || 0) - new Date(a.requestedAt || 0); // newest
    });

    return list;
  }, [allRefunds, search, filterStatus, sortBy]);

  const statusCounts = useMemo(() => {
    const counts = { ALL: allRefunds.length };
    allRefunds.forEach(r => {
      const s = r.status || 'INITIATED';
      counts[s] = (counts[s] || 0) + 1;
    });
    return counts;
  }, [allRefunds]);

  return (
    <div className="min-h-screen px-4 py-10" style={{ background: '#F0F4FF' }}>
      <div className="max-w-3xl mx-auto">

        {/* Header */}
        <motion.div initial={{ opacity: 0, y: -12 }} animate={{ opacity: 1, y: 0 }} className="mb-6">
          <h1 className="text-3xl font-extrabold text-blue-900" style={{ fontFamily: 'Space Grotesk,sans-serif' }}>
            Refund Tracking
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            {allRefunds.length} refund{allRefunds.length !== 1 ? 's' : ''} found
          </p>
        </motion.div>

        {/* Info banner */}
        <motion.div initial={{ opacity: 0, x: -8 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.05 }}
          className="bg-blue-50 border border-blue-200 rounded-2xl p-4 mb-5 flex items-start gap-3 text-sm text-blue-800">
          <FiAlertTriangle className="flex-shrink-0 mt-0.5 text-blue-500" />
          <span>Refunds typically take <strong>3–7 business days</strong> depending on your payment provider.</span>
        </motion.div>

        {/* Search + Filter + Sort */}
        <div className="bg-white rounded-2xl border border-blue-100 p-4 mb-5 space-y-3"
          style={{ boxShadow: '0 2px 12px rgba(21,101,192,0.06)' }}>

          {/* Search */}
          <div className="relative">
            <FiSearch className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search by event name, refund ID, or reason…"
              className="w-full pl-9 pr-4 py-2.5 rounded-xl border border-slate-200 bg-slate-50 text-sm outline-none focus:border-blue-400 focus:bg-white transition"
            />
          </div>

          {/* Filter + Sort row */}
          <div className="flex flex-wrap gap-2">
            {/* Status filter chips */}
            <div className="flex flex-wrap gap-1.5 flex-1">
              {['ALL', 'REFUND_REQUESTED', 'APPROVED', 'PROCESSING', 'COMPLETED', 'REJECTED'].map(s => (
                <button key={s} onClick={() => setFilter(s)}
                  className={`px-2.5 py-1 rounded-full text-xs font-semibold transition ${
                    filterStatus === s
                      ? 'bg-blue-600 text-white'
                      : 'bg-slate-100 text-slate-600 hover:bg-blue-50 hover:text-blue-700'
                  }`}>
                  {s === 'ALL' ? 'All' : (STATUS_META[s]?.label || s)}
                  {statusCounts[s] ? <span className="ml-1 opacity-70">({statusCounts[s]})</span> : null}
                </button>
              ))}
            </div>

            {/* Sort */}
            <div className="relative">
              <select value={sortBy} onChange={e => setSort(e.target.value)}
                className="appearance-none pl-3 pr-7 py-1.5 rounded-xl border border-slate-200 bg-white text-xs font-semibold text-slate-600 outline-none cursor-pointer">
                {SORT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
              </select>
              <FiChevronDown className="absolute right-2 top-1/2 -translate-y-1/2 h-3 w-3 text-slate-400 pointer-events-none" />
            </div>
          </div>
        </div>

        {/* List */}
        {isLoading ? (
          <div className="space-y-4">{[1, 2, 3].map(i => <div key={i} className="skeleton h-40 rounded-2xl" />)}</div>
        ) : refunds.length === 0 ? (
          <div className="bg-white rounded-3xl border border-blue-100 p-12 text-center"
            style={{ boxShadow: '0 4px 24px rgba(21,101,192,0.08)' }}>
            <FiCheckCircle className="w-12 h-12 text-gray-200 mx-auto mb-3" />
            <p className="font-semibold text-gray-400">
              {search || filterStatus !== 'ALL' ? 'No refunds match your filters.' : 'No refund requests found.'}
            </p>
            <p className="text-sm text-gray-400 mt-1">
              {search || filterStatus !== 'ALL' ? 'Try adjusting your search or filter.' : 'Cancellation refunds will appear here.'}
            </p>
          </div>
        ) : (
          refunds.map((refund, i) => <RefundCard key={refund.refundId || refund.id || i} refund={refund} index={i} />)
        )}
      </div>
    </div>
  );
}

function RefundCard({ refund, index }) {
  const status   = refund.status || 'INITIATED';
  const meta     = STATUS_META[status] || STATUS_META.INITIATED;
  const stepIdx  = REFUND_STEPS.indexOf(status);
  const rejected = status === 'REJECTED';

  return (
    <motion.div initial={{ opacity: 0, y: 16 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.06 }}
      className="bg-white rounded-3xl border border-blue-100 p-6 mb-4"
      style={{ boxShadow: '0 4px 24px rgba(21,101,192,0.08)' }}>

      {/* Header */}
      <div className="flex items-start justify-between gap-4 mb-4">
        <div className="flex-1 min-w-0">
          <div className="font-extrabold text-blue-900 text-base truncate">
            {refund.eventName || 'Event Booking'}
          </div>
          <div className="flex flex-wrap gap-3 text-xs text-gray-400 mt-0.5 font-mono">
            {refund.refundId && <span>Refund ID: {refund.refundId}</span>}
            {refund.transactionRef && <span>Txn: {refund.transactionRef}</span>}
          </div>
        </div>
        <span className="shrink-0 inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold"
          style={{ background: meta.bg, color: meta.color }}>
          {meta.icon} {meta.label}
        </span>
      </div>

      {/* Amount row */}
      <div className="flex flex-wrap gap-6 text-sm mb-5">
        <div>
          <span className="text-gray-400 text-xs block">Amount</span>
          <div className="font-extrabold text-blue-900 text-lg">{fmt(refund.amount)}</div>
        </div>
        {refund.requestedAt && (
          <div>
            <span className="text-gray-400 text-xs block">Requested</span>
            <div className="font-semibold text-gray-700">{new Date(refund.requestedAt).toLocaleDateString('en-IN')}</div>
          </div>
        )}
        {refund.expectedRefundDate && (
          <div>
            <span className="text-gray-400 text-xs block">Expected By</span>
            <div className="font-semibold text-green-700">{new Date(refund.expectedRefundDate).toLocaleDateString('en-IN')}</div>
          </div>
        )}
        {refund.reason && (
          <div className="flex-1 min-w-[120px]">
            <span className="text-gray-400 text-xs block">Reason</span>
            <div className="font-medium text-gray-700 text-xs">{refund.reason}</div>
          </div>
        )}
      </div>

      {/* Progress */}
      {!rejected && stepIdx >= 0 && (
        <div className="space-y-2">
          <div className="flex justify-between text-[10px] font-semibold text-gray-400 mb-1">
            {REFUND_STEPS.map(s => (
              <span key={s} className={stepIdx >= REFUND_STEPS.indexOf(s) ? 'text-blue-600' : ''}>
                {STATUS_META[s]?.label}
              </span>
            ))}
          </div>
          <div className="h-1.5 bg-blue-100 rounded-full overflow-hidden">
            <motion.div className="h-full rounded-full bg-blue-500"
              initial={{ width: 0 }}
              animate={{ width: `${((stepIdx + 1) / REFUND_STEPS.length) * 100}%` }}
              transition={{ duration: 0.8, ease: 'easeOut' }} />
          </div>
        </div>
      )}

      {/* Rejection reason */}
      {rejected && refund.reason && (
        <div className="mt-3 text-xs text-red-600 bg-red-50 rounded-xl px-4 py-2 border border-red-100">
          Rejection reason: {refund.reason}
        </div>
      )}
    </motion.div>
  );
}
