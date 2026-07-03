import { motion } from 'framer-motion';

export default function Spinner({ full, label = 'Loading…' }) {
  const rings = (
    <div className="relative w-11 h-11">
      <div className="absolute inset-0 rounded-full animate-spin"
           style={{ border:'3px solid #BBDEFB', borderTopColor:'#1565C0' }} />
      <div className="absolute inset-2 rounded-full animate-spin"
           style={{ border:'2px solid #FFCDD2', borderTopColor:'#D32F2F', animationDirection:'reverse', animationDuration:'0.55s' }} />
    </div>
  );

  if (full) return (
    <motion.div initial={{ opacity:0 }} animate={{ opacity:1 }} exit={{ opacity:0 }}
      className="fixed inset-0 flex items-center justify-center z-50 bg-white/80 backdrop-blur-sm">
      <div className="flex flex-col items-center gap-4">
        {rings}
        <p className="text-sm font-medium text-blue-600 animate-pulse">{label}</p>
      </div>
    </motion.div>
  );

  return <div className="flex items-center justify-center py-16">{rings}</div>;
}
