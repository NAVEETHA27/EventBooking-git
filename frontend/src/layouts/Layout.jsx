import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';
import Footer from './Footer';
import ChatbotWidget from '../components/common/ChatbotWidget';

export default function Layout() {
  return (
    <div className="flex flex-col min-h-screen" style={{ background: '#F0F4FF' }}>
      <Navbar />
      <main className="flex-1 pt-16">
        <Outlet />
      </main>
      <ChatbotWidget />
      <Footer />
    </div>
  );
}
