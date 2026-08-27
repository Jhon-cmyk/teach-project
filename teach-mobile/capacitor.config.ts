import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.ruyi.teach.mobile',
  appName: 'SmartEdu Student',
  webDir: 'dist',
  bundledWebRuntime: false,
  server: {
    androidScheme: 'https',
    cleartext: true
  },
  plugins: {
    LocalNotifications: {
      iconColor: '#1F7A5B'
    }
  }
}

export default config
