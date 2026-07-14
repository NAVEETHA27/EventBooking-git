import { useState, useCallback, useEffect, useRef } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useQuery, useQueryClient } from 'react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { bookingsAPI, paymentsAPI, getApiErrorMessage } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import {
  FiArrowLeft, FiCheckCircle, FiXCircle, FiShield, FiChevronDown,
  FiChevronUp, FiSmartphone, FiCreditCard, FiGlobe, FiDollarSign,
  FiAlertTriangle, FiClock, FiLock, FiTag
} from 'react-icons/fi';
import { MdAccountBalance, MdPayments, MdVerified, MdQrCode } from 'react-icons/md';

/* ─── Helpers ─────────────────────────────────────────────── */
const fmt = (v) => new Intl.NumberFormat('en-IN', {
  style: 'currency', currency: 'INR', maximumFractionDigits: 2
}).format(Number(v || 0));

function calcFees(baseAmount) {
  const base     = Number(baseAmount || 0);
  const platform = 0;
  const gst      = 0;
  const total    = base;
  return { base, platform, gst, total };
}

function loadRazorpay() {
  return new Promise((resolve) => {
    if (window.Razorpay) { resolve(true); return; }
    const s = document.createElement('script');
    s.src = 'https://checkout.razorpay.com/v1/checkout.js';
    s.onload  = () => resolve(true);
    s.onerror = () => resolve(false);
    document.body.appendChild(s);
  });
}

/* ─── Payment methods ─────────────────────────────────────── */
const METHODS = [
  { id:'UPI',         label:'UPI',         sub:'GPay · PhonePe · Paytm', icon:<FiSmartphone className="w-5 h-5"/>,         color:'#10B981' },
  { id:'CREDIT_CARD', label:'Credit Card', sub:'Visa · Mastercard · Amex',icon:<FiCreditCard className="w-5 h-5"/>,        color:'#3B82F6' },
  { id:'DEBIT_CARD',  label:'Debit Card',  sub:'All major banks',         icon:<FiDollarSign className="w-5 h-5"/>,         color:'#8B5CF6' },
  { id:'NET_BANKING', label:'Net Banking', sub:'HDFC · SBI · ICICI',      icon:<MdAccountBalance className="w-5 h-5"/>,   color:'#F59E0B' },
  { id:'WALLET',      label:'Wallet',      sub:'Paytm · Mobikwik · Ola',  icon:<MdPayments className="w-5 h-5"/>,          color:'#EF4444' },
  { id:'EMI',         label:'EMI',         sub:'No-cost EMI options',      icon:<FiGlobe className="w-5 h-5"/>,             color:'#06B6D4' },
];

/* ─── Progress steps ──────────────────────────────────────── */
const STEPS = ['Book Tickets', 'Payment', 'Confirm'];

/* ─── Countdown timer hook ────────────────────────────────── */
function useCountdown(totalSeconds) {
  const [secs, setSecs] = useState(totalSeconds);
  const ref = useRef(null);
  useEffect(() => {
    ref.current = setInterval(() => setSecs(s => Math.max(0, s - 1)), 1000);
    return () => clearInterval(ref.current);
  }, []);
  const m = String(Math.floor(secs / 60)).padStart(2, '0');
  const s = String(secs % 60).padStart(2, '0');
  return { display: `${m}:${s}`, expired: secs === 0, urgent: secs < 120 };
}

/* ─── Skeleton loader ─────────────────────────────────────── */
function PageSkeleton() {
  return (
    <div className="min-h-screen px-4 py-8" style={{background:'#F0F4FF'}}>
      <div className="max-w-5xl mx-auto">
        <div className="skeleton h-4 w-48 rounded-xl mb-8"/>
        <div className="skeleton h-2 w-full rounded-full mb-8"/>
        <div className="grid lg:grid-cols-[1.1fr_1fr] gap-6">
          <div className="space-y-4">
            <div className="skeleton h-52 rounded-3xl w-full"/>
            <div className="skeleton h-32 rounded-3xl w-full"/>
            <div className="skeleton h-28 rounded-3xl w-full"/>
          </div>
          <div className="space-y-4">
            <div className="skeleton h-24 rounded-3xl w-full"/>
            <div className="skeleton h-48 rounded-3xl w-full"/>
            <div className="skeleton h-14 rounded-2xl w-full"/>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ─── Step progress bar ───────────────────────────────────── */
function StepBar({ current = 1 }) {
  return (
    <div className="flex items-center justify-center gap-0 mb-8">
      {STEPS.map((label, i) => {
        const done   = i < current;
        const active = i === current;
        return (
          <div key={label} className="flex items-center">
            <div className="flex flex-col items-center">
              <motion.div
                initial={{ scale: 0.8 }} animate={{ scale: 1 }}
                className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-bold border-2 transition-all ${
                  done   ? 'bg-blue-600 border-blue-600 text-white' :
                  active ? 'bg-white border-blue-600 text-blue-600' :
                           'bg-white border-gray-200 text-gray-400'
                }`}>
                {done ? <FiCheckCircle className="w-4 h-4"/> : i + 1}
              </motion.div>
              <span className={`text-[10px] mt-1 font-semibold whitespace-nowrap ${active?'text-blue-600':done?'text-blue-500':'text-gray-400'}`}>{label}</span>
            </div>
            {i < STEPS.length - 1 && (
              <div className={`w-16 sm:w-24 h-0.5 mb-5 mx-1 transition-all ${done?'bg-blue-600':'bg-gray-200'}`}/>
            )}
          </div>
        );
      })}
    </div>
  );
}

/* ─── Order summary panel ─────────────────────────────────── */
function OrderSummary({ booking, expanded, setExpanded }) {
  const ev   = booking?.event || {};
  const fees = calcFees(booking?.totalAmount);
  const free = fees.base <= 0;

  return (
    <div className="bg-white rounded-3xl overflow-hidden border border-blue-100"
      style={{boxShadow:'0 8px 32px rgba(21,101,192,0.1)'}}>

      {/* Banner with overlay */}
      {ev.eventBanner ? (
        <div className="relative h-44 overflow-hidden">
          <img src={ev.eventBanner} alt={ev.eventName} className="w-full h-full object-cover"/>
          <div className="absolute inset-0" style={{background:'linear-gradient(0deg,rgba(0,0,0,0.7) 0%,transparent 60%)'}}/>
          <div className="absolute bottom-0 left-0 p-4">
            <span className="badge badge-blue text-[10px] mb-1 block w-fit">{ev.category||'EVENT'}</span>
            <h2 className="text-white font-extrabold text-lg leading-tight">{ev.eventName}</h2>
          </div>
        </div>
      ) : (
        <div className="px-6 pt-6">
          <span className="badge badge-blue text-[10px] mb-1 block w-fit">{ev.category||'EVENT'}</span>
          <h2 className="text-blue-900 font-extrabold text-xl">{ev.eventName}</h2>
        </div>
      )}

      <div className="p-5 space-y-4">
        {/* Event details */}
        <div className="space-y-2 text-sm">
          {ev.venueName && (
            <div className="flex items-center gap-2 text-gray-600">
              <span className="text-blue-400">📍</span> {ev.venueName || ev.location}
            </div>
          )}
          {ev.eventDate && (
            <div className="flex items-center gap-2 text-gray-600">
              <span className="text-blue-400">📅</span>
              {new Date(ev.eventDate).toLocaleDateString('en-IN',{day:'numeric',month:'long',year:'numeric'})}
              {ev.eventTime && ` · ${ev.eventTime}`}
            </div>
          )}
          <div className="flex items-center gap-2 text-gray-600">
            <span className="text-blue-400">🎟</span> {booking?.quantity} ticket{booking?.quantity!==1?'s':''}
          </div>
        </div>

        {/* Expandable ticket details */}
        <div className="rounded-2xl border border-blue-100 overflow-hidden">
          <button type="button" onClick={() => setExpanded(!expanded)}
            className="w-full flex items-center justify-between px-4 py-2.5 bg-blue-50 hover:bg-blue-100 transition-colors text-sm font-semibold text-blue-700">
            <span>🎫 Ticket Details</span>
            {expanded ? <FiChevronUp className="w-4 h-4"/> : <FiChevronDown className="w-4 h-4"/>}
          </button>
          <AnimatePresence initial={false}>
            {expanded && (
              <motion.div initial={{height:0,opacity:0}} animate={{height:'auto',opacity:1}}
                exit={{height:0,opacity:0}} transition={{duration:0.22}} className="overflow-hidden">
                <div className="px-4 py-3 space-y-2 text-xs">
                  <SRow label="Ticket ID"  value={booking?.ticketId||'—'} mono/>
                  <SRow label="Booking ID" value={booking?.id} mono/>
                  <SRow label="Status"     value={booking?.bookingStatus}/>
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>

        {/* Price breakdown */}
        <div className="rounded-2xl border border-blue-100 p-4 space-y-2.5 text-sm bg-blue-50/40">
          <p className="font-bold text-blue-900 text-xs uppercase tracking-widest mb-3">Price Breakdown</p>
          <SRow label={`Ticket × ${booking?.quantity||1}`} value={free?'Free':fmt(fees.base)}/>
          {!free && <>
            <SRow label="Platform fee (2%)" value={fmt(fees.platform)} hint/>
            <SRow label="GST (18% on fee)"  value={fmt(fees.gst)} hint/>
          </>}
          <div className="border-t border-blue-200 pt-2.5 flex justify-between font-extrabold text-base text-blue-900">
            <span>Grand Total</span>
            <span className="text-lg">{free?'Free':fmt(fees.total)}</span>
          </div>
        </div>
      </div>
    </div>
  );
}

function SRow({ label, value, mono, hint }) {
  return (
    <div className="flex justify-between gap-4">
      <span className={hint?'text-gray-400':'text-gray-500'}>{label}</span>
      <span className={`font-semibold text-right text-blue-900 truncate max-w-[55%] ${mono?'font-mono text-[11px] bg-blue-100 px-1.5 py-0.5 rounded':''}`}>{value||'—'}</span>
    </div>
  );
}

/* ─── Main component ─────────────────────────────────────── */
export default function PaymentCheckout() {
  const { bookingId }  = useParams();
  const navigate       = useNavigate();
  const { user }       = useAuth();
  const qc             = useQueryClient();
  const [method, setMethod]     = useState('UPI');
  const [payStatus, setPS]      = useState('IDLE'); // IDLE INIT REDIRECT PROCESSING SUCCESS FAILED CANCELLED
  const [errMsg, setErr]        = useState('');
  const [payResult, setResult]  = useState(null);
  const [expanded, setExpanded] = useState(false);
  const timer = useCountdown(600); // 10-min countdown

  const { data: booking, isLoading } = useQuery(
    ['checkout', bookingId],
    () => bookingsAPI.getById(bookingId).then(r => r.data?.data),
    { enabled: !!bookingId, staleTime: 30_000 }
  );

  const fees    = booking ? calcFees(booking.totalAmount) : {};
  const isFree  = fees.total <= 0;
  const isPaying= ['INIT','REDIRECT','PROCESSING'].includes(payStatus);
  const alreadyConfirmed = booking?.bookingStatus === 'CONFIRMED';

  const doPayment = useCallback(async () => {
    if (isPaying || alreadyConfirmed) return;
    setErr(''); setPS('INIT');

    try {
      if (isFree) {
        setPS('PROCESSING');
        await paymentsAPI.createRazorpayOrder(bookingId);
        qc.invalidateQueries(['checkout', bookingId]);
        qc.invalidateQueries(['booking', bookingId]);
        qc.invalidateQueries('my-bookings');
        qc.invalidateQueries('dash-bookings');
        qc.invalidateQueries('payments');
        qc.invalidateQueries('notifs');
        setPS('SUCCESS');
        toast.success('Free booking confirmed!');
        setTimeout(() => navigate(`/bookings/${bookingId}`, {replace:true}), 1800);
        return;
      }

      const loaded = await loadRazorpay();
      if (!loaded) throw new Error('Could not load Razorpay. Check your connection.');

      let orderRes;
      try {
        orderRes = await paymentsAPI.createRazorpayOrder(bookingId);
      } catch (orderErr) {
        // If the backend says payment is already successful, treat it as success
        const backendMsg = getApiErrorMessage(orderErr, '');
        if (backendMsg.toLowerCase().includes('already successful')) {
          qc.invalidateQueries(['checkout', bookingId]);
          qc.invalidateQueries(['booking', bookingId]);
          qc.invalidateQueries('my-bookings');
          qc.invalidateQueries('dash-bookings');
          qc.invalidateQueries('payments');
          qc.invalidateQueries('notifs');
          setPS('SUCCESS');
          toast.success('Payment already confirmed!');
          setTimeout(() => navigate(`/bookings/${bookingId}`, {replace:true}), 1800);
          return;
        }
        throw orderErr;
      }

      const order = orderRes.data?.data;
      if (!order?.keyId || !order?.orderId) throw new Error('Razorpay order creation failed. Please try again.');

      setPS('REDIRECT');

      const rzp = new window.Razorpay({
        key:         order.keyId,
        amount:      order.amount,
        currency:    order.currency || 'INR',
        name:        'CollegeEvents',
        description: booking?.event?.eventName || 'Event Ticket',
        order_id:    order.orderId,
        prefill: { name: user?.name||'', email: user?.email||'', contact: user?.phone||'' },
        notes:   { bookingId, userId: user?.id },
        theme:   { color: '#1565C0' },
        handler: async (resp) => {
          setPS('PROCESSING');
          try {
            const vRes = await paymentsAPI.verifyRazorpay(bookingId, {
              razorpayOrderId:   resp.razorpay_order_id,
              razorpayPaymentId: resp.razorpay_payment_id,
              razorpaySignature: resp.razorpay_signature,
              paymentMethod:     'RAZORPAY',
            });
            setResult(vRes.data?.data);
            if (vRes.data?.data) qc.setQueryData(['checkout', bookingId], vRes.data.data);
            qc.invalidateQueries(['checkout', bookingId]);
            qc.invalidateQueries(['booking', bookingId]);
            qc.invalidateQueries('my-bookings');
            qc.invalidateQueries('dash-bookings');
            qc.invalidateQueries('payments');
            qc.invalidateQueries('notifs');
            setPS('SUCCESS');
            toast.success('Payment verified and confirmed!');
          } catch (vErr) {
            const vMsg = getApiErrorMessage(vErr, 'Verification failed. Contact support.');
            setErr(vMsg);
            setPS('FAILED');
            toast.error(vMsg);
          }
        },
        modal: {
          ondismiss: async () => {
            const msg = 'Payment window was closed.';
            setPS('CANCELLED');
            setErr(msg);
            try {
              await paymentsAPI.markFailed(bookingId, {
                paymentMethod: 'RAZORPAY',
                failureReason: 'Payment cancelled by user',
              });
              qc.invalidateQueries(['checkout', bookingId]);
              qc.invalidateQueries('payments');
              qc.invalidateQueries('my-bookings');
              qc.invalidateQueries('dash-bookings');
            } catch {}
          },
        },
      });

      rzp.on('payment.failed', async (resp) => {
        const msg = resp?.error?.description || 'Payment failed.';
        setErr(msg); setPS('FAILED');
        try {
          await paymentsAPI.markFailed(bookingId, { paymentMethod: 'RAZORPAY', failureReason: msg });
          qc.invalidateQueries(['checkout', bookingId]);
          qc.invalidateQueries('payments');
        }
        catch {}
      });

      rzp.open();
    } catch (err) {
      const msg = getApiErrorMessage(err, 'Payment could not be started.');
      setErr(msg); setPS('FAILED'); toast.error(msg);
    }
  }, [bookingId, booking, isFree, isPaying, alreadyConfirmed, method, user, navigate, qc]);

  /* outcome screens */
  if (payStatus === 'SUCCESS')    return <PaymentSuccess booking={booking} result={payResult} bookingId={bookingId} navigate={navigate}/>;
  if (payStatus === 'FAILED')     return <PaymentFailed  error={errMsg} bookingId={bookingId} navigate={navigate} onRetry={() => setPS('IDLE')}/>;
  if (payStatus === 'CANCELLED')  return <PaymentCancelled bookingId={bookingId} navigate={navigate} onRetry={() => setPS('IDLE')}/>;

  if (isLoading) return <PageSkeleton/>;

  const btnLabel = (() => {
    if (alreadyConfirmed) return 'Already Confirmed';
    if (payStatus==='INIT')      return 'Initializing…';
    if (payStatus==='REDIRECT')  return 'Opening Razorpay…';
    if (payStatus==='PROCESSING')return 'Verifying Payment…';
    return isFree ? 'Confirm Free Booking' : `Pay ${fmt(fees.total)}`;
  })();

  return (
    <div className="min-h-screen px-4 py-8" style={{background:'#F0F4FF'}}>
      <div className="max-w-5xl mx-auto">

        {/* Back */}
        <motion.div initial={{opacity:0,x:-12}} animate={{opacity:1,x:0}} className="mb-5">
          <Link to={`/bookings/${bookingId}`}
            className="inline-flex items-center gap-2 text-sm font-semibold text-blue-600 hover:text-blue-800 transition-colors">
            <FiArrowLeft/> Back to booking
          </Link>
        </motion.div>

        {/* Step progress */}
        <StepBar current={1}/>

        {/* Countdown */}
        {!alreadyConfirmed && (
          <motion.div initial={{opacity:0,y:-6}} animate={{opacity:1,y:0}}
            className={`flex items-center justify-center gap-2 mb-5 px-4 py-2 rounded-2xl text-sm font-semibold w-fit mx-auto ${
              timer.urgent ? 'bg-red-50 text-red-700 border border-red-200' : 'bg-amber-50 text-amber-700 border border-amber-200'
            }`}>
            <FiClock className={timer.urgent?'animate-pulse':''}/> Seats reserved for {timer.display}
          </motion.div>
        )}

        <div className="grid lg:grid-cols-[1.1fr_1fr] gap-6 items-start">

          {/* LEFT — Order summary */}
          <motion.div initial={{opacity:0,x:-20}} animate={{opacity:1,x:0}} transition={{delay:0.05}}>
            <OrderSummary booking={booking} expanded={expanded} setExpanded={setExpanded}/>
          </motion.div>

          {/* RIGHT — Payment panel */}
          <motion.div initial={{opacity:0,x:20}} animate={{opacity:1,x:0}} transition={{delay:0.1}} className="space-y-4">

            {/* Customer info */}
            <div className="bg-white rounded-3xl border border-blue-100 p-5"
              style={{boxShadow:'0 4px 20px rgba(21,101,192,0.08)'}}>
              <div className="flex items-center gap-2 mb-3">
                <div className="w-7 h-7 rounded-lg bg-blue-100 flex items-center justify-center">
                  <span className="text-blue-600 text-sm">👤</span>
                </div>
                <h3 className="font-bold text-blue-900 text-sm">Customer Details</h3>
                <span className="ml-auto">
                  {user?.emailVerified && <span className="inline-flex items-center gap-1 text-[10px] font-bold text-green-600 bg-green-50 border border-green-200 px-2 py-0.5 rounded-full"><MdVerified className="w-3 h-3"/> Verified</span>}
                </span>
              </div>
              <div className="space-y-2">
                <IRow label="Name"    value={user?.name}/>
                <IRow label="Email"   value={user?.email}/>
                <IRow label="Phone"   value={user?.phone||'Not set'}/>
                <IRow label="User ID" value={user?.userCode||user?.id} mono/>
              </div>
            </div>

            {/* Payment methods */}
            <div className="bg-white rounded-3xl border border-blue-100 p-5"
              style={{boxShadow:'0 4px 20px rgba(21,101,192,0.08)'}}>
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-bold text-blue-900 text-sm">Payment Method</h3>
                <div className="flex items-center gap-1 px-3 py-1 rounded-full text-[10px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
                  <FiLock className="w-3 h-3"/> Razorpay Secured
                </div>
              </div>
              <div className="grid grid-cols-2 gap-2">
                {METHODS.map(m => (
                  <motion.button key={m.id} type="button" whileTap={{scale:0.96}}
                    onClick={() => setMethod(m.id)}
                    className={`relative flex items-center gap-2.5 p-3 rounded-2xl border text-left transition-all duration-200 ${
                      method===m.id
                        ? 'border-transparent ring-2 ring-blue-500 bg-blue-50'
                        : 'border-gray-200 hover:border-blue-200 hover:bg-blue-50/30'
                    }`}>
                    <span className="flex-shrink-0 w-8 h-8 rounded-xl flex items-center justify-center text-white"
                      style={{background: method===m.id ? m.color : '#E5E7EB', color: method===m.id ? '#fff' : '#9CA3AF'}}>
                      {m.icon}
                    </span>
                    <div className="min-w-0">
                      <div className={`text-xs font-bold truncate ${method===m.id?'text-blue-800':'text-gray-700'}`}>{m.label}</div>
                      <div className="text-[10px] text-gray-400 truncate">{m.sub}</div>
                    </div>
                    {method===m.id && (
                      <motion.div initial={{scale:0}} animate={{scale:1}}
                        className="absolute top-2 right-2 w-4 h-4 rounded-full bg-blue-600 flex items-center justify-center">
                        <FiCheckCircle className="w-3 h-3 text-white"/>
                      </motion.div>
                    )}
                  </motion.button>
                ))}
              </div>
            </div>

            {/* Error */}
            <AnimatePresence>
              {errMsg && (
                <motion.div initial={{opacity:0,y:-8}} animate={{opacity:1,y:0}} exit={{opacity:0}}
                  className="flex items-start gap-2.5 p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-sm">
                  <FiAlertTriangle className="flex-shrink-0 mt-0.5 w-4 h-4"/><span>{errMsg}</span>
                </motion.div>
              )}
            </AnimatePresence>

            {/* Pay button */}
            <motion.button type="button" onClick={doPayment}
              disabled={isPaying || alreadyConfirmed || isLoading}
              whileHover={isPaying||alreadyConfirmed||isLoading ? {} : {scale:1.02, boxShadow:'0 12px 36px rgba(21,101,192,0.45)'}}
              whileTap={isPaying||alreadyConfirmed||isLoading ? {} : {scale:0.97}}
              className="w-full py-4 rounded-2xl text-white text-base font-bold flex items-center justify-center gap-3 transition-all disabled:opacity-60 disabled:cursor-not-allowed"
              style={{background:'linear-gradient(135deg,#1565C0,#1976D2)', boxShadow:'0 6px 24px rgba(21,101,192,0.35)'}}>
              {isPaying
                ? <><motion.div animate={{rotate:360}} transition={{repeat:Infinity,duration:0.75,ease:'linear'}}
                    className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full"/>{btnLabel}</>
                : alreadyConfirmed
                ? <><FiCheckCircle className="w-5 h-5"/>Already Confirmed</>
                : <><FiLock className="w-4 h-4"/>{btnLabel}</>
              }
            </motion.button>

            <p className="text-center text-[11px] text-gray-400 leading-relaxed">
              🔒 256-bit SSL · Secured by Razorpay<br/>
              No card data is stored on our servers
            </p>
          </motion.div>
        </div>
      </div>
    </div>
  );
}

function IRow({label, value, mono}) {
  return (
    <div className="flex justify-between items-center gap-4 py-1 border-b border-gray-50 last:border-0">
      <span className="text-xs text-gray-400 flex-shrink-0">{label}</span>
      <span className={`text-xs font-semibold text-blue-900 text-right truncate ${mono?'font-mono bg-blue-50 px-2 py-0.5 rounded':''}`}>{value||'—'}</span>
    </div>
  );
}

/* ─── Success page ────────────────────────────────────────── */
function PaymentSuccess({ booking, result, bookingId, navigate }) {
  const ev = booking?.event || {};
  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12"
      style={{background:'linear-gradient(135deg,#E8F5E9 0%,#F0F4FF 100%)'}}>
      <motion.div initial={{opacity:0,scale:0.9,y:24}} animate={{opacity:1,scale:1,y:0}}
        transition={{duration:0.5,ease:[0.34,1.56,0.64,1]}}
        className="w-full max-w-md bg-white rounded-3xl overflow-hidden border border-green-100"
        style={{boxShadow:'0 24px 80px rgba(46,125,50,0.18)'}}>

        {/* Green header */}
        <div className="py-8 text-center" style={{background:'linear-gradient(135deg,#2E7D32,#388E3C)'}}>
          <motion.div initial={{scale:0}} animate={{scale:1}}
            transition={{delay:0.2,type:'spring',stiffness:260,damping:20}}
            className="w-20 h-20 rounded-full bg-white/20 flex items-center justify-center mx-auto mb-3">
            <FiCheckCircle className="w-10 h-10 text-white"/>
          </motion.div>
          <h1 className="text-2xl font-extrabold text-white" style={{fontFamily:'Space Grotesk,sans-serif'}}>
            Payment Successful!
          </h1>
          <p className="text-green-100 text-sm mt-1">Your ticket is confirmed 🎉</p>
        </div>

        <div className="p-6">
          {/* Receipt */}
          <div className="rounded-2xl bg-green-50 border border-green-100 p-4 space-y-2 mb-5 text-sm">
            {result?.paymentId     && <RRow label="Payment ID"    value={result.paymentId} mono/>}
            {result?.transactionId && <RRow label="Transaction ID" value={result.transactionId} mono/>}
            <RRow label="Booking ID"  value={booking?.id} mono/>
            <RRow label="Ticket ID"   value={booking?.ticketId} mono/>
            <RRow label="Event"       value={ev.eventName}/>
            <RRow label="Amount"      value={booking?.totalAmount ? new Intl.NumberFormat('en-IN',{style:'currency',currency:'INR'}).format(booking.totalAmount) : '—'}/>
            <RRow label="Date"        value={new Date().toLocaleString('en-IN',{dateStyle:'medium',timeStyle:'short'})}/>
          </div>

          {/* QR hint */}
          {booking?.qrCodePath && (
            <div className="flex items-center gap-2 text-xs text-gray-500 bg-blue-50 rounded-xl px-4 py-2 mb-4">
              <MdQrCode className="w-4 h-4 text-blue-500"/> QR ticket is ready — view your booking for the QR code.
            </div>
          )}

          <div className="space-y-2">
            <motion.button whileHover={{scale:1.02}} whileTap={{scale:0.97}}
              onClick={() => navigate(`/bookings/${bookingId}`)}
              className="w-full py-3 rounded-2xl text-white font-bold text-sm"
              style={{background:'linear-gradient(135deg,#2E7D32,#388E3C)'}}>
              View Ticket
            </motion.button>
            <motion.button whileHover={{scale:1.02}} whileTap={{scale:0.97}}
              onClick={() => navigate('/dashboard')}
              className="w-full py-3 rounded-2xl font-bold text-sm border border-green-200 text-green-700 hover:bg-green-50 transition-colors">
              Go to Dashboard
            </motion.button>
          </div>
        </div>
      </motion.div>
    </div>
  );
}

function RRow({label, value, mono}) {
  return (
    <div className="flex justify-between gap-4">
      <span className="text-gray-500 text-xs flex-shrink-0">{label}</span>
      <span className={`font-semibold text-green-800 text-xs text-right truncate max-w-[58%] ${mono?'font-mono':''}`}>{value||'—'}</span>
    </div>
  );
}

/* ─── Failure page ────────────────────────────────────────── */
function PaymentFailed({ error, bookingId, navigate, onRetry }) {
  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12"
      style={{background:'linear-gradient(135deg,#FFF5F5 0%,#F0F4FF 100%)'}}>
      <motion.div initial={{opacity:0,scale:0.9,y:24}} animate={{opacity:1,scale:1,y:0}}
        transition={{duration:0.5,ease:[0.34,1.56,0.64,1]}}
        className="w-full max-w-sm bg-white rounded-3xl overflow-hidden border border-red-100"
        style={{boxShadow:'0 24px 80px rgba(198,40,40,0.15)'}}>

        <div className="py-8 text-center" style={{background:'linear-gradient(135deg,#C62828,#D32F2F)'}}>
          <div className="w-20 h-20 rounded-full bg-white/20 flex items-center justify-center mx-auto mb-3">
            <FiXCircle className="w-10 h-10 text-white"/>
          </div>
          <h1 className="text-2xl font-extrabold text-white">Payment Failed</h1>
          <p className="text-red-100 text-sm mt-1">No amount has been deducted</p>
        </div>

        <div className="p-6">
          {error && (
            <div className="text-xs text-red-600 bg-red-50 rounded-xl px-4 py-3 mb-5 text-center border border-red-100">
              {error}
            </div>
          )}
          <div className="space-y-2">
            <motion.button whileHover={{scale:1.02}} whileTap={{scale:0.97}} onClick={onRetry}
              className="w-full py-3 rounded-2xl text-white font-bold text-sm"
              style={{background:'linear-gradient(135deg,#C62828,#D32F2F)'}}>
              Retry Payment
            </motion.button>
            <motion.button whileHover={{scale:1.02}} whileTap={{scale:0.97}}
              onClick={() => navigate(bookingId?`/bookings/${bookingId}`:'/events')}
              className="w-full py-3 rounded-2xl font-bold text-sm border border-red-200 text-red-600 hover:bg-red-50 transition-colors">
              Return to Booking
            </motion.button>
          </div>
        </div>
      </motion.div>
    </div>
  );
}

/* ─── Cancelled page ──────────────────────────────────────── */
function PaymentCancelled({ bookingId, navigate, onRetry }) {
  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12"
      style={{background:'linear-gradient(135deg,#FFFDE7 0%,#F0F4FF 100%)'}}>
      <motion.div initial={{opacity:0,scale:0.9,y:24}} animate={{opacity:1,scale:1,y:0}}
        transition={{duration:0.5,ease:[0.34,1.56,0.64,1]}}
        className="w-full max-w-sm bg-white rounded-3xl overflow-hidden border border-yellow-100"
        style={{boxShadow:'0 24px 80px rgba(245,127,23,0.12)'}}>

        <div className="py-8 text-center" style={{background:'linear-gradient(135deg,#F57F17,#F9A825)'}}>
          <div className="w-20 h-20 rounded-full bg-white/20 flex items-center justify-center mx-auto mb-3">
            <FiAlertTriangle className="w-10 h-10 text-white"/>
          </div>
          <h1 className="text-2xl font-extrabold text-white">Payment Cancelled</h1>
          <p className="text-yellow-100 text-sm mt-1">No amount has been deducted</p>
        </div>

        <div className="p-6">
          <p className="text-sm text-gray-500 text-center mb-5">
            Your seat reservation is still active. Complete payment to confirm your booking.
          </p>
          <div className="space-y-2">
            <motion.button whileHover={{scale:1.02}} whileTap={{scale:0.97}} onClick={onRetry}
              className="w-full py-3 rounded-2xl text-white font-bold text-sm"
              style={{background:'linear-gradient(135deg,#F57F17,#F9A825)'}}>
              Try Again
            </motion.button>
            <motion.button whileHover={{scale:1.02}} whileTap={{scale:0.97}}
              onClick={() => navigate(bookingId?`/bookings/${bookingId}`:'/events')}
              className="w-full py-3 rounded-2xl font-bold text-sm border border-yellow-200 text-yellow-700 hover:bg-yellow-50 transition-colors">
              Return to Booking
            </motion.button>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
