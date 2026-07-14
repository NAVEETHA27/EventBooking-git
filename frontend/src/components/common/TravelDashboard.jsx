/**
 * TravelDashboard — AI Travel Assistant for event participants.
 *
 * Shows all organizer-provided travel information in modern cards:
 * venue, bus, train, metro, parking, food, accommodation, emergency contacts.
 *
 * Registered participants also get an AI Travel Assistant chat that answers
 * questions using only the organizer-provided data stored in the database.
 *
 * Props:
 *  ev               — event object (full EventResponse from /events/{id})
 *  canAccessDetails — true if user has confirmed booking, is organizer, or admin
 *  user             — auth user object
 *  id               — event ID string
 */
import { useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQuery } from 'react-query';
import { aiAPI } from '../../services/api';
import { motion, AnimatePresence } from 'framer-motion';
import {
  FiMapPin, FiNavigation, FiExternalLink, FiCopy, FiSend,
  FiMessageSquare, FiLock, FiAlertCircle, FiPhone, FiUsers,
} from 'react-icons/fi';
import {
  MdDirectionsBus, MdTrain, MdSubway, MdDirectionsCar,
  MdHotel, MdRestaurant, MdFastfood, MdLocalParking,
  MdEmergency, MdWifi, MdAccessible,
} from 'react-icons/md';
import { toast } from 'react-toastify';

// ── helpers ──────────────────────────────────────────────────────────────────
function copy(text) {
  navigator.clipboard.writeText(text).then(() => toast.success('Copied!'));
}

function has(v) { return v != null && v !== '' && v !== 'null'; }

function Card({ icon, title, children, accent = 'blue' }) {
  const colors = {
    blue:   'border-blue-100 bg-blue-50/40',
    green:  'border-green-100 bg-green-50/40',
    amber:  'border-amber-100 bg-amber-50/40',
    purple: 'border-purple-100 bg-purple-50/40',
    red:    'border-red-100 bg-red-50/40',
    teal:   'border-teal-100 bg-teal-50/40',
  };
  return (
    <div className={`rounded-2xl border p-5 ${colors[accent] || colors.blue}`}>
      <div className="flex items-center gap-2 mb-3">
        <span className="text-xl">{icon}</span>
        <h3 className="font-bold text-slate-800 text-sm">{title}</h3>
      </div>
      {children}
    </div>
  );
}

function Row({ label, value, copyable }) {
  if (!has(value)) return null;
  return (
    <div className="flex items-start justify-between gap-2 py-1.5 border-b border-white/60 last:border-0">
      <span className="text-xs text-slate-500 shrink-0 w-36">{label}</span>
      <div className="flex items-center gap-1.5 min-w-0">
        <span className="text-xs font-semibold text-slate-800 text-right break-words">{value}</span>
        {copyable && (
          <button onClick={() => copy(value)} className="shrink-0 text-slate-400 hover:text-blue-600 transition-colors" title="Copy">
            <FiCopy className="w-3 h-3" />
          </button>
        )}
      </div>
    </div>
  );
}

// ── Travel Assistant Chat ─────────────────────────────────────────────────────
function TravelAssistant({ eventId }) {
  const [input, setInput]     = useState('');
  const [messages, setMessages] = useState([
    { role: 'assistant', text: "👋 Hi! I'm your Travel Assistant for this event. Ask me anything about how to reach the venue, bus routes, parking, food, accommodation, or emergency contacts." }
  ]);
  const bottomRef = useRef(null);

  const askMutation = useMutation(
    (question) => aiAPI.travelDetails ? aiAPI.travelDetails(eventId) : Promise.reject('No API'),
    // We use the eventQa endpoint for conversational travel questions
  );

  const travelQa = useMutation(
    (question) => fetch(`/api/ai/eventgpt/events/${eventId}/qa`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${localStorage.getItem('eb_token') || ''}`,
      },
      body: JSON.stringify({ question }),
    }).then(r => r.json()),
    {
      onSuccess: (res, question) => {
        const answer = res?.data?.answer || res?.answer || "I don't have that information for this event. Please contact the organizer directly.";
        setMessages(prev => [...prev, { role: 'assistant', text: answer }]);
        setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
      },
      onError: () => {
        setMessages(prev => [...prev, { role: 'assistant', text: "I couldn't reach the server right now. Please try again." }]);
      },
    }
  );

  const send = () => {
    const q = input.trim();
    if (!q) return;
    setMessages(prev => [...prev, { role: 'user', text: q }]);
    setInput('');
    setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 30);
    travelQa.mutate(q);
  };

  const QUICK = [
    'How do I reach the venue?',
    'Which bus should I take?',
    'Is parking available?',
    'Is food provided?',
    'Where to stay nearby?',
    'Emergency contacts?',
  ];

  return (
    <div className="rounded-2xl border border-blue-200 bg-white overflow-hidden shadow-sm">
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 bg-gradient-to-r from-blue-600 to-blue-700 text-white">
        <FiNavigation className="h-5 w-5" />
        <div>
          <p className="font-bold text-sm">AI Travel Assistant</p>
          <p className="text-[11px] text-blue-200">Answers using organizer-provided data only</p>
        </div>
      </div>

      {/* Messages */}
      <div className="h-72 overflow-y-auto p-4 space-y-3 bg-slate-50">
        {messages.map((msg, i) => (
          <div key={i} className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            <div className={`max-w-[85%] rounded-2xl px-3 py-2 text-sm ${
              msg.role === 'user'
                ? 'bg-blue-600 text-white rounded-br-sm'
                : 'bg-white border border-slate-200 text-slate-800 rounded-bl-sm shadow-sm'
            }`}>
              <p className="whitespace-pre-wrap">{msg.text}</p>
            </div>
          </div>
        ))}
        {travelQa.isLoading && (
          <div className="flex justify-start">
            <div className="bg-white border border-slate-200 rounded-2xl rounded-bl-sm px-4 py-3 shadow-sm">
              <div className="flex gap-1">
                {[0, 1, 2].map(i => (
                  <motion.span key={i} className="w-1.5 h-1.5 rounded-full bg-blue-400"
                    animate={{ y: [0, -5, 0] }}
                    transition={{ duration: 0.6, repeat: Infinity, delay: i * 0.15 }} />
                ))}
              </div>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Quick questions */}
      <div className="px-3 pt-2 pb-1 flex flex-wrap gap-1.5 border-t border-slate-100">
        {QUICK.map(q => (
          <button key={q} onClick={() => { setInput(q); }}
            className="text-[11px] font-semibold text-blue-700 bg-blue-50 border border-blue-100 rounded-full px-2.5 py-1 hover:bg-blue-100 transition-colors">
            {q}
          </button>
        ))}
      </div>

      {/* Input */}
      <div className="flex items-end gap-2 px-3 pb-3 pt-2 bg-white">
        <input
          value={input}
          onChange={e => setInput(e.target.value)}
          onKeyDown={e => { if (e.key === 'Enter') send(); }}
          placeholder="Ask about travel, food, parking…"
          className="flex-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none focus:border-blue-400 focus:bg-white transition-colors"
        />
        <button onClick={send} disabled={!input.trim() || travelQa.isLoading}
          className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-40 shrink-0 transition-colors">
          <FiSend className="h-4 w-4" />
        </button>
      </div>
    </div>
  );
}

// ── Main TravelDashboard ──────────────────────────────────────────────────────
export default function TravelDashboard({ ev, canAccessDetails, user, id }) {
  const hasTransport = has(ev.nearestBusStop) || has(ev.busNumbers) || has(ev.nearestRailwayStation) || has(ev.metroInformation);
  const hasFood      = ev.foodProvided;
  const hasAccomm    = ev.accommodationProvided;
  const hasPark      = has(ev.parkingAvailable) && ev.parkingAvailable !== 'NONE';
  const hasEmerg     = has(ev.emergencyContacts);
  const hasNearby    = has(ev.nearbyHotels) || has(ev.nearbyRestaurants);

  return (
    <div className="space-y-5">
      {/* Venue Hero Card */}
      <div className="rounded-2xl bg-gradient-to-br from-blue-700 via-blue-800 to-indigo-900 p-5 text-white shadow-lg">
        <p className="text-xs font-black uppercase tracking-widest text-blue-300 mb-1">Event Venue</p>
        <h2 className="text-xl font-extrabold mb-1">{ev.venueName || ev.eventName}</h2>
        {has(ev.location) && (
          <p className="text-sm text-blue-200 flex items-start gap-1.5 mb-3">
            <FiMapPin className="h-3.5 w-3.5 mt-0.5 shrink-0" />{ev.location}
          </p>
        )}
        <div className="flex flex-wrap gap-2">
          {has(ev.googleMapsUrl) && (
            <a href={ev.googleMapsUrl} target="_blank" rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 rounded-xl bg-white/15 hover:bg-white/25 px-3 py-2 text-xs font-bold transition-colors">
              <FiExternalLink className="h-3.5 w-3.5" /> Open Google Maps
            </a>
          )}
          {has(ev.whatsappGroupLink) && (
            <a href={ev.whatsappGroupLink} target="_blank" rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 rounded-xl bg-green-500/80 hover:bg-green-500 px-3 py-2 text-xs font-bold transition-colors">
              <FiUsers className="h-3.5 w-3.5" /> Join WhatsApp Group
            </a>
          )}
          {has(ev.whatsappContactNumber) && (
            <a href={`https://wa.me/${ev.whatsappContactNumber?.replace(/\D/g, '')}`} target="_blank" rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 rounded-xl bg-white/15 hover:bg-white/25 px-3 py-2 text-xs font-bold transition-colors">
              <FiPhone className="h-3.5 w-3.5" /> WhatsApp Organizer
            </a>
          )}
        </div>
      </div>

      {/* Step-by-step travel guide */}
      {has(ev.travelGuide) && (
        <Card icon={<FiNavigation />} title="Step-by-Step Travel Guide" accent="blue">
          <div className="text-sm text-slate-700 whitespace-pre-wrap leading-relaxed bg-white rounded-xl p-3 border border-blue-100">
            {ev.travelGuide}
          </div>
        </Card>
      )}

      {/* Transport grid */}
      {hasTransport && (
        <div className="grid sm:grid-cols-2 gap-4">
          {/* Bus */}
          {(has(ev.nearestBusStop) || has(ev.busNumbers)) && (
            <Card icon={<MdDirectionsBus className="text-blue-600" />} title="Bus" accent="blue">
              <Row label="Nearest Bus Stop" value={ev.nearestBusStop} />
              <Row label="Bus Numbers" value={ev.busNumbers} />
              <Row label="Walking Distance" value={ev.distanceFromBusStop} />
              <Row label="Travel Time" value={ev.estimatedTravelTime} />
            </Card>
          )}

          {/* Train */}
          {has(ev.nearestRailwayStation) && (
            <Card icon={<MdTrain className="text-purple-600" />} title="Train / Railway" accent="purple">
              <Row label="Railway Station" value={ev.nearestRailwayStation} />
              <Row label="Distance" value={ev.distanceFromRailwayStation} />
              <Row label="Nearest Airport" value={ev.nearestAirport} />
            </Card>
          )}

          {/* Metro */}
          {has(ev.metroInformation) && (
            <Card icon={<MdSubway className="text-teal-600" />} title="Metro / Local" accent="teal">
              <div className="text-sm text-slate-700">{ev.metroInformation}</div>
            </Card>
          )}

          {/* Private Vehicle */}
          {(hasPark || has(ev.cabEstimate)) && (
            <Card icon={<MdDirectionsCar className="text-amber-600" />} title="Private Vehicle / Cab" accent="amber">
              <Row label="Parking" value={ev.parkingAvailable} />
              <Row label="Cab Estimate" value={ev.cabEstimate} />
            </Card>
          )}
        </div>
      )}

      {/* Landmarks */}
      {has(ev.landmarks) && (
        <Card icon="📍" title="Landmarks & Directions" accent="blue">
          <p className="text-sm text-slate-700 whitespace-pre-wrap">{ev.landmarks}</p>
        </Card>
      )}

      {/* Food & Accommodation — shown only to registered participants */}
      {canAccessDetails ? (
        <>
          {(hasFood || hasAccomm) && (
            <div className="grid sm:grid-cols-2 gap-4">
              {hasFood && (
                <Card icon={<MdFastfood className="text-orange-600" />} title="Food" accent="amber">
                  <Row label="Food Available" value="Yes" />
                  <Row label="Meals" value={ev.foodMeals} />
                  <Row label="Type" value={ev.foodType === 'VEG' ? 'Vegetarian' : ev.foodType === 'NON_VEG' ? 'Non-Veg' : ev.foodType === 'BOTH' ? 'Veg & Non-Veg' : ev.foodType} />
                  <Row label="Tea / Coffee" value={ev.teaCoffeeProvided ? 'Yes' : null} />
                  <Row label="Special Diet" value={ev.specialDiet} />
                </Card>
              )}

              {hasAccomm && (
                <Card icon={<MdHotel className="text-indigo-600" />} title="Accommodation" accent="purple">
                  <Row label="Type" value={ev.accommodationType} />
                  <Row label="Charges" value={ev.accommodationCharges != null ? `₹${ev.accommodationCharges}` : null} />
                  <Row label="Beds Available" value={ev.accommodationBedsAvailable} />
                  <Row label="Check-in" value={ev.accommodationCheckIn} />
                  <Row label="Check-out" value={ev.accommodationCheckOut} />
                  <Row label="Contact Person" value={ev.accommodationContactPerson} copyable />
                  {ev.boysHostelAvailable && <p className="text-xs text-slate-600 mt-1">🏠 Boys hostel available</p>}
                  {ev.girlsHostelAvailable && <p className="text-xs text-slate-600">🏠 Girls hostel available</p>}
                  {ev.hotelTieupAvailable  && <p className="text-xs text-slate-600">🏨 Hotel tie-up available</p>}
                </Card>
              )}
            </div>
          )}

          {/* Nearby Hotels & Restaurants */}
          {hasNearby && (
            <div className="grid sm:grid-cols-2 gap-4">
              {has(ev.nearbyHotels) && (
                <Card icon={<MdHotel className="text-blue-600" />} title="Nearby Hotels" accent="blue">
                  <p className="text-sm text-slate-700 whitespace-pre-wrap">{ev.nearbyHotels}</p>
                </Card>
              )}
              {has(ev.nearbyRestaurants) && (
                <Card icon={<MdRestaurant className="text-green-600" />} title="Nearby Restaurants" accent="green">
                  <p className="text-sm text-slate-700 whitespace-pre-wrap">{ev.nearbyRestaurants}</p>
                </Card>
              )}
            </div>
          )}

          {/* Emergency Contacts */}
          {hasEmerg && (
            <Card icon={<MdEmergency className="text-red-600" />} title="Emergency Contacts" accent="red">
              <p className="text-sm text-slate-700 whitespace-pre-wrap">{ev.emergencyContacts}</p>
              {has(ev.whatsappContactNumber) && (
                <div className="mt-3 flex items-center gap-2 bg-white rounded-xl border border-red-100 px-3 py-2">
                  <FiPhone className="h-4 w-4 text-red-500 shrink-0" />
                  <span className="text-sm font-semibold text-slate-800 flex-1">{ev.whatsappContactNumber}</span>
                  <button onClick={() => copy(ev.whatsappContactNumber)} className="text-slate-400 hover:text-red-600 transition-colors">
                    <FiCopy className="h-3.5 w-3.5" />
                  </button>
                </div>
              )}
            </Card>
          )}

          {/* Reporting time & dress code */}
          {(has(ev.reportingTime) || has(ev.dressCode)) && (
            <Card icon="📋" title="Day-of Information" accent="teal">
              <Row label="Reporting Time" value={ev.reportingTime} />
              <Row label="Dress Code" value={ev.dressCode} />
            </Card>
          )}

          {/* AI Travel Assistant */}
          <TravelAssistant eventId={ev.id} />
        </>
      ) : (
        /* Gate for non-registered users */
        <div className="rounded-2xl border border-blue-100 bg-white p-8 text-center shadow-sm">
          <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-blue-50">
            <FiLock className="h-7 w-7 text-blue-400" />
          </div>
          <h3 className="font-bold text-slate-900 mb-2">Full Travel Details</h3>
          <p className="text-sm text-slate-500 mb-4 max-w-xs mx-auto">
            Register for this event to access food, accommodation, emergency contacts, and the AI Travel Assistant.
          </p>
          {!user ? (
            <Link to={`/login?redirect=/events/${id}`} className="btn-primary inline-flex items-center gap-2 text-sm">
              Sign In to Register
            </Link>
          ) : (
            <div className="inline-flex items-center gap-2 text-xs text-slate-400 bg-slate-50 rounded-xl px-4 py-2 border border-slate-200">
              <FiAlertCircle className="h-3.5 w-3.5" /> Registration required for full travel details
            </div>
          )}
        </div>
      )}

      {/* Show message if no travel data */}
      {!hasTransport && !hasFood && !hasAccomm && !has(ev.travelGuide) && !has(ev.landmarks) && (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-white py-12 text-center">
          <FiMapPin className="h-10 w-10 mx-auto mb-3 text-slate-300" />
          <p className="font-semibold text-slate-500">No travel information added yet</p>
          <p className="text-sm text-slate-400 mt-1">The organizer hasn't added travel details for this event.</p>
        </div>
      )}
    </div>
  );
}
