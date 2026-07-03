import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useForm } from 'react-hook-form';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { eventsAPI } from '../../services/api';
import { toast } from 'react-toastify';
import Spinner from '../../components/common/Spinner';
import { FiSave, FiArrowLeft } from 'react-icons/fi';

export default function EditEvent() {
  const { id }     = useParams();
  const navigate   = useNavigate();
  const qc         = useQueryClient();

  const { data: event, isLoading } = useQuery(
    ['event-edit', id],
    () => eventsAPI.getById(id).then(r => r.data.data)
  );

  const { register, handleSubmit, formState: { errors } } = useForm({
    values: event ? {
      eventName:    event.eventName,
      description:  event.description,
      category:     event.category,
      eventType:    event.eventType,
      eventDate:    event.eventDate,
      eventTime:    event.eventTime,
      endDate:      event.endDate,
      endTime:      event.endTime,
      venueName:    event.venueName,
      location:     event.location,
      googleMapsUrl:event.googleMapsUrl,
      ticketPrice:  event.ticketPrice,
      totalSeats:   event.totalSeats,
      tags:         event.tags,
      status:       event.status,
      visibility:   event.visibility,
    } : {},
  });

  const mutation = useMutation(
    (data) => eventsAPI.update(id, data),
    {
      onSuccess: () => {
        toast.success('Event updated!');
        qc.invalidateQueries(['event', id]);
        qc.invalidateQueries(['event-edit', id]);
        qc.invalidateQueries('org-events');
        qc.invalidateQueries('org-events-dash');
        qc.invalidateQueries('org-dash');
        qc.invalidateQueries('events');
        navigate('/organizer/events');
      },
    }
  );

  if (isLoading) return <Spinner full />;

  return (
    <div className="max-w-3xl mx-auto px-4 py-10 animate-fade-in">
      <div className="flex items-center gap-4 mb-8">
        <button onClick={() => navigate(-1)}
          className="p-2 rounded-xl hover:bg-gray-100 dark:hover:bg-dark-border transition-colors">
          <FiArrowLeft />
        </button>
        <h1 className="section-title">Edit Event</h1>
      </div>

      <form onSubmit={handleSubmit(d => mutation.mutate({
        ...d, ticketPrice: Number(d.ticketPrice), totalSeats: Number(d.totalSeats)
      }))}
        className="space-y-6">

        <div className="card p-6 space-y-4">
          <h2 className="font-bold text-gray-900 dark:text-white">Basic Info</h2>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Event Name</label>
            <input {...register('eventName', { required: true })} className="input-field" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Description</label>
            <textarea {...register('description')} rows={4} className="input-field resize-none" />
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Event Date</label>
              <input {...register('eventDate')} type="date" className="input-field" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Event Time</label>
              <input {...register('eventTime')} type="time" className="input-field" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Location</label>
              <input {...register('location')} className="input-field" />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Ticket Price (₹)</label>
              <input {...register('ticketPrice')} type="number" min="0" step="0.01" className="input-field" />
            </div>
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Status</label>
              <select {...register('status')} className="input-field">
                {['DRAFT','PUBLISHED','CANCELLED'].map(s => <option key={s} value={s}>{s}</option>)}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">Visibility</label>
              <select {...register('visibility')} className="input-field">
                <option value="PUBLIC">Public</option>
                <option value="PRIVATE">Private</option>
              </select>
            </div>
          </div>
        </div>

        <div className="flex gap-4">
          <button type="submit" disabled={mutation.isLoading}
            className="btn-primary flex items-center gap-2 disabled:opacity-60">
            <FiSave /> {mutation.isLoading ? 'Saving…' : 'Save Changes'}
          </button>
          <button type="button" onClick={() => navigate(-1)} className="btn-outline">Cancel</button>
        </div>
      </form>
    </div>
  );
}
