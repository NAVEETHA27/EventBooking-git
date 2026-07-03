/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        primary:   { DEFAULT: '#1565C0', dark: '#0D47A1', light: '#1976D2', 50: '#E3F2FD', 100: '#BBDEFB' },
        accent:    { DEFAULT: '#D32F2F', dark: '#B71C1C' },
        secondary: { DEFAULT: '#D32F2F' },
        dark:      { bg: '#F0F4FF', card: '#ffffff', border: '#BBDEFB', muted: '#EEF2FF' },
      },
      fontFamily: {
        sans:    ['Inter', 'system-ui', 'sans-serif'],
        display: ['Space Grotesk', 'sans-serif'],
      },
      animation: {
        'fade-in':  'fadeIn 0.45s ease-out both',
        'fade-up':  'fadeUp 0.5s  ease-out both',
        'scale-in': 'scaleIn 0.3s ease-out both',
        'float':    'float  6s ease-in-out infinite',
        'shimmer':  'shimmer 2s linear infinite',
      },
      keyframes: {
        fadeIn:  { from: { opacity: 0 }, to: { opacity: 1 } },
        fadeUp:  { from: { opacity: 0, transform: 'translateY(18px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
        scaleIn: { from: { opacity: 0, transform: 'scale(0.93)' }, to: { opacity: 1, transform: 'scale(1)' } },
        float:   { '0%,100%': { transform: 'translateY(0px)' }, '50%': { transform: 'translateY(-14px)' } },
        shimmer: { from: { backgroundPosition: '-200% 0' }, to: { backgroundPosition: '200% 0' } },
      },
      boxShadow: {
        'blue':    '0 4px 20px rgba(21,101,192,0.25)',
        'blue-lg': '0 8px 36px rgba(21,101,192,0.35)',
        'card':    '0 2px 12px rgba(21,101,192,0.08)',
      },
    },
  },
  plugins: [],
};
