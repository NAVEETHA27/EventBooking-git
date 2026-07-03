import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { motion, AnimatePresence } from 'framer-motion';
import * as yup from 'yup';
import { authAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { FiMail, FiLock, FiEye, FiEyeOff, FiArrowRight } from 'react-icons/fi';
import { MdSchool } from 'react-icons/md';

/* ─── validation ──────────────────────────────────────────── */
const schema = yup.object({
  email:    yup.string().email('Invalid email').required('Email is required'),
  password: yup.string().required('Password is required'),
});

/* ─── theme tokens ────────────────────────────────────────── */
const T = {
  user: {
    accent:'#1565C0', accentAlt:'#1976D2',
    bg:'linear-gradient(135deg,#E3F2FD 0%,#F0F4FF 60%,#E8EEF9 100%)',
    curtain:'linear-gradient(180deg,#1565C0,#0D47A1)',
    cardBorder:'#BBDEFB', inputBg:'#F0F7FF', inputBorder:'#BBDEFB',
    labelColor:'#1A237E', toggleBg:'#E3F2FD', toggleBorder:'#BBDEFB',
    subtitleColor:'#546E7A',
    bubbles:['rgba(21,101,192,0.35)','rgba(25,118,210,0.28)','rgba(13,71,161,0.32)','rgba(100,181,246,0.4)','rgba(21,101,192,0.25)','rgba(66,165,245,0.3)','rgba(21,101,192,0.4)','rgba(13,71,161,0.25)'],
    orbL:'rgba(21,101,192,0.08)', orbR:'rgba(187,222,251,0.5)',
  },
  organizer: {
    accent:'#C62828', accentAlt:'#D32F2F',
    bg:'linear-gradient(135deg,#FFEBEE 0%,#FFF0F0 60%,#FFF5F5 100%)',
    curtain:'linear-gradient(180deg,#C62828,#B71C1C)',
    cardBorder:'#FFCDD2', inputBg:'#FFF5F5', inputBorder:'#FFCDD2',
    labelColor:'#4A0000', toggleBg:'#FFEBEE', toggleBorder:'#FFCDD2',
    subtitleColor:'#78909C',
    bubbles:['rgba(198,40,40,0.3)','rgba(211,47,47,0.25)','rgba(183,28,28,0.28)','rgba(239,154,154,0.4)','rgba(198,40,40,0.22)','rgba(229,115,115,0.35)','rgba(198,40,40,0.35)','rgba(183,28,28,0.22)'],
    orbL:'rgba(198,40,40,0.07)', orbR:'rgba(255,205,210,0.5)',
  },
};

const BUBBLE_POS = [
  {size:12,x:6,delay:0},{size:8,x:18,delay:1.3},{size:16,x:34,delay:2.6},
  {size:10,x:50,delay:0.8},{size:14,x:65,delay:3.2},{size:7,x:78,delay:1.7},
  {size:18,x:88,delay:4.0},{size:9,x:96,delay:2.2},
];

function Bubble({size,x,delay,color}){
  return(
    <motion.div className="absolute rounded-full pointer-events-none"
      style={{width:size,height:size,left:`${x}%`,bottom:0,background:color,filter:'blur(0.8px)'}}
      initial={{opacity:0,y:0,scale:0}}
      animate={{opacity:[0,0.8,0.5,0.8,0],scale:[0,1,1.08,1,0],y:[0,-80,-160,-260,-360]}}
      transition={{duration:9+delay,repeat:Infinity,delay,ease:'easeInOut'}}/>
  );
}

const curtainT={initial:{scaleY:1},animate:{scaleY:0,transition:{duration:0.72,ease:[0.4,0,0.2,1],delay:0.05}}};
const curtainB={initial:{scaleY:1},animate:{scaleY:0,transition:{duration:0.72,ease:[0.4,0,0.2,1],delay:0.05}}};
const cardAnim={initial:{opacity:0,y:36,scale:0.96},animate:{opacity:1,y:0,scale:1,transition:{duration:0.5,delay:0.55,ease:[0.34,1.56,0.64,1]}}};
const stagger={animate:{transition:{staggerChildren:0.08,delayChildren:0.65}}};
const field={initial:{opacity:0,y:12},animate:{opacity:1,y:0,transition:{duration:0.32}}};

export default function Login(){
  const navigate   = useNavigate();
  const { login }  = useAuth();
  const [params]   = useSearchParams();
  const [role, setRole]     = useState(params.get('role')==='organizer'?'organizer':'user');
  const [showPw,setShowPw]  = useState(false);
  const [loading,setLoading]= useState(false);
  const [error,setError]    = useState('');

  const {register,handleSubmit,formState:{errors}} = useForm({resolver:yupResolver(schema)});
  const isOrg = role==='organizer';
  const t = T[role];
  const switchRole = r=>{if(r!==role){setRole(r);setError('');}};

  const onSubmit = async (data) => {
    setLoading(true); setError('');
    try {
      const fn = isOrg ? authAPI.organizerLogin : authAPI.userLogin;
      const res = await fn(data);
      const payload = res.data?.data;
      if (!payload?.accessToken) { setError('Invalid response. Please try again.'); return; }

      const dest = isOrg ? '/organizer/dashboard' : '/dashboard';

      /* Always require OTP verification on every login */
      sessionStorage.setItem('eb_pending_auth', JSON.stringify(payload));
      try {
        await authAPI.sendOtp({
          email: data.email,
          role: isOrg ? 'ORGANIZER' : 'USER',
        });
      } catch (otpErr) {
        const msg = otpErr?.response?.data?.message || 'Could not send verification email.';
        setError(msg);
        toast.error(msg);
        return;
      }
      const otpPath = isOrg ? '/verify-otp/organizer' : '/verify-otp/user';
      navigate(
        `${otpPath}?email=${encodeURIComponent(data.email)}&mode=login&redirect=${encodeURIComponent(dest)}&sent=1`,
        { replace: true },
      );
      return;
    } catch (err) {
      setError(err?.response?.data?.message || 'Login failed. Check your credentials.');
    } finally { setLoading(false); }
  };

  const onFocus = e => { e.target.style.borderColor=t.accent; e.target.style.boxShadow=`0 0 0 3px ${t.accent}22`; e.target.style.background='#fff'; };
  const onBlur  = (e,hasErr) => { e.target.style.borderColor=hasErr?'#EF5350':t.inputBorder; e.target.style.boxShadow='none'; e.target.style.background=t.inputBg; };

  return (
    <AnimatePresence>
      <div className="min-h-screen flex items-center justify-center px-4 py-12 relative overflow-hidden"
        style={{background:t.bg,transition:'background 0.5s ease'}}>

        <motion.div variants={curtainT} initial="initial" animate="animate"
          className="fixed inset-x-0 top-0 h-1/2 z-50 pointer-events-none"
          style={{background:t.curtain,transformOrigin:'top'}}/>
        <motion.div variants={curtainB} initial="initial" animate="animate"
          className="fixed inset-x-0 bottom-0 h-1/2 z-50 pointer-events-none"
          style={{background:t.curtain,transformOrigin:'bottom'}}/>

        <div className="fixed inset-0 pointer-events-none overflow-hidden">
          {BUBBLE_POS.map((b,i)=><Bubble key={i} {...b} color={t.bubbles[i%t.bubbles.length]}/>)}
          <motion.div className="absolute w-[480px] h-[480px] rounded-full"
            style={{background:`radial-gradient(circle,${t.orbL},transparent 70%)`,top:'-100px',left:'-80px'}}
            animate={{scale:[1,1.18,1]}} transition={{duration:13,repeat:Infinity,ease:'easeInOut'}}/>
          <motion.div className="absolute w-[380px] h-[380px] rounded-full"
            style={{background:`radial-gradient(circle,${t.orbR},transparent 70%)`,bottom:'-80px',right:'-60px'}}
            animate={{scale:[1,1.22,1]}} transition={{duration:16,repeat:Infinity,ease:'easeInOut',delay:3}}/>
          <div className="absolute inset-0 opacity-[0.025]"
            style={{backgroundImage:`radial-gradient(circle,${t.accent} 1px,transparent 1px)`,backgroundSize:'40px 40px'}}/>
        </div>

        <motion.div variants={cardAnim} initial="initial" animate="animate" className="w-full max-w-md relative z-10">
          <motion.div className="absolute inset-0 rounded-3xl pointer-events-none"
            animate={{opacity:[0.4,0.7,0.4]}} transition={{duration:4,repeat:Infinity,ease:'easeInOut'}}
            style={{background:`radial-gradient(ellipse at 50% 0%,${t.accent}20,transparent 65%)`,transform:'scale(1.06)',filter:'blur(20px)'}}/>

          <div className="relative rounded-3xl overflow-hidden bg-white"
            style={{border:`1.5px solid ${t.cardBorder}`,boxShadow:`0 24px 64px ${t.accent}18,0 4px 20px ${t.accent}10`}}>

            <motion.div className="h-1.5"
              style={{background:`linear-gradient(90deg,${t.accent},${t.accentAlt},${t.accent})`,backgroundSize:'200%'}}
              animate={{backgroundPosition:['0% 50%','100% 50%','0% 50%']}}
              transition={{duration:3.5,repeat:Infinity,ease:'linear'}}/>

            <div className="p-8">
              <div className="text-center mb-7">
                <motion.div whileHover={{rotate:12,scale:1.1}} transition={{type:'spring',stiffness:300}}
                  className="w-14 h-14 rounded-2xl mx-auto mb-4 flex items-center justify-center shadow-lg"
                  style={{background:`linear-gradient(135deg,${t.accent},${t.accentAlt})`}}>
                  <MdSchool className="text-white w-7 h-7"/>
                </motion.div>
                <h1 className="text-2xl font-extrabold" style={{color:t.accent,fontFamily:'Space Grotesk,sans-serif'}}>
                  Welcome Back
                </h1>
                <AnimatePresence mode="wait">
                  <motion.p key={role} initial={{opacity:0,y:6}} animate={{opacity:1,y:0}}
                    exit={{opacity:0,y:-6}} transition={{duration:0.2}}
                    className="text-sm mt-1" style={{color:t.subtitleColor}}>
                    Sign in as {isOrg?'Organizer':'Student'}
                  </motion.p>
                </AnimatePresence>
              </div>

              {/* role toggle */}
              <div className="relative flex p-1 rounded-2xl mb-6"
                style={{background:t.toggleBg,border:`1.5px solid ${t.toggleBorder}`}}>
                <motion.div className="absolute inset-y-1 w-[calc(50%-2px)] rounded-xl"
                  animate={{left:isOrg?'calc(50%)':'4px'}}
                  transition={{type:'spring',stiffness:380,damping:30}}
                  style={{background:`linear-gradient(135deg,${t.accent},${t.accentAlt})`,boxShadow:`0 4px 14px ${t.accent}35`}}/>
                {['user','organizer'].map(r=>(
                  <button key={r} type="button" onClick={()=>switchRole(r)}
                    className="flex-1 py-2.5 rounded-xl text-sm font-semibold relative z-10 transition-colors duration-200"
                    style={{color:role===r?'#fff':t.subtitleColor}}>
                    {r==='user'?'🎓 Student':'🏢 Organizer'}
                  </button>
                ))}
              </div>

              {/* error */}
              <AnimatePresence>
                {error&&(
                  <motion.div initial={{opacity:0,height:0}} animate={{opacity:1,height:'auto'}}
                    exit={{opacity:0,height:0}} transition={{duration:0.25}} className="overflow-hidden mb-4">
                    <div className="flex items-start gap-2 px-4 py-3 rounded-xl text-sm bg-red-50 border border-red-200 text-red-700">
                      ⚠️ <span>{error}</span>
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>

              <AnimatePresence mode="wait">
                <motion.form key={role}
                  initial={{opacity:0,x:18}} animate={{opacity:1,x:0}}
                  exit={{opacity:0,x:-18}} transition={{duration:0.25}}
                  onSubmit={handleSubmit(onSubmit)}>
                  <motion.div variants={stagger} initial="initial" animate="animate" className="space-y-4">

                    <motion.div variants={field}>
                      <label className="block text-sm font-semibold mb-1.5" style={{color:t.labelColor}}>Email Address</label>
                      <div className="relative">
                        <FiMail className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 pointer-events-none" style={{color:`${t.accent}80`}}/>
                        <input {...register('email')} type="email" placeholder="you@college.edu"
                          className="w-full pl-11 pr-4 py-3 rounded-xl text-sm outline-none transition-all"
                          style={{background:t.inputBg,border:`1.5px solid ${errors.email?'#EF5350':t.inputBorder}`,color:'#1A237E'}}
                          onFocus={onFocus} onBlur={e=>onBlur(e,!!errors.email)}/>
                      </div>
                      <AnimatePresence>
                        {errors.email&&<motion.p initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} className="text-xs mt-1 text-red-500">{errors.email.message}</motion.p>}
                      </AnimatePresence>
                    </motion.div>

                    <motion.div variants={field}>
                      <label className="block text-sm font-semibold mb-1.5" style={{color:t.labelColor}}>Password</label>
                      <div className="relative">
                        <FiLock className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 pointer-events-none" style={{color:`${t.accent}80`}}/>
                        <input {...register('password')} type={showPw?'text':'password'} placeholder="••••••••"
                          className="w-full pl-11 pr-11 py-3 rounded-xl text-sm outline-none transition-all"
                          style={{background:t.inputBg,border:`1.5px solid ${errors.password?'#EF5350':t.inputBorder}`,color:'#1A237E'}}
                          onFocus={onFocus} onBlur={e=>onBlur(e,!!errors.password)}/>
                        <motion.button type="button" whileTap={{scale:0.85}} onClick={()=>setShowPw(v=>!v)}
                          className="absolute right-4 top-1/2 -translate-y-1/2 transition-colors" style={{color:`${t.accent}70`}}>
                          <AnimatePresence mode="wait">
                            {showPw
                              ?<motion.span key="off" initial={{rotate:-90,opacity:0}} animate={{rotate:0,opacity:1}} exit={{rotate:90,opacity:0}} transition={{duration:0.15}}><FiEyeOff className="w-4 h-4"/></motion.span>
                              :<motion.span key="on"  initial={{rotate:90,opacity:0}}  animate={{rotate:0,opacity:1}} exit={{rotate:-90,opacity:0}} transition={{duration:0.15}}><FiEye className="w-4 h-4"/></motion.span>
                            }
                          </AnimatePresence>
                        </motion.button>
                      </div>
                      <AnimatePresence>
                        {errors.password&&<motion.p initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} className="text-xs mt-1 text-red-500">{errors.password.message}</motion.p>}
                      </AnimatePresence>
                    </motion.div>

                    <motion.div variants={field} className="flex justify-end">
                      <Link to="/forgot-password" className="text-xs font-semibold hover:underline" style={{color:t.accent}}>
                        Forgot password?
                      </Link>
                    </motion.div>

                    <motion.div variants={field}>
                      <motion.button type="submit" disabled={loading}
                        whileHover={loading?{}:{scale:1.02,boxShadow:`0 10px 32px ${t.accent}45`}}
                        whileTap={loading?{}:{scale:0.97}}
                        className="w-full py-3.5 text-sm font-bold flex items-center justify-center gap-2 rounded-xl text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                        style={{background:`linear-gradient(135deg,${t.accent},${t.accentAlt})`,boxShadow:`0 4px 18px ${t.accent}40`}}>
                        {loading
                          ?<><motion.div animate={{rotate:360}} transition={{repeat:Infinity,duration:0.75,ease:'linear'}} className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full"/>Signing in…</>
                          :<>Sign In as {isOrg?'Organizer':'Student'}<FiArrowRight className="w-4 h-4"/></>
                        }
                      </motion.button>
                    </motion.div>

                  </motion.div>
                </motion.form>
              </AnimatePresence>

              <p className="text-center text-sm mt-6 text-gray-500">
                Don't have an account?{' '}
                <Link to={`/register?role=${role}`} className="font-bold hover:underline" style={{color:t.accent}}>
                  Sign up free
                </Link>
              </p>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
