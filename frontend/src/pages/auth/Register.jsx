import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { motion, AnimatePresence } from 'framer-motion';
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import * as yup from 'yup';
import { authAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import { FiUser, FiMail, FiLock, FiPhone, FiEye, FiEyeOff, FiArrowRight, FiMapPin, FiNavigation } from 'react-icons/fi';
import { HiSparkles } from 'react-icons/hi2';

/* ─── validation ──────────────────────────────────────────── */
const PW     = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
const PW_MSG = '8+ chars, uppercase, number & special character';

const userSchema = yup.object({
  name:     yup.string().min(2).max(120).required('Name is required'),
  email:    yup.string().email('Invalid email').required('Email is required'),
  password: yup.string().matches(PW, PW_MSG).required('Password is required'),
  phone:    yup.string().matches(/^[+]?[0-9]{7,15}$/, 'Invalid phone').nullable().optional(),
  address:  yup.string().max(300).required('Address is required'),
  city:     yup.string().max(100).required('City is required'),
  state:    yup.string().max(100).required('State is required'),
  country:  yup.string().max(100).required('Country is required'),
  pinCode:  yup.string().max(20).required('PIN code is required'),
});

const orgSchema = yup.object({
  organizerName:    yup.string().min(2).required('Organizer name is required'),
  organizationName: yup.string().min(2).required('Organization name is required'),
  email:            yup.string().email('Invalid email').required('Email is required'),
  password:         yup.string().matches(PW, PW_MSG).required('Password is required'),
  phone:            yup.string().matches(/^[+]?[0-9]{7,15}$/, 'Invalid phone').nullable().optional(),
});

/* ─── theme tokens (same as Login) ───────────────────────── */
const ROLE_THEME = {
  user: {
    accent:'#1565C0', accentAlt:'#1976D2',
    bg:'linear-gradient(135deg,#E3F2FD 0%,#F0F4FF 60%,#E8EEF9 100%)',
    curtain:'linear-gradient(180deg,#1565C0,#0D47A1)',
    cardBorder:'#BBDEFB', inputBg:'#F0F7FF', inputBorder:'#BBDEFB',
    labelColor:'#1A237E', toggleBg:'#E3F2FD', toggleBorder:'#BBDEFB',
    subtitleColor:'#546E7A',
    bubbleColors:['rgba(21,101,192,0.35)','rgba(25,118,210,0.28)','rgba(100,181,246,0.4)','rgba(13,71,161,0.32)','rgba(21,101,192,0.25)','rgba(66,165,245,0.3)'],
    orbLeft:'rgba(21,101,192,0.08)', orbRight:'rgba(187,222,251,0.5)',
  },
  organizer: {
    accent:'#C62828', accentAlt:'#D32F2F',
    bg:'linear-gradient(135deg,#FFEBEE 0%,#FFF0F0 60%,#FFF5F5 100%)',
    curtain:'linear-gradient(180deg,#C62828,#B71C1C)',
    cardBorder:'#FFCDD2', inputBg:'#FFF5F5', inputBorder:'#FFCDD2',
    labelColor:'#4A0000', toggleBg:'#FFEBEE', toggleBorder:'#FFCDD2',
    subtitleColor:'#78909C',
    bubbleColors:['rgba(198,40,40,0.3)','rgba(211,47,47,0.25)','rgba(239,154,154,0.4)','rgba(183,28,28,0.28)','rgba(198,40,40,0.22)','rgba(229,115,115,0.35)'],
    orbLeft:'rgba(198,40,40,0.07)', orbRight:'rgba(255,205,210,0.5)',
  },
};

/* ─── bubble ──────────────────────────────────────────────── */
function Bubble({ size, x, delay, color }) {
  return (
    <motion.div className="absolute rounded-full pointer-events-none"
      style={{ width:size, height:size, left:`${x}%`, bottom:0, background:color, filter:'blur(0.8px)' }}
      initial={{ opacity:0, y:0, scale:0 }}
      animate={{ opacity:[0,0.75,0.45,0.75,0], scale:[0,1,1.08,1,0], y:[0,-80,-180,-280,-380] }}
      transition={{ duration:10+delay, repeat:Infinity, delay, ease:'easeInOut' }} />
  );
}

const BUBBLE_POS = [
  { size:10, x:5,  delay:0   },
  { size:14, x:20, delay:1.5 },
  { size:8,  x:40, delay:2.8 },
  { size:12, x:58, delay:0.9 },
  { size:16, x:74, delay:3.4 },
  { size:7,  x:90, delay:2.1 },
];

const DEFAULT_MAP_CENTER = [20.5937, 78.9629];
const locationMarker = L.divIcon({
  className: '',
  html: '<div style="width:18px;height:18px;border-radius:50%;background:#1565C0;border:3px solid white;box-shadow:0 2px 10px rgba(0,0,0,.35)"></div>',
  iconSize: [18, 18],
  iconAnchor: [9, 9],
});

function mapNominatimResult(result, latitude, longitude) {
  const address = result?.address || {};
  const road = address.road || address.pedestrian || address.footway || address.path || '';
  const area = address.suburb || address.neighbourhood || address.quarter || address.hamlet || '';
  const city = address.city || address.town || address.village || address.municipality || '';
  const district = address.county || address.state_district || address.district || '';

  return {
    address: result?.display_name || '',
    street: road,
    area,
    city,
    district,
    state: address.state || '',
    country: address.country || '',
    pinCode: address.postcode || '',
    latitude: latitude.toFixed(7),
    longitude: longitude.toFixed(7),
  };
}

async function reverseGeocode(latitude, longitude) {
  const params = new URLSearchParams({
    format: 'jsonv2',
    lat: String(latitude),
    lon: String(longitude),
    addressdetails: '1',
  });
  const res = await fetch(`https://nominatim.openstreetmap.org/reverse?${params.toString()}`, {
    headers: { Accept: 'application/json' },
  });
  if (!res.ok) throw new Error('Nominatim lookup failed');
  return mapNominatimResult(await res.json(), latitude, longitude);
}

function LocationPicker({ markerPosition, onPick }) {
  useMapEvents({
    click(e) {
      onPick(e.latlng.lat, e.latlng.lng);
    },
  });

  return markerPosition ? <Marker position={markerPosition} icon={locationMarker} /> : null;
}

function MapCenterSync({ markerPosition }) {
  const map = useMap();
  useEffect(() => {
    if (markerPosition) {
      map.setView(markerPosition, Math.max(map.getZoom(), 15));
    }
  }, [map, markerPosition]);
  return null;
}

/* ─── curtain open ────────────────────────────────────────── */
const curtainT = { initial:{ scaleY:1 }, animate:{ scaleY:0, transition:{ duration:0.72, ease:[0.4,0,0.2,1], delay:0.05 } } };
const curtainB = { initial:{ scaleY:1 }, animate:{ scaleY:0, transition:{ duration:0.72, ease:[0.4,0,0.2,1], delay:0.05 } } };
const cardAnim = { initial:{ opacity:0, y:36, scale:0.96 }, animate:{ opacity:1, y:0, scale:1, transition:{ duration:0.5, delay:0.55, ease:[0.34,1.56,0.64,1] } } };

/* ─── Field helper ────────────────────────────────────────── */
function Field({ label, error, icon, accent, children }) {
  return (
    <div>
      <label className="block text-sm font-semibold mb-1.5" style={{ color: accent === '#1565C0' ? '#1A237E' : '#4A0000' }}>
        {label}
      </label>
      <div className="relative">
        {icon && (
          <span className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 pointer-events-none"
            style={{ color:`${accent}80` }}>{icon}</span>
        )}
        {children}
      </div>
      <AnimatePresence>
        {error && (
          <motion.p initial={{ opacity:0, y:-4 }} animate={{ opacity:1, y:0 }} exit={{ opacity:0 }}
            className="text-xs mt-1 text-red-500">{error}</motion.p>
        )}
      </AnimatePresence>
    </div>
  );
}

export default function Register() {
  const { login }      = useAuth();
  const navigate       = useNavigate();
  const [params]       = useSearchParams();
  const [role, setRole] = useState(params.get('role') === 'organizer' ? 'organizer' : 'user');
  const [showPw, setShowPw]   = useState(false);
  const [loading, setLoading] = useState(false);
  const [locating, setLocating] = useState(false);

  const { register, handleSubmit, formState: { errors }, reset, setValue, watch } = useForm({
    resolver: yupResolver(role === 'user' ? userSchema : orgSchema),
  });

  const switchRole = r => { setRole(r); reset(); };
  const isOrg = role === 'organizer';
  const t     = ROLE_THEME[role];

  const onSubmit = async (data) => {
    setLoading(true);
    try {
      const fn      = isOrg ? authAPI.organizerRegister : authAPI.userRegister;
      const res     = await fn(data);
      const payload = res.data?.data;
      if (!payload?.accessToken) throw new Error('Invalid response');

      /* Store pending auth — login completes after OTP verification */
      sessionStorage.setItem('eb_pending_auth', JSON.stringify(payload));

      try {
        await authAPI.sendOtp({
          email: data.email,
          role: isOrg ? 'ORGANIZER' : 'USER',
          name: data.organizerName || data.name || '',
        });
      } catch (otpErr) {
        const msg = otpErr?.response?.data?.message || 'Could not send verification email.';
        toast.error(msg);
        return;
      }

      const dest = isOrg ? '/organizer/dashboard' : '/dashboard';
      const otpPath = isOrg ? '/verify-otp/organizer' : '/verify-otp/user';
      navigate(
        `${otpPath}?email=${encodeURIComponent(data.email)}&mode=register&redirect=${encodeURIComponent(dest)}&sent=1`,
        { replace: true },
      );
    } catch (err) {
      const msg = err?.response?.data?.message || 'Registration failed. Please try again.';
      toast.error(msg);
    } finally { setLoading(false); }
  };

  /* input style builders */
  const inputStyle = (hasErr) => ({
    width:'100%', paddingLeft:'2.75rem', paddingRight:'1rem',
    paddingTop:'0.75rem', paddingBottom:'0.75rem',
    borderRadius:'0.75rem', fontSize:'0.875rem', color:'#1A237E',
    background: t.inputBg,
    border:`1.5px solid ${hasErr ? '#EF5350' : t.inputBorder}`,
    outline:'none', transition:'all 0.2s ease',
  });
  const pwStyle = (hasErr) => ({ ...inputStyle(hasErr), paddingRight:'2.75rem' });
  const onFocus = e => { e.target.style.borderColor = t.accent; e.target.style.boxShadow = `0 0 0 3px ${t.accent}22`; e.target.style.background = '#fff'; };
  const onBlur  = (e, hasErr) => { e.target.style.borderColor = hasErr ? '#EF5350' : t.inputBorder; e.target.style.boxShadow = 'none'; e.target.style.background = t.inputBg; };
  const latitude = watch('latitude');
  const longitude = watch('longitude');
  const latNumber = Number(latitude);
  const lngNumber = Number(longitude);
  const markerPosition = Number.isFinite(latNumber) && Number.isFinite(lngNumber)
    ? [latNumber, lngNumber]
    : null;
  const mapsUrl = markerPosition
    ? `https://www.openstreetmap.org/?mlat=${latNumber}&mlon=${lngNumber}#map=16/${latNumber}/${lngNumber}`
    : null;

  const applyLocationFields = values => {
    Object.entries(values).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        setValue(key, value, { shouldDirty: true, shouldValidate: true });
      }
    });
  };

  const pickLocation = async (lat, lng) => {
    const base = {
      latitude: lat.toFixed(7),
      longitude: lng.toFixed(7),
    };

    try {
      applyLocationFields(await reverseGeocode(lat, lng));
      toast.success('Location selected. You can edit the details before registering.');
    } catch {
      applyLocationFields(base);
      toast.warn('Coordinates selected, but address lookup failed. Please fill address manually.');
    }
  };

  const detectLocation = () => {
    if (!navigator.geolocation) {
      toast.error('Location detection is not supported in this browser.');
      return;
    }

    setLocating(true);
    navigator.geolocation.getCurrentPosition(async ({ coords }) => {
      try {
        applyLocationFields(await reverseGeocode(coords.latitude, coords.longitude));
        toast.success('Location detected. You can edit the details before registering.');
      } catch {
        applyLocationFields({
          latitude: coords.latitude.toFixed(7),
          longitude: coords.longitude.toFixed(7),
        });
        toast.warn('Coordinates detected, but address lookup failed. Please fill address manually.');
      } finally {
        setLocating(false);
      }
    }, () => {
      setLocating(false);
      toast.error('Location permission was denied or unavailable.');
    }, { enableHighAccuracy: true, timeout: 12000, maximumAge: 60000 });
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12 relative overflow-hidden"
      style={{ background:t.bg, transition:'background 0.5s ease' }}>

      {/* curtain open */}
      <motion.div variants={curtainT} initial="initial" animate="animate"
        className="fixed inset-x-0 top-0 h-1/2 z-50 pointer-events-none"
        style={{ background:t.curtain, transformOrigin:'top' }} />
      <motion.div variants={curtainB} initial="initial" animate="animate"
        className="fixed inset-x-0 bottom-0 h-1/2 z-50 pointer-events-none"
        style={{ background:t.curtain, transformOrigin:'bottom' }} />

      {/* bubbles + orbs */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        {BUBBLE_POS.map((b,i) => <Bubble key={i} {...b} color={t.bubbleColors[i%t.bubbleColors.length]} />)}
        <motion.div className="absolute w-[480px] h-[480px] rounded-full"
          style={{ background:`radial-gradient(circle,${t.orbLeft},transparent 70%)`, top:'-100px', right:'-80px' }}
          animate={{ scale:[1,1.2,1] }} transition={{ duration:15, repeat:Infinity, ease:'easeInOut' }} />
        <motion.div className="absolute w-[360px] h-[360px] rounded-full"
          style={{ background:`radial-gradient(circle,${t.orbRight},transparent 70%)`, bottom:'-80px', left:'-60px' }}
          animate={{ scale:[1,1.25,1] }} transition={{ duration:18, repeat:Infinity, ease:'easeInOut', delay:2 }} />
        <div className="absolute inset-0 opacity-[0.025]"
          style={{ backgroundImage:`radial-gradient(circle,${t.accent} 1px,transparent 1px)`, backgroundSize:'40px 40px' }} />
      </div>

      {/* card */}
      <motion.div variants={cardAnim} initial="initial" animate="animate" className="w-full max-w-lg relative z-10">
        <motion.div className="absolute inset-0 rounded-3xl pointer-events-none"
          animate={{ opacity:[0.4,0.7,0.4] }} transition={{ duration:4, repeat:Infinity, ease:'easeInOut' }}
          style={{ background:`radial-gradient(ellipse at 50% 0%,${t.accent}1a,transparent 65%)`,
                   transform:'scale(1.05)', filter:'blur(20px)' }} />

        <div className="relative rounded-3xl overflow-hidden bg-white"
          style={{ border:`1.5px solid ${t.cardBorder}`,
                   boxShadow:`0 24px 64px ${t.accent}18, 0 4px 20px ${t.accent}10` }}>

          {/* animated top bar */}
          <motion.div className="h-1.5"
            style={{ background:`linear-gradient(90deg,${t.accent},${t.accentAlt},${t.accent})`, backgroundSize:'200%' }}
            animate={{ backgroundPosition:['0% 50%','100% 50%','0% 50%'] }}
            transition={{ duration:3.5, repeat:Infinity, ease:'linear' }} />

          <div className="p-8">
            {/* header */}
            <div className="text-center mb-7">
              <motion.div whileHover={{ rotate:12, scale:1.1 }} transition={{ type:'spring', stiffness:300 }}
                className="w-14 h-14 rounded-2xl mx-auto mb-4 flex items-center justify-center text-2xl shadow-lg"
                style={{ background:`linear-gradient(135deg,${t.accent},${t.accentAlt})` }}>
                ✨
              </motion.div>
              <h1 className="text-2xl font-extrabold" style={{ color:t.accent, fontFamily:'Space Grotesk,sans-serif' }}>
                Create Account
              </h1>
              <p className="text-sm mt-1" style={{ color:t.subtitleColor }}>
                Join thousands of college event lovers
              </p>
            </div>

            {/* role toggle */}
            <div className="relative flex p-1 rounded-2xl mb-6"
              style={{ background:t.toggleBg, border:`1.5px solid ${t.toggleBorder}` }}>
              <motion.div className="absolute inset-y-1 w-[calc(50%-2px)] rounded-xl"
                animate={{ left: isOrg ? 'calc(50%)' : '4px' }}
                transition={{ type:'spring', stiffness:380, damping:30 }}
                style={{ background:`linear-gradient(135deg,${t.accent},${t.accentAlt})`,
                         boxShadow:`0 4px 14px ${t.accent}35` }} />
              {['user','organizer'].map(r => (
                <button key={r} type="button" onClick={() => switchRole(r)}
                  className="flex-1 py-2.5 rounded-xl text-sm font-semibold relative z-10 transition-colors duration-200"
                  style={{ color: role===r ? '#fff' : t.subtitleColor }}>
                  {r==='user' ? '👤 User' : '🏢 Organizer'}
                </button>
              ))}
            </div>

            <form onSubmit={handleSubmit(onSubmit)}>
              <AnimatePresence mode="wait">
                <motion.div key={role}
                  initial={{ opacity:0, x:18 }} animate={{ opacity:1, x:0 }}
                  exit={{ opacity:0, x:-18 }} transition={{ duration:0.25 }}
                  className="space-y-4">

                  {isOrg ? (<>
                    <Field label="Organizer Name" icon={<FiUser/>} error={errors.organizerName?.message} accent={t.accent}>
                      <input {...register('organizerName')} placeholder="Your full name"
                        style={inputStyle(!!errors.organizerName)}
                        onFocus={onFocus} onBlur={e => onBlur(e, !!errors.organizerName)} />
                    </Field>
                    <Field label="Organization Name" icon={<FiUser/>} error={errors.organizationName?.message} accent={t.accent}>
                      <input {...register('organizationName')} placeholder="Company / College"
                        style={inputStyle(!!errors.organizationName)}
                        onFocus={onFocus} onBlur={e => onBlur(e, !!errors.organizationName)} />
                    </Field>
                  </>) : (
                    <Field label="Full Name" icon={<FiUser/>} error={errors.name?.message} accent={t.accent}>
                      <input {...register('name')} placeholder="John Doe"
                        style={inputStyle(!!errors.name)}
                        onFocus={onFocus} onBlur={e => onBlur(e, !!errors.name)} />
                    </Field>
                  )}

                  <Field label="Email Address" icon={<FiMail/>} error={errors.email?.message} accent={t.accent}>
                    <input {...register('email')} type="email" placeholder="you@college.edu"
                      style={inputStyle(!!errors.email)}
                      onFocus={onFocus} onBlur={e => onBlur(e, !!errors.email)} />
                  </Field>

                  <Field label="Password" icon={<FiLock/>} error={errors.password?.message} accent={t.accent}>
                    <input {...register('password')} type={showPw?'text':'password'}
                      placeholder="8+ chars, uppercase, number & symbol"
                      style={pwStyle(!!errors.password)}
                      onFocus={onFocus} onBlur={e => onBlur(e, !!errors.password)} />
                    <motion.button type="button" whileTap={{ scale:0.85 }} onClick={() => setShowPw(v => !v)}
                      className="absolute right-4 top-1/2 -translate-y-1/2 transition-colors"
                      style={{ color:`${t.accent}70` }}>
                      <AnimatePresence mode="wait">
                        {showPw
                          ? <motion.span key="off" initial={{ rotate:-90,opacity:0 }} animate={{ rotate:0,opacity:1 }} exit={{ rotate:90,opacity:0 }} transition={{ duration:0.15 }}><FiEyeOff className="w-4 h-4"/></motion.span>
                          : <motion.span key="on"  initial={{ rotate:90,opacity:0 }}  animate={{ rotate:0,opacity:1 }} exit={{ rotate:-90,opacity:0 }} transition={{ duration:0.15 }}><FiEye    className="w-4 h-4"/></motion.span>
                        }
                      </AnimatePresence>
                    </motion.button>
                  </Field>

                  <Field label="Phone Number" icon={<FiPhone/>} error={errors.phone?.message} accent={t.accent}>
                    <input {...register('phone')} placeholder="+91 9876543210"
                      style={inputStyle(!!errors.phone)}
                      onFocus={onFocus} onBlur={e => onBlur(e, !!errors.phone)} />
                  </Field>

                  {!isOrg && (
                    <Field label="Organization Name (optional)" icon={<FiUser/>} error={errors.organizationName?.message} accent={t.accent}>
                      <input {...register('organizationName')} placeholder="College / company"
                        style={inputStyle(!!errors.organizationName)}
                        onFocus={onFocus} onBlur={e => onBlur(e, !!errors.organizationName)} />
                    </Field>
                  )}

                  <div className="rounded-2xl p-4 space-y-4" style={{ background:t.toggleBg, border:`1px solid ${t.toggleBorder}` }}>
                    <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                      <div>
                        <p className="text-sm font-bold" style={{ color:t.labelColor }}>Location Details</p>
                        <p className="text-xs mt-0.5" style={{ color:t.subtitleColor }}>Detect with Nominatim, pick on the map, or edit manually.</p>
                      </div>
                      <button type="button" onClick={detectLocation} disabled={locating}
                        className="inline-flex items-center justify-center gap-2 rounded-xl px-3 py-2 text-xs font-bold text-white disabled:opacity-60"
                        style={{ background:`linear-gradient(135deg,${t.accent},${t.accentAlt})` }}>
                        <FiNavigation className="w-4 h-4" /> {locating ? 'Detecting...' : 'Use My Location'}
                      </button>
                    </div>

                    {mapsUrl && (
                      <a href={mapsUrl} target="_blank" rel="noreferrer"
                        className="flex items-center gap-2 text-xs font-semibold hover:underline"
                        style={{ color:t.accent }}>
                        <FiMapPin className="w-4 h-4" /> View detected point in OpenStreetMap
                      </a>
                    )}

                    <div className="h-56 overflow-hidden rounded-xl border" style={{ borderColor:t.toggleBorder }}>
                      <MapContainer
                        center={markerPosition || DEFAULT_MAP_CENTER}
                        zoom={markerPosition ? 15 : 4}
                        scrollWheelZoom={false}
                        className="h-full w-full"
                      >
                        <TileLayer
                          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                        />
                        <MapCenterSync markerPosition={markerPosition} />
                        <LocationPicker markerPosition={markerPosition} onPick={pickLocation} />
                      </MapContainer>
                    </div>

                    <Field label="Address" icon={<FiMapPin/>} error={errors.address?.message} accent={t.accent}>
                      <input {...register('address')} placeholder="Full address"
                        style={inputStyle(!!errors.address)}
                        onFocus={onFocus} onBlur={e => onBlur(e, !!errors.address)} />
                    </Field>

                    <div className="grid gap-3 sm:grid-cols-2">
                      {[
                        ['street', 'Street'],
                        ['area', 'Area'],
                        ['district', 'District'],
                      ].map(([name, label]) => (
                        <Field key={name} label={label} error={errors[name]?.message} accent={t.accent}>
                          <input {...register(name)} placeholder={label}
                            style={{...inputStyle(!!errors[name]), paddingLeft:'1rem'}}
                            onFocus={onFocus} onBlur={e => onBlur(e, !!errors[name])} />
                        </Field>
                      ))}
                    </div>
                  </div>

                    <div className="grid gap-3 sm:grid-cols-2">
                      <Field label="City" error={errors.city?.message} accent={t.accent}>
                        <input {...register('city')} placeholder="City"
                          style={{...inputStyle(!!errors.city), paddingLeft:'1rem'}}
                          onFocus={onFocus} onBlur={e => onBlur(e, !!errors.city)} />
                      </Field>
                      <Field label="State" error={errors.state?.message} accent={t.accent}>
                        <input {...register('state')} placeholder="State"
                          style={{...inputStyle(!!errors.state), paddingLeft:'1rem'}}
                          onFocus={onFocus} onBlur={e => onBlur(e, !!errors.state)} />
                      </Field>
                      <Field label="Country" error={errors.country?.message} accent={t.accent}>
                        <input {...register('country')} placeholder="Country"
                          style={{...inputStyle(!!errors.country), paddingLeft:'1rem'}}
                          onFocus={onFocus} onBlur={e => onBlur(e, !!errors.country)} />
                      </Field>
                      <Field label="PIN Code" error={errors.pinCode?.message} accent={t.accent}>
                        <input {...register('pinCode')} placeholder="PIN code"
                          style={{...inputStyle(!!errors.pinCode), paddingLeft:'1rem'}}
                          onFocus={onFocus} onBlur={e => onBlur(e, !!errors.pinCode)} />
                      </Field>
                    </div>
                    {/* latitude and longitude are captured silently from the map — not shown as inputs */}
                    <input type="hidden" {...register('latitude')} />
                    <input type="hidden" {...register('longitude')} />

                  <motion.button type="submit" disabled={loading}
                    whileHover={loading?{}:{ scale:1.02, boxShadow:`0 10px 32px ${t.accent}45` }}
                    whileTap={loading?{}:{ scale:0.97 }}
                    className="w-full py-3.5 text-sm font-bold flex items-center justify-center gap-2 rounded-xl text-white mt-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                    style={{ background:`linear-gradient(135deg,${t.accent},${t.accentAlt})`,
                             boxShadow:`0 4px 18px ${t.accent}40` }}>
                    {loading ? (
                      <><motion.div animate={{ rotate:360 }} transition={{ repeat:Infinity, duration:0.75, ease:'linear' }}
                        className="w-4 h-4 border-2 border-white/40 border-t-white rounded-full"/>Creating…</>
                    ) : (
                      <><HiSparkles className="w-4 h-4"/> Create Account <FiArrowRight className="w-4 h-4"/></>
                    )}
                  </motion.button>

                </motion.div>
              </AnimatePresence>
            </form>

            <p className="text-center text-sm mt-5 text-gray-500">
              Already have an account?{' '}
              <Link to={`/login?role=${role}`} className="font-bold hover:underline transition-colors"
                style={{ color:t.accent }}>
                Sign in
              </Link>
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}
