/**
 * ScannerRelay — Mobile-friendly QR scanner that relays scanned ticket IDs
 * back to the organizer's desktop AttendanceScanner page via the backend.
 *
 * URL: /scanner-relay?event=<eventId>&session=<sessionId>&token=<jwt>
 *
 * The device visiting this page does NOT need to be logged in as organizer —
 * it uses the token passed in the URL query string that was shared by the organizer.
 */
import { useCallback, useEffect, useRef, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import jsQR from 'jsqr';
import { toast } from 'react-toastify';
import { FiCamera, FiCheckCircle, FiAlertCircle, FiWifi } from 'react-icons/fi';

export default function ScannerRelay() {
  const [searchParams] = useSearchParams();
  const eventId   = searchParams.get('event');
  const sessionId = searchParams.get('session');
  const token     = searchParams.get('token');

  const videoRef    = useRef(null);
  const streamRef   = useRef(null);
  const scanLoopRef = useRef(null);
  const cooldownRef = useRef(false);
  const lastRef     = useRef('');

  const [cameraActive, setCameraActive] = useState(false);
  const [cameraError,  setCameraError]  = useState('');
  const [lastTicket,   setLastTicket]   = useState('');
  const [relayStatus,  setRelayStatus]  = useState('idle'); // idle | sending | ok | error
  const [scanCount,    setScanCount]    = useState(0);

  const deviceName = navigator.userAgent.includes('iPhone') ? "iPhone"
    : navigator.userAgent.includes('Android') ? "Android Device"
    : "Mobile Scanner";

  // Send ticket to relay endpoint
  const relay = useCallback(async (ticketId) => {
    if (cooldownRef.current || ticketId === lastRef.current) return;
    cooldownRef.current = true;
    lastRef.current     = ticketId;
    setLastTicket(ticketId);
    setRelayStatus('sending');

    try {
      const res = await fetch(`/api/attendance/relay/${eventId}/${sessionId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ ticketId, device: deviceName }),
      });
      if (res.ok) {
        setRelayStatus('ok');
        setScanCount(n => n + 1);
        // Flash green
        if (videoRef.current) videoRef.current.style.outline = '4px solid #22c55e';
        setTimeout(() => { if (videoRef.current) videoRef.current.style.outline = 'none'; }, 500);
      } else {
        setRelayStatus('error');
      }
    } catch {
      setRelayStatus('error');
    } finally {
      setTimeout(() => { cooldownRef.current = false; }, 1500);
    }
  }, [eventId, sessionId, token, deviceName]);

  const stopCamera = useCallback(() => {
    if (scanLoopRef.current) cancelAnimationFrame(scanLoopRef.current);
    streamRef.current?.getTracks?.().forEach(t => t.stop());
    scanLoopRef.current = null;
    streamRef.current   = null;
    setCameraActive(false);
  }, []);

  const startCamera = async () => {
    setCameraError('');
    if (!navigator.mediaDevices?.getUserMedia) {
      setCameraError('Camera not supported on this device.');
      return;
    }
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } });
      streamRef.current = stream;
      videoRef.current.srcObject = stream;
      await videoRef.current.play();
      setCameraActive(true);
      lastRef.current = '';

      const canvas = document.createElement('canvas');
      const ctx    = canvas.getContext('2d');

      const tick = () => {
        const video = videoRef.current;
        if (!video || video.readyState < 2) { scanLoopRef.current = requestAnimationFrame(tick); return; }
        canvas.width  = video.videoWidth;
        canvas.height = video.videoHeight;
        ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
        const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
        const code = jsQR(imageData.data, imageData.width, imageData.height);
        if (code?.data) relay(code.data);
        scanLoopRef.current = requestAnimationFrame(tick);
      };
      scanLoopRef.current = requestAnimationFrame(tick);
    } catch (err) {
      setCameraError(err?.message || 'Could not open camera.');
    }
  };

  useEffect(() => () => stopCamera(), [stopCamera]);

  if (!eventId || !sessionId || !token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-900 text-white p-8 text-center">
        <div>
          <FiAlertCircle className="w-12 h-12 mx-auto mb-4 text-red-400" />
          <h1 className="text-xl font-bold mb-2">Invalid Scanner Link</h1>
          <p className="text-slate-400 text-sm">This link is missing required parameters. Ask the organizer to share a fresh link.</p>
        </div>
      </div>
    );
  }

  const statusIcon = relayStatus === 'ok' ? '✅'
    : relayStatus === 'error' ? '❌'
    : relayStatus === 'sending' ? '⏳'
    : '📷';

  return (
    <div className="min-h-screen bg-slate-900 text-white flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 bg-slate-800 border-b border-slate-700">
        <div>
          <p className="text-xs text-slate-400 uppercase tracking-widest font-bold">External Scanner</p>
          <p className="text-sm font-bold">Event #{eventId}</p>
        </div>
        <div className="flex items-center gap-2">
          <span className={`w-2 h-2 rounded-full ${cameraActive ? 'bg-green-400 animate-pulse' : 'bg-slate-500'}`} />
          <FiWifi className={cameraActive ? 'text-green-400' : 'text-slate-500'} />
        </div>
      </div>

      {/* Camera */}
      <div className="flex-1 flex flex-col p-4 gap-4">
        <div className="relative rounded-2xl overflow-hidden bg-black aspect-[4/3] flex items-center justify-center">
          <video ref={videoRef} className="w-full h-full object-cover" muted playsInline />
          {!cameraActive && (
            <div className="absolute inset-0 flex flex-col items-center justify-center gap-3 text-slate-400">
              <FiCamera className="w-16 h-16" />
              <p className="text-sm font-semibold">Camera off</p>
            </div>
          )}
          {/* Scan crosshair */}
          {cameraActive && (
            <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
              <div className="w-48 h-48 border-2 border-white/60 rounded-xl" style={{
                boxShadow: '0 0 0 9999px rgba(0,0,0,0.4)',
              }} />
            </div>
          )}
          {/* Status overlay */}
          {relayStatus !== 'idle' && (
            <div className={`absolute bottom-3 left-0 right-0 flex justify-center`}>
              <span className={`px-3 py-1.5 rounded-full text-xs font-bold ${
                relayStatus === 'ok' ? 'bg-green-500 text-white'
                : relayStatus === 'error' ? 'bg-red-500 text-white'
                : 'bg-amber-500 text-white'
              }`}>
                {relayStatus === 'ok' ? '✓ Ticket sent to organizer'
                : relayStatus === 'error' ? '✗ Failed to relay'
                : '⟳ Sending...'}
              </span>
            </div>
          )}
        </div>

        {cameraError && (
          <p className="text-sm text-red-400 bg-red-950 border border-red-800 rounded-xl px-4 py-3">{cameraError}</p>
        )}

        <button
          onClick={cameraActive ? stopCamera : startCamera}
          className={`w-full py-4 rounded-2xl font-bold text-base transition-all ${
            cameraActive ? 'bg-red-600 hover:bg-red-700' : 'bg-blue-600 hover:bg-blue-700'
          }`}
        >
          <span className="flex items-center justify-center gap-2">
            <FiCamera className="w-5 h-5" />
            {cameraActive ? 'Stop Camera' : 'Open Camera to Scan'}
          </span>
        </button>

        {/* Stats */}
        <div className="grid grid-cols-2 gap-3">
          <div className="bg-slate-800 rounded-2xl p-4 text-center">
            <p className="text-3xl font-extrabold text-green-400">{scanCount}</p>
            <p className="text-xs text-slate-400 mt-1 uppercase tracking-wide font-bold">Tickets Relayed</p>
          </div>
          <div className="bg-slate-800 rounded-2xl p-4 text-center">
            <p className="text-sm font-mono text-slate-300 truncate mt-1">{lastTicket || '—'}</p>
            <p className="text-xs text-slate-400 mt-1 uppercase tracking-wide font-bold">Last Ticket</p>
          </div>
        </div>

        <div className="bg-slate-800 rounded-2xl p-4 text-xs text-slate-400 space-y-1">
          <p><span className="text-slate-300 font-bold">Session:</span> {sessionId}</p>
          <p><span className="text-slate-300 font-bold">Device:</span> {deviceName}</p>
          <p className="text-slate-500">Point camera at a ticket QR code — it will be marked automatically on the organizer's screen.</p>
        </div>
      </div>
    </div>
  );
}
