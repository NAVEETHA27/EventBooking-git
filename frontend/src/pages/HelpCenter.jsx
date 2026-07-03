import { useEffect, useMemo, useState } from 'react';
import { FiHelpCircle, FiMail, FiSearch, FiVideo } from 'react-icons/fi';
import { helpAPI } from '../services/api';

export default function HelpCenter() {
  const [search, setSearch] = useState('');
  const [category, setCategory] = useState('');
  const [data, setData] = useState({ faqs: [], videos: [], contactSupport: {} });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    helpAPI.get({ search, category }).then((r) => setData(r.data?.data ?? data)).finally(() => setLoading(false));
  }, [search, category]);

  const categories = useMemo(() => [...new Set((data.faqs || []).map((f) => f.category).filter(Boolean))], [data.faqs]);

  return (
    <div className="mx-auto max-w-7xl px-4 py-10">
      <div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
        <div>
          <h1 className="text-3xl font-extrabold text-slate-950">Help Center</h1>
          <p className="mt-2 text-sm text-slate-500">Find answers, tutorials, and support contact details.</p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <label className="relative">
            <FiSearch className="absolute left-3 top-3 text-slate-400" />
            <input className="input-field pl-9" placeholder="Search FAQs" value={search} onChange={(e) => setSearch(e.target.value)} />
          </label>
          <select className="input-field" value={category} onChange={(e) => setCategory(e.target.value)}>
            <option value="">All categories</option>
            {categories.map((c) => <option key={c}>{c}</option>)}
          </select>
        </div>
      </div>

      {loading ? <div className="text-slate-500">Loading help content...</div> : (
        <div className="grid gap-6 lg:grid-cols-[1fr_380px]">
          <section>
            <div className="mb-3 flex items-center gap-2 text-lg font-bold text-slate-900"><FiHelpCircle /> FAQs</div>
            <div className="space-y-3">
              {(data.faqs || []).length === 0 && <div className="rounded-lg border border-dashed p-6 text-slate-500">No FAQs found.</div>}
              {(data.faqs || []).map((faq) => (
                <details key={faq.id} className="rounded-lg border border-slate-200 bg-white p-4">
                  <summary className="cursor-pointer font-semibold text-slate-900">{faq.question}</summary>
                  <p className="mt-3 text-sm leading-6 text-slate-600">{faq.answer}</p>
                </details>
              ))}
            </div>
          </section>

          <aside className="space-y-5">
            <div>
              <div className="mb-3 flex items-center gap-2 text-lg font-bold text-slate-900"><FiVideo /> Tutorials</div>
              <div className="space-y-4">
                {(data.videos || []).map((video) => (
                  <article key={video.id} className="rounded-lg border border-slate-200 bg-white p-3">
                    <video className="aspect-video w-full rounded-md bg-slate-950" src={video.videoUrl} controls preload="metadata" />
                    <h3 className="mt-3 font-bold text-slate-900">{video.title}</h3>
                    <p className="mt-1 text-sm text-slate-500">{video.description}</p>
                  </article>
                ))}
              </div>
            </div>
            <div className="rounded-lg border border-teal-100 bg-teal-50 p-4">
              <div className="flex items-center gap-2 font-bold text-teal-900"><FiMail /> Contact Support</div>
              <p className="mt-2 text-sm text-teal-800">{data.contactSupport?.email || 'support@eventbooking.local'}</p>
              <p className="text-xs text-teal-700">{data.contactSupport?.hours}</p>
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}
