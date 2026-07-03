import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { authAPI } from '../../services/api';

export default function VerifyEmail() {
  const [params]  = useSearchParams();
  const [status, setStatus] = useState('loading');

  useEffect(() => {
    const token = params.get('token');
    const role  = params.get('role') || 'USER';
    if (!token) { setStatus('error'); return; }
    authAPI.verifyEmail(token, role).then(() => setStatus('success')).catch(() => setStatus('error'));
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ background:'#F0F4FF' }}>
      <div className="bg-white rounded-3xl shadow-blue p-12 text-center max-w-sm border border-blue-100">
        {status === 'loading' && (<><div className="text-5xl mb-4 animate-spin">⏳</div><h2 className="text-xl font-bold text-blue-900">Verifying…</h2></>)}
        {status === 'success' && (<>
          <div className="text-5xl mb-4">✅</div>
          <h2 className="text-2xl font-bold text-blue-900 mb-2">Email Verified!</h2>
          <p className="text-gray-500 mb-6">Your account is now active.</p>
          <Link to="/login" className="btn-primary">Go to Sign In</Link>
        </>)}
        {status === 'error' && (<>
          <div className="text-5xl mb-4">❌</div>
          <h2 className="text-2xl font-bold text-blue-900 mb-2">Verification Failed</h2>
          <p className="text-gray-500 mb-6">The link may be invalid or expired.</p>
          <Link to="/login" className="btn-outline">Back to Sign In</Link>
        </>)}
      </div>
    </div>
  );
}
