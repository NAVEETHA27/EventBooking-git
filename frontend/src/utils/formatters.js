/**
 * Shared formatting utilities used across pages and components.
 */

/** Format a date string to "DD MMM YYYY" */
export function formatDate(dateStr) {
  if (!dateStr) return '-';
  return new Date(dateStr).toLocaleDateString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
  });
}

/** Format a time string to "HH:MM AM/PM" */
export function formatTime(timeStr) {
  if (!timeStr) return '-';
  const [h, m] = timeStr.split(':');
  const hour = parseInt(h, 10);
  const ampm = hour >= 12 ? 'PM' : 'AM';
  return `${hour % 12 || 12}:${m} ${ampm}`;
}

/** Format a number as Indian Rupees */
export function formatCurrency(amount) {
  if (amount == null) return '₹0';
  return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(amount);
}

/** Truncate text to a given length */
export function truncate(str, max = 100) {
  if (!str) return '';
  return str.length > max ? str.slice(0, max) + '...' : str;
}

/** Return a readable booking status label */
export function bookingStatusLabel(status) {
  const map = {
    CONFIRMED: 'Confirmed',
    PENDING: 'Pending Payment',
    CANCELLED: 'Cancelled',
    EXPIRED: 'Expired',
  };
  return map[status] || status;
}

/** Return a colour class for a booking/payment status */
export function statusColor(status) {
  const map = {
    CONFIRMED: 'text-green-600',
    SUCCESSFUL: 'text-green-600',
    PENDING: 'text-yellow-600',
    PROCESSING: 'text-blue-600',
    CANCELLED: 'text-red-600',
    FAILED: 'text-red-600',
    EXPIRED: 'text-gray-500',
    REFUNDED: 'text-purple-600',
  };
  return map[status] || 'text-gray-600';
}
