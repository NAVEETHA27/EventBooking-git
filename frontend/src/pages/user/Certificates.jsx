import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useQuery } from 'react-query';
import { certificatesAPI } from '../../services/api';
import Spinner from '../../components/common/Spinner';
import { FiAward, FiCheckCircle, FiDownload, FiSearch } from 'react-icons/fi';

export default function Certificates() {
  const [page, setPage] = useState(0);
  const [q, setQ] = useState('');
  const { data, isLoading } = useQuery(
    ['my-certificates', page, q],
    () => certificatesAPI.my({ page, size: 10, q }).then(r => r.data?.data),
    { keepPreviousData: true }
  );
  const certs = data?.content || [];

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <div className="mb-6 flex items-center gap-3">
        <FiAward className="h-7 w-7 text-amber-500" />
        <h1 className="text-2xl font-bold text-gray-900">My Certificates</h1>
        <span className="ml-auto rounded-full bg-blue-100 px-3 py-1 text-sm font-semibold text-blue-700">{data?.totalElements || 0} total</span>
      </div>
      <div className="relative mb-5">
        <FiSearch className="absolute left-3 top-3 text-gray-400" />
        <input value={q} onChange={e => { setQ(e.target.value); setPage(0); }} className="input-field pl-9" placeholder="Search certificates" />
      </div>
      {isLoading ? (
        <div className="flex justify-center py-16"><Spinner /></div>
      ) : certs.length === 0 ? (
        <div className="rounded-xl border border-dashed border-gray-300 bg-white py-20 text-center">
          <FiAward className="mx-auto mb-4 h-12 w-12 text-gray-300" />
          <p className="font-medium text-gray-500">No certificates yet</p>
          <p className="mt-1 text-sm text-gray-400">Attend events to earn certificates.</p>
        </div>
      ) : (
        <div className="space-y-3">{certs.map(cert => <CertCard key={cert.id} cert={cert} />)}</div>
      )}
      {data?.totalPages > 1 && (
        <div className="mt-6 flex justify-center gap-2">
          <button disabled={page === 0} onClick={() => setPage(p => p - 1)} className="rounded border px-4 py-2 text-sm hover:bg-gray-50 disabled:opacity-40">Prev</button>
          <span className="px-4 py-2 text-sm text-gray-500">Page {page + 1} / {data.totalPages}</span>
          <button disabled={page >= data.totalPages - 1} onClick={() => setPage(p => p + 1)} className="rounded border px-4 py-2 text-sm hover:bg-gray-50 disabled:opacity-40">Next</button>
        </div>
      )}
    </div>
  );
}

function CertCard({ cert }) {
  const statusColor = ['GENERATED', 'RELEASED', 'EMAILED'].includes(cert.status) ? 'bg-green-100 text-green-700' : 'bg-yellow-100 text-yellow-700';
  const download = async () => {
    const res = await certificatesAPI.download(cert.certificateId);
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${cert.certificateId}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
  };
  return (
    <div className="flex items-center gap-4 rounded-xl border border-gray-200 bg-white p-5 shadow-sm">
      <div className="flex h-14 w-14 shrink-0 items-center justify-center rounded-xl bg-amber-100">
        <FiAward className="h-7 w-7 text-amber-500" />
      </div>
      <div className="min-w-0 flex-1">
        <h3 className="truncate font-semibold text-gray-900">{cert.eventName}</h3>
        <p className="text-sm text-gray-500">{cert.recipientName} - {cert.collegeName}</p>
        <p className="mt-1 font-mono text-xs text-gray-400">{cert.certificateId}</p>
      </div>
      <div className="flex shrink-0 flex-col items-end gap-2">
        <span className={`rounded-full px-2.5 py-0.5 text-xs font-semibold ${statusColor}`}>{cert.status}</span>
        <span className="text-xs text-gray-400">{cert.issuedAt ? new Date(cert.issuedAt).toLocaleDateString() : '-'}</span>
        <div className="flex gap-2">
          <button onClick={download} className="rounded-lg border px-2 py-1 text-xs text-blue-600 hover:bg-blue-50" title="Download PDF"><FiDownload /></button>
          <Link to={`/verify/certificate/${cert.certificateId}`} className="rounded-lg border px-2 py-1 text-xs text-green-600 hover:bg-green-50" title="Verify"><FiCheckCircle /></Link>
        </div>
      </div>
    </div>
  );
}
