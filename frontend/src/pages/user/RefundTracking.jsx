import { useQuery } from 'react-query';
import { motion } from 'framer-motion';
import { paymentsAPI } from '../../services/api';
import { FiRefreshCw, FiAlertTriangle, FiCheckCircle, FiClock } from 'react-icons/fi';

const fmt = (v) => new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR',maximumFractionDigits:2}).format(Number(v||0));

const REFUND_STEPS = [
  'REFUND_REQUESTED', 'UNDER_VERIFICATION', 'APPROVED', 'PROCESSING', 'COMPLETED'
];

const STATUS_META = {
  INITIATED:          { color:'#546E7A', bg:'#ECEFF1', label:'Initiated',          icon:<FiClock/> },
  REFUND_REQUESTED:   { color:'#1565C0', bg:'#E3F2FD', label:'Refund Requested',   icon:<FiClock/> },
  UNDER_VERIFICATION: { color:'#7B1FA2', bg:'#F3E5F5', label:'Under Verification', icon:<FiClock/> },
  APPROVED:           { color:'#2E7D32', bg:'#E8F5E9', label:'Approved',           icon:<FiCheckCircle/> },
  PROCESSING:         { color:'#1565C0', bg:'#E3F2FD', label:'Processing',         icon:<FiRefreshCw className="animate-spin"/> },
  COMPLETED:          { color:'#2E7D32', bg:'#E8F5E9', label:'Completed',          icon:<FiCheckCircle/> },
  REJECTED:           { color:'#C62828', bg:'#FFEBEE', label:'Rejected',           icon:<FiAlertTriangle/> },
};

export default function RefundTracking() {
  const { data, isLoading, refetch } = useQuery(
    'my-refunds',
    () => paymentsAPI.myRefunds().then(r => r.data?.data ?? []),
    { staleTime: 30_000 }
  );

  const refunds = Array.isArray(data) ? data : [];

  return (
    <div className="min-h-screen px-4 py-10" style={{background:'#F0F4FF'}}>
      <div className="max-w-3xl mx-auto">

        <motion.div initial={{opacity:0,y:-12}} animate={{opacity:1,y:0}} className="mb-7">
          <h1 className="text-3xl font-extrabold text-blue-900" style={{fontFamily:'Space Grotesk,sans-serif'}}>Refund Tracking</h1>
          <p className="text-sm text-gray-500 mt-1">Track your refund requests in real time.</p>
        </motion.div>

        {/* Acknowledgement banner */}
        <motion.div initial={{opacity:0,x:-8}} animate={{opacity:1,x:0}} transition={{delay:0.05}}
          className="bg-blue-50 border border-blue-200 rounded-2xl p-4 mb-6 flex items-start gap-3 text-sm text-blue-800">
          <FiAlertTriangle className="flex-shrink-0 mt-0.5 text-blue-500"/>
          <span>Your refund request has been received successfully. Refunds typically take <strong>3–7 business days</strong> depending on your payment provider.</span>
        </motion.div>

        {isLoading ? (
          <div className="space-y-4">{[1,2,3].map(i=><div key={i} className="skeleton h-40 rounded-2xl"/>)}</div>
        ) : refunds.length === 0 ? (
          <div className="bg-white rounded-3xl border border-blue-100 p-12 text-center"
            style={{boxShadow:'0 4px 24px rgba(21,101,192,0.08)'}}>
            <FiCheckCircle className="w-12 h-12 text-gray-200 mx-auto mb-3"/>
            <p className="font-semibold text-gray-400">No refund requests found</p>
            <p className="text-sm text-gray-400 mt-1">Any cancellation refunds will appear here.</p>
          </div>
        ) : refunds.map((refund, i) => (
          <RefundCard key={refund.refundId||i} refund={refund} index={i}/>
        ))}
      </div>
    </div>
  );
}

function RefundCard({ refund, index }) {
  const meta    = STATUS_META[refund.status] || STATUS_META.INITIATED;
  const stepIdx = REFUND_STEPS.indexOf(refund.status);
  const rejected = refund.status === 'REJECTED';

  return (
    <motion.div initial={{opacity:0,y:16}} animate={{opacity:1,y:0}} transition={{delay:index*0.08}}
      className="bg-white rounded-3xl border border-blue-100 p-6 mb-4"
      style={{boxShadow:'0 4px 24px rgba(21,101,192,0.08)'}}>

      {/* Header */}
      <div className="flex items-start justify-between gap-4 mb-4">
        <div>
          <div className="font-extrabold text-blue-900 text-base">{refund.eventName || 'Event Booking'}</div>
          <div className="text-xs text-gray-400 mt-0.5 font-mono">Refund ID: {refund.refundId || '—'}</div>
        </div>
        <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold"
          style={{background:meta.bg, color:meta.color}}>
          {meta.icon} {meta.label}
        </span>
      </div>

      {/* Amount + date */}
      <div className="flex gap-6 text-sm mb-5">
        <div><span className="text-gray-400 text-xs">Amount</span><div className="font-extrabold text-blue-900 text-lg">{fmt(refund.amount)}</div></div>
        {refund.requestedAt && <div><span className="text-gray-400 text-xs">Requested</span><div className="font-semibold text-gray-700">{new Date(refund.requestedAt).toLocaleDateString('en-IN')}</div></div>}
        {refund.expectedRefundDate && <div><span className="text-gray-400 text-xs">Expected By</span><div className="font-semibold text-green-700">{new Date(refund.expectedRefundDate).toLocaleDateString('en-IN')}</div></div>}
      </div>

      {/* Progress bar (not shown for rejected) */}
      {!rejected && (
        <div className="space-y-2">
          <div className="flex justify-between text-[10px] font-semibold text-gray-400 mb-1">
            {REFUND_STEPS.map(s => <span key={s} className={stepIdx >= REFUND_STEPS.indexOf(s) ? 'text-blue-600' : ''}>{STATUS_META[s]?.label}</span>)}
          </div>
          <div className="h-1.5 bg-blue-100 rounded-full overflow-hidden">
            <motion.div className="h-full rounded-full bg-blue-500"
              initial={{width:0}} animate={{width: stepIdx>=0 ? `${((stepIdx+1)/REFUND_STEPS.length)*100}%` : '0%'}}
              transition={{duration:0.8,ease:'easeOut'}}/>
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
