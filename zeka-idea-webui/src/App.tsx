import {useEffect, useState} from 'react';
import {Header} from './components/Header';
import {Footer} from './components/Footer';
import {Home} from './pages/Home';
import {FeatureRequests} from './pages/FeatureRequests';
import {PrivacyPolicy} from './pages/PrivacyPolicy';
import {Statistics} from './pages/Statistics';
import {EngineHome} from './pages/plugins/EngineHome';
import {JavadocHome} from './pages/plugins/JavadocHome';
import {ChangelogHome} from './pages/plugins/ChangelogHome';
import {Donate} from './pages/Donate';
import {Changelog} from './pages/Changelog';
import {Login} from './pages/Login';
import {Settings} from './pages/Settings';

function App() {
    // Simple hash-based router
    const normalizeRoute = (hash: string) => {
        const base = hash.split('?')[0];
        return base || '#/';
    };
    const [route, setRoute] = useState(normalizeRoute(window.location.hash));

    useEffect(() => {
        const handleHashChange = () => {
            setRoute(normalizeRoute(window.location.hash));
            // Scroll to top on route change
            window.scrollTo(0, 0);
        };

        window.addEventListener('hashchange', handleHashChange);
        return () => window.removeEventListener('hashchange', handleHashChange);
    }, []);

    let Component;
    let footerVariant: 'default' | 'dark' | 'transparent' | 'transparent-dark' = 'default';

    // Handle routes
    if (route === '#/plugins/engine') {
        Component = EngineHome;
        footerVariant = 'dark';
    } else if (route === '#/plugins/javadoc') {
        Component = JavadocHome;
    } else if (route === '#/plugins/changelog') {
        Component = ChangelogHome;
    } else {
        // Switch for other routes
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
        case '#/changelog':
            Component = Changelog;
            break;
        case '#/login':
            Component = Login;
            footerVariant = 'transparent-dark';
            break;
        case '#/settings':
            Component = Settings;
            footerVariant = 'transparent-dark';
            break;
        case '#/donate':
            Component = Donate;
            footerVariant = 'transparent-dark';
            break;
        case '#/':
        case '':
        default:
            Component = Home;
    }
    }

    return (
        <div className="min-h-screen bg-[#F9FAFB] font-sans selection:bg-indigo-100 selection:text-indigo-900 flex flex-col">
            <Header/>
            <div className="flex-1">
                <Component/>
            </div>
            <Footer variant={footerVariant}/>
        </div>
    );
}

export default App;
