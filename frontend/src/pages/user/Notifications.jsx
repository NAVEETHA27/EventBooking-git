import { useEffect } from 'react';
import { useMutation, useQuery, useQueryClient } from 'react-query';
import { motion } from 'framer-motion';
import { formatDistanceToNow } from 'date-fns';
import { notificationsAPI } from '../../services/api';
import Spinner from '../../components/common/Spinner';
import { FiAlertTriangle, FiBell, FiCheck, FiCreditCard, FiInfo, FiRefreshCcw, FiShield, FiFileText } from 'react-icons/fi';

const TYPE_ICON = {
  BOOKING_CONFIRMED: FiFileText,
  TICKET_CANCELLED: FiAlertTriangle,
  PAYMENT_SUCCESS: FiCreditCard,
  PAYMENT_FAILED: FiAlertTriangle,
  REFUND_INITIATED: FiRefreshCcw,
  REFUND_COMPLETED: FiCheck,
  EVENT_CANCELLED: FiAlertTriangle,
  EVENT_UPDATED: FiInfo,
  LOGIN_ALERT: FiShield,
  REGISTRATION_SUCCESS: FiCheck,
};

export default function Notifications() {
  const qc = useQueryClient();
  const { data, isLoading } = useQuery('notifs', () => notificationsAPI.getAll({ page: 0, size: 50 }).then((r) => r.data?.data));
  const markAll = useMutation(() => notificationsAPI.markAllRead(), { onSuccess: () => qc.invalidateQueries('notifs') });

  useEffect(() => {
    const source = new EventSource(notificationsAPI.streamUrl());
    source.addEventListener('notification', () => {
      qc.invalidateQueries('notifs');
    });
    source.onerror = () => source.close();
    return () => source.close();
  }, [qc]);

  if (isLoading) return <Spinner />;
  const items = data?.content ?? [];

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-10 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-3xl">
        <div className="mb-8 flex flex-col gap-4 rounded-2xl bg-white p-6 shadow-card sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.2em] text-teal-700">Inbox</p>
            <h1 className="mt-1 flex items-center gap-2 text-3xl font-extrabold text-slate-950" style={{ fontFamily: 'Space Grotesk,sans-serif' }}>
              <FiBell /> Notifications
            </h1>
          </div>
          {items.some((item) => !item.read) && (
            <button onClick={() => markAll.mutate()} className="inline-flex items-center gap-2 rounded-xl bg-teal-600 px-4 py-2 text-sm font-bold text-white transition hover:bg-teal-700">
              <FiCheck /> Mark all read
            </button>
          )}
        </div>

        {!items.length ? (
          <div className="rounded-2xl border border-slate-200 bg-white p-16 text-center shadow-card">
            <FiBell className="mx-auto mb-4 h-10 w-10 text-slate-300" />
            <p className="text-slate-500">No notifications yet.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {items.map((item, index) => {
              const Icon = TYPE_ICON[item.notificationType] || FiBell;
              return (
                <motion.div
                  key={item.id}
                  initial={{ opacity: 0, y: 8 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{ delay: index * 0.03 }}
                  className={`flex gap-4 rounded-xl border bg-white p-4 shadow-card transition-all ${!item.read ? 'border-teal-200 ring-2 ring-teal-50' : 'border-slate-200'}`}
                >
                  <div className={`mt-0.5 grid h-10 w-10 flex-shrink-0 place-items-center rounded-xl ${!item.read ? 'bg-teal-50 text-teal-700' : 'bg-slate-100 text-slate-500'}`}>
                    <Icon />
                  </div>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-2">
                      <p className={`text-sm font-bold ${!item.read ? 'text-slate-950' : 'text-slate-700'}`}>{item.title}</p>
                      {!item.read && <span className="mt-1.5 h-2 w-2 flex-shrink-0 rounded-full bg-teal-500" />}
                    </div>
                    <p className="mt-1 text-sm leading-relaxed text-slate-500">{item.message}</p>
                    <p className="mt-2 text-xs text-slate-400">
                      {item.createdAt ? formatDistanceToNow(new Date(item.createdAt), { addSuffix: true }) : '-'}
                    </p>
                  </div>
                </motion.div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
