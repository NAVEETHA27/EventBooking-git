import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from 'react-query';
import { certificatesAPI } from '../../services/api';
import Spinner from '../../components/common/Spinner';
import { toast } from 'react-toastify';
import { FiAward, FiDownload, FiSearch, FiTrash2 } from 'react-icons/fi';

export default function AdminCertificates() {
  const qc = useQueryClient();
  const [q, setQ] = useState('');
  const { data: stats } = useQuery('admin-cert-stats', () => certificatesAPI.adminStats().then(r => r.data?.data));
  const { data, isLoading } = useQuery(['admin-certs', q], () => certificatesAPI.admin({ q, page: 0, size: 50 }).then(r => r.data?.data));
  const revoke = useMutation(id => certificatesAPI.adminDelete(id), {
    onSuccess: () => {
      toast.success('Certificate revoked');
      qc.invalidateQueries('admin-cert-stats');
      qc.invalidateQueries(['admin-certs', q]);
    },
  });

  const download = async (id) => {
    const res = await certificatesAPI.download(id);
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${id}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
  };

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-10">
      <div className="mx-auto max-w-7xl space-y-6">
        <div>
          <p className="text-xs font-black uppercase tracking-[0.2em] text-teal-700">Admin Panel</p>
          <h1 className="section-title">Certificates</h1>
        </div>
        <div className="grid gap-4 md:grid-cols-4">
          {[
            ['Generated', stats?.certificatesGenerated],
            ['Downloaded', stats?.certificatesDownloaded],
            ['Verification Requests', stats?.verificationRequests],
            ['Failed Emails', stats?.failedEmails],
          ].map(([label, value]) => (
            <div key={label} className="rounded-xl border border-slate-200 bg-white p-5 shadow-card">
              <p className="text-sm text-slate-500">{label}</p>
              <p className="mt-2 text-2xl font-black text-slate-950">{value ?? 0}</p>
            </div>
          ))}
        </div>
        <div className="relative max-w-md">
          <FiSearch className="absolute left-3 top-3 text-slate-400" />
          <input value={q} onChange={e => setQ(e.target.value)} className="input-field pl-9" placeholder="Search certificates" />
        </div>
        {isLoading ? <Spinner /> : (
          <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-card">
            <table className="data-table">
              <thead><tr>{['Certificate', 'Participant', 'Event', 'Status', 'Counts', 'Actions'].map(h => <th key={h}>{h}</th>)}</tr></thead>
              <tbody>
                {(data?.content || []).map(cert => (
                  <tr key={cert.id}>
                    <td className="font-mono text-xs">{cert.certificateId}</td>
                    <td>{cert.recipientName}<p className="text-xs text-slate-400">{cert.participantEmail}</p></td>
                    <td>{cert.eventName}</td>
                    <td><span className="badge badge-blue">{cert.status}</span></td>
                    <td className="text-xs text-slate-500">D {cert.downloadCount} / V {cert.verificationCount}</td>
                    <td>
                      <button onClick={() => download(cert.certificateId)} className="p-2 text-blue-600"><FiDownload /></button>
                      <button onClick={() => revoke.mutate(cert.certificateId)} className="p-2 text-red-600"><FiTrash2 /></button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
