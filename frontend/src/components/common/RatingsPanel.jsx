import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from 'react-query';
import { FiBarChart2, FiEyeOff, FiMessageCircle, FiStar, FiThumbsUp } from 'react-icons/fi';
import { toast } from 'react-toastify';
import { useAuth } from '../../context/AuthContext';
import { ratingsAPI } from '../../services/api';

const STAR_FIELDS = [
  { key: 'overallRating', label: 'Overall', required: true },
  { key: 'eventQualityRating', label: 'Event Quality' },
  { key: 'speakerRating', label: 'Speaker Quality' },
  { key: 'venueRating', label: 'Venue' },
  { key: 'organizationRating', label: 'Organization' },
  { key: 'foodRating', label: 'Food' },
  { key: 'accommodationRating', label: 'Accommodation' },
];

export default function RatingsPanel({ eventId }) {
  const { user } = useAuth();
  const qc = useQueryClient();
  const [form, setForm] = useState({ overallRating: 0, reviewText: '', suggestions: '', anonymous: false });
  const [showForm, setShowForm] = useState(false);

  const { data: summary } = useQuery(
    ['rating-summary', eventId],
    () => ratingsAPI.getSummary(eventId).then(r => r.data?.data),
    { staleTime: 60_000 }
  );
  const { data: reviewsData } = useQuery(
    ['event-reviews', eventId],
    () => ratingsAPI.getByEvent(eventId, { page: 0, size: 5 }).then(r => r.data?.data),
    { staleTime: 60_000 }
  );
  const { data: myReview } = useQuery(
    ['my-review', eventId],
    () => ratingsAPI.getMyReview(eventId).then(r => r.data?.data),
    { enabled: !!user, staleTime: 60_000 }
  );
  const { data: eligibility } = useQuery(
    ['rating-eligibility', eventId],
    () => ratingsAPI.eligibility(eventId).then(r => r.data?.data),
    { enabled: !!user, staleTime: 60_000, retry: false }
  );

  const submit = useMutation(
    (data) => myReview ? ratingsAPI.updateMine(eventId, data) : ratingsAPI.submit(eventId, data),
    {
      onSuccess: () => {
        toast.success(myReview ? 'Review updated' : 'Review submitted');
        setShowForm(false);
        qc.invalidateQueries(['rating-summary', eventId]);
        qc.invalidateQueries(['event-reviews', eventId]);
        qc.invalidateQueries(['my-review', eventId]);
        qc.invalidateQueries(['rating-eligibility', eventId]);
      },
      onError: err => toast.error(err?.response?.data?.message || 'Failed to submit review'),
    }
  );

  const reviews = reviewsData?.content || [];
  const average = Number(summary?.averageRating || 0);
  const totalReviews = Number(summary?.totalReviews || 0);
  const canWrite = Boolean(user && (eligibility?.canSubmit || eligibility?.canEdit));
  const blockedReason = getBlockedReason(user, eligibility);
  const distribution = summary?.ratingDistribution || {};
  const categoryAverages = summary?.categoryAverages || {};

  return (
    <div className="space-y-5">
      <div className="grid gap-4 lg:grid-cols-[18rem_1fr]">
        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <p className="text-xs font-black uppercase tracking-wide text-slate-400">Feedback score</p>
          <div className="mt-3 flex items-end gap-3">
            <span className="text-5xl font-extrabold text-blue-700">{average ? average.toFixed(1) : '-'}</span>
            <span className="pb-2 text-sm font-bold text-slate-500">/ 5</span>
          </div>
          <Stars rating={average} />
          <p className="mt-2 text-sm text-slate-500">{totalReviews} verified review{totalReviews === 1 ? '' : 's'}</p>
          {totalReviews > 0 && (
            <div className="mt-4 space-y-1">
              {[5, 4, 3, 2, 1].map(star => {
                const count = Number(distribution[star] || distribution[String(star)] || 0);
                const pct = totalReviews ? Math.round((count / totalReviews) * 100) : 0;
                return (
                  <div key={star} className="flex items-center gap-2 text-xs text-slate-500">
                    <span className="w-6 font-bold">{star}</span>
                    <div className="h-2 flex-1 overflow-hidden rounded-full bg-slate-100">
                      <div className="h-full rounded-full bg-amber-400" style={{ width: `${pct}%` }} />
                    </div>
                    <span className="w-7 text-right">{count}</span>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="rounded-xl border border-slate-200 bg-white p-5">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-black uppercase tracking-wide text-slate-400">Attendee sentiment</p>
              <h3 className="mt-1 text-lg font-extrabold text-slate-900">Post-event feedback</h3>
            </div>
            {summary?.satisfactionScore != null && (
              <span className="rounded-full bg-green-50 px-3 py-1 text-xs font-black text-green-700">
                {Number(summary.satisfactionScore).toFixed(1)}/10 satisfaction
              </span>
            )}
          </div>
          {summary?.strengths || summary?.aiTestimonial ? (
            <div className="mt-4 space-y-3 text-sm text-slate-600">
              {summary.strengths && <p className="whitespace-pre-line">{summary.strengths}</p>}
              {summary.aiTestimonial && <blockquote className="border-l-4 border-blue-300 pl-3 italic">{summary.aiTestimonial}</blockquote>}
            </div>
          ) : (
            <p className="mt-4 text-sm text-slate-500">Reviews submitted by checked-in attendees will appear here.</p>
          )}
          {Object.keys(categoryAverages).length > 0 && (
            <div className="mt-4 grid gap-2 sm:grid-cols-2">
              {Object.entries(categoryAverages).map(([key, value]) => (
                <div key={key} className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2 text-sm">
                  <span className="font-semibold capitalize text-slate-600">{labelize(key)}</span>
                  <span className="font-black text-blue-700">{Number(value).toFixed(1)}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {blockedReason && !canWrite && (
        <div className="rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-800">
          <p className="font-extrabold">Feedback is locked</p>
          <p className="mt-1">{blockedReason}</p>
        </div>
      )}

      {canWrite && (
        <div>
          {!showForm ? (
            <button
              onClick={() => {
                if (myReview) setForm({ ...myReview });
                setShowForm(true);
              }}
              className="inline-flex items-center gap-2 rounded-lg border border-blue-300 bg-blue-50 px-4 py-2.5 text-sm font-bold text-blue-700 hover:bg-blue-100"
            >
              <FiMessageCircle /> {myReview ? 'Edit feedback' : 'Write feedback'}
            </button>
          ) : (
            <div className="rounded-xl border border-slate-200 bg-white p-5">
              <h3 className="font-extrabold text-slate-900">{myReview ? 'Edit your feedback' : 'Write your feedback'}</h3>
              <div className="mt-4 grid gap-3 sm:grid-cols-2">
                {STAR_FIELDS.map(field => (
                  <div key={field.key} className="flex items-center justify-between rounded-lg bg-slate-50 px-3 py-2">
                    <label className="text-sm font-semibold text-slate-700">
                      {field.label} {field.required && <span className="text-red-500">*</span>}
                    </label>
                    <StarPicker value={form[field.key] || 0} onChange={value => setForm(prev => ({ ...prev, [field.key]: value }))} />
                  </div>
                ))}
              </div>
              <label className="mt-4 flex cursor-pointer items-center justify-between rounded-lg border border-slate-200 bg-slate-50 px-3 py-3">
                <span className="flex items-center gap-2 text-sm font-bold text-slate-700"><FiEyeOff /> Submit anonymously</span>
                <input
                  type="checkbox"
                  checked={Boolean(form.anonymous)}
                  onChange={e => setForm(prev => ({ ...prev, anonymous: e.target.checked }))}
                  className="h-4 w-4"
                />
              </label>
              <textarea
                rows={4}
                placeholder="Comments about your experience."
                value={form.reviewText || ''}
                onChange={e => setForm(prev => ({ ...prev, reviewText: e.target.value }))}
                className="mt-4 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-200"
              />
              <textarea
                rows={3}
                placeholder="Suggestions for improvement."
                value={form.suggestions || ''}
                onChange={e => setForm(prev => ({ ...prev, suggestions: e.target.value }))}
                className="mt-3 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-1 focus:ring-blue-200"
              />
              <div className="mt-4 flex gap-2">
                <button
                  disabled={!form.overallRating || submit.isLoading}
                  onClick={() => submit.mutate(form)}
                  className="rounded-lg bg-blue-700 px-5 py-2 text-sm font-bold text-white hover:bg-blue-800 disabled:opacity-50"
                >
                  {submit.isLoading ? 'Submitting...' : myReview ? 'Update review' : 'Submit review'}
                </button>
                <button onClick={() => setShowForm(false)} className="rounded-lg border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-600 hover:bg-slate-50">
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {myReview && (
        <div className="flex items-center gap-2 rounded-lg border border-green-200 bg-green-50 px-4 py-3 text-sm font-semibold text-green-800">
          <FiThumbsUp /> You rated this event {myReview.overallRating}/5
          {eligibility?.canEdit && <span className="ml-auto text-xs">Editable for 7 days</span>}
        </div>
      )}

      <div className="space-y-3">
        <h3 className="flex items-center gap-2 font-extrabold text-slate-900"><FiBarChart2 /> Recent public reviews</h3>
        {reviews.length === 0 ? (
          <div className="rounded-xl border border-dashed border-slate-200 bg-white p-5 text-sm text-slate-500">No reviews yet.</div>
        ) : reviews.map(review => (
          <div key={review.id} className="rounded-xl border border-slate-200 bg-white p-4">
            <div className="flex items-center justify-between gap-3">
              <div>
                <Stars rating={review.overallRating} small />
                <p className="mt-1 text-xs font-bold text-slate-500">{review.authorName || 'Verified attendee'}</p>
              </div>
              {review.verifiedAttendance && <span className="rounded-full bg-green-50 px-2 py-0.5 text-xs font-bold text-green-600">Verified</span>}
            </div>
            {review.reviewText && <p className="mt-2 text-sm text-slate-700">{review.reviewText}</p>}
            {review.suggestions && <p className="mt-2 rounded-lg bg-blue-50 px-3 py-2 text-sm text-blue-800"><span className="font-bold">Suggestion:</span> {review.suggestions}</p>}
            <p className="mt-2 text-xs text-slate-400">{review.createdAt ? new Date(review.createdAt).toLocaleDateString() : ''}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

function getBlockedReason(user, eligibility) {
  if (!user) return 'Login to submit feedback after attending this event.';
  if (!eligibility) return '';
  if (eligibility.hasReview && !eligibility.canEdit) return 'You already submitted feedback and the 7-day edit window has closed.';
  if (!eligibility.eventEnded) return 'Feedback opens after the event is completed.';
  if (!eligibility.registered) return 'Only registered participants can submit feedback.';
  if (!eligibility.attended) return 'Ask the organizer to mark you Present. Feedback is enabled only for checked-in attendees.';
  return '';
}

function labelize(value) {
  return String(value).replace(/([A-Z])/g, ' $1').trim();
}

function Stars({ rating, small }) {
  const stars = Math.round(rating || 0);
  return (
    <div className={`flex ${small ? 'gap-0.5' : 'mt-2 gap-1'}`}>
      {[1, 2, 3, 4, 5].map(i => (
        <FiStar key={i} className={`${small ? 'h-3.5 w-3.5' : 'h-4 w-4'} ${i <= stars ? 'fill-amber-400 text-amber-400' : 'text-slate-300'}`} />
      ))}
    </div>
  );
}

function StarPicker({ value, onChange }) {
  const [hover, setHover] = useState(0);
  return (
    <div className="flex gap-1">
      {[1, 2, 3, 4, 5].map(i => (
        <button key={i} type="button" onMouseEnter={() => setHover(i)} onMouseLeave={() => setHover(0)} onClick={() => onChange(i)} title={`${i} star`}>
          <FiStar className={`h-5 w-5 transition-colors ${i <= (hover || value) ? 'fill-amber-400 text-amber-400' : 'text-slate-300'}`} />
        </button>
      ))}
    </div>
  );
}
