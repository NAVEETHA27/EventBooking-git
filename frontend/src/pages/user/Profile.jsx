import { useState, useRef, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { useForm } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import { motion, AnimatePresence } from 'framer-motion';
import * as yup from 'yup';
import { userAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import { toast } from 'react-toastify';
import ErrorBoundary from '../../components/common/ErrorBoundary';
import Spinner from '../../components/common/Spinner';
import {
  FiCamera, FiSave, FiLock, FiUser, FiMail, FiPhone,
  FiMapPin, FiShield, FiEye, FiEyeOff, FiAlertTriangle,
  FiCheckCircle, FiEdit3,
} from 'react-icons/fi';
import { MdVerified } from 'react-icons/md';

/* ── validation ───────────────────────────────────────────── */
const profileSchema = yup.object({
  name:    yup.string().min(2,'Min 2 chars').max(120).required('Name is required'),
  phone:   yup.string().transform(v => v || null)
               .nullable()
               .test('phone','Invalid phone number', v => !v || /^[+]?[0-9]{7,15}$/.test(v)),
  address: yup.string().max(300).nullable().transform(v => v || null),
  organizationName: yup.string().max(160).nullable().transform(v => v || null),
  city: yup.string().max(100).nullable().transform(v => v || null),
  state: yup.string().max(100).nullable().transform(v => v || null),
  country: yup.string().max(100).nullable().transform(v => v || null),
  pinCode: yup.string().max(20).nullable().transform(v => v || null),
  gender:  yup.string().nullable().transform(v => v || null),
});

const locationSchema = yup.object({
  address: yup.string().max(300).nullable().transform(v => v || null),
  street: yup.string().max(120).nullable().transform(v => v || null),
  area: yup.string().max(120).nullable().transform(v => v || null),
  city: yup.string().max(100).nullable().transform(v => v || null),
  district: yup.string().max(100).nullable().transform(v => v || null),
  state: yup.string().max(100).nullable().transform(v => v || null),
  country: yup.string().max(100).nullable().transform(v => v || null),
  pinCode: yup.string().max(20).nullable().transform(v => v || null),
  latitude: yup.number().min(-90).max(90).nullable().transform((_, v) => v === '' ? null : Number(v)),
  longitude: yup.number().min(-180).max(180).nullable().transform((_, v) => v === '' ? null : Number(v)),
});

const PW_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/;
const pwSchema = yup.object({
  currentPassword: yup.string().required('Current password is required'),
  newPassword:     yup.string().matches(PW_REGEX,'8+ chars, uppercase, number & symbol').required('Required'),
  confirmPassword: yup.string().oneOf([yup.ref('newPassword')],'Passwords do not match').required('Required'),
});

/* ── password strength ────────────────────────────────────── */
function pwStrength(pw='') {
  let s=0;
  if(pw.length>=8)s++; if(/[A-Z]/.test(pw))s++;
  if(/[0-9]/.test(pw))s++; if(/[@$!%*?&]/.test(pw))s++;
  if(pw.length>=12)s++;
  return s;
}
const SL=['','Weak','Fair','Good','Strong','Excellent'];
const SC=['','#EF5350','#FF9800','#FFCA28','#66BB6A','#00897B'];

/* ── animations ───────────────────────────────────────────── */
const tab = {
  initial:{opacity:0,x:16},
  animate:{opacity:1,x:0,transition:{duration:0.28,ease:[0.4,0,0.2,1]}},
  exit:   {opacity:0,x:-16,transition:{duration:0.2}},
};

/* ── Field ────────────────────────────────────────────────── */
function Field({label,icon,error,children}){
  return(
    <div>
      <label className="flex items-center gap-1.5 text-sm font-semibold text-blue-900 mb-1.5">
        {icon&&<span className="text-blue-400 w-4 h-4">{icon}</span>}{label}
      </label>
      {children}
      <AnimatePresence>
        {error&&<motion.p initial={{opacity:0,y:-4}} animate={{opacity:1,y:0}} exit={{opacity:0}}
          className="flex items-center gap-1 text-xs text-red-500 mt-1">
          <FiAlertTriangle className="w-3 h-3"/>{error}
        </motion.p>}
      </AnimatePresence>
    </div>
  );
}

/* ── Avatar ───────────────────────────────────────────────── */
function AvatarUploader({profile,onUpload}){
  const [uploading,setUploading]=useState(false);
  const ref=useRef(null);
  const handle=async e=>{
    const file=e.target.files[0]; if(!file)return;
    if(file.size>5*1024*1024){toast.error('Image must be under 5 MB');return;}
    if(!file.type.startsWith('image/')){toast.error('Only images allowed');return;}
    setUploading(true);
    try{
      const fd=new FormData(); fd.append('file',file);
      const res=await userAPI.uploadPicture(fd);
      onUpload(res.data?.data);
      toast.success('Photo updated!');
    }catch{}finally{setUploading(false);}
  };
  return(
    <div className="relative w-24 h-24">
      <motion.div whileHover={{scale:1.04}}
        className="w-24 h-24 rounded-2xl overflow-hidden shadow-lg flex items-center justify-center text-white text-3xl font-bold cursor-pointer"
        style={{background:'linear-gradient(135deg,#1565C0,#D32F2F)'}}
        onClick={()=>ref.current?.click()}>
        {profile?.profilePicture
          ?<img src={profile.profilePicture} alt="" className="w-full h-full object-cover"/>
          :(profile?.name?.charAt(0)?.toUpperCase()||'?')}
        {uploading&&<div className="absolute inset-0 bg-black/50 flex items-center justify-center">
          <motion.div animate={{rotate:360}} transition={{repeat:Infinity,duration:0.8,ease:'linear'}}
            className="w-6 h-6 border-2 border-white/30 border-t-white rounded-full"/>
        </div>}
      </motion.div>
      <motion.button whileHover={{scale:1.12}} whileTap={{scale:0.9}} type="button"
        onClick={()=>ref.current?.click()}
        className="absolute -bottom-2 -right-2 w-8 h-8 rounded-full flex items-center justify-center text-white shadow-lg"
        style={{background:'linear-gradient(135deg,#1565C0,#1976D2)'}}>
        <FiCamera className="w-3.5 h-3.5"/>
      </motion.button>
      <input ref={ref} type="file" accept="image/*" className="hidden" onChange={handle}/>
    </div>
  );
}

/* ── Profile tab ──────────────────────────────────────────── */
function ProfileTab({profile,refetch}){
  const {updateUser}=useAuth();
  const qc=useQueryClient();

  const {register,handleSubmit,formState:{errors,isDirty},reset}=useForm({
    resolver:yupResolver(profileSchema),
    defaultValues:{name:'',phone:'',address:'',gender:''},
  });

  /* populate form once profile loads */
  useEffect(()=>{
    if(profile) reset({
      name:    profile.name    || '',
      phone:   profile.phone   || '',
      address: profile.address || '',
      organizationName: profile.organizationName || '',
      city: profile.city || '',
      state: profile.state || '',
      country: profile.country || '',
      pinCode: profile.pinCode || '',
      gender:  profile.gender  || '',
    });
  },[profile,reset]);

  const mutation=useMutation(
    data=>userAPI.updateProfile(data),
    {
      onSuccess:res=>{
        const d=res.data?.data;
        updateUser({name:d?.name,profilePicture:d?.profilePicture});
        qc.invalidateQueries('user-profile');
        toast.success('Profile saved!');
        reset({
          name:d?.name||'', phone:d?.phone||'', address:d?.address||'', gender:d?.gender||'',
          organizationName:d?.organizationName||'', city:d?.city||'', state:d?.state||'',
          country:d?.country||'', pinCode:d?.pinCode||''
        });
      },
      onError:err=>{
        const msg=err?.response?.data?.message||'Failed to save profile. Please try again.';
        toast.error(msg);
      },
    }
  );

  const onAvatarUpload=url=>{updateUser({profilePicture:url});qc.invalidateQueries('user-profile');};

  return(
    <motion.div variants={tab} initial="initial" animate="animate" exit="exit">
      {/* identity card */}
      <div className="bg-white rounded-2xl p-6 mb-5 border border-blue-100 flex flex-col sm:flex-row items-center sm:items-start gap-6"
        style={{boxShadow:'0 4px 20px rgba(21,101,192,0.08)'}}>
        <AvatarUploader profile={profile} onUpload={onAvatarUpload}/>
        <div className="text-center sm:text-left">
          <h2 className="text-xl font-extrabold text-blue-900">{profile?.name}</h2>
          <p className="mt-1 inline-flex rounded-lg bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-700">
            ID: {profile?.userCode || 'Generating'}
          </p>
          <div className="flex items-center gap-1.5 mt-1 justify-center sm:justify-start">
            <FiMail className="w-3.5 h-3.5 text-gray-400"/>
            <span className="text-sm text-gray-500">{profile?.email}</span>
          </div>
          <div className="flex items-center gap-2 mt-2 justify-center sm:justify-start">
            {profile?.emailVerified
              ?<span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-green-50 text-green-700 border border-green-200">
                <MdVerified className="w-3.5 h-3.5"/>Email Verified
               </span>
              :<span className="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-semibold bg-yellow-50 text-yellow-700 border border-yellow-200">
                <FiAlertTriangle className="w-3.5 h-3.5"/>Unverified
               </span>
            }
            <span className="badge badge-blue text-[10px]">USER</span>
          </div>
        </div>
      </div>

      <form onSubmit={handleSubmit(d=>mutation.mutate(d))}>
        <div className="bg-white rounded-2xl p-6 border border-blue-100 space-y-5"
          style={{boxShadow:'0 4px 20px rgba(21,101,192,0.08)'}}>
          <h3 className="text-base font-bold text-blue-900 flex items-center gap-2">
            <FiEdit3 className="w-4 h-4 text-blue-500"/>Personal Information
          </h3>

          <Field label="Full Name" icon={<FiUser/>} error={errors.name?.message}>
            <input {...register('name')} placeholder="John Doe" className="input-field"/>
          </Field>
          <Field label="Phone Number" icon={<FiPhone/>} error={errors.phone?.message}>
            <input {...register('phone')} placeholder="+91 9876543210" className="input-field"/>
          </Field>
          <Field label="Address" icon={<FiMapPin/>} error={errors.address?.message}>
            <input {...register('address')} placeholder="Your address" className="input-field"/>
          </Field>
          <Field label="Organization" icon={<FiUser/>} error={errors.organizationName?.message}>
            <input {...register('organizationName')} placeholder="College / company" className="input-field"/>
          </Field>
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="City" error={errors.city?.message}>
              <input {...register('city')} placeholder="City" className="input-field"/>
            </Field>
            <Field label="State" error={errors.state?.message}>
              <input {...register('state')} placeholder="State" className="input-field"/>
            </Field>
            <Field label="Country" error={errors.country?.message}>
              <input {...register('country')} placeholder="Country" className="input-field"/>
            </Field>
            <Field label="PIN Code" error={errors.pinCode?.message}>
              <input {...register('pinCode')} placeholder="PIN code" className="input-field"/>
            </Field>
          </div>
          <Field label="Gender" error={errors.gender?.message}>
            <select {...register('gender')} className="input-field">
              <option value="">Select gender…</option>
              <option value="Male">Male</option>
              <option value="Female">Female</option>
              <option value="Other">Other</option>
              <option value="Prefer not to say">Prefer not to say</option>
            </select>
          </Field>

          <motion.button type="submit"
            disabled={mutation.isLoading||!isDirty}
            whileHover={mutation.isLoading||!isDirty?{}:{scale:1.02}}
            whileTap={mutation.isLoading||!isDirty?{}:{scale:0.97}}
            className="btn-primary flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
            {mutation.isLoading
              ?<><motion.div animate={{rotate:360}} transition={{repeat:Infinity,duration:0.75,ease:'linear'}}
                  className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full"/>Saving…</>
              :<><FiSave className="w-4 h-4"/>Save Changes</>}
          </motion.button>
          {!isDirty&&<p className="text-xs text-gray-400">No unsaved changes.</p>}
        </div>
      </form>
    </motion.div>
  );
}

/* ── Security tab ─────────────────────────────────────────── */
function SecurityTab(){
  const [show,setShow]=useState({cur:false,nw:false,cn:false});
  const {register,handleSubmit,formState:{errors},reset,watch}=useForm({resolver:yupResolver(pwSchema)});
  const watchedPw=watch('newPassword','');

  const mutation=useMutation(
    data=>userAPI.changePassword({currentPassword:data.currentPassword,newPassword:data.newPassword}),
    {
      onSuccess:()=>{toast.success('Password changed!');reset();},
      onError:err=>toast.error(err?.response?.data?.message||'Failed to change password'),
    }
  );
  const toggle=k=>setShow(s=>({...s,[k]:!s[k]}));

  const PwField=({label,regKey,showKey,error})=>(
    <Field label={label} icon={<FiLock/>} error={error}>
      <div className="relative">
        <FiLock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-blue-300 pointer-events-none"/>
        <input {...register(regKey)} type={show[showKey]?'text':'password'} placeholder="••••••••"
          className={`input-field pl-10 pr-10 ${error?'border-red-300':''}`}/>
        <button type="button" onClick={()=>toggle(showKey)}
          className="absolute right-3.5 top-1/2 -translate-y-1/2 text-blue-300 hover:text-blue-600 transition-colors">
          {show[showKey]?<FiEyeOff className="w-4 h-4"/>:<FiEye className="w-4 h-4"/>}
        </button>
      </div>
    </Field>
  );

  return(
    <motion.div variants={tab} initial="initial" animate="animate" exit="exit" className="space-y-5">
      <div className="bg-white rounded-2xl p-5 border border-blue-100 flex items-start gap-4"
        style={{boxShadow:'0 4px 20px rgba(21,101,192,0.08)'}}>
        <div className="w-11 h-11 rounded-xl flex items-center justify-center flex-shrink-0"
          style={{background:'linear-gradient(135deg,#1565C0,#1976D2)'}}>
          <FiShield className="text-white w-5 h-5"/>
        </div>
        <div>
          <h3 className="font-bold text-blue-900 text-sm">Account Security</h3>
          <p className="text-xs text-gray-500 mt-0.5">Passwords are BCrypt-hashed. Tokens expire in 15 min.</p>
        </div>
      </div>

      <div className="bg-white rounded-2xl p-6 border border-blue-100 space-y-5"
        style={{boxShadow:'0 4px 20px rgba(21,101,192,0.08)'}}>
        <h3 className="text-base font-bold text-blue-900 flex items-center gap-2">
          <FiLock className="w-4 h-4 text-blue-500"/>Change Password
        </h3>
        <form onSubmit={handleSubmit(d=>mutation.mutate(d))} className="space-y-4">
          <PwField label="Current Password" regKey="currentPassword" showKey="cur" error={errors.currentPassword?.message}/>
          <PwField label="New Password"     regKey="newPassword"     showKey="nw"  error={errors.newPassword?.message}/>
          {watchedPw.length>0&&(
            <motion.div initial={{opacity:0}} animate={{opacity:1}} className="space-y-1.5">
              <div className="flex gap-1">
                {[1,2,3,4,5].map(i=>(
                  <motion.div key={i} className="h-1.5 flex-1 rounded-full"
                    animate={{background:i<=pwStrength(watchedPw)?SC[pwStrength(watchedPw)]:'#E3F2FD'}}
                    transition={{duration:0.3}}/>
                ))}
              </div>
              <p className="text-xs font-semibold" style={{color:SC[pwStrength(watchedPw)]}}>
                {SL[pwStrength(watchedPw)]}
              </p>
            </motion.div>
          )}
          <PwField label="Confirm New Password" regKey="confirmPassword" showKey="cn" error={errors.confirmPassword?.message}/>
          <motion.button type="submit" disabled={mutation.isLoading}
            whileHover={mutation.isLoading?{}:{scale:1.02}} whileTap={mutation.isLoading?{}:{scale:0.97}}
            className="btn-primary flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
            {mutation.isLoading
              ?<><motion.div animate={{rotate:360}} transition={{repeat:Infinity,duration:0.75,ease:'linear'}}
                  className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full"/>Changing…</>
              :<><FiLock className="w-4 h-4"/>Update Password</>}
          </motion.button>
        </form>
      </div>

      <div className="bg-white rounded-2xl p-5 border border-green-100" style={{boxShadow:'0 4px 16px rgba(46,125,50,0.07)'}}>
        <div className="flex items-center gap-3">
          <FiCheckCircle className="w-5 h-5 text-green-600 flex-shrink-0"/>
          <div>
            <p className="text-sm font-semibold text-green-800">Active Session</p>
            <p className="text-xs text-green-600">JWT Bearer token active. Auto-refresh enabled.</p>
          </div>
        </div>
      </div>
    </motion.div>
  );
}

function LocationTab(){
  const qc = useQueryClient();
  const {data:location,isLoading}=useQuery(
    'user-location',
    ()=>userAPI.getLocation().then(r=>r.data?.data),
    {retry:1,staleTime:30_000}
  );
  const {register,handleSubmit,formState:{errors,isDirty},reset,setValue,watch}=useForm({
    resolver:yupResolver(locationSchema),
    defaultValues:{address:'',street:'',area:'',city:'',district:'',state:'',country:'',pinCode:'',latitude:'',longitude:''},
  });

  useEffect(()=>{
    if(location) reset({
      address:location.address||'', street:location.street||'', area:location.area||'',
      city:location.city||'', district:location.district||'', state:location.state||'',
      country:location.country||'', pinCode:location.pinCode||'',
      latitude:location.latitude ?? '', longitude:location.longitude ?? ''
    });
  },[location,reset]);

  const mutation=useMutation(data=>userAPI.updateLocation(data), {
    onSuccess:()=>{toast.success('Location saved!'); qc.invalidateQueries('user-location');},
    onError:err=>toast.error(err?.response?.data?.message||'Failed to save location'),
  });

  const detect=()=>{
    if(!navigator.geolocation){toast.error('Location is not supported in this browser');return;}
    navigator.geolocation.getCurrentPosition(
      pos=>{
        setValue('latitude', Number(pos.coords.latitude.toFixed(7)), {shouldDirty:true});
        setValue('longitude', Number(pos.coords.longitude.toFixed(7)), {shouldDirty:true});
        toast.success('Coordinates detected. You can edit address details manually.');
      },
      ()=>toast.error('Location permission was denied or unavailable'),
      {enableHighAccuracy:true, timeout:10000}
    );
  };

  const lat=watch('latitude');
  const lng=watch('longitude');
  const mapsUrl = lat && lng ? `https://www.google.com/maps?q=${lat},${lng}` : null;

  if(isLoading) return <div className="skeleton h-72 rounded-2xl"/>;

  return(
    <motion.div variants={tab} initial="initial" animate="animate" exit="exit">
      <form onSubmit={handleSubmit(d=>mutation.mutate(d))} className="bg-white rounded-2xl p-6 border border-blue-100 space-y-5"
        style={{boxShadow:'0 4px 20px rgba(21,101,192,0.08)'}}>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h3 className="text-base font-bold text-blue-900 flex items-center gap-2">
            <FiMapPin className="w-4 h-4 text-blue-500"/>Location
          </h3>
          <button type="button" onClick={detect} className="btn-outline px-4 py-2 text-sm">Detect Location</button>
        </div>
        <Field label="Address" icon={<FiMapPin/>} error={errors.address?.message}>
          <input {...register('address')} placeholder="Full address" className="input-field"/>
        </Field>
        <div className="grid gap-4 sm:grid-cols-2">
          {[
            ['street','Street'], ['area','Area'], ['city','City'], ['district','District'],
            ['state','State'], ['country','Country'], ['pinCode','PIN Code'],
          ].map(([name,label])=>(
            <Field key={name} label={label} error={errors[name]?.message}>
              <input {...register(name)} placeholder={label} className="input-field"/>
            </Field>
          ))}
          <Field label="Latitude" error={errors.latitude?.message}>
            <input {...register('latitude')} placeholder="Latitude" className="input-field"/>
          </Field>
          <Field label="Longitude" error={errors.longitude?.message}>
            <input {...register('longitude')} placeholder="Longitude" className="input-field"/>
          </Field>
        </div>
        {mapsUrl&&(
          <a href={mapsUrl} target="_blank" rel="noreferrer" className="inline-flex text-sm font-bold text-blue-700 hover:underline">
            Open in Google Maps
          </a>
        )}
        <button type="submit" disabled={mutation.isLoading||!isDirty}
          className="btn-primary flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed">
          <FiSave className="w-4 h-4"/>{mutation.isLoading?'Saving...':'Save Location'}
        </button>
      </form>
    </motion.div>
  );
}

/* ── Main ─────────────────────────────────────────────────── */
const TABS=[{id:'profile',label:'Profile'},{id:'location',label:'Location'},{id:'security',label:'Security'}];

export default function UserProfile(){
  const [activeTab,setActiveTab]=useState('profile');
  const {data:profile,isLoading,refetch}=useQuery(
    'user-profile',
    ()=>userAPI.getProfile().then(r=>r.data?.data),
    {retry:1,staleTime:30_000}
  );

  if(isLoading) return(
    <div style={{background:'#F0F4FF',minHeight:'100vh'}} className="px-4 py-10">
      <div className="max-w-2xl mx-auto space-y-5">
        <div className="skeleton h-8 w-40 rounded-xl"/>
        <div className="skeleton h-36 rounded-2xl"/>
        <div className="skeleton h-12 rounded-xl"/>
        <div className="skeleton h-64 rounded-2xl"/>
      </div>
    </div>
  );

  return(
    <div style={{background:'#F0F4FF',minHeight:'100vh'}} className="px-4 sm:px-6 py-10">
      <div className="max-w-2xl mx-auto">
        <motion.div initial={{opacity:0,y:-16}} animate={{opacity:1,y:0}} transition={{duration:0.4}} className="mb-7">
          <h1 className="text-3xl font-extrabold text-blue-900" style={{fontFamily:'Space Grotesk,sans-serif'}}>
            My Profile
          </h1>
          <p className="text-sm text-gray-500 mt-1">Manage your account and security settings.</p>
        </motion.div>

        {/* tab strip */}
        <motion.div initial={{opacity:0,y:10}} animate={{opacity:1,y:0}} transition={{delay:0.1}}
          className="flex p-1 rounded-2xl bg-white border border-blue-100 mb-6"
          style={{boxShadow:'0 2px 12px rgba(21,101,192,0.07)'}}>
          {TABS.map(t=>(
            <button key={t.id} type="button" onClick={()=>setActiveTab(t.id)}
              className="flex-1 py-2.5 rounded-xl text-sm font-semibold relative z-10 transition-all duration-200">
              <AnimatePresence>
                {activeTab===t.id&&(
                  <motion.div layoutId="user-tab-pill" className="absolute inset-0 rounded-xl"
                    style={{background:'linear-gradient(135deg,#1565C0,#1976D2)'}}
                    initial={{opacity:0}} animate={{opacity:1}} exit={{opacity:0}} transition={{duration:0.2}}/>
                )}
              </AnimatePresence>
              <span className="relative z-10" style={{color:activeTab===t.id?'#fff':'#546E7A'}}>{t.label}</span>
            </button>
          ))}
        </motion.div>

        <ErrorBoundary onRetry={refetch}>
          <AnimatePresence mode="wait">
            {activeTab==='profile'
              ?<ProfileTab key="profile" profile={profile} refetch={refetch}/>
              :activeTab==='location'
                ?<LocationTab key="location"/>
                :<SecurityTab key="security"/>
            }
          </AnimatePresence>
        </ErrorBoundary>
      </div>
    </div>
  );
}
