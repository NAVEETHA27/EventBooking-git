import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from 'react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { paymentsAPI } from '../../services/api';
import {
  FiSearch, FiFilter, FiRefreshCw, FiCreditCard, FiChevronLeft,
  FiChevronRight, FiCalendar, FiHash, FiDownload
} from 'react-icons/fi';

const fmt = (v) => new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:2}).format(Number(v||0));

const STATUS_STYLE = {
  PENDING:            'bg-amber-50  text-amber-700  ring-amber-200',
  PROCESSING:         'bg-blue-50   text-blue-700   ring-blue-200',
  SUCCESSFUL:         'bg-green-50  text-green-700  ring-green-200',
  FAILED:             'bg-red-50    text-red-600    ring-red-200',
  REFUNDED:           'bg-gray-100  text-gray-600   ring-gray-200',
  PARTIALLY_REFUNDED: 'bg-violet-50 text-violet-700 ring-violet-200',
};

const STATUS_OPTIONS = ['','PENDING','PROCESSING','SUCCESSFUL','FAILED','REFUNDED','PARTIALLY_REFUNDED'];

export default function PaymentHistory() {
  const [page,   setPage]   = useState(0);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [showFilter, setShowFilter] = useState(false);

  const { data, isLoading, isFetching, refetch } = useQuery(
    ['payment-history', page, status],
    () => paymentsAPI.history({ page, size: 10, status: status || undefined }).then(r => r.data?.data),
    { keepPreviousData: true }
  );

  const payments = data?.content ?? [];
  const filtered = search.trim()
    ? payments.filter(p =>
        (p.paymentId||'').toLowerCase().includes(search.toLowerCase()) ||
        (p.eventName||'').toLowerCase().includes(search.toLowerCase()) ||
        (p.gatewayTransactionId||'').toLowerCase().includes(search.toLowerCase())
      )
    : payments;

  return (
    <div className="min-h-screen px-4 py-10" style={{background:'#F0F4FF'}}>
      <div className="max-w-6xl mx-auto">

        {/* Header */}
        <motion.div initial={{opacity:0,y:-12}} animate={{opacity:1,y:0}} className="mb-7">
          <h1 className="text-3xl font-extrabold text-blue-900" style={{fontFamily:'Space Grotesk,sans-serif'}}>Payment History</h1>
          <p className="text-sm text-gray-500 mt-1">{data?.totalElements ?? 0} payment records</p>
        </motion.div>

        {/* Search + Filter bar */}
        <motion.div initial={{opacity:0,y:8}} animate={{opacity:1,y:0}} transition={{delay:0.05}}
          className="flex flex-wrap gap-3 mb-5">
          <div className="relative flex-1 min-w-[200px]">
            <FiSearch className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 pointer-events-none"/>
            <input value={search} onChange={e=>setSearch(e.target.value)}
              placeholder="Search by payment ID, event, transaction…"
              className="input-field pl-10 text-sm"/>
          </div>
          <motion.button whileTap={{scale:0.95}} onClick={() => setShowFilter(!showFilter)}
            className={`flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold border transition-all ${showFilter?'bg-blue-600 text-white border-blue-600':'bg-white text-blue-700 border-blue-200 hover:border-blue-400'}`}>
            <FiFilter/> Filter {status && `(${status})`}
          </motion.button>
          <motion.button whileTap={{scale:0.95}} onClick={() => refetch()}
            className="flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-semibold bg-white text-blue-700 border border-blue-200 hover:border-blue-400 transition-all">
            <FiRefreshCw className={isFetching?'animate-spin':''}/> Refresh
          </motion.button>
        </motion.div>

        {/* Filter panel */}
        <AnimatePresence>
          {showFilter && (
            <motion.div initial={{height:0,opacity:0}} animate={{height:'auto',opacity:1}} exit={{height:0,opacity:0}}
              className="overflow-hidden mb-4">
              <div className="bg-white rounded-2xl border border-blue-100 p-4 flex flex-wrap gap-2">
                {STATUS_OPTIONS.map(s => (
                  <button key={s||'all'} type="button" onClick={() => { setStatus(s); setPage(0); }}
                    className={`px-4 py-1.5 rounded-full text-xs font-bold border transition-all ${status===s?'bg-blue-600 text-white border-blue-600':'bg-blue-50 text-blue-700 border-blue-100 hover:border-blue-400'}`}>
                    {s || 'All'}
                  </button>
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Table */}
        <motion.div initial={{opacity:0,y:12}} animate={{opacity:1,y:0}} transition={{delay:0.1}}
          className="bg-white rounded-3xl border border-blue-100 overflow-hidden"
          style={{boxShadow:'0 4px 24px rgba(21,101,192,0.08)'}}>

          {/* Desktop header */}
          <div className="hidden lg:grid grid-cols-[1.8fr_2fr_1.2fr_1fr_1fr_1.2fr] gap-4 px-5 py-3 bg-blue-50 border-b border-blue-100 text-[11px] font-bold uppercase tracking-widest text-blue-500">
            <div>Payment ID</div><div>Transaction ID / Event</div>
            <div>Date</div><div>Method</div><div>Amount</div><div>Status</div>
          </div>

          {isLoading ? (
            <div className="p-8 space-y-4">
              {[1,2,3,4,5].map(i => <div key={i} className="skeleton h-14 rounded-xl w-full"/>)}
            </div>
          ) : filtered.length === 0 ? (
            <div className="flex flex-col items-center py-20 text-gray-400">
              <FiCreditCard className="w-12 h-12 mb-3 opacity-30"/>
              <p className="font-semibold">No payments found</p>
              <p className="text-sm mt-1">Try adjusting your search or filter</p>
            </div>
          ) : filtered.map((p, i) => (
            <motion.div key={p.paymentId||i}
              initial={{opacity:0,x:-8}} animate={{opacity:1,x:0}} transition={{delay:i*0.04}}
              className="grid gap-3 px-5 py-4 border-b border-blue-50 last:border-0 lg:grid-cols-[1.8fr_2fr_1.2fr_1fr_1fr_1.2fr] lg:items-center hover:bg-blue-50/40 transition-colors">

              <div>
                <div className="flex items-center gap-1.5 font-mono text-xs font-bold text-blue-800">
                  <FiHash className="text-blue-300"/>{p.paymentId||'—'}
                </div>
                <div className="text-[10px] text-gray-400 mt-0.5">{p.bookingId ? `Booking #${p.bookingId}` : ''}</div>
              </div>

              <div>
                <div className="font-mono text-xs text-gray-600 truncate">{p.gatewayTransactionId||'—'}</div>
                <div className="text-xs text-blue-700 font-medium mt-0.5 truncate">{p.eventName||''}</div>
              </div>

              <div className="flex items-center gap-1.5 text-xs text-gray-600">
                <FiCalendar className="text-gray-300 flex-shrink-0"/>
                <span>{p.paidAt ? new Date(p.paidAt).toLocaleDateString('en-IN') : p.date||'—'}</span>
              </div>

              <div className="text-xs font-semibold text-gray-700">{p.paymentMethod||'—'}</div>

              <div className="font-extrabold text-blue-900">{fmt(p.amount)}</div>

              <div>
                <span className={`inline-flex rounded-full px-3 py-1 text-[11px] font-bold ring-1 ${STATUS_STYLE[p.paymentStatus||p.status]||'bg-gray-100 text-gray-600 ring-gray-200'}`}>
                  {p.paymentStatus||p.status||'—'}
                </span>
              </div>
            </motion.div>
          ))}
        </motion.div>

        {/* Pagination */}
        {(data?.totalPages||0) > 1 && (
          <div className="flex items-center justify-center gap-3 mt-5">
            <motion.button whileTap={{scale:0.9}} onClick={() => setPage(p=>Math.max(0,p-1))}
              disabled={page<=0}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-semibold bg-white border border-blue-200 text-blue-700 hover:border-blue-400 disabled:opacity-40 transition-all">
              <FiChevronLeft/> Prev
            </motion.button>
            <span className="text-sm font-semibold text-blue-700">
              Page {page+1} of {data.totalPages}
            </span>
            <motion.button whileTap={{scale:0.9}} onClick={() => setPage(p=>p+1)}
              disabled={page+1 >= (data?.totalPages||0)}
              className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-sm font-semibold bg-white border border-blue-200 text-blue-700 hover:border-blue-400 disabled:opacity-40 transition-all">
              Next <FiChevronRight/>
            </motion.button>
          </div>
        )}
      </div>
    </div>
  );
}
