import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useMutation } from 'react-query';
import { useNavigate } from 'react-router-dom';
import { chatbotAPI } from '../../services/api';
import { useAuth } from '../../context/AuthContext';
import {
  FiArrowDown, FiCheck, FiCopy, FiMessageCircle,
  FiRefreshCw, FiRotateCcw, FiSend, FiX,
} from 'react-icons/fi';

/* ── Simple AI logo — star/sparkle mark ────────────────────── */
function AILogo({ size = 20 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="11" fill="#1D4ED8" />
      {/* 4-point star */}
      <path
        d="M12 4 L13.2 10.8 L20 12 L13.2 13.2 L12 20 L10.8 13.2 L4 12 L10.8 10.8 Z"
        fill="white"
      />
    </svg>
  );
}

/* ── Constants ───────────────────────────────────────────────── */
const SUGGESTIONS = [
  { label: 'Upcoming Events',   prompt: 'Show me upcoming events' },
  { label: 'My Bookings',       prompt: 'Show my upcoming bookings' },
  { label: 'Refund Status',     prompt: 'How can I check my refund status?' },
  { label: 'Payment Help',      prompt: 'I need help with event payments' },
  { label: 'Organizer Tools',   prompt: 'How do I create an event as an organizer?' },
  { label: 'Contact Support',   prompt: 'How do I contact support?' },
];

const QUICK_LINKS = [
  { label: 'Events',   path: '/events' },
  { label: 'Bookings', path: '/bookings' },
  { label: 'Payments', path: '/payments' },
  { label: 'Refunds',  path: '/refunds' },
  { label: 'Help',     path: '/help' },
];

const WELCOME = {
  role: 'assistant',
  content: "Hi! I'm your **Event AI Assistant**. I can help with events, bookings, refunds, payments, organizer tools, and FAQs.\n\nWhat would you like to know?",
  timestamp: new Date().toISOString(),
};

/* ── Main component ───────────────────────────────────────────── */
export default function ChatbotWidget({ preloadQuestion }) {
  const navigate  = useNavigate();
  const { user }  = useAuth();
  const [open, setOpen]   = useState(false);
  const [input, setInput] = useState('');
  const [showJump, setShowJump] = useState(false);
  const [lastPrompt, setLastPrompt] = useState('');
  const [copiedId, setCopiedId]     = useState('');
  const scrollRef  = useRef(null);
  const bottomRef  = useRef(null);
  const inputRef   = useRef(null);
  const askResetRef = useRef(null);

  const [messages, setMessages] = useState(() => {
    try { return JSON.parse(sessionStorage.getItem('eb_ai_chat') || 'null') || [WELCOME]; }
    catch { return [WELCOME]; }
  });

  const unread = !open && messages.length > 1;

  /* persist + scroll */
  useEffect(() => {
    sessionStorage.setItem('eb_ai_chat', JSON.stringify(messages.slice(-40)));
    if (open) bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages, open]);

  /* focus input when opened */
  useEffect(() => {
    if (open) setTimeout(() => inputRef.current?.focus(), 150);
  }, [open]);

  /* preload from external caller (e.g. FAQ "Ask AI") */
  useEffect(() => {
    if (preloadQuestion && open) {
      setInput(preloadQuestion);
      setTimeout(() => inputRef.current?.focus(), 200);
    }
  }, [preloadQuestion, open]);

  /* ── API call ─────────────────────────────────────────────── */
  const ask = useMutation(
    ({ message, history }) => chatbotAPI.ask(message, history).then(r => r.data?.data),
    {
      onSuccess: data => {
        setMessages(m => [...m, {
          role:      'assistant',
          content:   data?.answer || 'I can help with college event booking questions.',
          timestamp: data?.timestamp || new Date().toISOString(),
          actions:   data?.actions || [],
        }]);
      },
      onError: err => {
        const is401 = err?.response?.status === 401;
        setMessages(m => [...m, {
          role:      'assistant',
          content:   is401
            ? 'Please **log in** to ask about personal bookings, refunds, or payments.'
            : "I couldn't reach the assistant right now. Please try again or visit the Help page.",
          timestamp: new Date().toISOString(),
          actions:   is401 ? [{ label: 'Login', path: '/login' }] : [{ label: 'Help', path: '/help' }],
          error:     !is401,
        }]);
      },
    }
  );
  askResetRef.current = ask.reset;

  const resetConversation = useCallback(() => {
    askResetRef.current?.();
    setLastPrompt('');
    setMessages([{ ...WELCOME, timestamp: new Date().toISOString() }]);
    sessionStorage.removeItem('eb_ai_chat');
  }, []);

  useEffect(() => {
    resetConversation();
  }, [resetConversation, user?.id, user?.role]);

  useEffect(() => {
    window.addEventListener('eb:chat-refresh', resetConversation);
    return () => window.removeEventListener('eb:chat-refresh', resetConversation);
  }, [resetConversation]);

  const history = useMemo(() =>
    messages.filter(m => m.role === 'user' || m.role === 'assistant')
      .slice(-8).map(m => ({ role: m.role, content: m.content })),
    [messages]
  );

  const send = useCallback((text = input) => {
    const clean = text.trim();
    if (!clean || ask.isLoading) return;
    setLastPrompt(clean);
    setMessages(m => [...m, { role: 'user', content: clean, timestamp: new Date().toISOString() }]);
    setInput('');
    ask.mutate({ message: clean, history });
  }, [input, ask, history]);

  const clear = () => {
    resetConversation();
  };

  const copy = async (id, text) => {
    await navigator.clipboard?.writeText(text);
    setCopiedId(id);
    setTimeout(() => setCopiedId(''), 1500);
  };

  return (
    <>
      {/* ── Floating button ─────────────────────────────────── */}
      {!open && (
        <button
          onClick={() => setOpen(true)}
          className="group fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full border border-slate-200 bg-white shadow-lg transition hover:shadow-xl focus:outline-none focus:ring-2 focus:ring-blue-300"
          aria-label="Open AI Assistant"
        >
          {/* Sparkle / twinkle icon */}
          <style>{`
            @keyframes ai-twinkle {
              0%,100% { opacity:1;  transform:scale(1)    rotate(0deg); }
              25%     { opacity:.5; transform:scale(0.85) rotate(-15deg); }
              50%     { opacity:1;  transform:scale(1.25) rotate(20deg); }
              75%     { opacity:.6; transform:scale(0.9)  rotate(-10deg); }
            }
            .ai-sparkle { animation: ai-twinkle 2s ease-in-out infinite; }
          `}</style>
          <svg
            className="ai-sparkle"
            width="26" height="26" viewBox="0 0 24 24"
            fill="none" aria-hidden="true"
          >
            {/* Big 4-point star */}
            <path
              d="M12 2 L13.5 9.5 L21 12 L13.5 14.5 L12 22 L10.5 14.5 L3 12 L10.5 9.5 Z"
              fill="#1D4ED8"
            />
            {/* Small accent star — top-right */}
            <path
              d="M19 3 L19.7 5.3 L22 6 L19.7 6.7 L19 9 L18.3 6.7 L16 6 L18.3 5.3 Z"
              fill="#93C5FD"
            />
          </svg>
          <span className="pointer-events-none absolute bottom-full right-0 mb-2 whitespace-nowrap rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 opacity-0 shadow-md transition-opacity group-hover:opacity-100">
            AI Assistant
          </span>
        </button>
      )}

      {/* ── Chat panel ──────────────────────────────────────── */}
      {open && (
        <div
          className="fixed bottom-6 right-6 z-50 flex h-[min(680px,calc(100vh-3rem))] w-[calc(100vw-2rem)] max-w-[420px] flex-col overflow-hidden rounded-lg border border-slate-200 bg-white shadow-lg"
          role="dialog"
          aria-label="AI Assistant"
        >
          {/* Header — same style as FAQ section headings */}
          <div className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-3">
            <div className="flex items-center gap-2.5">
              <AILogo size={24} />
              <div>
                <p className="text-sm font-bold text-slate-900">Event AI Assistant</p>
                <p className="text-xs text-slate-500">Ask about events, bookings, refunds</p>
              </div>
            </div>
            <div className="flex items-center gap-1">
              <button onClick={clear} title="Clear chat" aria-label="Clear chat"
                className="rounded p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 focus:outline-none focus:ring-2 focus:ring-blue-300">
                <FiRefreshCw className="h-4 w-4" />
              </button>
              <button onClick={() => setOpen(false)} title="Close" aria-label="Close chat"
                className="rounded p-1.5 text-slate-400 transition hover:bg-slate-100 hover:text-slate-600 focus:outline-none focus:ring-2 focus:ring-blue-300">
                <FiX className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Message area */}
          <div
            ref={scrollRef}
            onScroll={e => {
              const el = e.currentTarget;
              setShowJump(el.scrollHeight - el.scrollTop - el.clientHeight > 180);
            }}
            className="relative min-h-0 flex-1 overflow-y-auto bg-white p-4"
          >
            {/* Welcome */}
            {messages.length <= 1 && (
              <div className="mb-4 rounded-lg border border-slate-200 bg-white p-4">
                <div className="flex items-center gap-2 text-sm font-bold text-slate-900">
                  <FiMessageCircle className="h-4 w-4 text-blue-700" />
                  How can I help?
                </div>
                <p className="mt-1 text-sm text-slate-500">Ask about events, bookings, refunds, payments, or organizer tools.</p>
              </div>
            )}

            {/* Messages */}
            <div className="space-y-4">
              {messages.map((m, i) => {
                const id = `${m.timestamp}-${i}`;
                return (
                  <ChatMessage
                    key={id} id={id} message={m}
                    copied={copiedId === id}
                    onCopy={() => copy(id, m.content)}
                    onRetry={() => send(lastPrompt)}
                    onAction={path => { setOpen(false); navigate(path); }}
                  />
                );
              })}
              {ask.isLoading && <TypingBubble />}
            </div>
            <div ref={bottomRef} className="h-1" />

            {/* Jump to bottom */}
            {showJump && (
              <button
                onClick={() => bottomRef.current?.scrollIntoView({ behavior: 'smooth' })}
                className="absolute bottom-4 left-1/2 -translate-x-1/2 flex items-center gap-1.5 rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-600 shadow focus:outline-none"
              >
                <FiArrowDown className="h-3.5 w-3.5" /> Latest
              </button>
            )}
          </div>

          {/* Bottom panel */}
          <div className="border-t border-slate-200 bg-white">
            {/* Suggestions — same style as FAQ categories */}
            <div className="border-b border-slate-200 p-3">
              <p className="mb-2 text-xs font-semibold text-slate-500">Suggested questions</p>
              <div className="flex flex-wrap gap-1.5">
                {SUGGESTIONS.map(s => (
                  <button
                    key={s.label}
                    onClick={() => send(s.prompt)}
                    className="rounded border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-300"
                  >
                    {s.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Quick nav links */}
            <div className="flex flex-wrap gap-1.5 border-b border-slate-200 px-3 py-2">
              {QUICK_LINKS.map(a => (
                <button
                  key={a.label}
                  onClick={() => { setOpen(false); navigate(a.path); }}
                  className="rounded border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-slate-500 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-300"
                >
                  {a.label}
                </button>
              ))}
            </div>

            {/* Input */}
            <div className="p-3">
              <form onSubmit={e => { e.preventDefault(); send(); }} className="flex items-end gap-2">
                <label className="sr-only" htmlFor="ai-chat-input">Ask AI Assistant</label>
                <div className="relative min-w-0 flex-1">
                  <textarea
                    ref={inputRef}
                    id="ai-chat-input"
                    value={input}
                    onChange={e => {
                      setInput(e.target.value);
                      e.target.style.height = 'auto';
                      e.target.style.height = Math.min(e.target.scrollHeight, 108) + 'px';
                    }}
                    onKeyDown={e => {
                      if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); }
                    }}
                    maxLength={1200}
                    rows={1}
                    placeholder="Ask about tickets, refunds, payments…"
                    className="max-h-28 min-h-[40px] w-full resize-none rounded border border-slate-200 bg-white px-3 py-2.5 pr-8 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                  />
                  {input && (
                    <button type="button" onClick={() => setInput('')} aria-label="Clear input"
                      className="absolute right-2 top-2.5 text-slate-400 hover:text-slate-600 focus:outline-none">
                      <FiX className="h-3.5 w-3.5" />
                    </button>
                  )}
                </div>
                <button
                  type="submit"
                  disabled={ask.isLoading || !input.trim()}
                  className="flex h-10 w-10 shrink-0 items-center justify-center rounded border border-slate-200 bg-blue-700 text-white transition hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-40 focus:outline-none focus:ring-2 focus:ring-blue-300"
                  aria-label="Send"
                >
                  {ask.isLoading
                    ? <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                    : <FiSend className="h-4 w-4" />}
                </button>
              </form>
              <p className="mt-1 text-right text-[10px] text-slate-400">{input.length}/1200 · Shift+Enter for new line</p>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

/* ── Message bubble ───────────────────────────────────────────── */
function ChatMessage({ id, message, copied, onCopy, onRetry, onAction }) {
  const isUser = message.role === 'user';
  return (
    <div className={`group flex gap-2 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}>

      {/* Avatar */}
      {!isUser && (
        <div className="mt-0.5 shrink-0">
          <AILogo size={24} />
        </div>
      )}

      <div className={`flex max-w-[84%] flex-col ${isUser ? 'items-end' : 'items-start'}`}>
        {/* Bubble — FAQ card style */}
        <div className={`rounded-lg border px-3.5 py-2.5 text-sm leading-relaxed
          ${isUser
            ? 'border-blue-200 bg-blue-700 text-white'
            : 'border-slate-200 bg-white text-slate-700'}`}>
          <MarkdownRenderer text={message.content} isUser={isUser} />
        </div>

        {/* Meta */}
        <div className={`mt-1 flex items-center gap-2 text-[11px] text-slate-400 ${isUser ? 'flex-row-reverse' : ''}`}>
          <span>{fmtTime(message.timestamp)}</span>
          {!isUser && (
            <span className="flex items-center gap-1 opacity-0 transition-opacity group-hover:opacity-100">
              <button onClick={onCopy} title="Copy"
                className="flex items-center gap-1 rounded px-1.5 py-0.5 hover:bg-slate-100 focus:outline-none">
                {copied
                  ? <><FiCheck className="h-3 w-3 text-green-500" /> Copied</>
                  : <><FiCopy className="h-3 w-3" /> Copy</>}
              </button>
              {message.error && (
                <button onClick={onRetry}
                  className="flex items-center gap-1 rounded px-1.5 py-0.5 text-red-500 hover:bg-red-50 focus:outline-none">
                  <FiRotateCcw className="h-3 w-3" /> Retry
                </button>
              )}
            </span>
          )}
        </div>

        {/* Action chips */}
        {!!message.actions?.length && !isUser && (
          <div className="mt-2 flex flex-wrap gap-1.5">
            {message.actions.map(a => (
              <button key={`${id}-${a.path}`} onClick={() => onAction(a.path)}
                className="rounded border border-slate-200 bg-white px-2.5 py-1 text-xs font-medium text-blue-700 transition hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-300">
                {a.label}
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

/* ── Typing indicator ─────────────────────────────────────────── */
function TypingBubble() {
  return (
    <div className="flex gap-2">
      <div className="mt-0.5 shrink-0"><AILogo size={24} /></div>
      <div className="rounded-lg border border-slate-200 bg-white px-3.5 py-2.5">
        <span className="flex gap-1.5" aria-label="AI is typing">
          {[0, 1, 2].map(i => (
            <span
              key={i}
              className="h-2 w-2 rounded-full bg-blue-600"
              style={{ animation: `bounce 0.9s ${i * 0.15}s infinite` }}
            />
          ))}
        </span>
        <style>{`@keyframes bounce{0%,100%{transform:translateY(0);opacity:.4}50%{transform:translateY(-4px);opacity:1}}`}</style>
      </div>
    </div>
  );
}

/* ── Markdown renderer ────────────────────────────────────────── */
function MarkdownRenderer({ text = '', isUser }) {
  const blocks = text.split(/(```[\s\S]*?```)/g);
  return (
    <div className="space-y-1.5">
      {blocks.map((block, i) => {
        if (block.startsWith('```')) {
          const code = block.replace(/^```\w*\n?/, '').replace(/```$/, '').trim();
          return (
            <pre key={i} className="overflow-x-auto rounded border border-slate-200 bg-slate-950 p-3 text-[12px] leading-5 text-slate-100">
              <code>{code}</code>
            </pre>
          );
        }
        return <MarkdownBlock key={i} text={block} isUser={isUser} />;
      })}
    </div>
  );
}

function MarkdownBlock({ text, isUser }) {
  const lines = text.split('\n').filter((l, i, a) =>
    !(l.trim() === '' && (i === 0 || a[i - 1]?.trim() === ''))
  );
  const tableLines = lines.filter(l => l.includes('|'));
  if (tableLines.length >= 2) {
    const rows = tableLines.filter(l => !/^\s*\|?\s*[-:]+/.test(l));
    if (rows.length >= 2) {
      return (
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-xs">
            <tbody>
              {rows.map((row, i) => (
                <tr key={i} className={i % 2 ? 'bg-slate-50' : 'bg-white'}>
                  {row.split('|').filter(c => c.trim()).map((cell, j) => (
                    <td key={j} className="border border-slate-200 px-2.5 py-1.5">
                      {inlineRender(cell.trim(), isUser)}
                    </td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      );
    }
  }
  return (
    <div className="space-y-1">
      {lines.map((line, i) => {
        if (/^#{1,3}\s/.test(line)) {
          const lvl   = line.match(/^#+/)[0].length;
          const txt   = line.replace(/^#+\s/, '');
          const cls   = lvl === 1 ? 'font-bold text-sm' : 'font-semibold text-sm';
          return <p key={i} className={cls}>{inlineRender(txt, isUser)}</p>;
        }
        if (/^\s*[-*]\s+/.test(line)) {
          return (
            <div key={i} className="flex gap-2 pl-1">
              <span className="mt-2 h-1.5 w-1.5 shrink-0 rounded-full bg-slate-400" />
              <span className="text-sm">{inlineRender(line.replace(/^\s*[-*]\s+/, ''), isUser)}</span>
            </div>
          );
        }
        if (/^\d+\.\s/.test(line)) {
          const num = line.match(/^(\d+)\./)[1];
          return (
            <div key={i} className="flex gap-2 pl-1">
              <span className="shrink-0 font-semibold text-slate-500 text-sm">{num}.</span>
              <span className="text-sm">{inlineRender(line.replace(/^\d+\.\s/, ''), isUser)}</span>
            </div>
          );
        }
        if (line.trim() === '') return <div key={i} className="h-1" />;
        return <p key={i} className="text-sm">{inlineRender(line, isUser)}</p>;
      })}
    </div>
  );
}

function inlineRender(text, isUser) {
  return text.split(/(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`|https?:\/\/\S+)/g).map((chunk, i) => {
    if (chunk.startsWith('**') && chunk.endsWith('**'))
      return <strong key={i}>{chunk.slice(2, -2)}</strong>;
    if (chunk.startsWith('*') && chunk.endsWith('*'))
      return <em key={i}>{chunk.slice(1, -1)}</em>;
    if (chunk.startsWith('`') && chunk.endsWith('`'))
      return (
        <code key={i} className={`rounded px-1 py-0.5 font-mono text-[0.85em] ${isUser ? 'bg-blue-600 text-blue-100' : 'bg-slate-100 text-slate-700'}`}>
          {chunk.slice(1, -1)}
        </code>
      );
    if (/^https?:\/\//.test(chunk))
      return <a key={i} href={chunk} target="_blank" rel="noreferrer" className="underline underline-offset-2">{chunk}</a>;
    return <span key={i}>{chunk}</span>;
  });
}

function fmtTime(value) {
  try { return new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }); }
  catch { return ''; }
}
