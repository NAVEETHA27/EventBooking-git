import { useEffect, useMemo, useRef, useState } from 'react';
import { FiDownload, FiEdit2, FiMoreVertical, FiPaperclip, FiRefreshCw, FiSend, FiTrash2, FiWifi, FiWifiOff } from 'react-icons/fi';
import { MdPushPin } from 'react-icons/md';
import { toast } from 'react-toastify';
import { useAuth } from '../../context/AuthContext';
import { eventCommunityAPI, getApiErrorMessage } from '../../services/api';

export default function EventCommunityChat({ eventId, event, organizer }) {
  const { user } = useAuth();
  const [context, setContext] = useState(null);
  const [messages, setMessages] = useState([]);
  const [draft, setDraft] = useState('');
  const [editingId, setEditingId] = useState(null);
  const [connected, setConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [loadingOlder, setLoadingOlder] = useState(false);
  const bottomRef = useRef(null);
  const fileRef = useRef(null);
  const lastIdRef = useRef(0);

  const lastId = useMemo(() => messages.reduce((max, msg) => Math.max(max, Number(msg.id) || 0), 0), [messages]);
  const firstId = useMemo(() => messages.reduce((min, msg) => Math.min(min, Number(msg.id) || min), Number.MAX_SAFE_INTEGER), [messages]);
  const pinnedMessages = messages.filter(m => m.pinned && !m.deleted);
  const canModerate = Boolean(context?.moderator);
  const canSend = Boolean(context?.canSend && user);

  useEffect(() => { lastIdRef.current = lastId; }, [lastId]);

  const mergeMessages = (incoming) => {
    setMessages(prev => {
      const byId = new Map(prev.map(msg => [msg.id, msg]));
      incoming.filter(Boolean).forEach(msg => byId.set(msg.id, { ...byId.get(msg.id), ...msg }));
      return Array.from(byId.values()).sort((a, b) => Number(a.id) - Number(b.id));
    });
  };

  const loadInitial = () => {
    if (!user) {
      setLoading(false);
      setError('Login with a confirmed booking to join this community.');
      return;
    }
    setLoading(true);
    setError('');
    Promise.all([eventCommunityAPI.context(eventId), eventCommunityAPI.messages(eventId)])
      .then(([ctx, msgs]) => {
        setContext(ctx.data?.data || null);
        setMessages(msgs.data?.data || []);
      })
      .catch(err => setError(getApiErrorMessage(err, 'Could not load the community chat.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadInitial();
  }, [eventId, user]);

  useEffect(() => {
    if (!user) return undefined;
    const token = localStorage.getItem('eb_token');
    let ws;
    if (token) {
      ws = new WebSocket(eventCommunityAPI.wsUrl());
      ws.onopen = () => {
        ws.send(stompFrame('CONNECT', {
          Authorization: `Bearer ${token}`,
          'accept-version': '1.2',
          'heart-beat': '10000,10000',
        }));
      };
      ws.onmessage = (evt) => {
        parseFrames(evt.data).forEach(frame => {
          if (frame.command === 'CONNECTED') {
            setConnected(true);
            ws.send(stompFrame('SUBSCRIBE', {
              id: `event-community-${eventId}`,
              destination: `/topic/events/${eventId}/community`,
              ack: 'auto',
            }));
          }
          if (frame.command === 'MESSAGE' && frame.body) {
            try {
              const payload = JSON.parse(frame.body);
              mergeMessages([payload.message]);
            } catch {}
          }
        });
      };
      ws.onclose = () => setConnected(false);
      ws.onerror = () => setConnected(false);
    }

    const timer = setInterval(() => {
      eventCommunityAPI.poll(eventId, lastIdRef.current)
        .then(res => mergeMessages(res.data?.data || []))
        .catch(() => {});
    }, 2500);

    return () => {
      clearInterval(timer);
      setConnected(false);
      try {
        if (ws?.readyState === WebSocket.OPEN) ws.send(stompFrame('DISCONNECT', {}));
        ws?.close();
      } catch {}
    };
  }, [eventId, user]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages.length]);

  const send = async () => {
    const text = draft.trim();
    if (!text || !canSend) return;
    setDraft('');
    try {
      const res = editingId
        ? await eventCommunityAPI.edit(eventId, editingId, text)
        : await eventCommunityAPI.send(eventId, { message: text, messageType: 'TEXT' });
      mergeMessages([res.data?.data]);
      setEditingId(null);
    } catch (err) {
      setDraft(text);
      toast.error(getApiErrorMessage(err, 'Could not send message.'));
    }
  };

  const upload = async (file) => {
    if (!file || !canSend) return;
    const form = new FormData();
    form.append('file', file);
    if (draft.trim()) form.append('message', draft.trim());
    try {
      const res = await eventCommunityAPI.upload(eventId, form);
      setDraft('');
      mergeMessages([res.data?.data]);
    } catch (err) {
      toast.error(getApiErrorMessage(err, 'Could not upload attachment.'));
    } finally {
      if (fileRef.current) fileRef.current.value = '';
    }
  };

  const loadOlder = async () => {
    if (!messages.length || loadingOlder) return;
    setLoadingOlder(true);
    try {
      const res = await eventCommunityAPI.older(eventId, firstId);
      mergeMessages(res.data?.data || []);
    } finally {
      setLoadingOlder(false);
    }
  };

  return (
    <div className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-card dark:border-slate-700 dark:bg-slate-900">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-200 bg-white px-4 py-4 dark:border-slate-700 dark:bg-slate-900">
        <div>
          <h2 className="text-lg font-extrabold text-blue-950 dark:text-white">{context?.groupName || `${event?.eventName || 'Event'} Community`}</h2>
          <p className="text-xs font-semibold text-slate-500 dark:text-slate-400">
            {organizer?.organizerName || 'Organizer'} community - {context?.readOnlyReason || 'Registration and live-event discussion'}
          </p>
        </div>
        <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-black ${context?.canSend ? 'bg-green-50 text-green-700' : 'bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300'}`}>
          {connected ? <FiWifi /> : <FiWifiOff />}
          {context?.canSend ? (connected ? 'Live' : 'Polling') : 'Read only'}
        </span>
      </div>

      {pinnedMessages.length > 0 && (
        <div className="border-b border-amber-100 bg-amber-50 px-4 py-3 dark:border-amber-900/40 dark:bg-amber-950/30">
          <div className="flex items-center gap-2 text-xs font-black uppercase text-amber-700 dark:text-amber-300"><MdPushPin /> Pinned</div>
          <div className="mt-2 space-y-2">
            {pinnedMessages.slice(0, 3).map(msg => <p key={msg.id} className="text-sm text-amber-950 dark:text-amber-100">{msg.message || msg.content}</p>)}
          </div>
        </div>
      )}

      <div className="h-[34rem] overflow-y-auto bg-slate-950 px-3 py-4 sm:px-5">
        <button type="button" onClick={loadOlder} disabled={loadingOlder || !messages.length} className="mx-auto mb-4 block rounded-full border border-slate-700 bg-slate-900 px-4 py-1.5 text-xs font-bold text-blue-100 disabled:opacity-50">
          {loadingOlder ? 'Loading...' : 'Load older messages'}
        </button>

        {loading && <div className="rounded-xl bg-slate-900 p-5 text-sm text-slate-300 shadow-sm">Loading conversation...</div>}
        {!loading && error && (
          <div className="rounded-xl border border-red-200 bg-red-50 p-5 text-sm text-red-700 dark:border-red-900/50 dark:bg-red-950/30 dark:text-red-200">
            <p className="font-bold">Community chat unavailable</p>
            <p className="mt-1">{error}</p>
            <button type="button" onClick={loadInitial} className="mt-3 inline-flex items-center gap-2 rounded-lg bg-red-600 px-3 py-1.5 text-xs font-bold text-white">
              <FiRefreshCw /> Retry
            </button>
          </div>
        )}
        {!loading && !error && messages.length === 0 && (
          <div className="rounded-xl border border-dashed border-slate-700 bg-slate-900/80 p-5 text-sm text-blue-100">
            No messages yet. The community opens for confirmed attendees, the organizer, and admins.
          </div>
        )}

        <div className="space-y-3">
          {messages.map((message, index) => (
            <MessageBubble
              key={message.id}
              message={message}
              previous={messages[index - 1]}
              mine={String(message.senderId) === String(user?.id) && String(message.senderRole) === String(user?.role)}
              canModerate={canModerate}
              onEdit={() => { setEditingId(message.id); setDraft(message.message || message.content || ''); }}
              onDelete={async () => mergeMessages([await eventCommunityAPI.delete(eventId, message.id).then(r => r.data?.data)])}
              onPin={async () => mergeMessages([await eventCommunityAPI.pin(eventId, message.id, !message.pinned).then(r => r.data?.data)])}
            />
          ))}
          <div ref={bottomRef} />
        </div>
      </div>

      <div className="border-t border-slate-800 bg-slate-950 p-3">
        {editingId && (
          <div className="mb-2 flex items-center justify-between rounded-lg bg-blue-50 px-3 py-2 text-xs font-bold text-blue-700 dark:bg-blue-950/40 dark:text-blue-200">
            Editing message
            <button type="button" onClick={() => { setEditingId(null); setDraft(''); }}>Cancel</button>
          </div>
        )}
        <div className="flex items-end gap-2 rounded-xl border border-slate-700 bg-slate-900 p-2 shadow-inner">
          <input ref={fileRef} type="file" accept="image/*,.pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document" className="hidden" onChange={e => upload(e.target.files?.[0])} />
          <button type="button" onClick={() => fileRef.current?.click()} disabled={!canSend} className="grid h-11 w-11 shrink-0 place-items-center rounded-lg border border-slate-700 bg-slate-950 text-blue-200 transition hover:border-blue-400 hover:text-white disabled:opacity-40" title="Attach file">
            <FiPaperclip />
          </button>
          <textarea
            value={draft}
            rows={1}
            onChange={(e) => setDraft(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
            className="min-h-11 flex-1 resize-none rounded-lg border border-slate-700 bg-white px-4 py-3 text-sm font-semibold text-slate-950 caret-blue-600 outline-none placeholder:text-slate-400 focus:border-blue-500 focus:ring-4 focus:ring-blue-500/20 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-400"
            placeholder={user ? (context?.readOnlyReason || 'Message the community') : 'Login to join the community'}
            disabled={!canSend}
          />
          <button onClick={send} disabled={!canSend || !draft.trim()} className="grid h-11 w-11 shrink-0 place-items-center rounded-lg bg-blue-600 text-white shadow-lg shadow-blue-950/30 transition hover:bg-blue-500 disabled:opacity-50" title="Send message">
            <FiSend />
          </button>
        </div>
      </div>
    </div>
  );
}

function MessageBubble({ message, previous, mine, canModerate, onEdit, onDelete, onPin }) {
  const date = message.sentAt || message.createdAt;
  const showDate = !previous || dayKey(previous.sentAt || previous.createdAt) !== dayKey(date);
  const canEdit = mine && !message.deleted;
  const canDelete = (mine || canModerate) && !message.deleted;

  return (
    <>
      {showDate && <div className="text-center text-[11px] font-black uppercase text-slate-400">{formatDay(date)}</div>}
      <div className={`flex gap-2 ${mine ? 'justify-end' : 'justify-start'}`}>
        {!mine && <Avatar name={message.senderName} />}
        <div className={`group max-w-[86%] rounded-xl px-3 py-2 text-sm shadow-sm sm:max-w-[74%] ${mine ? 'bg-blue-600 text-white' : 'border border-slate-200 bg-white text-slate-800 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100'}`}>
          <div className={`mb-1 flex items-center gap-2 text-[11px] font-black ${mine ? 'text-blue-100' : 'text-slate-500 dark:text-slate-400'}`}>
            <span>{message.senderName}</span>
            {message.senderRole === 'ORGANIZER' && <Badge label="Organizer" />}
            {message.senderRole === 'ADMIN' && <Badge label="Admin" />}
            {message.pinned && <MdPushPin />}
          </div>
          {renderBody(message)}
          <div className={`mt-1 flex items-center justify-end gap-2 text-[10px] ${mine ? 'text-blue-100' : 'text-slate-400'}`}>
            {message.edited && <span>edited</span>}
            {message.moderationStatus === 'REVIEW' && <span>review</span>}
            <span>{formatTime(date)}</span>
            {(canEdit || canDelete || canModerate) && (
              <span className="flex items-center gap-1 opacity-100 sm:opacity-0 sm:transition-opacity sm:group-hover:opacity-100">
                {canEdit && <button type="button" onClick={onEdit} title="Edit"><FiEdit2 /></button>}
                {canModerate && <button type="button" onClick={onPin} title={message.pinned ? 'Unpin' : 'Pin'}><MdPushPin /></button>}
                {canDelete && <button type="button" onClick={onDelete} title="Delete"><FiTrash2 /></button>}
                <FiMoreVertical />
              </span>
            )}
          </div>
        </div>
        {mine && <Avatar name={message.senderName} mine />}
      </div>
    </>
  );
}

function renderBody(message) {
  if (message.deleted) return <p className="italic opacity-70">This message was deleted</p>;
  const text = message.message || message.content;
  return (
    <div className="space-y-2">
      {message.attachmentUrl && message.messageType === 'IMAGE' && (
        <a href={message.attachmentUrl} target="_blank" rel="noopener noreferrer" className="block overflow-hidden rounded-lg">
          <img src={message.attachmentUrl} alt="Attachment" className="max-h-64 w-full object-cover" />
        </a>
      )}
      {message.attachmentUrl && message.messageType !== 'IMAGE' && (
        <a href={message.attachmentUrl} target="_blank" rel="noopener noreferrer" className="flex items-center gap-2 rounded-lg bg-black/5 px-3 py-2 font-bold dark:bg-white/10">
          <FiDownload /> Download attachment
        </a>
      )}
      {text && <p className="whitespace-pre-wrap break-words">{text}</p>}
    </div>
  );
}

function Avatar({ name, mine }) {
  const initials = String(name || 'EM').split(/\s+/).slice(0, 2).map(part => part[0]).join('').toUpperCase();
  return <div className={`mt-1 grid h-8 w-8 shrink-0 place-items-center rounded-full text-xs font-black ${mine ? 'bg-blue-100 text-blue-700' : 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-100'}`}>{initials}</div>;
}

function Badge({ label }) {
  return <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-black text-amber-700">{label}</span>;
}

function dayKey(value) {
  return value ? new Date(value).toDateString() : '';
}

function formatDay(value) {
  if (!value) return 'Today';
  return new Date(value).toLocaleDateString([], { month: 'short', day: 'numeric', year: 'numeric' });
}

function formatTime(value) {
  if (!value) return 'Sending';
  return new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function stompFrame(command, headers = {}, body = '') {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`).join('\n');
  return `${command}\n${headerLines}\n\n${body}\0`;
}

function parseFrames(raw) {
  return String(raw).split('\0').filter(Boolean).map(part => {
    const [head, ...bodyParts] = part.split('\n\n');
    const lines = head.split('\n').filter(Boolean);
    return {
      command: lines[0],
      headers: Object.fromEntries(lines.slice(1).map(line => {
        const index = line.indexOf(':');
        return index === -1 ? [line, ''] : [line.slice(0, index), line.slice(index + 1)];
      })),
      body: bodyParts.join('\n\n'),
    };
  });
}
