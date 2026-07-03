import { motion } from 'framer-motion';
import { FiAlertTriangle, FiRefreshCw } from 'react-icons/fi';

/**
 * Inline error state for React Query failures — shows a retry button.
 */
export default function QueryError({ message, onRetry }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 12 }}
      animate={{ opacity: 1, y: 0 }}
      className="flex flex-col items-center justify-center gap-4 p-10 rounded-2xl text-center bg-white border border-red-100 shadow-card">
      <div className="w-12 h-12 rounded-2xl flex items-center justify-center bg-red-50">
        <FiAlertTriangle className="w-6 h-6 text-red-500" />
      </div>
      <div>
        <h3 className="font-bold text-red-800 mb-1">Could not load data</h3>
        <p className="text-sm text-red-600 max-w-sm">
          {message || 'Something went wrong while fetching. Please try again.'}
        </p>
      </div>
      {onRetry && (
        <motion.button
          whileHover={{ scale: 1.04 }} whileTap={{ scale: 0.97 }}
          onClick={onRetry}
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold text-white"
          style={{ background: 'linear-gradient(135deg,#1565C0,#1976D2)' }}>
          <FiRefreshCw className="w-4 h-4" /> Retry
        </motion.button>
      )}
    </motion.div>
  );
}
