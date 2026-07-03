import { Link } from 'react-router-dom';

export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center px-4" style={{ background:'#F0F4FF' }}>
      <div className="text-center">
        <div className="text-8xl mb-5">🎓</div>
        <h1 className="text-7xl font-extrabold text-blue-700 mb-3" style={{ fontFamily:'Space Grotesk,sans-serif' }}>404</h1>
        <h2 className="text-2xl font-bold text-blue-900 mb-3">Page Not Found</h2>
        <p className="text-gray-500 mb-8 max-w-sm mx-auto">This page seems to have skipped class. Let's get you back on track.</p>
        <Link to="/" className="btn-primary text-base px-10 py-3.5">Go Home</Link>
      </div>
    </div>
  );
}
