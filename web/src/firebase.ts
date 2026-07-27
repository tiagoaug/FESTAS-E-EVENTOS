import { initializeApp } from 'firebase/app'
import { getAuth, GoogleAuthProvider } from 'firebase/auth'
import { getFirestore } from 'firebase/firestore'

const firebaseConfig = {
  apiKey: 'AIzaSyDNIGF8TpykjXg4y8CFm7jaQf1aVeLLYV0',
  authDomain: 'festas-e-eventos-app.firebaseapp.com',
  projectId: 'festas-e-eventos-app',
  storageBucket: 'festas-e-eventos-app.firebasestorage.app',
  messagingSenderId: '902643175428',
  appId: '1:902643175428:web:2b1b306546af959fdee551',
}

export const app = initializeApp(firebaseConfig)
export const auth = getAuth(app)
export const db = getFirestore(app)
export const googleProvider = new GoogleAuthProvider()
