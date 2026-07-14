import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { motion, AnimatePresence } from 'framer-motion';
import * as yup from 'yup';
import { organizerAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import {
  FiSave, FiLock, FiEye, FiEyeOff, FiAlertTriangle,
  FiEdit3, FiList, FiCheckCircle, FiUsers, FiShield,
  FiBell, FiSettings, FiCamera, FiGlobe,
} from 'react-icons/fi';
import { MdBusiness, MdVerified } from 'react-icons/md';
import { format } from 'date-fns';

/* ── validation ─────────────────────────────────────────── */
const profileSchema = yup.object({
  organizerName:    yup.string().min(2).max(120).required('Name is required'),
  organizationName: yup.string().min(2).max(160).required('Organization name is required'),
  phone:       yup.string().transform(v=>v||null).nullable()
                   .test('ph','Invalid phone',v=>!v||/^[+]?[0-9]{7,15}$/.test(v)),
  address:     yup.string().max(300).nullable().transform(v=>v||null),
  website:     yup.string().transform(v=>v||null).nullable()
                   .test('url','Must be a valid URL',v=>!v||/^https?:\/\/.+/.test(v)),
  description: yup.string().max(1000).nullable().transform(v=>v||null),
});

const PW_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
const pwSchema = yup.object({
  currentPassword: yup.string().required('Required'),
  newPassword:     yup.string().matches(PW_REGEX,'8+ chars, uppercase, number & symbol').required(),
  confirmPassword: yup.string().oneOf([yup.ref('newPassword')],'Passwords do not match').required(),
});

function pwStrength(pw='') {
  let s=0;
  if(pw.length>=8)s++; if(/[A-Z]/.test(pw))s++;
  if(/[0-9]/.test(pw))s++; if(/[@$!%*?&]/.test(pw))s++;
  if(pw.length>=12)s++; return s;
}
const SL=['','Weak','Fair','Good','Strong','Excellent'];
const SC=['','#EF5350','#FF9800','#FFCA28','#66BB6A','#00897B'];

function fmtDate(d) {
  if (!d) return '—';
  try { return format(new Date(d), 'dd MMMM yyyy'); } catch { return d; }
}

/* ── nav config ─────────────────────────────────────────── */
const NAV = [
  { id:'overview',      label:'Profile Overview', icon:<MdBusiness/> },
  { id:'edit',          label:'Edit Profile',     icon:<FiEdit3/> },
  { id:'events',        label:'My Events',        icon:<FiList/>,    href:'/organizer/events' },
  { id:'attendees',     label:'Attendees',         icon:<FiUsers/>,   href:'/organizer/attendees' },
  { id:'analytics',     label:'Analytics',         icon:<FiCheckCircle/>, href:'/organizer/analytics' },
  { id:'security',      label:'Security',          icon:<FiShield/> },
  { id:'notifications', label:'Notifications',     icon:<FiBell/>,    href:'/notifications' },
  { id:'preferences',   label:'Preferences',       icon:<FiSettings/> },
];

function Field({ label, error, children }) {
  return (
    <div>
      <label className="block text-sm font-medium text-slate-500 mb-1">{label}</label>
      {children}
      {error && <p className="text-xs text-red-500 mt-1 flex items-center gap-1"><FiAlertTriangle className="w-3 h-3"/>{error}</p>}
    </div>
  );
}

function InfoRow({ label, value, green }) {
  return (
    <div className="flex items-start gap-4 py-3 border-b border-slate-100 last:border-0">
      <span className="w-36 shrink-0 text-sm text-slate-400">{label}</span>
      <span className={`text-sm font-medium ${green ? 'text-emerald-600 font-bold' : 'text-slate-800'}`}>{value || '—'}</span>
    </div>
  );
}

/* ── Logo uploader ──────────────────────────────────────── */
function LogoUploader({ profile, onUpload }) {
  const [uploading, setUploading] = useState(false);
  const ref = useRef(null);
  const handle = async e => {
    const file = e.target.files[0]; if (!file) return;
    if (file.size > 5*1024*1024) { toast.error('Logo must be under 5 MB'); return; }
    if (!file.type.startsWith('image/')) { toast.error('Only images allowed'); return; }
    setUploading(true);
    try {
      const fd = new FormData(); fd.append('file', file);
      const res = await organizerAPI.uploadLogo(fd);
      onUpload(res.data?.data);
      toast.success('Logo updated!');
    } catch {} finally { setUploading(false); }
  };
  return (
    <div className="relative w-20 h-20">
      <div className="w-20 h-20 rounded-full overflow-hidden border-2 border-white shadow-md flex items-center justify-center text-white text-2xl font-bold cursor-pointer"
        style={{background:'linear-gradient(135deg,#C62828,#1565C0)'}}
        onClick={() => ref.current?.click()}>
        {profile?.organizationLogo
          ? <img src={profile.organizationLogo} alt="logo" className="w-full h-full object-cover"/>
          : profile?.organizerName?.charAt(0)?.toUpperCase()}
        {uploading && <div className="absolute inset-0 rounded-full bg-black/50 flex items-center justify-center">
          <motion.div animate={{rotate:360}} transition={{repeat:Infinity,duration:0.8,ease:'linear'}}
            className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full"/>
        </div>}
      </div>
      <button type="button" onClick={() => ref.current?.click()}
        className="absolute -bottom-1 -right-1 w-7 h-7 rounded-full bg-red-500 border-2 border-white flex items-center justify-center text-white shadow">
        <FiCamera className="w-3 h-3"/>
      </button>
      <input ref={ref} type="file" accept="image/*" className="hidden" onChange={handle}/>
    </div>
  );
}

/* ── Overview ───────────────────────────────────────────── */
function OverviewPanel({ profile }) {
  return (
    <div className="space-y-5">
      <div className="bg-white rounded-2xl border border-slate-200 p-6">
        <h3 className="font-bold text-slate-900 mb-4">Organization Information</h3>
        <InfoRow label="Organizer Name"    value={profile?.organizerName} />
        <InfoRow label="Organization"      value={profile?.organizationName} />
        <InfoRow label="Email"             value={profile?.email} />
        <InfoRow label="Phone"             value={profile?.phone} />
        <InfoRow label="Website"           value={profile?.website} />
        <InfoRow label="Role"              value={profile?.role} />
        <InfoRow label="Member Since"      value={fmtDate(profile?.createdAt)} />
        <InfoRow label="Organizer ID"      value={profile?.organizerCode} green />
      </div>
      <div className="bg-white rounded-2xl border border-slate-200 p-6">
        <h3 className="font-bold text-slate-900 mb-4">Quick Actions</h3>
        <div className="grid grid-cols-3 gap-3">
          {[
            { label:'My Events',  icon:<FiList className="w-4 h-4 text-teal-600"/>,        to:'/organizer/events' },
            { label:'Attendees',  icon:<FiUsers className="w-4 h-4 text-blue-600"/>,       to:'/organizer/attendees' },
            { label:'Analytics',  icon:<FiCheckCircle className="w-4 h-4 text-amber-500"/>,to:'/organizer/analytics' },
          ].map(s => (
            <Link key={s.label} to={s.to}
              className="flex flex-col items-center gap-1.5 rounded-xl border border-slate-100 bg-slate-50 py-4 hover:bg-red-50 hover:border-red-200 transition-colors">
              {s.icon}
              <span className="text-xs font-semibold text-slate-600">{s.label}</span>
            </Link>
          ))}
        </div>
      </div>
    </div>
  );
}

/* ── Edit ───────────────────────────────────────────────── */
function EditPanel({ profile, refetch }) {
  const { updateUser } = useAuth();
  const qc = useQueryClient();
  const { register, handleSubmit, formState:{errors,isDirty}, reset } = useForm({
    resolver: yupResolver(profileSchema),
    defaultValues: { organizerName:'', organizationName:'', phone:'', address:'', website:'', description:'' },
  });
  useEffect(() => {
    if (profile) reset({
      organizerName: profile.organizerName||'', organizationName: profile.organizationName||'',
      phone: profile.phone||'', address: profile.address||'',
      website: profile.website||'', description: profile.description||'',
    });
  }, [profile, reset]);

  const mut = useMutation(d => organizerAPI.updateProfile(d), {
    onSuccess: res => {
      const d = res.data?.data;
      updateUser({ name:d?.organizerName, profilePicture:d?.organizationLogo });
      qc.invalidateQueries('org-profile');
      toast.success('Profile saved!');
      reset({ organizerName:d?.organizerName||'', organizationName:d?.organizationName||'',
        phone:d?.phone||'', address:d?.address||'', website:d?.website||'', description:d?.description||'' });
    },
    onError: err => toast.error(err?.response?.data?.message||'Failed to save'),
  });

  const onLogoUpload = url => { updateUser({ profilePicture:url }); qc.invalidateQueries('org-profile'); };

  return (
    <form onSubmit={handleSubmit(d => mut.mutate(d))}>
      <div className="bg-white rounded-2xl border border-slate-200 p-6 space-y-4">
        <div className="flex items-center gap-4 pb-4 border-b border-slate-100">
          <LogoUploader profile={profile} onUpload={onLogoUpload}/>
          <div>
            <p className="font-semibold text-slate-800">{profile?.organizerName}</p>
            <p className="text-xs text-slate-400">{profile?.email}</p>
          </div>
        </div>
        <h3 className="font-bold text-slate-900">Edit Profile</h3>
        <Field label="Organizer Name" error={errors.organizerName?.message}>
          <input {...register('organizerName')} className="input-field" placeholder="Your name"/>
        </Field>
        <Field label="Organization Name" error={errors.organizationName?.message}>
          <input {...register('organizationName')} className="input-field" placeholder="Company / College"/>
        </Field>
        <Field label="Phone" error={errors.phone?.message}>
          <input {...register('phone')} className="input-field" placeholder="+91 9876543210"/>
        </Field>
        <Field label="Website" error={errors.website?.message}>
          <div className="relative">
            <FiGlobe className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400"/>
            <input {...register('website')} className="input-field pl-9" placeholder="https://example.com"/>
          </div>
        </Field>
        <Field label="Address" error={errors.address?.message}>
          <input {...register('address')} className="input-field" placeholder="Office / College address"/>
        </Field>
        <Field label="Description" error={errors.description?.message}>
          <textarea {...register('description')} rows={3} className="input-field resize-none"
            placeholder="Tell participants about your organization…"/>
        </Field>
        <button type="submit" disabled={mut.isLoading||!isDirty}
          className="btn-primary flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
          <FiSave className="w-4 h-4"/>{mut.isLoading ? 'Saving…' : 'Save Changes'}
        </button>
      </div>
    </form>
  );
}

/* ── Security ───────────────────────────────────────────── */
function SecurityPanel() {
  const [show, setShow] = useState({ cur:false, nw:false, cn:false });
  const { register, handleSubmit, formState:{errors}, reset, watch } = useForm({ resolver:yupResolver(pwSchema) });
  const pw = watch('newPassword','');
  const str = pwStrength(pw);
  const mut = useMutation(
    d => organizerAPI.changePassword({ currentPassword:d.currentPassword, newPassword:d.newPassword }),
    { onSuccess:()=>{ toast.success('Password changed!'); reset(); },
      onError: err => toast.error(err?.response?.data?.message||'Failed') }
  );
  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6 space-y-4">
      <h3 className="font-bold text-slate-900">Change Password</h3>
      <form onSubmit={handleSubmit(d => mut.mutate(d))} className="space-y-4">
        {[['currentPassword','Current Password','cur'],['newPassword','New Password','nw'],['confirmPassword','Confirm Password','cn']].map(([key,lbl,sk])=>(
          <Field key={key} label={lbl} error={errors[key]?.message}>
            <div className="relative">
              <input {...register(key)} type={show[sk]?'text':'password'} className="input-field pr-10" placeholder="••••••••"/>
              <button type="button" onClick={()=>setShow(s=>({...s,[sk]:!s[sk]}))}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600">
                {show[sk]?<FiEyeOff className="w-4 h-4"/>:<FiEye className="w-4 h-4"/>}
              </button>
            </div>
          </Field>
        ))}
        {pw.length>0 && (
          <div className="space-y-1">
            <div className="flex gap-1">{[1,2,3,4,5].map(i=>(
              <div key={i} className="h-1.5 flex-1 rounded-full transition-all duration-300"
                style={{background:i<=str?SC[str]:'#E2E8F0'}}/>
            ))}</div>
            <p className="text-xs font-semibold" style={{color:SC[str]}}>{SL[str]}</p>
          </div>
        )}
        <button type="submit" disabled={mut.isLoading} className="btn-primary flex items-center gap-2 disabled:opacity-50">
          <FiLock className="w-4 h-4"/>{mut.isLoading ? 'Updating…' : 'Update Password'}
        </button>
      </form>
      <div className="flex items-center gap-2 pt-2 border-t border-slate-100">
        <FiCheckCircle className="w-4 h-4 text-emerald-500"/>
        <span className="text-xs text-slate-500">JWT session active · auto-refresh enabled</span>
      </div>
    </div>
  );
}

function PreferencesPanel() {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-6">
      <h3 className="font-bold text-slate-900 mb-3">Preferences</h3>
      <p className="text-sm text-slate-400">Notification and display preferences will appear here.</p>
    </div>
  );
}

/* ── Main ───────────────────────────────────────────────── */
export default function OrganizerProfile() {
  const [active, setActive] = useState('overview');
  const { data:profile, isLoading, refetch } = useQuery(
    'org-profile',
    () => organizerAPI.getProfile().then(r => r.data?.data),
    { retry:1, staleTime:30_000 }
  );

  if (isLoading) return (
    <div style={{background:'#F8FAFC',minHeight:'100vh'}} className="px-4 py-10">
      <div className="max-w-5xl mx-auto space-y-4">
        <div className="skeleton h-8 w-52 rounded-xl"/>
        <div className="skeleton h-28 rounded-2xl"/>
        <div className="grid lg:grid-cols-[240px_1fr] gap-5">
          <div className="skeleton h-80 rounded-2xl"/>
          <div className="skeleton h-80 rounded-2xl"/>
        </div>
      </div>
    </div>
  );

  return (
    <div style={{background:'#F8FAFC',minHeight:'100vh'}} className="px-4 sm:px-6 py-8">
      <div className="max-w-5xl mx-auto space-y-5">

        <h1 className="text-2xl font-extrabold text-slate-900" style={{fontFamily:'Space Grotesk,sans-serif'}}>
          Organizer Profile
        </h1>

        {/* Top identity bar */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 flex flex-col sm:flex-row items-start sm:items-center gap-5">
          <div className="flex items-center gap-4 flex-1 min-w-0">
            {/* Logo — click goes to edit */}
            <div className="relative cursor-pointer" onClick={() => setActive('edit')}>
              <div className="w-20 h-20 rounded-full overflow-hidden border-2 border-white shadow-md flex items-center justify-center text-white text-2xl font-bold"
                style={{background:'linear-gradient(135deg,#C62828,#1565C0)'}}>
                {profile?.organizationLogo
                  ? <img src={profile.organizationLogo} alt="logo" className="w-full h-full object-cover"/>
                  : profile?.organizerName?.charAt(0)?.toUpperCase()}
              </div>
              <div className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-red-500 border-2 border-white flex items-center justify-center">
                <FiCamera className="w-3 h-3 text-white"/>
              </div>
            </div>
            <div>
              <h2 className="text-xl font-extrabold text-slate-900">{profile?.organizerName}</h2>
              <div className="flex items-center gap-1.5 mt-0.5">
                <MdBusiness className="w-3.5 h-3.5 text-red-400"/>
                <span className="text-sm text-slate-500">{profile?.organizationName}</span>
              </div>
              <span className="mt-1.5 inline-flex items-center gap-1 rounded-full bg-emerald-50 border border-emerald-200 px-2.5 py-0.5 text-xs font-semibold text-emerald-700">
                {profile?.emailVerified
                  ? <><MdVerified className="w-3.5 h-3.5"/>Active</>
                  : <><FiAlertTriangle className="w-3.5 h-3.5"/>Unverified</>}
              </span>
            </div>
          </div>
          {/* ID card */}
          <div className="shrink-0 rounded-xl border border-slate-200 bg-slate-50 px-5 py-3 min-w-[160px] text-center">
            <p className="text-xs text-slate-400 font-medium mb-1">My ID</p>
            <p className="text-2xl font-extrabold text-red-600 tracking-wide">{profile?.organizerCode || '—'}</p>
            <p className="text-xs text-slate-400 mt-0.5">Organizer ID</p>
          </div>
        </div>

        {/* Body */}
        <div className="grid gap-5 lg:grid-cols-[240px_1fr]">

          {/* Sidebar */}
          <nav className="bg-white rounded-2xl border border-slate-200 p-3 h-fit">
            {NAV.map(item => {
              const isActive = active === item.id;
              const cls = `flex items-center gap-3 rounded-xl px-4 py-2.5 text-sm font-semibold transition-colors w-full
                ${isActive ? 'bg-red-50 text-red-700 border-l-4 border-red-500' : 'text-slate-600 hover:bg-slate-50'}`;
              if (item.href) {
                return (
                  <Link key={item.id} to={item.href} className={cls.replace('w-full','')}>
                    <span className="text-slate-400">{item.icon}</span>
                    {item.label}
                  </Link>
                );
              }
              return (
                <button key={item.id} type="button" onClick={() => setActive(item.id)} className={cls}>
                  <span className={isActive ? 'text-red-500' : 'text-slate-400'}>{item.icon}</span>
                  {item.label}
                </button>
              );
            })}
          </nav>

          {/* Content */}
          <AnimatePresence mode="wait">
            <motion.div key={active}
              initial={{opacity:0,x:10}} animate={{opacity:1,x:0}} exit={{opacity:0,x:-10}}
              transition={{duration:0.2}}>
              {active==='overview'    && <OverviewPanel profile={profile}/>}
              {active==='edit'        && <EditPanel profile={profile} refetch={refetch}/>}
              {active==='security'    && <SecurityPanel/>}
              {active==='preferences' && <PreferencesPanel/>}
            </motion.div>
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
