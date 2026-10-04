import React from 'react';
import ReactDOM from 'react-dom/client';
import { ThemeProvider } from './theme/ThemeContext';
import { BusinessApp } from './business/BusinessApp';
import { PlatformAdminApp } from './platform-admin/PlatformAdminApp';

const appType = process.env.REACT_APP_APP_TYPE || 'business';

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);

if (appType === 'platform-admin') {
  root.render(
    <React.StrictMode>
      <ThemeProvider>
        <PlatformAdminApp />
      </ThemeProvider>
    </React.StrictMode>
  );
} else {
  root.render(
    <React.StrictMode>
      <ThemeProvider>
        <BusinessApp />
      </ThemeProvider>
    </React.StrictMode>
  );
}
