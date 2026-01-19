import {useEffect, useState} from 'react';
import {Header} from './components/Header';
import {Home} from './pages/Home';
import {FeatureRequests} from './pages/FeatureRequests';
import {PrivacyPolicy} from './pages/PrivacyPolicy';
import {Statistics} from './pages/Statistics';

function App() {
    // Simple hash-based router
    const [route, setRoute] = useState(window.location.hash);

    useEffect(() => {
        const handleHashChange = () => {
            setRoute(window.location.hash);
            // Scroll to top on route change
            window.scrollTo(0, 0);
        };

        window.addEventListener('hashchange', handleHashChange);
        return () => window.removeEventListener('hashchange', handleHashChange);
    }, []);

    let Component;
    switch (route) {
        case '#/feedback':
            Component = FeatureRequests;
            break;
        case '#/privacy':
            Component = PrivacyPolicy;
            break;
        case '#/statistics':
            Component = Statistics;
            break;
        case '#/':
        case '':
        default:
            Component = Home;
    }

    return (
        <div className="min-h-screen bg-[#F9FAFB] font-sans selection:bg-indigo-100 selection:text-indigo-900">
            <Header/>
            <Component/>
        </div>
    );
}

export default App;
