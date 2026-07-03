export default function QueueStatus() {
  return (
    <div className="mx-auto max-w-3xl px-4 py-10">
      <h1 className="text-3xl font-extrabold text-slate-950">Booking Status</h1>
      <div className="mt-6 rounded-lg border border-slate-200 bg-white p-6">
        <p className="text-slate-600">Booking requests are processed securely on the server in chronological order.</p>
        <div className="mt-4 grid gap-3 sm:grid-cols-2">
          {['Payment Pending', 'Booking Successful', 'Payment Failed', 'Tickets Sold Out'].map((status) => (
            <div key={status} className="rounded-lg bg-slate-50 p-4 text-sm font-bold text-slate-700">{status}</div>
          ))}
        </div>
      </div>
    </div>
  );
}
