import { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from 'react-query';
import { toast } from 'react-toastify';
import { eventsAPI } from '../../services/api';
import {
  FiArrowLeft,
  FiCalendar,
  FiCheckCircle,
  FiImage,
  FiMapPin,
  FiSave,
  FiSettings,
  FiUploadCloud,
  FiUsers,
} from 'react-icons/fi';

const ORG_TYPES = ['College', 'Department', 'Club', 'Company', 'Community', 'Other'];

export default function CreateEvent() {
  const navigate = useNavigate();
  const qc = useQueryClient();
  const [posterFile, setPosterFile] = useState(null);
  const [posterPreview, setPosterPreview] = useState('');
  const [authorizedFile, setAuthorizedFile] = useState(null);
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm({
    defaultValues: {
      status: 'DRAFT',
      visibility: 'PUBLIC',
      eventType: 'OFFLINE',
      organizationType: 'College',
      hasCertificate: false,
    },
  });

  const organizationType = watch('organizationType');
  const isCollege = organizationType === 'College';

  const { data: categories } = useQuery('categories', () => eventsAPI.categories().then((r) => r.data.data));

  const posterName = useMemo(() => posterFile?.name ?? 'Upload event poster image', [posterFile]);
  const authorizedName = useMemo(() => authorizedFile?.name ?? 'Upload authorized organization document', [authorizedFile]);

  const mutation = useMutation(
    async (formData) => {
      const { organizationType: orgType, ...eventData } = formData;
      const created = await eventsAPI.create({
        ...eventData,
        ticketPrice: Number(eventData.ticketPrice),
        totalSeats: Number(eventData.totalSeats),
        hasCertificate: Boolean(eventData.hasCertificate),
        organizerDetails: [
          `Organization type: ${orgType}`,
          eventData.organizerDetails ? `Details: ${eventData.organizerDetails}` : '',
        ].filter(Boolean).join('\n'),
      });

      const eventId = created.data?.data?.id ?? created.data?.id;
      if (posterFile && eventId) {
        const upload = new FormData();
        upload.append('file', posterFile);
        await eventsAPI.uploadBanner(eventId, upload);
      }
      if (authorizedFile && eventId) {
        const upload = new FormData();
        upload.append('file', authorizedFile);
        await eventsAPI.uploadAuthorizedDocument(eventId, upload);
      }
      return created;
    },
    {
      onSuccess: () => {
        toast.success('Event created successfully!');
        qc.invalidateQueries('org-events');
        qc.invalidateQueries('org-events-dash');
        qc.invalidateQueries('org-dash');
        qc.invalidateQueries('events');
        qc.invalidateQueries('notifs');
        navigate('/organizer/events');
      },
    }
  );

  const onPosterChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    if (!file.type.startsWith('image/')) {
      toast.error('Please choose an image file.');
      return;
    }
    setPosterFile(file);
    setPosterPreview(URL.createObjectURL(file));
  };

  const onAuthorizedDocumentChange = (event) => {
    const file = event.target.files?.[0];
    if (!file) return;
    const allowed = ['application/pdf', 'image/png', 'image/jpeg'];
    if (!allowed.includes(file.type)) {
      toast.error('Authorized document must be PDF, PNG, JPG, or JPEG.');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      toast.error('Authorized document must be 5MB or smaller.');
      return;
    }
    setAuthorizedFile(file);
  };

  return (
    <div className="min-h-screen bg-slate-50 px-4 py-10 sm:px-6 lg:px-8">
      <div className="mx-auto max-w-6xl">
        <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <button onClick={() => navigate(-1)} className="rounded-xl border border-slate-200 bg-white p-2 text-slate-600 transition-colors hover:bg-slate-100">
              <FiArrowLeft />
            </button>
            <div>
              <p className="text-xs font-black uppercase tracking-[0.2em] text-teal-700">Organizer Workspace</p>
              <h1 className="section-title">Create New Event</h1>
            </div>
          </div>
          <button type="submit" form="create-event-form" disabled={mutation.isLoading} className="btn-primary flex items-center gap-2 disabled:opacity-60">
            <FiSave /> {mutation.isLoading ? 'Creating...' : 'Create Event'}
          </button>
        </div>

        <form id="create-event-form" onSubmit={handleSubmit((data) => mutation.mutate(data))} className="grid gap-6 lg:grid-cols-[1fr_360px]">
          <div className="space-y-6">
            <Section icon={<FiSettings />} title="Basic Information" text="Give participants the clearest possible event summary.">
              <FormField label="Event Name" error={errors.eventName?.message}>
                <input {...register('eventName', { required: 'Event name is required' })} className="input-field" placeholder="e.g. National Tech Summit 2026" />
              </FormField>

              <FormField label="Description">
                <textarea {...register('description')} rows={5} className="input-field resize-none" placeholder="Describe the event, agenda, eligibility, and expected outcomes." />
              </FormField>

              <div className="grid gap-4 md:grid-cols-2">
                <FormField label="Category" error={errors.category?.message}>
                  <select {...register('category', { required: 'Category is required' })} className="input-field">
                    <option value="">Select category</option>
                    {(categories || []).map((category) => <option key={category} value={category}>{category.replace(/_/g, ' ')}</option>)}
                  </select>
                </FormField>
                <FormField label="Event Type">
                  <select {...register('eventType')} className="input-field">
                    <option value="OFFLINE">In-Person</option>
                    <option value="ONLINE">Online</option>
                    <option value="HYBRID">Hybrid</option>
                  </select>
                </FormField>
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <FormField label="Organization Type">
                  <select {...register('organizationType')} className="input-field">
                    {ORG_TYPES.map((type) => <option key={type} value={type}>{type}</option>)}
                  </select>
                </FormField>
              </div>

              {isCollege && (
                <div className="grid gap-4 rounded-xl border border-teal-100 bg-teal-50/60 p-4 md:grid-cols-2">
                  <FormField label="College Name" error={errors.collegeName?.message}>
                    <input {...register('collegeName', { required: isCollege ? 'College name is required' : false })} className="input-field" placeholder="e.g. ABC Engineering College" />
                  </FormField>
                  <FormField label="Department Where It Will Be Held" error={errors.departmentName?.message}>
                    <input {...register('departmentName', { required: isCollege ? 'Department is required' : false })} className="input-field" placeholder="e.g. Computer Science Department" />
                  </FormField>
                </div>
              )}

              {!isCollege && (
                <FormField label="Organization Details">
                  <input {...register('organizerDetails')} className="input-field" placeholder="Club, company, or community name" />
                </FormField>
              )}

              <FormField label="Tags">
                <input {...register('tags')} className="input-field" placeholder="React, AI, Java, Workshop" />
              </FormField>
            </Section>

            <Section icon={<FiCalendar />} title="Date & Time" text="Add the full schedule so attendees can plan ahead.">
              <div className="grid gap-4 md:grid-cols-2">
                <FormField label="Start Date" error={errors.eventDate?.message}>
                  <input {...register('eventDate', { required: 'Start date is required' })} type="date" className="input-field" />
                </FormField>
                <FormField label="Start Time" error={errors.eventTime?.message}>
                  <input {...register('eventTime', { required: 'Start time is required' })} type="time" className="input-field" />
                </FormField>
                <FormField label="End Date">
                  <input {...register('endDate')} type="date" className="input-field" />
                </FormField>
                <FormField label="End Time">
                  <input {...register('endTime')} type="time" className="input-field" />
                </FormField>
                <FormField label="Registration Deadline">
                  <input {...register('registrationDeadline')} type="date" className="input-field" />
                </FormField>
              </div>
            </Section>

            <Section icon={<FiMapPin />} title="Location" text="Use venue details that make the event easy to find.">
              <FormField label="Venue Name">
                <input {...register('venueName')} className="input-field" placeholder="Auditorium, Seminar Hall, Online Room" />
              </FormField>
              <FormField label="Full Address / Location" error={errors.location?.message}>
                <input {...register('location', { required: 'Location is required' })} className="input-field" placeholder="Campus address, city, state" />
              </FormField>
              <FormField label="Google Maps URL">
                <input {...register('googleMapsUrl')} type="url" className="input-field" placeholder="https://maps.google.com/..." />
              </FormField>
            </Section>
          </div>

          <aside className="space-y-6">
            <Section icon={<FiImage />} title="Event Poster" text="Upload a poster that will appear as the event banner.">
              <label className="poster-drop">
                {posterPreview ? (
                  <img src={posterPreview} alt="Event poster preview" />
                ) : (
                  <div className="grid place-items-center gap-3 text-center">
                    <FiUploadCloud className="h-9 w-9 text-teal-600" />
                    <span>{posterName}</span>
                    <small>PNG, JPG, or WebP works best</small>
                  </div>
                )}
                <input type="file" accept="image/*" onChange={onPosterChange} />
              </label>
              {posterPreview && (
                <button type="button" onClick={() => { setPosterFile(null); setPosterPreview(''); }} className="btn-outline w-full">
                  Remove Poster
                </button>
              )}
            </Section>

            <Section icon={<FiUploadCloud />} title="Authorized Organization Document" text="Upload the approval letter, permission note, or official event authorization.">
              <label className="poster-drop">
                <div className="grid place-items-center gap-3 text-center">
                  <FiUploadCloud className="h-9 w-9 text-teal-600" />
                  <span>{authorizedName}</span>
                  <small>PDF, PNG, JPG, or JPEG up to 5MB</small>
                </div>
                <input type="file" accept=".pdf,.png,.jpg,.jpeg,application/pdf,image/png,image/jpeg" onChange={onAuthorizedDocumentChange} />
              </label>
              {authorizedFile && (
                <button type="button" onClick={() => setAuthorizedFile(null)} className="btn-outline w-full">
                  Remove Document
                </button>
              )}
            </Section>

            <Section icon={<FiUsers />} title="Tickets & Seats" text="Set capacity and pricing clearly.">
              <FormField label="Ticket Price (Rs.)" error={errors.ticketPrice?.message}>
                <input {...register('ticketPrice', { required: 'Ticket price is required', min: 0 })} type="number" min="0" step="0.01" className="input-field" placeholder="0 for free" />
              </FormField>
              <FormField label="Total Seats" error={errors.totalSeats?.message}>
                <input {...register('totalSeats', { required: 'Total seats is required', min: 1 })} type="number" min="1" className="input-field" placeholder="100" />
              </FormField>
              <label className="flex items-center gap-2 rounded-xl border border-slate-200 bg-white p-3 text-sm font-semibold text-slate-700">
                <input {...register('hasCertificate')} type="checkbox" className="accent-teal-600" />
                Certificate provided
              </label>
            </Section>

            <Section icon={<FiCheckCircle />} title="Publish Settings" text="Save as a draft or publish immediately.">
              <FormField label="Status">
                <select {...register('status')} className="input-field">
                  <option value="DRAFT">Draft</option>
                  <option value="PUBLISHED">Publish Now</option>
                </select>
              </FormField>
              <FormField label="Visibility">
                <select {...register('visibility')} className="input-field">
                  <option value="PUBLIC">Public</option>
                  <option value="PRIVATE">Private</option>
                </select>
              </FormField>
              <div className="flex gap-3">
                <button type="submit" disabled={mutation.isLoading} className="btn-primary flex-1 disabled:opacity-60">
                  {mutation.isLoading ? 'Creating...' : 'Create'}
                </button>
                <button type="button" onClick={() => navigate(-1)} className="btn-outline flex-1">
                  Cancel
                </button>
              </div>
            </Section>
          </aside>
        </form>
      </div>
    </div>
  );
}

function Section({ icon, title, text, children }) {
  return (
    <section className="rounded-xl border border-slate-200 bg-white p-6 shadow-card">
      <div className="mb-5 flex items-start gap-3">
        <div className="grid h-10 w-10 flex-shrink-0 place-items-center rounded-xl bg-teal-50 text-teal-700">{icon}</div>
        <div>
          <h2 className="font-bold text-slate-950">{title}</h2>
          <p className="mt-0.5 text-sm text-slate-500">{text}</p>
        </div>
      </div>
      <div className="space-y-4">{children}</div>
    </section>
  );
}

function FormField({ label, error, children }) {
  return (
    <div>
      <label className="mb-1.5 block text-sm font-semibold text-slate-700">{label}</label>
      {children}
      {error && <p className="mt-1 text-xs text-red-500">{error}</p>}
    </div>
  );
}
