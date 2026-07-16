import { useEffect, useRef, useState } from 'react';
import { useQuery, useMutation, useQueryClient } from 'react-query';
import { motion, AnimatePresence } from 'framer-motion';
import { networkingAPI, directChatAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import Spinner from '../../components/common/Spinner';
import { toast } from 'react-toastify';
import {
  FiUsers, FiUserPlus, FiCheck, FiX, FiMessageSquare, FiSend,
  FiArrowLeft, FiSearch, FiInbox, FiLink2, FiMic, FiTrash2, FiRotateCcw, FiEdit2,
} from 'react-icons/fi';
import { MdSchool, MdWork } from 'react-icons/md';

// ── Avatar ──────────────────────────────────────────────────────────────────
function Avatar({ name, size = 'md', online }) {
  const s = size === 'sm' ? 'h-9 w-9 text-xs' : size === 'lg' ? 'h-14 w-14 text-xl' : 'h-11 w-11 text-sm';
  return (
    <div className="relative shrink-0">
      <div className={`${s} rounded-full bg-gradient-to-br from-teal-500 to-blue-600 flex items-center justify-center text-white font-bold`}>
        {name?.charAt(0)?.toUpperCase() || '?'}
      </div>
      {online != null && (
        <span className={`absolute -bottom-0.5 -right-0.5 h-2.5 w-2.5 rounded-full border-2 border-white ${online ? 'bg-green-400' : 'bg-slate-300'}`} />
      )}
    </div>
  );
}

// ── Suggestion Card ──────────────────────────────────────────────────────────
function SuggestionCard({ s, onConnect, loading }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-2xl border border-slate-200 p-5 flex flex-col gap-3 hover:shadow-md transition-shadow"
    >
      <div className="flex items-start gap-3">
        <Avatar name={s.name} size="lg" />
        <div className="flex-1 min-w-0">
          <p className="font-bold text-slate-900 text-sm truncate">{s.name}</p>
          {s.college && (
            <p className="text-xs text-slate-500 flex items-center gap-1 mt-0.5">
              <MdSchool className="h-3 w-3 shrink-0" />{s.college}
            </p>
          )}
          {s.department && (
            <p className="text-xs text-slate-400 flex items-center gap-1 mt-0.5">
              <MdWork className="h-3 w-3 shrink-0" />{s.department}
            </p>
          )}
        </div>
      </div>
      {s.matchReason && (
        <p className="text-[11px] text-teal-700 bg-teal-50 border border-teal-100 rounded-lg px-2.5 py-1.5 leading-relaxed">
          {s.matchReason}
        </p>
      )}
      <button
        onClick={() => onConnect(s.userId)}
        disabled={loading}
        className="w-full flex items-center justify-center gap-1.5 rounded-xl bg-teal-600 hover:bg-teal-700 text-white text-xs font-bold py-2 transition-colors disabled:opacity-50"
      >
        <FiUserPlus className="h-3.5 w-3.5" /> Connect
      </button>
    </motion.div>
  );
}

// ── Connection Card ──────────────────────────────────────────────────────────
function ConnectionCard({ c, onChat }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3 hover:shadow-md transition-shadow"
    >
      <Avatar name={c.name} online={false} />
      <div className="flex-1 min-w-0">
        <p className="font-bold text-slate-900 text-sm truncate">{c.name}</p>
        {c.college && <p className="text-xs text-slate-500 truncate">{c.college}</p>}
        {c.matchReason && (
          <p className="text-[10px] text-slate-400 mt-0.5 truncate">{c.matchReason}</p>
        )}
      </div>
      <button
        onClick={() => onChat({ userId: c.userId, name: c.name })}
        className="shrink-0 flex items-center gap-1.5 rounded-xl bg-teal-50 hover:bg-teal-100 border border-teal-200 text-teal-700 text-xs font-bold px-3 py-2 transition-colors"
      >
        <FiMessageSquare className="h-3.5 w-3.5" /> Chat
      </button>
    </motion.div>
  );
}

// ── Pending Card ────────────────────────────────────────────────────────────
function PendingCard({ p, onRespond }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }}
      className="bg-white rounded-2xl border border-slate-200 p-4 flex items-center gap-3"
    >
      <Avatar name={p.fromName} />
      <div className="flex-1 min-w-0">
        <p className="font-bold text-slate-900 text-sm">{p.fromName}</p>
        {p.matchReason && <p className="text-[11px] text-slate-400 mt-0.5">{p.matchReason}</p>}
      </div>
      <div className="flex gap-2 shrink-0">
        <button
          onClick={() => onRespond(p.connectionId, 'ACCEPTED')}
          className="flex items-center gap-1 rounded-xl bg-green-600 hover:bg-green-700 text-white text-xs font-bold px-3 py-2 transition-colors"
        >
          <FiCheck className="h-3.5 w-3.5" /> Accept
        </button>
        <button
          onClick={() => onRespond(p.connectionId, 'REJECTED')}
          className="flex items-center gap-1 rounded-xl border border-slate-200 hover:bg-red-50 text-red-500 text-xs font-bold px-3 py-2 transition-colors"
        >
          <FiX className="h-3.5 w-3.5" /> Decline
        </button>
      </div>
    </motion.div>
  );
}

// ── Empty State ──────────────────────────────────────────────────────────────
function Empty({ icon, title, desc }) {
  return (
    <div className="rounded-2xl border border-dashed border-slate-200 bg-white py-16 text-center">
      <div className="mx-auto mb-3 h-12 w-12 rounded-2xl bg-slate-50 flex items-center justify-center text-slate-300">
        {icon}
      </div>
      <p className="font-bold text-slate-600">{title}</p>
      <p className="text-sm text-slate-400 mt-1 max-w-xs mx-auto">{desc}</p>
    </div>
  );
}

// ── Skeleton ─────────────────────────────────────────────────────────────────
function Skeleton() {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      {[1, 2, 3, 4, 5, 6].map(i => (
        <div key={i} className="bg-white rounded-2xl border border-slate-200 p-5 animate-pulse space-y-3">
          <div className="flex gap-3">
            <div className="h-14 w-14 rounded-full bg-slate-200" />
            <div className="flex-1 space-y-2 pt-1">
              <div className="h-3 bg-slate-200 rounded w-3/4" />
              <div className="h-2.5 bg-slate-100 rounded w-1/2" />
            </div>
          </div>
          <div className="h-8 bg-slate-100 rounded-xl" />
          <div className="h-8 bg-slate-200 rounded-xl" />
        </div>
      ))}
    </div>
  );
}

// ── Main Component ────────────────────────────────────────────────────────────
export default function Networking() {
  const qc = useQueryClient();
  const [tab,      setTab]      = useState('suggestions');
  const [search,   setSearch]   = useState('');
  const [chatWith, setChatWith] = useState(null);

  const { data: suggestions = [], isLoading: sLoading } = useQuery(
    'net-suggestions',
    () => networkingAPI.suggestions().then(r => r.data?.data || []),
    { staleTime: 5 * 60_000 }
  );
  const { data: connections = [], isLoading: cLoading } = useQuery(
    'net-connections',
    () => networkingAPI.myConnections().then(r => r.data?.data || []),
    { staleTime: 60_000 }
  );
  const { data: pending = [], isLoading: pLoading } = useQuery(
    'net-pending',
    () => networkingAPI.pending().then(r => r.data?.data || []),
    { staleTime: 30_000 }
  );

  const connectMut = useMutation(
    id => networkingAPI.connect(id).then(r => r.data),
    {
      onSuccess: (res) => {
        toast.success('Connection request sent!');
        qc.invalidateQueries('net-suggestions');
        qc.invalidateQueries('net-pending');
      },
      onError: e => toast.error(e?.response?.data?.message || 'Already connected or request pending'),
    }
  );

  const respondMut = useMutation(
    ({ id, status }) => networkingAPI.respond(id, status).then(r => r.data?.data),
    {
      onSuccess: (data, { id, status }) => {
        toast.success(status === 'ACCEPTED' ? '✓ Connected!' : 'Request declined');
        qc.setQueryData('net-pending', (old = []) => old.filter(req => req.connectionId !== id));
        if (status === 'ACCEPTED' && data?.connection) {
          qc.setQueryData('net-connections', (old = []) => {
            const exists = old.some(conn => conn.connectionId === data.connection.connectionId);
            return exists ? old : [data.connection, ...old];
          });
          setTab('connections');
        }
        qc.invalidateQueries('net-pending');
        qc.invalidateQueries('net-connections');
      },
    }
  );

  const q = search.toLowerCase().trim();
  const filteredSuggestions = q
    ? suggestions.filter(s =>
        [s.name, s.college, s.department, s.matchReason]
          .some(v => v?.toLowerCase().includes(q)))
    : suggestions;

  const filteredConnections = q
    ? connections.filter(c =>
        [c.name, c.college, c.matchReason].some(v => v?.toLowerCase().includes(q)))
    : connections;

  const tabs = [
    { key: 'suggestions', label: 'Discover',   icon: <FiUsers className="h-4 w-4" />,   count: suggestions.length },
    { key: 'pending',     label: 'Requests',   icon: <FiInbox className="h-4 w-4" />,   count: pending.length },
    { key: 'connections', label: 'My Network', icon: <FiLink2 className="h-4 w-4" />,   count: connections.length },
  ];

  return (
    <div className="min-h-screen bg-slate-50">
      {/* Hero header */}
      <div className="bg-gradient-to-br from-teal-700 via-teal-800 to-blue-900 px-4 pt-10 pb-14 text-white">
        <div className="mx-auto max-w-5xl">
          <p className="text-xs font-black uppercase tracking-[0.2em] text-teal-300 mb-1">Professional Networking</p>
          <h1 className="text-3xl font-extrabold" style={{ fontFamily: 'Space Grotesk,sans-serif' }}>
            Smart Networking
          </h1>
          <p className="text-sm text-teal-200 mt-1 mb-6">
            Connect with peers based on shared events, skills, and department
          </p>

          {/* Search */}
          <div className="relative max-w-lg">
            <FiSearch className="absolute left-4 top-1/2 -translate-y-1/2 h-4 w-4 text-teal-300" />
            <input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Search by name, college, or skill…"
              className="w-full bg-white/10 backdrop-blur border border-white/20 rounded-xl pl-11 pr-4 py-3 text-sm text-white placeholder:text-teal-300 outline-none focus:bg-white/20 transition"
            />
          </div>
        </div>
      </div>

      <div className="mx-auto max-w-5xl px-4 -mt-6 pb-16">
        {/* Stats row */}
        <div className="grid grid-cols-3 gap-3 mb-6">
          {[
            { label: 'Suggestions', value: suggestions.length, color: 'text-teal-700', bg: 'bg-teal-50 border-teal-100' },
            { label: 'Pending',     value: pending.length,     color: 'text-amber-700', bg: 'bg-amber-50 border-amber-100' },
            { label: 'Connected',   value: connections.length, color: 'text-blue-700',  bg: 'bg-blue-50 border-blue-100' },
          ].map(stat => (
            <div key={stat.label} className={`bg-white rounded-2xl border p-4 text-center ${stat.bg}`}>
              <p className={`text-2xl font-extrabold ${stat.color}`}>{stat.value}</p>
              <p className="text-xs text-slate-500 font-semibold mt-0.5">{stat.label}</p>
            </div>
          ))}
        </div>

        {/* Tabs */}
        <div className="flex gap-1 p-1 bg-white rounded-2xl border border-slate-200 mb-6 w-fit shadow-sm">
          {tabs.map(t => (
            <button
              key={t.key}
              onClick={() => setTab(t.key)}
              className={`flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-bold transition-all ${
                tab === t.key
                  ? 'bg-teal-600 text-white shadow'
                  : 'text-slate-600 hover:bg-slate-50'
              }`}
            >
              {t.icon}
              {t.label}
              {t.count > 0 && (
                <span className={`rounded-full px-1.5 py-0.5 text-[10px] font-bold ${
                  tab === t.key ? 'bg-white/25 text-white' : 'bg-teal-100 text-teal-700'
                }`}>{t.count}</span>
              )}
            </button>
          ))}
        </div>

        <AnimatePresence mode="wait">
          {/* Suggestions */}
          {tab === 'suggestions' && (
            <motion.div key="suggestions"
              initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.18 }}>
              {sLoading ? <Skeleton /> :
               filteredSuggestions.length === 0 ? (
                <Empty
                  icon={<FiUsers className="h-6 w-6" />}
                  title={search ? 'No results' : 'No suggestions yet'}
                  desc={search ? 'Try different keywords.' : 'Register for more events and update your interests to get matched with peers.'}
                />
               ) : (
                <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                  {filteredSuggestions.map(s => (
                    <SuggestionCard
                      key={s.userId}
                      s={s}
                      onConnect={id => connectMut.mutate(id)}
                      loading={connectMut.isLoading}
                    />
                  ))}
                </div>
               )}
            </motion.div>
          )}

          {/* Pending */}
          {tab === 'pending' && (
            <motion.div key="pending"
              initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.18 }}>
              {pLoading ? (
                <div className="space-y-3">{[1,2,3].map(i => <div key={i} className="h-20 bg-white rounded-2xl border animate-pulse"/>)}</div>
              ) : pending.length === 0 ? (
                <Empty
                  icon={<FiInbox className="h-6 w-6" />}
                  title="No pending requests"
                  desc="When someone sends you a connection request it will appear here."
                />
              ) : (
                <div className="space-y-3">
                  {pending.map(p => (
                    <PendingCard
                      key={p.connectionId}
                      p={p}
                      onRespond={(id, status) => respondMut.mutate({ id, status })}
                    />
                  ))}
                </div>
              )}
            </motion.div>
          )}

          {/* Connections */}
          {tab === 'connections' && (
            <motion.div key="connections"
              initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -8 }}
              transition={{ duration: 0.18 }}>
              {cLoading ? <Skeleton /> :
               filteredConnections.length === 0 ? (
                <Empty
                  icon={<FiLink2 className="h-6 w-6" />}
                  title={search ? 'No results' : 'No connections yet'}
                  desc={search ? 'Try different keywords.' : 'Accept suggestions to grow your network.'}
                />
               ) : (
                <div className="grid gap-3 sm:grid-cols-2">
                  {filteredConnections.map(c => (
                    <ConnectionCard key={c.connectionId} c={c} onChat={setChatWith} />
                  ))}
                </div>
               )}
            </motion.div>
          )}
        </AnimatePresence>
      </div>

      {/* Chat overlay */}
      <AnimatePresence>
        {chatWith && (
          <ChatWindow
            otherId={chatWith.userId}
            otherName={chatWith.name}
            onClose={() => setChatWith(null)}
          />
        )}
      </AnimatePresence>
    </div>
  );
}

// ── Chat Window ───────────────────────────────────────────────────────────────
function ChatWindow({ otherId, otherName, onClose }) {
  const { user } = useAuth();
  const [input, setInput]         = useState('');
  const [lastId, setLastId]       = useState(0);
  const [messages, setMessages]   = useState([]);
  const [online, setOnline]       = useState(false);
  const [typing, setTyping]       = useState(false);
  const [editing, setEditing]     = useState(null);
  const [recording, setRecording] = useState(false);
  const [audioBlob, setAudioBlob] = useState(null);
  const [audioDuration, setAudioDuration]   = useState(0);
  const [audioPreviewUrl, setAudioPreviewUrl] = useState(null);
  const bottomRef    = useRef(null);
  const mediaRef     = useRef(null);
  const chunksRef    = useRef([]);
  const timerRef     = useRef(null);
  const typingTimerRef = useRef(null);

  const mergeMessages = (incoming) => {
    setMessages(prev => {
      const byId = new Map(prev.map(m => [m.id, m]));
      incoming.filter(Boolean).forEach(m => byId.set(m.id, { ...byId.get(m.id), ...m }));
      const next = Array.from(byId.values()).sort((a, b) => Number(a.id) - Number(b.id));
      if (next.length) setLastId(next[next.length - 1].id);
      return next;
    });
  };

  const { data: history = [], refetch } = useQuery(
    ['dm-conversation', otherId],
    () => directChatAPI.conversation(otherId).then(r => {
      const msgs = r.data?.data ?? [];
      setMessages(msgs);
      if (msgs.length) setLastId(msgs[msgs.length - 1].id);
      return msgs;
    }),
    { staleTime: 0, refetchOnWindowFocus: false }
  );

  const pollCb = useRef(null);
  pollCb.current = async () => {
    try {
      const res = await directChatAPI.poll(otherId, lastId);
      const newMsgs = res.data?.data ?? [];
      if (newMsgs.length) mergeMessages(newMsgs);
    } catch {}
  };

  useEffect(() => {
    const timer = setInterval(() => pollCb.current?.(), 2500);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    const token = localStorage.getItem('eb_token');
    if (!token || !user?.id) return undefined;
    const ws = new WebSocket(directChatAPI.wsUrl());
    ws.onopen = () => ws.send(stompFrame('CONNECT', {
      Authorization: `Bearer ${token}`,
      'accept-version': '1.2',
      'heart-beat': '10000,10000',
    }));
    ws.onmessage = evt => parseFrames(evt.data).forEach(frame => {
      if (frame.command === 'CONNECTED') {
        setOnline(true);
        ws.send(stompFrame('SUBSCRIBE', {
          id: `dm-${user.id}`,
          destination: `/queue/users/${user.id}/chat`,
          ack: 'auto',
        }));
      }
      if (frame.command === 'MESSAGE' && frame.body) {
        try {
          const payload = JSON.parse(frame.body);
          const msg = payload.message;
          const sameConversation = msg && (String(msg.senderId) === String(otherId) || String(msg.receiverId) === String(otherId));
          if (!sameConversation) return;
          if (payload.action === 'deletedForMe') setMessages(prev => prev.filter(m => String(m.id) !== String(msg.id)));
          else mergeMessages([msg]);
          refetch();
        } catch {}
      }
    });
    ws.onclose = () => setOnline(false);
    ws.onerror = () => setOnline(false);
    return () => {
      try {
        if (ws.readyState === WebSocket.OPEN) ws.send(stompFrame('DISCONNECT', {}));
        ws.close();
      } catch {}
    };
  }, [otherId, user?.id]);

  const prevLen = useRef(0);
  if (messages.length !== prevLen.current) {
    prevLen.current = messages.length;
    setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' }), 50);
  }

  const sendMut = useMutation(
    () => editing ? directChatAPI.edit(editing.id, input.trim()) : directChatAPI.send(otherId, input.trim()),
    {
      onSuccess: (res) => { setInput(''); setEditing(null); mergeMessages([res.data?.data]); },
      onError: e => toast.error(e?.response?.data?.message || 'Failed to send'),
    }
  );

  const sendVoiceMut = useMutation(
    ({ blob, duration }) => {
      const form = new FormData();
      form.append('audio', blob, 'voice.webm');
      form.append('duration', String(duration));
      return directChatAPI.sendVoice(otherId, form);
    },
    {
      onSuccess: () => {
        setAudioBlob(null);
        setAudioPreviewUrl(null);
        setAudioDuration(0);
        mergeMessages([res?.data?.data]);
        toast.success('Voice message sent');
      },
      onError: e => toast.error(e?.response?.data?.message || 'Failed to send voice'),
    }
  );

  const startRecording = async () => {
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const mr = new MediaRecorder(stream);
      chunksRef.current = [];
      mr.ondataavailable = e => chunksRef.current.push(e.data);
      mr.onstop = () => {
        stream.getTracks().forEach(t => t.stop());
        const blob = new Blob(chunksRef.current, { type: 'audio/webm' });
        const url  = URL.createObjectURL(blob);
        setAudioBlob(blob);
        setAudioPreviewUrl(url);
        setRecording(false);
        clearInterval(timerRef.current);
      };
      mr.start();
      mediaRef.current = mr;
      setRecording(true);
      setAudioDuration(0);
      timerRef.current = setInterval(() => setAudioDuration(d => d + 1), 1000);
    } catch {
      toast.error('Microphone access denied');
    }
  };

  const stopRecording = () => { mediaRef.current?.stop(); };
  const cancelRecording = () => {
    mediaRef.current?.stop();
    setAudioBlob(null);
    setAudioPreviewUrl(null);
    setAudioDuration(0);
    clearInterval(timerRef.current);
    setRecording(false);
  };

  const send = () => { if (input.trim() && !sendMut.isLoading) sendMut.mutate(); };
  const deleteForMe = async (id) => {
    try {
      await directChatAPI.deleteForMe(id);
      setMessages(prev => prev.filter(m => m.id !== id));
    } catch (e) { toast.error(e?.response?.data?.message || 'Could not delete message'); }
  };
  const unsend = async (id) => {
    try {
      const res = await directChatAPI.unsend(id);
      mergeMessages([res.data?.data]);
    } catch (e) { toast.error(e?.response?.data?.message || 'Could not unsend message'); }
  };

  const fmtTime = s => `${Math.floor(s / 60)}:${String(s % 60).padStart(2, '0')}`;

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95, y: 20 }}
      animate={{ opacity: 1, scale: 1, y: 0 }}
      exit={{ opacity: 0, scale: 0.95, y: 20 }}
      transition={{ duration: 0.18 }}
      className="fixed bottom-6 right-6 z-50 flex flex-col w-[360px] h-[520px] bg-white rounded-2xl border border-slate-200 shadow-2xl overflow-hidden"
    >
      {/* Header */}
      <div className="flex items-center gap-3 px-4 py-3 bg-gradient-to-r from-teal-600 to-teal-700 text-white shrink-0">
        <button onClick={onClose} className="text-white/70 hover:text-white transition-colors">
          <FiArrowLeft className="h-4 w-4" />
        </button>
        <div className="h-8 w-8 rounded-full bg-white/20 flex items-center justify-center font-bold text-sm shrink-0">
          {otherName?.charAt(0)?.toUpperCase()}
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-sm font-bold truncate">{otherName}</p>
          <p className="text-[10px] text-teal-200">{online ? 'Online' : 'Syncing'} · Direct Message</p>
        </div>
        <span className="h-2 w-2 rounded-full bg-green-400 animate-pulse shrink-0" />
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-2 bg-slate-50">
        {messages.length === 0 && (
          <div className="flex flex-col items-center pt-10 text-slate-400">
            <FiMessageSquare className="h-10 w-10 mb-3 opacity-30" />
            <p className="text-sm font-semibold">No messages yet</p>
            <p className="text-xs mt-1">Say hi! 👋</p>
          </div>
        )}
        {history.map(msg => {
          const isMine = msg.senderId !== otherId;
          return (
            <div key={msg.id} className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
              <div className={`max-w-[78%] rounded-2xl px-3 py-2 text-sm shadow-sm ${
                isMine ? 'bg-teal-600 text-white rounded-br-sm' : 'bg-white border border-slate-200 text-slate-800 rounded-bl-sm'
              }`}>
                {msg.messageType === 'VOICE' ? (
                  <VoicePlayer url={msg.voiceUrl} duration={msg.voiceDurationSeconds} mine={isMine} />
                ) : (
                  <p className="whitespace-pre-wrap break-words">{msg.content}</p>
                )}
                <p className={`text-[10px] mt-1 flex items-center justify-end gap-1 ${isMine ? 'text-teal-200' : 'text-slate-400'}`}>
                  {msg.sentAt ? new Date(msg.sentAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}
                  {isMine && <span>{msg.read ? '✓✓' : '✓'}</span>}
                </p>
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      {/* Voice preview bar */}
      {audioPreviewUrl && !recording && (
        <div className="px-3 py-2 border-t border-slate-100 bg-teal-50 flex items-center gap-2 shrink-0">
          <audio src={audioPreviewUrl} controls className="flex-1 h-8" style={{ width: '100%', height: '32px' }} />
          <button onClick={() => sendVoiceMut.mutate({ blob: audioBlob, duration: audioDuration })}
            disabled={sendVoiceMut.isLoading}
            className="shrink-0 rounded-xl bg-teal-600 text-white text-xs font-bold px-3 py-1.5 hover:bg-teal-700 disabled:opacity-50">
            {sendVoiceMut.isLoading ? '…' : 'Send'}
          </button>
          <button onClick={() => { setAudioBlob(null); setAudioPreviewUrl(null); }}
            className="shrink-0 text-slate-400 hover:text-red-500 transition-colors">
            <FiX className="h-4 w-4" />
          </button>
        </div>
      )}

      {/* Recording indicator */}
      {recording && (
        <div className="px-3 py-2 border-t border-slate-100 bg-red-50 flex items-center gap-2 shrink-0">
          <span className="h-2 w-2 rounded-full bg-red-500 animate-pulse" />
          <span className="text-xs font-bold text-red-600 flex-1">Recording {fmtTime(audioDuration)}</span>
          <button onClick={stopRecording} className="rounded-xl bg-red-600 text-white text-xs font-bold px-3 py-1.5">Stop</button>
          <button onClick={cancelRecording} className="text-slate-400 hover:text-red-500 transition-colors"><FiX className="h-4 w-4" /></button>
        </div>
      )}

      {/* Input */}
      {!audioPreviewUrl && !recording && (
        <div className="flex items-end gap-2 px-3 py-3 border-t border-slate-100 bg-white shrink-0">
          <textarea
            value={input}
            onChange={e => { setInput(e.target.value); e.target.style.height = 'auto'; e.target.style.height = Math.min(e.target.scrollHeight, 96) + 'px'; }}
            onKeyDown={e => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); } }}
            rows={1}
            placeholder="Type a message…"
            className="flex-1 resize-none rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-sm outline-none focus:border-teal-400 focus:bg-white transition-colors"
            style={{ minHeight: '40px', maxHeight: '96px' }}
          />
          {/* Voice record button */}
          <button onClick={startRecording} title="Record voice message"
            className="flex h-10 w-10 items-center justify-center rounded-xl border border-slate-200 bg-slate-50 text-slate-500 hover:bg-red-50 hover:text-red-500 hover:border-red-200 transition-colors shrink-0">
            <FiMic className="h-4 w-4" />
          </button>
          <button onClick={send} disabled={!input.trim() || sendMut.isLoading}
            className="flex h-10 w-10 items-center justify-center rounded-xl bg-teal-600 text-white hover:bg-teal-700 disabled:opacity-40 transition-colors shrink-0">
            <FiSend className="h-4 w-4" />
          </button>
        </div>
      )}
    </motion.div>
  );
}

// ── Voice message player ──────────────────────────────────────────────────────
function VoicePlayer({ url, duration, mine }) {
  const [playing, setPlaying] = useState(false);
  const [progress, setProgress] = useState(0);
  const [currentTime, setCurrentTime] = useState(0);
  const audioRef = useRef(null);

  const toggle = () => {
    if (!audioRef.current) return;
    if (playing) { audioRef.current.pause(); setPlaying(false); }
    else { audioRef.current.play(); setPlaying(true); }
  };

  const fmtTime = s => isNaN(s) ? '0:00' : `${Math.floor(s / 60)}:${String(Math.floor(s % 60)).padStart(2, '0')}`;

  return (
    <div className="flex items-center gap-2 min-w-[180px]">
      <audio ref={audioRef} src={url} preload="metadata"
        onTimeUpdate={e => { setCurrentTime(e.target.currentTime); setProgress(e.target.duration ? (e.target.currentTime / e.target.duration) * 100 : 0); }}
        onEnded={() => { setPlaying(false); setProgress(0); setCurrentTime(0); }} />
      <button onClick={toggle}
        className={`shrink-0 h-8 w-8 rounded-full flex items-center justify-center transition-colors ${mine ? 'bg-white/20 hover:bg-white/30' : 'bg-teal-100 hover:bg-teal-200'}`}>
        {playing
          ? <span className={`text-[8px] font-black ${mine ? 'text-white' : 'text-teal-700'}`}>❚❚</span>
          : <span className={`text-[9px] font-black ${mine ? 'text-white' : 'text-teal-700'}`}>▶</span>}
      </button>
      <div className="flex-1 space-y-1">
        <div className="relative h-1 rounded-full bg-white/30 overflow-hidden cursor-pointer"
          onClick={e => {
            if (!audioRef.current || !audioRef.current.duration) return;
            const rect = e.currentTarget.getBoundingClientRect();
            const pct = (e.clientX - rect.left) / rect.width;
            audioRef.current.currentTime = pct * audioRef.current.duration;
          }}>
          <div className={`h-full rounded-full transition-all ${mine ? 'bg-white' : 'bg-teal-500'}`}
            style={{ width: `${progress}%` }} />
        </div>
        <p className={`text-[10px] ${mine ? 'text-white/70' : 'text-slate-400'}`}>
          {playing ? fmtTime(currentTime) : fmtTime(duration)} · 🎤
        </p>
      </div>
    </div>
  );
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
