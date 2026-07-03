import { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { authAPI } from '../../services/api';
import { toast } from 'react-toastify';
import { FiLock, FiEye, FiEyeOff } from 'react-icons/fi';

export default function ResetPassword() {
  const [params]      = useSearchParams();
  const navigate      = useNavigate();
  const [pw, setPw]   = useState('');
  const [otp, setOtp] = useState('');
  const [show, setShow] = useState(false);
  const [loading, setLoading] = useState(false);
  const email = params.get('email') || '';
  const role = params.get('role') || 'USER';
  const useOtp = params.get('otp') === 'true';

  const onSubmit = async e => {
    e.preventDefault(); setLoading(true);
    try {
      if (useOtp) {
        await authAPI.resetPasswordWithOtp({ email, role, otp, newPassword: pw });
      } else {
        await authAPI.resetPassword({ token:params.get('token'), newPassword:pw, role });
      }
      toast.success('Password reset!'); navigate('/login');
    } catch {} finally { setLoading(false); }
  };

  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ background:'#F0F4FF' }}>
      <div className="w-full max-w-md bg-white rounded-3xl shadow-blue overflow-hidden border border-blue-100">
        <div className="h-1.5" style={{ background:'linear-gradient(90deg,#1565C0,#D32F2F)' }} />
        <div className="p-8">
          <div className="text-center mb-7">
            <div className="text-4xl mb-3">🔑</div>
            <h1 className="text-2xl font-extrabold text-blue-900" style={{ fontFamily:'Space Grotesk,sans-serif' }}>Set New Password</h1>
          </div>
          <form onSubmit={onSubmit} className="space-y-5">
            {useOtp && (
              <div>
                <label className="block text-sm font-semibold text-blue-900 mb-1.5">OTP</label>
                <input value={otp} onChange={e => setOtp(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  type="text" inputMode="numeric" required minLength={6} maxLength={6}
                  placeholder="Enter 6-digit OTP" className="input-field" />
              </div>
            )}
            <div>
              <label className="block text-sm font-semibold text-blue-900 mb-1.5">New Password</label>
              <div className="relative">
                <FiLock className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
                <input value={pw} onChange={e => setPw(e.target.value)} type={show ? 'text' : 'password'}
                  required placeholder="8+ chars, uppercase, number, symbol" className="input-field pl-10 pr-10" />
                <button type="button" onClick={() => setShow(!show)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-gray-400 hover:text-blue-600">
                  {show ? <FiEyeOff className="w-4 h-4" /> : <FiEye className="w-4 h-4" />}
                </button>
              </div>
            </div>
            <button type="submit" disabled={loading} className="btn-primary w-full py-3.5 text-sm font-bold disabled:opacity-50">
              {loading ? 'Resetting…' : 'Reset Password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
