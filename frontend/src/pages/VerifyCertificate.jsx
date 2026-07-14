import { useParams, Link } from 'react-router-dom';
import { useQuery } from 'react-query';
import { certificatesAPI } from '../services/api';
import Spinner from '../components/common/Spinner';
import QueryError from '../components/common/QueryError';
import { FiAward, FiCheckCircle, FiXCircle } from 'react-icons/fi';

export default function VerifyCertificate() {
  const { certificateId } = useParams();
  const { data, isLoading, isError, error, refetch } = useQuery(
    ['verify-certificate', certificateId],
    () => certificatesAPI.verify(certificateId).then(r => r.data?.data),
    { retry: false }
  );

  if (isLoading) return <Spinner full />;

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-12">
      <div className="mx-auto max-w-2xl rounded-2xl border border-slate-200 bg-white p-8 shadow-card">
        {isError ? (
          <div className="text-center">
            <FiXCircle className="mx-auto mb-4 h-12 w-12 text-red-500" />
            <h1 className="text-2xl font-bold text-slate-950">Certificate Not Found</h1>
            <QueryError message={error?.response?.data?.message || 'Certificate could not be verified.'} onRetry={refetch} />
          </div>
        ) : (
          <div>
            <div className="mb-6 flex items-center gap-3">
              <div className="grid h-12 w-12 place-items-center rounded-xl bg-green-50 text-green-700"><FiCheckCircle /></div>
              <div>
                <h1 className="text-2xl font-bold text-slate-950">Certificate Valid</h1>
                <p className="text-sm text-slate-500">Verified through CollegeEvents</p>
              </div>
            </div>
            <div className="grid gap-3 text-sm">
              <Row label="Participant Name" value={data.recipientName} />
              <Row label="Certificate ID" value={data.certificateId} />
              <Row label="Event" value={data.eventName} />
              <Row label="Issue Date" value={data.issuedAt ? new Date(data.issuedAt).toLocaleDateString() : '-'} />
              <Row label="Organizer" value={data.organizerName || data.organizationName} />
              <Row label="Verification Status" value={data.status === 'REVOKED' ? 'Revoked' : 'Valid'} />
            </div>
            <Link to="/events" className="btn-primary mt-8 inline-flex items-center gap-2"><FiAward /> Browse Events</Link>
          </div>
        )}
      </div>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <div className="grid grid-cols-[160px_1fr] gap-3 border-b border-slate-100 pb-2">
      <span className="font-semibold text-slate-700">{label}</span>
      <span className="text-slate-600">{value || '-'}</span>
    </div>
  );
}
