import { useRef, useState } from 'react';
import { useMutation, useQuery, useQueryClient } from 'react-query';
import { certificatesAPI, eventsAPI } from '../../services/api';
import Spinner from '../../components/common/Spinner';
import { toast } from 'react-toastify';
import { FiAward, FiDownload, FiMail, FiRefreshCw, FiSearch, FiSend, FiUploadCloud, FiXCircle } from 'react-icons/fi';

export default function OrganizerCertificates() {
  const qc = useQueryClient();
  const [eventId, setEventId] = useState('');
  const [q, setQ] = useState('');
  const [selected, setSelected] = useState([]);
  const [templateFile, setTemplateFile] = useState(null);
  const [signatureFile, setSignatureFile] = useState(null);
  const templateRef = useRef(null);
  const signatureRef = useRef(null);

  const { data: eventsData, isLoading: eventsLoading } = useQuery('org-events-certs', () => eventsAPI.myEvents({ page: 0, size: 100 }).then(r => r.data?.data));
  const { data: participants, isLoading } = useQuery(
    ['cert-participants', eventId, q],
    () => certificatesAPI.participants(eventId, { q }).then(r => r.data?.data),
    { enabled: !!eventId }
  );
  const { data: certs } = useQuery(['event-certs', eventId], () => certificatesAPI.forEvent(eventId).then(r => r.data?.data), { enabled: !!eventId });

  const generateAll = useMutation(() => certificatesAPI.generate(eventId), { onSuccess: () => done('Certificates generated') });
  const generateSelected = useMutation(() => certificatesAPI.generateSelected(eventId, selected), { onSuccess: () => done('Selected certificates generated') });
  const release = useMutation(() => certificatesAPI.release(eventId), { onSuccess: () => done('Certificates released') });
  const email = useMutation(id => certificatesAPI.resendEmail(id), { onSuccess: () => done('Email queued') });
  const revoke = useMutation(id => certificatesAPI.revoke(id, 'Revoked by organizer'), { onSuccess: () => done('Certificate revoked') });

  const generateMissing = useMutation(() => certificatesAPI.generateMissing(eventId), {
    onSuccess: (res) => {
      const n = res.data?.data?.generated ?? 0;
      toast.success(n > 0 ? `Generated ${n} missing certificate(s)` : 'No missing certificates found');
      qc.invalidateQueries(['cert-participants', eventId, q]);
      qc.invalidateQueries(['event-certs', eventId]);
    },
    onError: (e) => toast.error(e?.response?.data?.message || 'Failed'),
  });

  const uploadTemplate = useMutation(async () => {
    if (!templateFile) return toast.error('Choose a template file first');
    const form = new FormData();
    form.append('file', templateFile);
    await certificatesAPI.uploadTemplate(eventId, form, '');
    setTemplateFile(null);
    if (templateRef.current) templateRef.current.value = '';
    toast.success('Certificate template uploaded. Certificates generated from now will use this design.');
    qc.invalidateQueries(['event-certs', eventId]);
  }, { onError: (e) => toast.error(e?.response?.data?.message || 'Upload failed') });

  const uploadSignature = useMutation(async () => {
    if (!signatureFile) return toast.error('Choose a signature file first');
    const form = new FormData();
    form.append('file', signatureFile);
    await certificatesAPI.uploadSignature(eventId, form);
    setSignatureFile(null);
    if (signatureRef.current) signatureRef.current.value = '';
    toast.success('Organizer signature uploaded.');
    qc.invalidateQueries(['event-certs', eventId]);
  }, { onError: (e) => toast.error(e?.response?.data?.message || 'Upload failed') });

  const done = (message) => {
    toast.success(message);
    qc.invalidateQueries(['cert-participants', eventId, q]);
    qc.invalidateQueries(['event-certs', eventId]);
    setSelected([]);
  };

  const download = async (id) => {
    const res = await certificatesAPI.download(id);
    const url = URL.createObjectURL(res.data);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${id}.pdf`;
    a.click();
    URL.revokeObjectURL(url);
  };

  if (eventsLoading) return <Spinner full />;
  const events = eventsData?.content || [];
  const rows = participants || [];

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-10">
      <div className="mx-auto max-w-7xl space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <p className="text-xs font-black uppercase tracking-[0.2em] text-teal-700">Organizer Workspace</p>
            <h1 className="section-title">Certificate Management</h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <button disabled={!eventId || generateAll.isLoading} onClick={() => generateAll.mutate()} className="btn-primary flex items-center gap-2"><FiRefreshCw /> Generate All</button>
            <button disabled={!eventId || generateMissing.isLoading} onClick={() => generateMissing.mutate()} className="btn-outline flex items-center gap-2 text-teal-700 border-teal-300"><FiAward /> Generate Missing</button>
            <button disabled={!eventId || !selected.length || generateSelected.isLoading} onClick={() => generateSelected.mutate()} className="btn-outline flex items-center gap-2"><FiAward /> Generate Selected</button>
            <button disabled={!eventId || release.isLoading} onClick={() => release.mutate()} className="btn-outline flex items-center gap-2"><FiSend /> Release</button>
          </div>
        </div>

        <div className="grid gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-card md:grid-cols-[1fr_280px]">
          <select value={eventId} onChange={e => setEventId(e.target.value)} className="input-field">
            <option value="">Select event</option>
            {events.map(ev => <option key={ev.id} value={ev.id}>{ev.eventName}</option>)}
          </select>
          <div className="relative">
            <FiSearch className="absolute left-3 top-3 text-slate-400" />
            <input value={q} onChange={e => setQ(e.target.value)} className="input-field pl-9" placeholder="Search participants" />
          </div>
        </div>

        {!eventId ? (
          <Empty text="Select an event to manage certificates." />
        ) : isLoading ? <Spinner /> : (
          <>
            {/* Certificate Template Upload */}
            <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-card">
              <h2 className="text-sm font-bold text-slate-800 mb-1">Certificate Template</h2>
              <p className="text-xs text-slate-500 mb-4">
                Upload a custom design (PNG/PDF). Participant name, college, department, event name, certificate ID, and dates are auto-filled when generating.
              </p>
              <div className="grid gap-4 sm:grid-cols-2">
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1.5">Custom Template (PNG / PDF)</label>
                  <div className="flex items-center gap-2">
                    <input
                      ref={templateRef}
                      type="file"
                      accept=".pdf,.png,.jpg,.jpeg"
                      className="input-field text-xs flex-1"
                      onChange={e => setTemplateFile(e.target.files?.[0] || null)}
                    />
                    <button
                      disabled={!templateFile || uploadTemplate.isLoading}
                      onClick={() => uploadTemplate.mutate()}
                      className="btn-primary flex items-center gap-1.5 whitespace-nowrap text-xs px-3 py-2 disabled:opacity-50"
                    >
                      <FiUploadCloud className="w-3.5 h-3.5" />
                      {uploadTemplate.isLoading ? 'Uploading…' : 'Upload'}
                    </button>
                  </div>
                  {templateFile && <p className="mt-1 text-xs text-teal-600 truncate">{templateFile.name}</p>}
                </div>
                <div>
                  <label className="block text-xs font-semibold text-slate-600 mb-1.5">Organizer Signature (PNG / JPG)</label>
                  <div className="flex items-center gap-2">
                    <input
                      ref={signatureRef}
                      type="file"
                      accept=".png,.jpg,.jpeg"
                      className="input-field text-xs flex-1"
                      onChange={e => setSignatureFile(e.target.files?.[0] || null)}
                    />
                    <button
                      disabled={!signatureFile || uploadSignature.isLoading}
                      onClick={() => uploadSignature.mutate()}
                      className="btn-primary flex items-center gap-1.5 whitespace-nowrap text-xs px-3 py-2 disabled:opacity-50"
                    >
                      <FiUploadCloud className="w-3.5 h-3.5" />
                      {uploadSignature.isLoading ? 'Uploading…' : 'Upload'}
                    </button>
                  </div>
                  {signatureFile && <p className="mt-1 text-xs text-teal-600 truncate">{signatureFile.name}</p>}
                </div>
              </div>
              <p className="mt-3 text-[11px] text-slate-400">
                Participant details (name, college, department) are pulled automatically from registration data — no manual entry needed.
              </p>
            </div>

            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-card">
              <table className="data-table">
                <thead><tr>{['', 'Participant', 'College', 'Eligibility', 'Certificate', 'Actions'].map(h => <th key={h}>{h}</th>)}</tr></thead>
                <tbody>
                  {rows.map(row => (
                    <tr key={row.userId}>
                      <td><input type="checkbox" checked={selected.includes(row.userId)} onChange={e => setSelected(s => e.target.checked ? [...s, row.userId] : s.filter(id => id !== row.userId))} /></td>
                      <td><b>{row.participantName}</b><p className="text-xs text-slate-400">{row.email}</p></td>
                      <td>{row.college}<p className="text-xs text-slate-400">{row.department}</p></td>
                      <td><span className={`badge ${row.eligible ? 'badge-green' : 'badge-red'}`}>{row.eligible ? 'Eligible' : row.reason}</span></td>
                      <td className="font-mono text-xs">{row.certificateId || '-'}</td>
                      <td>
                        <div className="flex gap-2">
                          {row.certificateId && <button title="Download" onClick={() => download(row.certificateId)} className="p-2 text-blue-600"><FiDownload /></button>}
                          {row.certificateId && <button title="Send email" onClick={() => email.mutate(row.certificateId)} className="p-2 text-green-600"><FiMail /></button>}
                          {row.certificateId && <button title="Revoke" onClick={() => revoke.mutate(row.certificateId)} className="p-2 text-red-600"><FiXCircle /></button>}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}

        {certs?.length > 0 && <p className="text-sm text-slate-500">{certs.length} certificates generated for this event.</p>}
      </div>
    </div>
  );
}

function Empty({ text }) {
  return <div className="rounded-xl border border-dashed border-slate-300 bg-white py-16 text-center text-slate-500">{text}</div>;
}
