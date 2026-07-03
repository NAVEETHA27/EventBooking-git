import { Component } from 'react';
import { motion } from 'framer-motion';
import { FiAlertTriangle, FiRefreshCw } from 'react-icons/fi';

/**
 * React error boundary — catches render errors in subtree.
 * Displays a friendly fallback with retry capability.
 * Satisfies Requirement 6.2 (profile page stability).
 */
export default class ErrorBoundary extends Component {
  state = { hasError: false, error: null };

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    // In production replace with a real error reporting service
    console.error('[ErrorBoundary]', error, info.componentStack);
  }

  retry = () => {
    this.setState({ hasError: false, error: null });
    this.props.onRetry?.();
  };

  render() {
    if (!this.state.hasError) return this.props.children;

    return (
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.4 }}
        className="flex flex-col items-center justify-center gap-4 p-10 rounded-2xl text-center"
        style={{ background: '#FFF5F5', border: '1.5px solid #FFCDD2' }}>
        <div className="w-14 h-14 rounded-2xl flex items-center justify-center"
          style={{ background: '#FFEBEE' }}>
          <FiAlertTriangle className="w-7 h-7 text-red-500" />
        </div>
        <div>
          <h3 className="font-bold text-red-800 text-lg mb-1">Something went wrong</h3>
          <p className="text-sm text-red-600 max-w-xs">
            {this.state.error?.message || 'An unexpected error occurred while loading this section.'}
          </p>
        </div>
        <motion.button
          whileHover={{ scale: 1.04 }} whileTap={{ scale: 0.97 }}
          onClick={this.retry}
          className="flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold text-white"
          style={{ background: 'linear-gradient(135deg,#C62828,#D32F2F)' }}>
          <FiRefreshCw className="w-4 h-4" /> Retry
        </motion.button>
      </motion.div>
    );
  }
}
