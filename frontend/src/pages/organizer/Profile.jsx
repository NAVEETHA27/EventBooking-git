/**
 * Organizer Profile Page
 *
 * Security pillars:
 *  - Authentication : Bearer JWT on every API call; 401 → silent refresh → /login.
 *  - Authorization  : <PrivateRoute roles={['ORGANIZER']}> + backend @PreAuthorize("hasRole('ORGANIZER')").
 *  - Confidentiality: password never shown; org logo visible only to authenticated owner;
 *    cross-organizer data access blocked server-side via AuthenticationPrincipal.
 *  - Integrity      : yup validation on all fields; password change requires currentPassword;
 *    avatar/logo upload capped at 5 MB client-side; isDirty guard prevents empty saves.
 */
import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { motion, AnimatePresence } from 'framer-motion';
import * as yup from 'yup';
import { organizerAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import ErrorBoundary from '../../components/common/ErrorBoundary';
import {
  FiCamera, FiSave, FiLock, FiUser, FiMail, FiPhone,
  FiMapPin, FiGlobe, FiShield, FiEye, FiEyeOff,
  FiAlertTriangle, FiCheckCircle, FiEdit3
} from 'react-icons/fi';
import { MdVerified, MdBusiness } from 'react-icons/md';

/* ─── Validation ──────────────────────────────────────────── */
const profileSchema = yup.object({
  organizerName:    yup.string().min(2).max(120).required('Organizer name is required'),
  organizationName: yup.string().min(2).max(160).required('Organization name is required'),
  phone:       yup.string().transform(v=>v||null).nullable()
                   .test('phone','Invalid phone number',v=>!v||/^[+]?[0-9]{7,15}$/.test(v)),
  address:     yup.string().max(300).nullable().transform(v=>v||null),
  website:     yup.string().transform(v=>v||null).nullable()
                   .test('url','Must be a valid URL (https://…)',v=>!v||/^https?:\/\/.+/.test(v)),
  description: yup.string().max(1000).nullable().transform(v=>v||null),
});

const PW_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
const pwSchema = yup.object({
  currentPassword: yup.string().required('Current password is required'),
  newPassword:     yup.string().matches(PW_REGEX, '8+ chars, uppercase, number & symbol').required('Required'),
  confirmPassword: yup.string().oneOf([yup.ref('newPassword')], 'Passwords do not match').required('Required'),
});

/* ─── Strength ────────────────────────────────────────────── */
function pwStrength(pw = '') {
  let s = 0;
  if (pw.length >= 8) s++; if (/[A-Z]/.test(pw)) s++;
  if (/[0-9]/.test(pw)) s++; if (/[@$!%*?&]/.test(pw)) s++;
  if (pw.length >= 12) s++;
  return s;
}
const SL = ['','Weak','Fair','Good','Strong','Excellent'];
const SC = ['','#EF5350','#FF9800','#FFCA28','#66BB6A','#00897B'];

/* ─── Animations ─────────────────────────────────────────── */
const tabContent = {
  initial: { opacity: 0, x: 16 },
  animate: { opacity: 1, x: 0, transition: { duration: 0.28, ease: [0.4,0,0.2,1] } },
  exit:    { opacity: 0, x: -16, transition: { duration: 0.2 } },
};

/* ─── Field wrapper ──────────────────────────────────────── */
function Field({ label, icon, error, children }) {
  return (
    <div>
      <label className="flex items-center gap-1.5 text-sm font-semibold text-blue-900 mb-1.5">
        {icon && <span className="text-red-400 w-4 h-4">{icon}</span>}{label}
      </label>
      {children}
      <AnimatePresence>
        {error && (
          <motion.p initial={{ opacity:0, y:-4 }} animate={{ opacity:1, y:0 }} exit={{ opacity:0 }}
            className="flex items-center gap-1 text-xs text-red-500 mt-1">
            <FiAlertTriangle className="w-3 h-3"/>{error}
          </motion.p>
        )}
      </AnimatePresence>
    </div>
  );
}

/* ─── Logo uploader ──────────────────────────────────────── */
function LogoUploader({ profile, onUpload }) {
  const [uploading, setUploading] = useState(false);
  const inputRef = useRef(null);

  const handleFile = async e => {
    const file = e.target.files[0]; if (!file) return;
    if (file.size > 5 * 1024 * 1024) { toast.error('Logo must be under 5 MB'); return; }
    if (!file.type.startsWith('image/')) { toast.error('Only image files allowed'); return; }
    setUploading(true);
    try {
      const fd = new FormData(); fd.append('file', file);
      const res = await organizerAPI.uploadLogo(fd);
      onUpload(res.data?.data);
      toast.success('Organization logo updated!');
    } catch {} finally { setUploading(false); }
  };

  return (
    <div className="relative w-24 h-24">
      <motion.div whileHover={{ scale: 1.04 }}
        className="w-24 h-24 rounded-2xl overflow-hidden shadow-lg flex items-center justify-center text-white text-3xl font-bold cursor-pointer"
        style={{ background: 'linear-gradient(135deg,#C62828,#1565C0)' }}
        onClick={() => inputRef.current?.click()}>
        {profile?.organizationLogo
          ? <img src={profile.organizationLogo} alt="logo" className="w-full h-full object-cover" />
          : (profile?.organizerName?.charAt(0)?.toUpperCase() ?? '?')}
        {uploading && (
          <div className="absolute inset-0 rounded-2xl flex items-center justify-center bg-black/50">
            <motion.div animate={{ rotate:360 }} transition={{ repeat:Infinity, duration:0.8, ease:'linear' }}
              className="w-6 h-6 border-2 border-white/30 border-t-white rounded-full" />
          </div>
        )}
      </motion.div>
      <motion.button whileHover={{ scale:1.12 }} whileTap={{ scale:0.9 }}
        type="button" onClick={() => inputRef.current?.click()}
        className="absolute -bottom-2 -right-2 w-8 h-8 rounded-full flex items-center justify-center text-white shadow-lg"
        style={{ background: 'linear-gradient(135deg,#C62828,#D32F2F)' }}>
        <FiCamera className="w-3.5 h-3.5" />
      </motion.button>
      <input ref={inputRef} type="file" accept="image/*" className="hidden" onChange={handleFile} />
    </div>
  );
}

/* ─── Profile Tab ────────────────────────────────────────── */
function ProfileTab({ profile, refetch }) {
  const { updateUser } = useAuth();
  const qc = useQueryClient();

  const { register, handleSubmit, formState: { errors, isDirty }, reset } = useForm({
    resolver: yupResolver(profileSchema),
    defaultValues: {
      organizerName:    '',
      organizationName: '',
      phone:            '',
      address:          '',
      website:          '',
      description:      '',
    },
  });

  /* populate once loaded */
  useEffect(() => {
    if (profile) reset({
      organizerName:    profile.organizerName    || '',
      organizationName: profile.organizationName || '',
      phone:            profile.phone            || '',
      address:          profile.address          || '',
      website:          profile.website          || '',
      description:      profile.description      || '',
    });
  }, [profile, reset]);

  const mutation = useMutation(
    data => organizerAPI.updateProfile(data),
    {
      onSuccess: res => {
        const d = res.data?.data;
        updateUser({ name: d?.organizerName, profilePicture: d?.organizationLogo });
        qc.invalidateQueries('org-profile');
        toast.success('Organization profile saved!');
        reset({
          organizerName:    d?.organizerName    || '',
          organizationName: d?.organizationName || '',
          phone:            d?.phone            || '',
          address:          d?.address          || '',
          website:          d?.website          || '',
          description:      d?.description      || '',
        });
      },
      onError: err => {
        const msg = err?.response?.data?.message || 'Failed to save. Please try again.';
        toast.error(msg);
      },
    }
  );

  const handleLogoUpload = url => {
    updateUser({ profilePicture: url });
    qc.invalidateQueries('org-profile');
  };

  return (
    <motion.div variants={tabContent} initial="initial" animate="animate" exit="exit">
      {/* Identity card */}
      <div className="bg-white rounded-2xl p-6 mb-5 border border-red-100 flex flex-col sm:flex-row items-center sm:items-start gap-6"
        style={{ boxShadow: '0 4px 20px rgba(198,40,40,0.08)' }}>
        <LogoUploader profile={profile} onUpload={handleLogoUpload} />
        <div className="text-center sm:text-left">
          <h2 className="text-xl font-extrabold text-blue-900">{profile?.organizerName}</h2>
          <div className="flex items-center gap-1.5 mt-1 justify-center sm:justify-start">
            <MdBusiness className="w-3.5 h-3.5 text-red-400" />
            <span className="text-sm text-gray-600 font-medium">{profile?.organizationName}</span>
          </div>
          <div className="flex items-center gap-1.5 mt-0.5 justify-center sm:justify-start">
            <FiMail className="w-3.5 h-3.5 text-gray-400" />
            <span className="text-sm text-gray-500">{profile?.email}</span>
          </div>
          <div className="flex items-center gap-2 mt-2 justify-center sm:justify-start">
            {profile?.emailVerified
              ? <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-green-50 text-green-700 border border-green-200">
                  <MdVerified className="w-3.5 h-3.5"/> Email Verified
                </span>
              : <span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-yellow-50 text-yellow-700 border border-yellow-200">
                  <FiAlertTriangle className="w-3.5 h-3.5"/> Unverified
                </span>
            }
            <span className="badge badge-red text-[10px]">ORGANIZER</span>
          </div>
        </div>
      </div>

      {/* Edit form */}
      <form onSubmit={handleSubmit(d => mutation.mutate(d))}>
        <div className="bg-white rounded-2xl p-6 border border-red-100 space-y-5"
          style={{ boxShadow: '0 4px 20px rgba(198,40,40,0.08)' }}>
          <h3 className="text-base font-bold text-blue-900 flex items-center gap-2">
            <FiEdit3 className="w-4 h-4 text-red-400" /> Organization Details
          </h3>

          <div className="grid sm:grid-cols-2 gap-5">
            <Field label="Organizer Name" icon={<FiUser/>} error={errors.organizerName?.message}>
              <input {...register('organizerName')} placeholder="Your name" className="input-field" />
            </Field>
            <Field label="Organization Name" icon={<MdBusiness/>} error={errors.organizationName?.message}>
              <input {...register('organizationName')} placeholder="Company / College" className="input-field" />
            </Field>
          </div>

          <div className="grid sm:grid-cols-2 gap-5">
            <Field label="Phone" icon={<FiPhone/>} error={errors.phone?.message}>
              <input {...register('phone')} placeholder="+91 9876543210" className="input-field" />
            </Field>
            <Field label="Website" icon={<FiGlobe/>} error={errors.website?.message}>
              <input {...register('website')} placeholder="https://example.com" className="input-field" />
            </Field>
          </div>

          <Field label="Address" icon={<FiMapPin/>} error={errors.address?.message}>
            <input {...register('address')} placeholder="Office / College address" className="input-field" />
          </Field>

          <Field label="Description" error={errors.description?.message}>
            <textarea {...register('description')} rows={3}
              placeholder="Tell participants about your organization…"
              className="input-field resize-none" />
          </Field>

          <motion.button type="submit"
            disabled={mutation.isLoading || !isDirty}
            whileHover={mutation.isLoading || !isDirty ? {} : { scale: 1.02 }}
            whileTap={mutation.isLoading || !isDirty ? {} : { scale: 0.97 }}
            className="flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-bold text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            style={{ background: 'linear-gradient(135deg,#C62828,#D32F2F)', boxShadow: '0 4px 16px rgba(198,40,40,0.35)' }}>
            {mutation.isLoading
              ? <><motion.div animate={{ rotate:360 }} transition={{ repeat:Infinity, duration:0.75, ease:'linear' }}
                  className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full"/>Saving…</>
              : <><FiSave className="w-4 h-4"/>Save Changes</>}
          </motion.button>

          {!isDirty && <p className="text-xs text-gray-400 mt-1">No unsaved changes.</p>}
        </div>
      </form>
    </motion.div>
  );
}

/* ─── Security Tab ───────────────────────────────────────── */
function SecurityTab() {
  const [show, setShow] = useState({ cur:false, nw:false, cn:false });
  const toggle = k => setShow(s => ({ ...s, [k]: !s[k] }));

  const { register, handleSubmit, formState:{errors}, reset, watch } = useForm({ resolver: yupResolver(pwSchema) });
  const watchedPw = watch('newPassword', '');

  const mutation = useMutation(
    data => organizerAPI.changePassword({ currentPassword: data.currentPassword, newPassword: data.newPassword }),
    {
      onSuccess: () => { toast.success('Password changed successfully!'); reset(); },
      onError: err => toast.error(err?.response?.data?.message || 'Failed to change password'),
    }
  );

  const PwField = ({ label, regKey, showKey, error }) => (
    <Field label={label} icon={<FiLock/>} error={error}>
      <div className="relative">
        <FiLock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-red-300 pointer-events-none" />
        <input {...register(regKey)} type={show[showKey]?'text':'password'} placeholder="••••••••"
          className={`input-field pl-10 pr-10 ${error?'border-red-300':''}`} />
        <button type="button" onClick={() => toggle(showKey)}
          className="absolute right-3.5 top-1/2 -translate-y-1/2 text-red-300 hover:text-red-600 transition-colors">
          {show[showKey] ? <FiEyeOff className="w-4 h-4"/> : <FiEye className="w-4 h-4"/>}
        </button>
      </div>
    </Field>
  );

  return (
    <motion.div variants={tabContent} initial="initial" animate="animate" exit="exit" className="space-y-5">
      {/* Security notice */}
      <div className="bg-white rounded-2xl p-5 border border-red-100 flex items-start gap-4"
        style={{ boxShadow:'0 4px 20px rgba(198,40,40,0.08)' }}>
        <div className="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0"
          style={{ background:'linear-gradient(135deg,#C62828,#D32F2F)' }}>
          <FiShield className="text-white w-5 h-5" />
        </div>
        <div>
          <h3 className="font-bold text-blue-900 text-sm">Account Security</h3>
          <p className="text-xs text-gray-500 mt-0.5 leading-relaxed">
            Passwords are BCrypt-hashed. Your organizer token carries ROLE_ORGANIZER and cannot
            access user or admin endpoints. All tokens expire in 15 minutes.
          </p>
        </div>
      </div>

      {/* Password change */}
      <div className="bg-white rounded-2xl p-6 border border-red-100 space-y-5"
        style={{ boxShadow:'0 4px 20px rgba(198,40,40,0.08)' }}>
        <h3 className="text-base font-bold text-blue-900 flex items-center gap-2">
          <FiLock className="w-4 h-4 text-red-400"/> Change Password
        </h3>

        <form onSubmit={handleSubmit(d => mutation.mutate(d))} className="space-y-4">
          <PwField label="Current Password" regKey="currentPassword" showKey="cur" error={errors.currentPassword?.message} />
          <PwField label="New Password"     regKey="newPassword"     showKey="nw"  error={errors.newPassword?.message} />

          {watchedPw.length > 0 && (
            <motion.div initial={{ opacity:0 }} animate={{ opacity:1 }} className="space-y-1.5">
              <div className="flex gap-1">
                {[1,2,3,4,5].map(i => (
                  <motion.div key={i} className="h-1.5 flex-1 rounded-full"
                    animate={{ background: i <= pwStrength(watchedPw) ? SC[pwStrength(watchedPw)] : '#FFEBEE' }}
                    transition={{ duration:0.3 }} />
                ))}
              </div>
              <p className="text-xs font-semibold" style={{ color: SC[pwStrength(watchedPw)] }}>
                {SL[pwStrength(watchedPw)]}
              </p>
            </motion.div>
          )}

          <PwField label="Confirm New Password" regKey="confirmPassword" showKey="cn" error={errors.confirmPassword?.message} />

          <motion.button type="submit" disabled={mutation.isLoading}
            whileHover={mutation.isLoading ? {} : { scale:1.02 }}
            whileTap={mutation.isLoading ? {} : { scale:0.97 }}
            className="flex items-center gap-2 px-6 py-3 rounded-xl text-sm font-bold text-white transition-all disabled:opacity-50 disabled:cursor-not-allowed"
            style={{ background:'linear-gradient(135deg,#C62828,#D32F2F)', boxShadow:'0 4px 16px rgba(198,40,40,0.35)' }}>
            {mutation.isLoading
              ? <><motion.div animate={{ rotate:360 }} transition={{ repeat:Infinity, duration:0.75, ease:'linear' }}
                  className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full"/>Changing…</>
              : <><FiLock className="w-4 h-4"/>Update Password</>}
          </motion.button>
        </form>
      </div>

      {/* Active session indicator */}
      <div className="bg-white rounded-2xl p-5 border border-green-100"
        style={{ boxShadow:'0 4px 16px rgba(46,125,50,0.07)' }}>
        <div className="flex items-center gap-3">
          <FiCheckCircle className="w-5 h-5 text-green-600 flex-shrink-0" />
          <div>
            <p className="text-sm font-semibold text-green-800">Active Organizer Session</p>
            <p className="text-xs text-green-600">
              Authenticated as ORGANIZER. Token auto-refreshes silently.
            </p>
          </div>
        </div>
      </div>
    </motion.div>
  );
}

/* ─── Main Page ──────────────────────────────────────────── */
const TABS = [
  { id:'profile',  label:'🏢 Profile'  },
  { id:'security', label:'🔐 Security' },
];

export default function OrganizerProfile() {
  const [tab, setTab] = useState('profile');

  const { data: profile, isLoading, refetch } = useQuery(
    'org-profile',
    () => organizerAPI.getProfile().then(r => r.data?.data),
    { retry:1, staleTime:30_000 }
  );

  if (isLoading) {
    return (
      <div style={{ background:'#F0F4FF', minHeight:'100vh' }} className="px-4 py-10">
        <div className="max-w-2xl mx-auto space-y-5">
          <div className="skeleton h-8 w-52 rounded-xl" />
          <div className="skeleton h-36 rounded-2xl" />
          <div className="skeleton h-12 rounded-xl" />
          <div className="skeleton h-80 rounded-2xl" />
        </div>
      </div>
    );
  }

  return (
    <div style={{ background:'#F0F4FF', minHeight:'100vh' }} className="px-4 sm:px-6 py-10">
      <div className="max-w-2xl mx-auto">

        <motion.div initial={{ opacity:0, y:-16 }} animate={{ opacity:1, y:0 }} transition={{ duration:0.4 }} className="mb-7">
          <h1 className="text-3xl font-extrabold text-blue-900" style={{ fontFamily:'Space Grotesk,sans-serif' }}>
            Organizer Profile
          </h1>
          <p className="text-sm text-gray-500 mt-1">Manage your organization details and account security.</p>
        </motion.div>

        {/* Tab strip */}
        <motion.div initial={{ opacity:0, y:10 }} animate={{ opacity:1, y:0 }} transition={{ delay:0.1 }}
          className="flex p-1 rounded-2xl bg-white border border-red-100 mb-6"
          style={{ boxShadow:'0 2px 12px rgba(198,40,40,0.07)' }}>
          {TABS.map(t => (
            <button key={t.id} type="button" onClick={() => setTab(t.id)}
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold relative z-10 transition-all duration-200">
              <AnimatePresence>
                {tab === t.id && (
                  <motion.div layoutId="org-tab-pill"
                    className="absolute inset-0 rounded-xl"
                    style={{ background:'linear-gradient(135deg,#C62828,#D32F2F)' }}
                    initial={{ opacity:0 }} animate={{ opacity:1 }} exit={{ opacity:0 }}
                    transition={{ duration:0.2 }} />
                )}
              </AnimatePresence>
              <span className="relative z-10" style={{ color: tab===t.id ? '#fff' : '#546E7A' }}>
                {t.label}
              </span>
            </button>
          ))}
        </motion.div>

        <ErrorBoundary onRetry={refetch}>
          <AnimatePresence mode="wait">
            {tab === 'profile'
              ? <ProfileTab key="profile" profile={profile} refetch={refetch} />
              : <SecurityTab key="security" />
            }
          </AnimatePresence>
        </ErrorBoundary>
      </div>
    </div>
  );
}
