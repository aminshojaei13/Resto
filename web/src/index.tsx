import React from 'react';
import ReactDOM from 'react-dom/client';
import { BusinessApp } from './business/BusinessApp';
import { PlatformAdminApp } from './platform-admin/PlatformAdminApp';

const appType = process.env.REACT_APP_APP_TYPE || 'business';

const root = ReactDOM.createRoot(
  document.getElementById('root') as HTMLElement
);

if (appType === 'platform-admin') {
  root.render(
    <React.StrictMode>
      <PlatformAdminApp />
    </React.StrictMode>
  );
} else {
  root.render(
    <React.StrictMode>
      <BusinessApp />
    </React.StrictMode>
  );
}
