import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'dev.selonick.owomi',
  appName: 'OWOMI',
  webDir: 'dist/owomi-frontend/browser',
  server: {
    androidScheme: 'https',
  },
  plugins: {
    SplashScreen: {
      launchShowDuration: 2000,
      backgroundColor: '#0C131E',
      androidSplashResourceName: 'splash',
      showSpinner: false,
    },
  },
};

export default config;
